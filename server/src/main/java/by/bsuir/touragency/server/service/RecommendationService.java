package by.bsuir.touragency.server.service;

import by.bsuir.touragency.common.exception.AppException;
import by.bsuir.touragency.common.exception.NotFoundException;
import by.bsuir.touragency.common.exception.ValidationException;
import by.bsuir.touragency.common.model.DictionaryItem;
import by.bsuir.touragency.common.model.Lang;
import by.bsuir.touragency.common.model.MatchResult;
import by.bsuir.touragency.common.model.Preferences;
import by.bsuir.touragency.common.model.Recommendations;
import by.bsuir.touragency.common.model.Tour;
import by.bsuir.touragency.common.model.TourFilter;
import by.bsuir.touragency.common.model.User;
import by.bsuir.touragency.server.dao.AuditDao;
import by.bsuir.touragency.server.dao.BookingDao;
import by.bsuir.touragency.server.dao.CatalogDao;
import by.bsuir.touragency.server.dao.FavoriteDao;
import by.bsuir.touragency.server.dao.PreferenceDao;
import by.bsuir.touragency.server.dao.TourDao;
import by.bsuir.touragency.server.dao.TripRecord;
import by.bsuir.touragency.server.service.TourMatcher.ClientContext;
import by.bsuir.touragency.server.util.Validator;

import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Анкета предпочтений, подбор туров по баллам, персональные рекомендации и избранное.
 */
public class RecommendationService {

    private static final int MIN_PERCENT_FOR_YOU = 40;
    private static final int MIN_PERCENT_NEW = 50;
    private static final int LIKED_RATING = 4;

    private final PreferenceDao preferenceDao;
    private final TourDao tourDao;
    private final FavoriteDao favoriteDao;
    private final BookingDao bookingDao;
    private final CatalogDao catalogDao;
    private final AuditDao auditDao;

    public RecommendationService(PreferenceDao preferenceDao, TourDao tourDao, FavoriteDao favoriteDao,
                                 BookingDao bookingDao, CatalogDao catalogDao, AuditDao auditDao) {
        this.preferenceDao = preferenceDao;
        this.tourDao = tourDao;
        this.favoriteDao = favoriteDao;
        this.bookingDao = bookingDao;
        this.catalogDao = catalogDao;
        this.auditDao = auditDao;
    }

    // ------------------------------------------------------------ анкета

    public Preferences getPreferences(int userId) {
        return preferenceDao.find(userId).orElseGet(Preferences::new);
    }

    public void savePreferences(User user, Preferences p) throws AppException {
        if (p.getMinBudget() != null && p.getMaxBudget() != null && p.getMinBudget().compareTo(p.getMaxBudget()) > 0) {
            throw new ValidationException("prefs.budgetOrder");
        }
        if (p.getDesiredFrom() != null && p.getDesiredTo() != null && p.getDesiredTo().isBefore(p.getDesiredFrom())) {
            throw new ValidationException("validation.datesOrder");
        }
        if (p.getMinNights() != null && p.getMaxNights() != null && p.getMinNights() > p.getMaxNights()) {
            throw new ValidationException("prefs.nightsOrder");
        }
        if (p.getMinStars() != null) Validator.range(p.getMinStars(), 1, 5, "field.stars");
        if (p.getTouristsCount() != null) Validator.range(p.getTouristsCount(), 1, 20, "field.tourists");
        preferenceDao.save(user.getId(), p);
        auditDao.log(user.getId(), "UPDATE", "PREFERENCES", user.getId(), "Анкета предпочтений сохранена");
    }

    // ------------------------------------------------------------ контекст клиента

    private ClientContext context(int userId, Lang lang) {
        Map<Integer, String> liked = new LinkedHashMap<>();
        for (TripRecord trip : bookingDao.findTrips(userId, lang)) {
            if (trip.rating() != null && trip.rating() >= LIKED_RATING) {
                liked.putIfAbsent(trip.countryId(), trip.countryName());
            }
        }
        Set<Integer> favoriteTours = favoriteDao.findTourIds(userId);
        Set<Integer> favoriteCountries = new HashSet<>();
        for (Integer id : favoriteTours) {
            tourDao.findById(id, lang).ifPresent(t -> favoriteCountries.add(t.getCountryId()));
        }
        return new ClientContext(liked, favoriteTours, favoriteCountries);
    }

    private String mealName(Preferences p, Lang lang) {
        if (p.getMealTypeId() == null) return null;
        return catalogDao.findMealTypes(lang).stream().filter(m -> m.getId().equals(p.getMealTypeId()))
                .map(DictionaryItem::getName).findFirst().orElse(null);
    }

    // ------------------------------------------------------------ подбор

