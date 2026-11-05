package by.bsuir.touragency.server.command;

import by.bsuir.touragency.server.service.WeatherService;
import by.bsuir.touragency.common.model.User;
import java.util.HashMap;
import by.bsuir.touragency.common.model.BookingStatus;
import by.bsuir.touragency.common.model.City;
import by.bsuir.touragency.common.model.Country;
import by.bsuir.touragency.common.model.Discount;
import by.bsuir.touragency.common.model.Hotel;
import by.bsuir.touragency.common.model.Preferences;
import by.bsuir.touragency.common.model.Role;
import by.bsuir.touragency.common.model.Tour;
import by.bsuir.touragency.common.model.Tourist;
import by.bsuir.touragency.common.protocol.CommandType;
import by.bsuir.touragency.common.protocol.Request;
import by.bsuir.touragency.common.protocol.Response;
import by.bsuir.touragency.server.dao.AuditFilter;
import by.bsuir.touragency.server.dao.JdbcAuditDao;
import by.bsuir.touragency.server.dao.JdbcBookingDao;
import by.bsuir.touragency.server.dao.JdbcCatalogDao;
import by.bsuir.touragency.server.dao.JdbcDiscountDao;
import by.bsuir.touragency.server.dao.JdbcFavoriteDao;
import by.bsuir.touragency.server.dao.JdbcNotificationDao;
import by.bsuir.touragency.server.dao.JdbcPreferenceDao;
import by.bsuir.touragency.server.dao.JdbcReviewDao;
import by.bsuir.touragency.server.dao.JdbcSettingsDao;
import by.bsuir.touragency.server.dao.JdbcStatsDao;
import by.bsuir.touragency.server.dao.JdbcTourDao;
import by.bsuir.touragency.server.dao.JdbcUserDao;
import by.bsuir.touragency.server.dao.SettingsDao;
import by.bsuir.touragency.server.service.AdminService;
import by.bsuir.touragency.server.service.AuthService;
import by.bsuir.touragency.server.service.BookingService;
import by.bsuir.touragency.server.service.CatalogService;
import by.bsuir.touragency.server.service.NotificationService;
import by.bsuir.touragency.server.service.RecommendationService;
import by.bsuir.touragency.server.service.event.EventPublisher;
import by.bsuir.touragency.server.util.ServerMessages;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static by.bsuir.touragency.server.command.AbstractCommand.ADMINS;
import static by.bsuir.touragency.server.command.AbstractCommand.AUTHENTICATED;
import static by.bsuir.touragency.server.command.AbstractCommand.CLIENTS;
import static by.bsuir.touragency.server.command.AbstractCommand.STAFF;

/**
 * Фабрика команд (паттерн Factory): по типу запроса возвращает обработчик.
 * Здесь же собираются зависимости: DAO → сервисы → команды, и подписчики событий.
 * Команды не хранят состояния клиента, поэтому один экземпляр
 * безопасно используется всеми потоками.
 */
public class CommandFactory {

    private final Map<CommandType, Command> commands = new EnumMap<>(CommandType.class);

