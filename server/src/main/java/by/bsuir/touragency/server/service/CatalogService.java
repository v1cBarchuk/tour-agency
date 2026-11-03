package by.bsuir.touragency.server.service;

import by.bsuir.touragency.common.exception.AppException;
import by.bsuir.touragency.common.exception.BusinessRuleException;
import by.bsuir.touragency.common.exception.NotFoundException;
import by.bsuir.touragency.common.exception.ValidationException;
import by.bsuir.touragency.common.model.City;
import by.bsuir.touragency.common.model.Country;
import by.bsuir.touragency.common.model.Discount;
import by.bsuir.touragency.common.model.Hotel;
import by.bsuir.touragency.common.model.Lang;
import by.bsuir.touragency.common.model.ReferenceData;
import by.bsuir.touragency.common.model.Tour;
import by.bsuir.touragency.common.model.TourFilter;
import by.bsuir.touragency.common.model.User;
import by.bsuir.touragency.server.dao.AuditDao;
import by.bsuir.touragency.server.dao.CatalogDao;
import by.bsuir.touragency.server.dao.DiscountDao;
import by.bsuir.touragency.server.dao.TourDao;
import by.bsuir.touragency.server.db.DataAccessException;
import by.bsuir.touragency.server.util.Validator;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.function.Supplier;

/**
 * Бизнес-логика каталога: просмотр и управление турами, отелями,
 * географией и скидками.
 */
public class CatalogService {

    private final CatalogDao catalogDao;
    private final TourDao tourDao;
    private final DiscountDao discountDao;
    private final AuditDao auditDao;

    public CatalogService(CatalogDao catalogDao, TourDao tourDao, DiscountDao discountDao, AuditDao auditDao) {
        this.catalogDao = catalogDao;
        this.tourDao = tourDao;
        this.discountDao = discountDao;
        this.auditDao = auditDao;
    }

    // ------------------------------------------------------------ чтение

    public ReferenceData getReferenceData(Lang lang) {
        return new ReferenceData(catalogDao.findCountries(lang), catalogDao.findCities(lang),
                catalogDao.findVacationTypes(lang), catalogDao.findMealTypes(lang), catalogDao.findAmenities(lang));
    }

    public List<Country> getCountries(Lang lang) {
        return catalogDao.findCountries(lang);
    }

    public List<Hotel> getHotels(Integer countryId, Integer cityId, Lang lang) {
        return catalogDao.findHotels(countryId, cityId, lang);
    }

    public Hotel getHotel(int id, Lang lang) throws NotFoundException {
        return catalogDao.findHotel(id, lang).orElseThrow(() -> new NotFoundException("catalog.notFound"));
    }

    public List<Tour> getTours(TourFilter filter, Lang lang) {
        return tourDao.find(filter == null ? new TourFilter() : filter, lang);
    }

    public Tour getTour(int id, Lang lang) throws NotFoundException {
        return tourDao.findById(id, lang).orElseThrow(() -> new NotFoundException("catalog.notFound"));
    }

    public List<Discount> getDiscounts() {
        return discountDao.findAll();
    }

    // ------------------------------------------------------------ география

    public int saveCountry(Country c, User actor) throws AppException {
        c.setNameRu(Validator.requireNotBlank(c.getNameRu(), "field.nameRu"));
        c.setNameEn(Validator.requireNotBlank(c.getNameEn(), "field.nameEn"));
        c.setDescriptionRu(Validator.optional(c.getDescriptionRu()));
        c.setDescriptionEn(Validator.optional(c.getDescriptionEn()));
        boolean isNew = c.getId() == null;
        int id = saveUnique(() -> catalogDao.saveCountry(c));
        auditDao.log(actor.getId(), isNew ? "CREATE" : "UPDATE", "COUNTRY", id, c.getNameRu());
        return id;
    }