    /**
     * Проставляет турам процент совпадения и отметку «в избранном»;
     * при сортировке MATCH упорядочивает по проценту.
     */
    public List<Tour> annotate(List<Tour> tours, int userId, Lang lang, boolean sortByMatch) {
        Preferences p = getPreferences(userId);
        ClientContext ctx = context(userId, lang);
        String meal = mealName(p, lang);
        for (Tour t : tours) {
            t.setFavorite(ctx.favoriteTourIds().contains(t.getId()));
            if (!p.isEmpty()) {
                t.setMatchPercent(TourMatcher.evaluate(p, t, ctx, meal).getPercent());
            }
        }
        if (sortByMatch) {
            return tours.stream().sorted(Comparator.comparing(
                    (Tour t) -> t.getMatchPercent() == null ? -1 : t.getMatchPercent()).reversed()).toList();
        }
        return tours;
    }

    /** Подробный разбор оценки одного тура. */
    public MatchResult match(int tourId, int userId, Lang lang) throws NotFoundException {
        Tour tour = tourDao.findById(tourId, lang).orElseThrow(() -> new NotFoundException("catalog.notFound"));
        Preferences p = getPreferences(userId);
        ClientContext ctx = context(userId, lang);
        tour.setFavorite(ctx.favoriteTourIds().contains(tourId));
        MatchResult result = TourMatcher.evaluate(p, tour, ctx, mealName(p, lang));
        tour.setMatchPercent(p.isEmpty() ? null : result.getPercent());
        return result;
    }

    public Recommendations recommendations(int userId, Lang lang) {
        Preferences p = getPreferences(userId);
        ClientContext ctx = context(userId, lang);
        String meal = mealName(p, lang);
        int tourists = p.getTouristsCount() == null ? 1 : p.getTouristsCount();

        List<Tour> available = tourDao.find(new TourFilter().setPersons(tourists), lang);
        List<MatchResult> scored = available.stream()
                .map(t -> {
                    MatchResult r = TourMatcher.evaluate(p, t, ctx, meal);
                    t.setMatchPercent(p.isEmpty() ? null : r.getPercent());
                    t.setFavorite(ctx.favoriteTourIds().contains(t.getId()));
                    return r;
                })
                .sorted(Comparator.comparingInt(MatchResult::getPercent).reversed())
                .toList();

        Recommendations rec = new Recommendations();
        rec.setHasPreferences(!p.isEmpty());

        // 1. Рекомендуем именно вам — лучшие совпадения
        if (!p.isEmpty()) {
            rec.setForYou(scored.stream().filter(r -> r.getPercent() >= MIN_PERCENT_FOR_YOU).limit(6).toList());
        }

        // 2. На основании ваших поездок: тот же тип отдыха, но другая страна
        List<TripRecord> trips = bookingDao.findTrips(userId, lang);
        Optional<TripRecord> best = trips.stream()
                .filter(t -> t.rating() != null && t.rating() >= LIKED_RATING).findFirst();
        best.ifPresent(trip -> {
            rec.setVisitedCountry(trip.countryName());
            Set<Integer> shown = new HashSet<>();
            rec.getForYou().stream().limit(3).forEach(r -> shown.add(r.getTour().getId()));
            List<MatchResult> similar = scored.stream()
                    .filter(r -> !shown.contains(r.getTour().getId()))
                    .filter(r -> r.getTour().getCountryId() != trip.countryId())
                    .filter(r -> r.getTour().getVacationTypeId() == trip.vacationTypeId()
                            || r.getTour().getHotelStars() >= 5)
                    .limit(3).toList();
            rec.setBasedOnTrips(similar);
        });

        // 3. Популярно среди путешественников
        rec.setPopular(scored.stream()
                .sorted(Comparator.comparingInt((MatchResult r) -> r.getTour().getBookingsCount()).reversed()
                        .thenComparing(r -> r.getTour().getRating() == null ? 0 : -r.getTour().getRating()))
                .limit(3).toList());

        // 4. Новые туры по вашим предпочтениям (или просто новинки, если анкеты нет)
        rec.setNewMatching(scored.stream()
                .filter(r -> r.getTour().isNewTour())
                .filter(r -> p.isEmpty() || r.getPercent() >= MIN_PERCENT_NEW)
                .limit(3).toList());
        return rec;
    }

    // ------------------------------------------------------------ избранное

    public boolean toggleFavorite(User user, int tourId, Lang lang) throws NotFoundException {
        tourDao.findById(tourId, lang).orElseThrow(() -> new NotFoundException("catalog.notFound"));
        boolean now = favoriteDao.toggle(user.getId(), tourId);
        auditDao.log(user.getId(), now ? "FAVORITE_ADD" : "FAVORITE_REMOVE", "TOUR", tourId, null);
        return now;
    }

    public List<Tour> favorites(int userId, Lang lang) {
        Set<Integer> ids = favoriteDao.findTourIds(userId);
        List<Tour> tours = ids.stream().map(id -> tourDao.findById(id, lang)).flatMap(Optional::stream).toList();
        return annotate(new java.util.ArrayList<>(tours), userId, lang, false);
    }
}
