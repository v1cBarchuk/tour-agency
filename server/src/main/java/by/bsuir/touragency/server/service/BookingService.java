package by.bsuir.touragency.server.service;

import by.bsuir.touragency.common.exception.AppException;
import by.bsuir.touragency.common.exception.AuthException;
import by.bsuir.touragency.common.exception.BusinessRuleException;
import by.bsuir.touragency.common.exception.NotFoundException;
import by.bsuir.touragency.common.exception.ValidationException;
import by.bsuir.touragency.common.model.Booking;
import by.bsuir.touragency.common.model.BookingQuote;
import by.bsuir.touragency.common.model.BookingStatus;
import by.bsuir.touragency.common.model.CancelQuote;
import by.bsuir.touragency.common.model.Lang;
import by.bsuir.touragency.common.model.Review;
import by.bsuir.touragency.common.model.Role;
import by.bsuir.touragency.common.model.Tour;
import by.bsuir.touragency.common.model.Tourist;
import by.bsuir.touragency.common.model.User;
import by.bsuir.touragency.server.dao.AuditDao;
import by.bsuir.touragency.server.dao.BookingDao;
import by.bsuir.touragency.server.dao.BookingFilter;
import by.bsuir.touragency.server.dao.DiscountDao;
import by.bsuir.touragency.server.dao.ReviewDao;
import by.bsuir.touragency.server.dao.SeatsUnavailableException;
import by.bsuir.touragency.server.dao.SettingsDao;
import by.bsuir.touragency.server.dao.TourDao;
import by.bsuir.touragency.server.db.DataAccessException;
import by.bsuir.touragency.server.service.discount.DiscountCalculator;
import by.bsuir.touragency.server.service.discount.DiscountContext;
import by.bsuir.touragency.server.service.event.EventPublisher;
import by.bsuir.touragency.server.util.Validator;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.Period;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Бронирование: расчёт цены со скидкой, оформление с проверкой мест,
 * смена статусов, оплата, отмена со штрафом и отзывы.
 */
public class BookingService {

    private static final int MAX_PERSONS = 9;
    private static final Set<String> PAYMENT_METHODS = Set.of("CARD", "CASH", "TRANSFER");

    private final BookingDao bookingDao;
    private final TourDao tourDao;
    private final DiscountDao discountDao;
    private final ReviewDao reviewDao;
    private final SettingsDao settings;
    private final AuditDao auditDao;
    private final EventPublisher events;
    private final DiscountCalculator calculator = new DiscountCalculator();

    public BookingService(BookingDao bookingDao, TourDao tourDao, DiscountDao discountDao, ReviewDao reviewDao,
                          SettingsDao settings, AuditDao auditDao, EventPublisher events) {
        this.bookingDao = bookingDao;
        this.tourDao = tourDao;
        this.discountDao = discountDao;
        this.reviewDao = reviewDao;
        this.settings = settings;
        this.auditDao = auditDao;
        this.events = events;
    }

    // ------------------------------------------------------------ расчёт цены

    public BookingQuote quote(int tourId, int persons, User client, Lang lang) throws AppException {
        Tour tour = bookableTour(tourId, lang);
        Validator.range(persons, 1, MAX_PERSONS, "field.persons");
        BigDecimal base = tour.getPricePerPerson().multiply(BigDecimal.valueOf(persons));
        DiscountContext ctx = new DiscountContext(tour.getStartDate(), LocalDate.now(),
                bookingDao.countCompletedTrips(client.getId()));
        Optional<DiscountCalculator.Applied> applied = calculator.best(discountDao.findActive(), ctx);
        if (applied.isEmpty()) {
            return new BookingQuote(persons, tour.getPricePerPerson(), base, null, null, null, BigDecimal.ZERO,
                    base, tour.getFreeSeats(), "discount.why.NONE", new String[0]);
        }
        var a = applied.get();
        BigDecimal amount = base.multiply(a.discount().getPercent())
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        return new BookingQuote(persons, tour.getPricePerPerson(), base, a.discount().getId(),
                a.discount().getName(lang), a.discount().getPercent(), amount, base.subtract(amount),
                tour.getFreeSeats(), a.strategy().explanationKey(), a.explanationArgs());
    }

    private Tour bookableTour(int tourId, Lang lang) throws AppException {
        Tour tour = tourDao.findById(tourId, lang).orElseThrow(() -> new NotFoundException("catalog.notFound"));
        if (!tour.isActive() || !tour.getStartDate().isAfter(LocalDate.now())) {
            throw new BusinessRuleException("booking.tourUnavailable");
        }
        return tour;
    }

    // ------------------------------------------------------------ оформление