    public int saveCity(City c, User actor) throws AppException {
        Validator.requireNotNull(c.getCountryId(), "field.country");
        c.setNameRu(Validator.requireNotBlank(c.getNameRu(), "field.nameRu"));
        c.setNameEn(Validator.requireNotBlank(c.getNameEn(), "field.nameEn"));
        boolean isNew = c.getId() == null;
        int id = saveUnique(() -> catalogDao.saveCity(c));
        auditDao.log(actor.getId(), isNew ? "CREATE" : "UPDATE", "CITY", id, c.getNameRu());
        return id;
    }

    public int saveHotel(Hotel h, User actor) throws AppException {
        Validator.requireNotNull(h.getCityId(), "field.city");
        h.setName(Validator.requireNotBlank(h.getName(), "field.name"));
        Validator.range(h.getStars(), 1, 5, "field.stars");
        h.setAddress(Validator.optional(h.getAddress()));
        h.setDescriptionRu(Validator.optional(h.getDescriptionRu()));
        h.setDescriptionEn(Validator.optional(h.getDescriptionEn()));
        boolean isNew = h.getId() == null;
        int id = saveUnique(() -> catalogDao.saveHotel(h));
        auditDao.log(actor.getId(), isNew ? "CREATE" : "UPDATE", "HOTEL", id, h.getName() + " " + h.getStars() + "*");
        return id;
    }

    /** Уникальные ключи БД превращаются в понятную ошибку «уже существует». */
    private static int saveUnique(Supplier<Integer> action) throws ValidationException {
        try {
            return action.get();
        } catch (DataAccessException e) {
            if (e.isDuplicateKey()) {
                throw new ValidationException("validation.duplicate");
            }
            throw e;
        }
    }

    // ------------------------------------------------------------ туры

    /**
     * Создание или изменение тура. Возвращает сохранённый тур
     * (нужен для уведомлений клиентов о новом туре).
     */
    public Tour saveTour(Tour t, User actor, Lang lang) throws AppException {
        Validator.requireNotNull(t.getHotelId(), "field.hotel");
        Validator.requireNotNull(t.getVacationTypeId(), "field.vacationType");
        Validator.requireNotNull(t.getMealTypeId(), "field.mealType");
        t.setTitleRu(Validator.requireNotBlank(t.getTitleRu(), "field.title"));
        t.setTitleEn(t.getTitleEn() == null || t.getTitleEn().isBlank() ? t.getTitleRu() : t.getTitleEn().trim());
        t.setDescriptionRu(Validator.optional(t.getDescriptionRu()));
        t.setDescriptionEn(Validator.optional(t.getDescriptionEn()));
        Validator.requireNotNull(t.getStartDate(), "field.startDate");
        Validator.requireNotNull(t.getEndDate(), "field.endDate");
        Validator.datesOrder(t.getStartDate(), t.getEndDate());
        Validator.positive(t.getPricePerPerson(), "field.price");
        Validator.range(t.getTotalSeats(), 1, 500, "field.seats");
        catalogDao.findHotel(t.getHotelId(), lang).orElseThrow(() -> new NotFoundException("catalog.notFound"));

        int id;
        if (t.getId() == null) {
            if (t.getStartDate().isBefore(LocalDate.now())) {
                throw new ValidationException("validation.pastDate");
            }
            id = tourDao.create(t, actor.getId());
            auditDao.log(actor.getId(), "CREATE", "TOUR", id, t.getTitleRu());
        } else {
            Tour old = getTour(t.getId(), lang);
            int booked = tourDao.countBookedSeats(t.getId());
            if (t.getTotalSeats() < booked) {
                throw new BusinessRuleException("catalog.seatsBelowBooked", booked);
            }
            tourDao.update(t);
            id = t.getId();
            auditDao.log(actor.getId(), "UPDATE", "TOUR", id, describeChanges(old, t));
        }
        return getTour(id, lang);
    }