    public CommandFactory() {
        // ---- зависимости
        JdbcAuditDao auditDao = new JdbcAuditDao();
        SettingsDao settings = new JdbcSettingsDao();
        JdbcUserDao userDao = new JdbcUserDao();
        JdbcCatalogDao catalogDao = new JdbcCatalogDao();
        JdbcTourDao tourDao = new JdbcTourDao(settings);
        JdbcDiscountDao discountDao = new JdbcDiscountDao();
        JdbcBookingDao bookingDao = new JdbcBookingDao();
        JdbcPreferenceDao preferenceDao = new JdbcPreferenceDao();

        EventPublisher events = new EventPublisher();
        NotificationService notifications = new NotificationService(new JdbcNotificationDao(), preferenceDao, settings);
        events.subscribe((by.bsuir.touragency.server.service.event.TourEventListener) notifications);
        events.subscribe((by.bsuir.touragency.server.service.event.BookingEventListener) notifications);

        AuthService auth = new AuthService(userDao, auditDao);
        CatalogService catalog = new CatalogService(catalogDao, tourDao, discountDao, auditDao);
        catalog.setEvents(events);
        RecommendationService rec = new RecommendationService(preferenceDao, tourDao, new JdbcFavoriteDao(),
                bookingDao, catalogDao, auditDao);
        BookingService booking = new BookingService(bookingDao, tourDao, discountDao, new JdbcReviewDao(),
                settings, auditDao, events);
        AdminService admin = new AdminService(userDao, auditDao, new JdbcStatsDao());

        // ---- служебные и авторизация
        commands.put(CommandType.PING, new PingCommand());
        commands.put(CommandType.LOGIN, new LoginCommand(auth));
        commands.put(CommandType.REGISTER, new RegisterCommand(auth));
        commands.put(CommandType.LOGOUT, new LogoutCommand(auth));
        commands.put(CommandType.SET_LANGUAGE, new SetLanguageCommand(auth));

        // ---- каталог: просмотр
        put(CommandType.GET_REFERENCE_DATA, AUTHENTICATED, (rq, s) -> Response.ok(catalog.getReferenceData(rq.getLang())));
        put(CommandType.GET_COUNTRIES, AUTHENTICATED, (rq, s) -> Response.ok(new ArrayList<>(catalog.getCountries(rq.getLang()))));
        put(CommandType.GET_HOTELS, AUTHENTICATED, (rq, s) -> Response.ok(new ArrayList<>(
                catalog.getHotels(rq.getInt("countryId"), rq.getInt("cityId"), rq.getLang()))));
        put(CommandType.GET_HOTEL, AUTHENTICATED, (rq, s) -> Response.ok(catalog.getHotel(rq.getInt("hotelId"), rq.getLang())));
        commands.put(CommandType.GET_TOURS, new GetToursCommand(catalog, rec));
        put(CommandType.GET_TOUR, AUTHENTICATED, (rq, s) -> {
            Tour tour = catalog.getTour(rq.getInt("tourId"), rq.getLang());
            if (s.getUser().getRole() == Role.CLIENT) {
                rec.annotate(new ArrayList<>(List.of(tour)), s.getUser().getId(), rq.getLang(), false);
                if (!rq.getBool("noView")) tourDao.registerView(s.getUser().getId(), tour.getId());
            }
            return Response.ok(tour);
        });
        put(CommandType.GET_SIMILAR_TOURS, AUTHENTICATED, (rq, s) -> {
            Tour tour = catalog.getTour(rq.getInt("tourId"), rq.getLang());
            ArrayList<Tour> list = new ArrayList<>(tourDao.findSimilar(tour, 8, rq.getLang()));
            if (s.getUser().getRole() == Role.CLIENT) rec.annotate(list, s.getUser().getId(), rq.getLang(), false);
            return Response.ok(list);
        });
        put(CommandType.GET_RECENTLY_VIEWED, CLIENTS, (rq, s) -> {
            ArrayList<Tour> list = new ArrayList<>(tourDao.findRecentlyViewed(s.getUser().getId(), 10, rq.getLang()));
            rec.annotate(list, s.getUser().getId(), rq.getLang(), false);
            return Response.ok(list);
        });
        WeatherService weather = new WeatherService(settings);
        put(CommandType.GET_WEATHER, AUTHENTICATED, (rq, s) ->
                Response.ok(weather.forecast(catalog.getTour(rq.getInt("tourId"), rq.getLang()))));

        // курсы валют для переключателя BYN / USD / EUR (хранятся в настройках, задаёт администратор)
        put(CommandType.GET_RATES, AUTHENTICATED, (rq, s) -> {
            HashMap<String, java.math.BigDecimal> rates = new HashMap<>();
            for (String c : List.of("USD", "EUR")) {
                rates.put(c, new java.math.BigDecimal(settings.get("currency.rate." + c, c.equals("USD") ? "3.27" : "3.55")));
            }
            return Response.ok(rates);
        });

        // ---- личный кабинет
        put(CommandType.GET_PROFILE, AUTHENTICATED, (rq, s) -> {
            HashMap<String, java.io.Serializable> data = new HashMap<>();
            data.put("user", userDao.findById(s.getUser().getId()).orElse(s.getUser()));
            data.put("stats", auth.profileStats(s.getUser().getId()));
            return Response.ok(data);
        });
        put(CommandType.SAVE_PROFILE, AUTHENTICATED, (rq, s) -> {
            User updated = auth.updateProfile(s.getUser(), rq.<User>getObject("user"));
            s.setUser(updated);
            return Response.ok(msg(rq, "profile.saved"), updated);
        });
        put(CommandType.CHANGE_PASSWORD, AUTHENTICATED, (rq, s) -> {
            auth.changePassword(s.getUser(), rq.getString("oldPassword"), rq.getString("newPassword"));
            return Response.ok(msg(rq, "profile.passwordChanged"), null);
        });
        put(CommandType.GET_HOTEL_REVIEWS, AUTHENTICATED, (rq, s) -> Response.ok(new ArrayList<>(
                booking.hotelReviews(rq.getInt("hotelId")))));

        // ---- каталог: управление (менеджер, администратор)
        put(CommandType.SAVE_COUNTRY, STAFF, (rq, s) -> Response.ok(msg(rq, "catalog.saved"),
                catalog.saveCountry(rq.<Country>getObject("country"), s.getUser())));
        put(CommandType.SAVE_CITY, STAFF, (rq, s) -> Response.ok(msg(rq, "catalog.saved"),
                catalog.saveCity(rq.<City>getObject("city"), s.getUser())));
        put(CommandType.SAVE_HOTEL, STAFF, (rq, s) -> Response.ok(msg(rq, "catalog.saved"),
                catalog.saveHotel(rq.<Hotel>getObject("hotel"), s.getUser())));
        // новый тур → событие → уведомления клиентам с подходящей анкетой (Observer)
        commands.put(CommandType.SAVE_TOUR, new SaveTourCommand(catalog, events::tourCreated));
        put(CommandType.SET_TOUR_ACTIVE, STAFF, (rq, s) -> {
            boolean active = rq.getBool("active");
            catalog.setTourActive(rq.getInt("tourId"), active, s.getUser(), rq.getLang());
            return Response.ok(msg(rq, active ? "catalog.tourActivated" : "catalog.tourDeactivated"), null);
        });
        commands.put(CommandType.DELETE_TOUR, new DeleteTourCommand(catalog));
        put(CommandType.GET_DISCOUNTS, STAFF, (rq, s) -> Response.ok(new ArrayList<>(catalog.getDiscounts())));
        put(CommandType.SAVE_DISCOUNT, STAFF, (rq, s) -> Response.ok(msg(rq, "catalog.saved"),
                catalog.saveDiscount(rq.<Discount>getObject("discount"), s.getUser())));
        put(CommandType.SET_DISCOUNT_ACTIVE, STAFF, (rq, s) -> {
            boolean active = rq.getBool("active");
            catalog.setDiscountActive(rq.getInt("discountId"), active, s.getUser());
            return Response.ok(msg(rq, active ? "discount.activated" : "discount.deactivated"), null);
        });

        // ---- анкета, подбор, избранное
        put(CommandType.GET_PREFERENCES, CLIENTS, (rq, s) -> Response.ok(rec.getPreferences(s.getUser().getId())));
        put(CommandType.SAVE_PREFERENCES, CLIENTS, (rq, s) -> {
            rec.savePreferences(s.getUser(), rq.<Preferences>getObject("preferences"));
            return Response.ok(msg(rq, "prefs.saved"), null);
        });
        put(CommandType.GET_RECOMMENDATIONS, CLIENTS, (rq, s) ->
                Response.ok(rec.recommendations(s.getUser().getId(), rq.getLang())));
        put(CommandType.GET_MATCH, CLIENTS, (rq, s) ->
                Response.ok(rec.match(rq.getInt("tourId"), s.getUser().getId(), rq.getLang())));
        put(CommandType.TOGGLE_FAVORITE, CLIENTS, (rq, s) -> {
            boolean now = rec.toggleFavorite(s.getUser(), rq.getInt("tourId"), rq.getLang());
            return Response.ok(msg(rq, now ? "fav.added" : "fav.removed"), now);
        });
        put(CommandType.GET_FAVORITES, CLIENTS, (rq, s) ->
                Response.ok(new ArrayList<>(rec.favorites(s.getUser().getId(), rq.getLang()))));

        // ---- бронирование
        put(CommandType.QUOTE_BOOKING, CLIENTS, (rq, s) ->
                Response.ok(booking.quote(rq.getInt("tourId"), rq.getInt("persons"), rq.getString("promoCode"),
                        s.getUser(), rq.getLang())));
        put(CommandType.CREATE_BOOKING, CLIENTS, (rq, s) -> {
            ArrayList<Tourist> tourists = rq.getObject("tourists");
            return Response.ok(msg(rq, "booking.created"), booking.create(s.getUser(), rq.getInt("tourId"),
                    tourists, rq.getString("phone"), rq.getString("email"), rq.getString("promoCode"), rq.getLang()));
        });
        put(CommandType.GET_MY_BOOKINGS, CLIENTS, (rq, s) ->
                Response.ok(new ArrayList<>(booking.myBookings(s.getUser(), rq.getLang()))));
        put(CommandType.GET_BOOKINGS, STAFF, (rq, s) -> {
            String status = rq.getString("status");
            return Response.ok(new ArrayList<>(booking.allBookings(
                    status == null ? null : BookingStatus.valueOf(status), rq.getString("search"), rq.getLang())));
        });
        put(CommandType.GET_BOOKING, AUTHENTICATED, (rq, s) ->
                Response.ok(booking.get(rq.getInt("bookingId"), s.getUser(), rq.getLang())));
        put(CommandType.CHANGE_BOOKING_STATUS, STAFF, (rq, s) -> Response.ok(msg(rq, "booking.statusChanged"),
                booking.changeStatus(rq.getInt("bookingId"), BookingStatus.valueOf(rq.getString("status")),
                        s.getUser(), rq.getLang())));
        put(CommandType.PAY_BOOKING, AUTHENTICATED, (rq, s) -> Response.ok(msg(rq, "booking.paid"),
                booking.pay(rq.getInt("bookingId"), rq.getString("method"), s.getUser(), rq.getLang())));
        put(CommandType.QUOTE_CANCEL, AUTHENTICATED, (rq, s) ->
                Response.ok(booking.quoteCancel(rq.getInt("bookingId"), s.getUser(), rq.getLang())));
        put(CommandType.CANCEL_BOOKING, AUTHENTICATED, (rq, s) -> Response.ok(msg(rq, "booking.cancelled"),
                booking.cancel(rq.getInt("bookingId"), s.getUser(), rq.getLang())));
        put(CommandType.ADD_REVIEW, CLIENTS, (rq, s) -> {
            booking.addReview(rq.getInt("bookingId"), rq.getInt("rating"), rq.getString("comment"),
                    s.getUser(), rq.getLang());
            return Response.ok(msg(rq, "review.saved"), null);
        });

        // ---- уведомления
        put(CommandType.GET_NOTIFICATIONS, AUTHENTICATED, (rq, s) ->
                Response.ok(new ArrayList<>(notifications.list(s.getUser().getId(), rq.getLang()))));
        put(CommandType.MARK_NOTIFICATIONS_READ, AUTHENTICATED, (rq, s) -> {
            notifications.markRead(s.getUser().getId(), rq.getInt("notificationId"));
            return Response.ok();
        });

        // ---- панель управления и аналитика
        put(CommandType.GET_DASHBOARD, STAFF, (rq, s) -> Response.ok(admin.dashboard(rq.getLang())));
        put(CommandType.GET_ANALYTICS, STAFF, (rq, s) -> Response.ok(admin.analytics(
                rq.<LocalDate>getObject("from"), rq.<LocalDate>getObject("to"), rq.getLang())));

        // ---- администрирование
        put(CommandType.GET_USERS, ADMINS, (rq, s) -> Response.ok(new ArrayList<>(admin.users())));
        put(CommandType.SET_USER_ROLE, ADMINS, (rq, s) -> {
            admin.setRole(rq.getInt("userId"), Role.valueOf(rq.getString("role")), s.getUser());
            return Response.ok(msg(rq, "admin.roleChanged"), null);
        });
        put(CommandType.SET_USER_BLOCKED, ADMINS, (rq, s) -> {
            boolean blocked = rq.getBool("blocked");
            admin.setBlocked(rq.getInt("userId"), blocked, s.getUser());
            return Response.ok(msg(rq, blocked ? "admin.blocked" : "admin.unblocked"), null);
        });
        put(CommandType.GET_AUDIT, ADMINS, (rq, s) -> Response.ok(new ArrayList<>(admin.audit(new AuditFilter(
                rq.getInt("userId"), rq.getString("action"), rq.getString("objectType"),
                rq.<LocalDate>getObject("from"), rq.<LocalDate>getObject("to"))))));
    }

    private void put(CommandType type, java.util.Set<Role> roles, SimpleCommand.Handler handler) {
        commands.put(type, new SimpleCommand(roles, handler));
    }

    private static String msg(Request rq, String key) {
        return ServerMessages.get(rq.getLang(), key);
    }

    public Optional<Command> create(CommandType type) {
        return Optional.ofNullable(commands.get(type));
    }
}