    public Booking create(User client, int tourId, List<Tourist> tourists, String phone, String email, Lang lang)
            throws AppException {
        if (client.getRole() != Role.CLIENT) {
            throw new AuthException("error.forbidden");
        }
        if (tourists == null || tourists.isEmpty()) {
            throw new ValidationException("booking.touristsRequired");
        }
        int persons = tourists.size();
        validateTourists(tourists);
        String contactPhone = Validator.requireNotBlank(Validator.phone(phone), "field.phone");
        String contactEmail = Validator.email(email);

        // цена и скидка всегда пересчитываются на сервере
        BookingQuote q = quote(tourId, persons, client, lang);
        if (q.freeSeats() < persons) {
            throw new BusinessRuleException("booking.noSeats", q.freeSeats());
        }
        Booking draft = new Booking();
        draft.setUserId(client.getId());
        draft.setTourId(tourId);
        draft.setPersonsCount(persons);
        draft.setBasePrice(q.basePrice());
        draft.setDiscountAmount(q.discountAmount());
        draft.setTotalPrice(q.total());
        draft.setContactPhone(contactPhone);
        draft.setContactEmail(contactEmail);

        int id;
        try {
            id = bookingDao.createLocked(draft, q.discountId(), tourists, client.getId());
        } catch (SeatsUnavailableException e) {
            // место успел занять другой клиент — проверка под блокировкой
            throw new BusinessRuleException("booking.noSeats", Math.max(0, e.getFreeSeats()));
        }
        auditDao.log(client.getId(), "CREATE", "BOOKING", id,
                "Тур " + tourId + ", туристов: " + persons + ", сумма: " + q.total());
        events.bookingStatusChanged(id, client.getId(), BookingStatus.NEW, client.getId());
        return get(id, client, lang);
    }

    private static void validateTourists(List<Tourist> tourists) throws ValidationException {
        if (tourists.size() > MAX_PERSONS) {
            throw new ValidationException("validation.range", "field.persons", 1, MAX_PERSONS);
        }
        boolean adult = false;
        for (Tourist t : tourists) {
            Validator.requireNotBlank(t.fullName(), "field.touristName");
            Validator.requireNotNull(t.birthDate(), "field.birthDate");
            if (t.birthDate().isAfter(LocalDate.now())) {
                throw new ValidationException("booking.birthFuture");
            }
            if (Period.between(t.birthDate(), LocalDate.now()).getYears() >= 18) {
                adult = true;
            }
        }
        if (!adult) {
            throw new ValidationException("booking.adultRequired");
        }
    }

    // ------------------------------------------------------------ просмотр

    public List<Booking> myBookings(User client, Lang lang) {
        return bookingDao.find(new BookingFilter(client.getId(), null, null), lang);
    }

    public List<Booking> allBookings(BookingStatus status, String search, Lang lang) {
        return bookingDao.find(new BookingFilter(null, status, search), lang);
    }

    /** Клиент видит только свои бронирования, сотрудники — все. */
    public Booking get(int bookingId, User actor, Lang lang) throws AppException {
        Booking b = bookingDao.findById(bookingId, lang).orElseThrow(() -> new NotFoundException("catalog.notFound"));
        if (actor.getRole() == Role.CLIENT && !b.getUserId().equals(actor.getId())) {
            throw new AuthException("error.forbidden");
        }
        return b;
    }

    // ------------------------------------------------------------ статусы

    /** Смена статуса менеджером в рамках допустимых переходов. */
    public Booking changeStatus(int bookingId, BookingStatus target, User manager, Lang lang) throws AppException {
        Booking b = get(bookingId, manager, lang);
        if (target == BookingStatus.CANCELLED) {
            return cancel(bookingId, manager, lang);
        }
        if (!b.getStatus().canMoveTo(target)) {
            throw new BusinessRuleException("booking.badTransition");
        }
        if (target == BookingStatus.COMPLETED && b.getEndDate().isAfter(LocalDate.now())) {
            throw new BusinessRuleException("booking.notFinished");
        }
        if (target == BookingStatus.PAID) {
            bookingDao.pay(bookingId, b.getTotalPrice().subtract(b.getPaidAmount()).max(BigDecimal.ZERO)
                    .max(new BigDecimal("0.01")), "TRANSFER", manager.getId());
        } else {
            bookingDao.changeStatus(bookingId, target, manager.getId(), null);
        }
        auditDao.log(manager.getId(), "UPDATE", "BOOKING", bookingId, "status: " + b.getStatus() + " → " + target);
        events.bookingStatusChanged(bookingId, b.getUserId(), target, manager.getId());
        return get(bookingId, manager, lang);
    }