    private static String describeChanges(Tour old, Tour t) {
        StringBuilder sb = new StringBuilder();
        change(sb, "price_per_person", old.getPricePerPerson(), t.getPricePerPerson());
        change(sb, "total_seats", old.getTotalSeats(), t.getTotalSeats());
        change(sb, "start_date", old.getStartDate(), t.getStartDate());
        change(sb, "end_date", old.getEndDate(), t.getEndDate());
        change(sb, "hotel_id", old.getHotelId(), t.getHotelId());
        change(sb, "title_ru", old.getTitleRu(), t.getTitleRu());
        change(sb, "is_active", old.isActive(), t.isActive());
        return sb.isEmpty() ? "без изменений ключевых полей" : sb.toString();
    }

    private static void change(StringBuilder sb, String field, Object before, Object after) {
        boolean same = before instanceof BigDecimal b && after instanceof BigDecimal a
                ? b.compareTo(a) == 0 : java.util.Objects.equals(before, after);
        if (!same) {
            if (!sb.isEmpty()) sb.append("; ");
            sb.append(field).append(": ").append(before).append(" → ").append(after);
        }
    }

    public void setTourActive(int tourId, boolean active, User actor, Lang lang) throws AppException {
        getTour(tourId, lang);
        tourDao.setActive(tourId, active);
        auditDao.log(actor.getId(), "UPDATE", "TOUR", tourId, "is_active → " + active);
    }

    /** Удаление с проверкой зависимостей: тур с бронированиями можно только снять с продажи. */
    public void deleteTour(int tourId, User actor, Lang lang) throws AppException {
        Tour tour = getTour(tourId, lang);
        int bookings = tourDao.countBookings(tourId);
        if (bookings > 0) {
            throw new BusinessRuleException("catalog.tourHasBookings", bookings);
        }
        tourDao.delete(tourId);
        auditDao.log(actor.getId(), "DELETE", "TOUR", tourId, tour.getTitleRu());
    }

    // ------------------------------------------------------------ скидки

    public int saveDiscount(Discount d, User actor) throws AppException {
        d.setNameRu(Validator.requireNotBlank(d.getNameRu(), "field.nameRu"));
        d.setNameEn(d.getNameEn() == null || d.getNameEn().isBlank() ? d.getNameRu() : d.getNameEn().trim());
        Validator.requireNotNull(d.getType(), "field.discountType");
        Validator.requireNotNull(d.getPercent(), "field.percent");
        if (d.getPercent().compareTo(BigDecimal.ONE) < 0 || d.getPercent().compareTo(BigDecimal.valueOf(90)) > 0) {
            throw new ValidationException("validation.range", "field.percent", 1, 90);
        }
        // параметры, не относящиеся к типу скидки, сбрасываются
        switch (d.getType()) {
            case SEASONAL -> {
                if (d.getValidFrom() == null || d.getValidTo() == null) {
                    throw new ValidationException("discount.periodRequired");
                }
                Validator.datesOrder(d.getValidFrom(), d.getValidTo());
                d.setMinDaysBefore(null);
                d.setMinCompletedTrips(null);
            }
            case EARLY_BOOKING -> {
                if (d.getMinDaysBefore() == null || d.getMinDaysBefore() < 1) {
                    throw new ValidationException("discount.daysRequired");
                }
                d.setValidFrom(null);
                d.setValidTo(null);
                d.setMinCompletedTrips(null);
            }
            case LOYALTY -> {
                if (d.getMinCompletedTrips() == null || d.getMinCompletedTrips() < 1) {
                    throw new ValidationException("discount.tripsRequired");
                }
                d.setValidFrom(null);
                d.setValidTo(null);
                d.setMinDaysBefore(null);
            }
        }
        boolean isNew = d.getId() == null;
        int id = discountDao.save(d);
        auditDao.log(actor.getId(), isNew ? "CREATE" : "UPDATE", "DISCOUNT", id,
                d.getNameRu() + " " + d.getPercent() + "%");
        return id;
    }

    public void setDiscountActive(int id, boolean active, User actor) throws AppException {
        discountDao.findById(id).orElseThrow(() -> new NotFoundException("catalog.notFound"));
        discountDao.setActive(id, active);
        auditDao.log(actor.getId(), "UPDATE", "DISCOUNT", id, "is_active → " + active);
    }
}