    /** Оплата клиентом (демонстрационная — без платёжной системы). */
    public Booking pay(int bookingId, String method, User actor, Lang lang) throws AppException {
        Booking b = get(bookingId, actor, lang);
        if (b.getStatus() != BookingStatus.AWAITING_PAYMENT) {
            throw new BusinessRuleException("booking.notAwaitingPayment");
        }
        String m = method == null ? "CARD" : method.toUpperCase();
        if (!PAYMENT_METHODS.contains(m)) {
            throw new ValidationException("booking.badMethod");
        }
        bookingDao.pay(bookingId, b.getTotalPrice(), m, actor.getId());
        auditDao.log(actor.getId(), "PAY", "BOOKING", bookingId, b.getTotalPrice() + " BYN, " + m);
        events.bookingStatusChanged(bookingId, b.getUserId(), BookingStatus.PAID, actor.getId());
        return get(bookingId, actor, lang);
    }

    // ------------------------------------------------------------ отмена

    /**
     * Штраф по шкале из настроек («не менее N дней : процент»): чем ближе начало тура,
     * тем выше процент. Штраф удерживается из оплаченной суммы; без оплаты — 0.
     */
    public CancelQuote quoteCancel(int bookingId, User actor, Lang lang) throws AppException {
        Booking b = get(bookingId, actor, lang);
        if (!b.canCancel()) {
            throw new BusinessRuleException("booking.cannotCancel");
        }
        long days = Math.max(0, ChronoUnit.DAYS.between(LocalDate.now(), b.getStartDate()));
        int percent = penaltyPercent(days);
        BigDecimal paid = b.getPaidAmount();
        BigDecimal penalty = paid.signum() > 0
                ? b.getTotalPrice().multiply(BigDecimal.valueOf(percent)).divide(BigDecimal.valueOf(100), 2,
                RoundingMode.HALF_UP).min(paid)
                : BigDecimal.ZERO;
        BigDecimal refund = paid.subtract(penalty).max(BigDecimal.ZERO);
        return new CancelQuote(b.getTotalPrice(), paid, days, percent, penalty, refund);
    }

    int penaltyPercent(long daysBefore) {
        String scale = settings.get("penalty.scale", "31:5;15:15;7:30;3:50;0:80");
        List<int[]> steps = new ArrayList<>();
        for (String part : scale.split(";")) {
            String[] kv = part.trim().split(":");
            steps.add(new int[]{Integer.parseInt(kv[0].trim()), Integer.parseInt(kv[1].trim())});
        }
        steps.sort((a, b) -> Integer.compare(b[0], a[0]));
        for (int[] step : steps) {
            if (daysBefore >= step[0]) return step[1];
        }
        return steps.isEmpty() ? 0 : steps.get(steps.size() - 1)[1];
    }

    public Booking cancel(int bookingId, User actor, Lang lang) throws AppException {
        Booking b = get(bookingId, actor, lang);
        CancelQuote q = quoteCancel(bookingId, actor, lang);
        String who = actor.getRole() == Role.CLIENT ? "клиентом" : "менеджером";
        bookingDao.cancel(bookingId, q.penalty(), q.refund(), actor.getId(),
                "Отмена " + who + " за " + q.daysBefore() + " дн. до начала тура, штраф " + q.percent() + "%");
        auditDao.log(actor.getId(), "CANCEL", "BOOKING", bookingId,
                "штраф " + q.penalty() + ", возврат " + q.refund());
        events.bookingStatusChanged(bookingId, b.getUserId(), BookingStatus.CANCELLED, actor.getId());
        return get(bookingId, actor, lang);
    }

    // ------------------------------------------------------------ отзывы

    public void addReview(int bookingId, int rating, String comment, User client, Lang lang) throws AppException {
        Booking b = get(bookingId, client, lang);
        if (!b.getUserId().equals(client.getId())) {
            throw new AuthException("error.forbidden");
        }
        if (b.getStatus() != BookingStatus.COMPLETED) {
            throw new BusinessRuleException("review.notCompleted");
        }
        Validator.range(rating, 1, 5, "field.rating");
        String text = Validator.optional(comment);
        if (text != null && text.length() > 1000) {
            throw new ValidationException("validation.tooLong", "field.comment", 1000);
        }
        try {
            int id = reviewDao.add(bookingId, rating, text);
            auditDao.log(client.getId(), "CREATE", "REVIEW", id, "Оценка " + rating + " за бронь " + bookingId);
        } catch (DataAccessException e) {
            if (e.isDuplicateKey()) throw new BusinessRuleException("review.exists");
            throw e;
        }
    }

    public List<Review> hotelReviews(int hotelId) {
        return reviewDao.findByHotel(hotelId);
    }
}
