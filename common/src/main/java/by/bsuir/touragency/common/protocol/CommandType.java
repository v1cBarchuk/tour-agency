package by.bsuir.touragency.common.protocol;

/**
 * Команды протокола клиент–сервер.
 * Новые варианты использования добавляются сюда и в CommandFactory на сервере.
 */
public enum CommandType {
    // служебные и авторизация
    PING,
    LOGIN,
    REGISTER,
    LOGOUT,
    DISCONNECT,
    SET_LANGUAGE,

    // каталог (просмотр)
    GET_REFERENCE_DATA,
    GET_COUNTRIES,
    GET_HOTELS,
    GET_HOTEL,
    GET_TOURS,
    GET_TOUR,

    // каталог (управление, менеджер)
    SAVE_COUNTRY,
    SAVE_CITY,
    SAVE_HOTEL,
    SAVE_TOUR,
    SET_TOUR_ACTIVE,
    DELETE_TOUR,
    GET_DISCOUNTS,
    SAVE_DISCOUNT,
    SET_DISCOUNT_ACTIVE,

    // анкета, подбор, избранное (клиент)
    GET_PREFERENCES,
    SAVE_PREFERENCES,
    GET_RECOMMENDATIONS,
    GET_MATCH,
    TOGGLE_FAVORITE,
    GET_FAVORITES,

    // бронирование
    QUOTE_BOOKING,
    CREATE_BOOKING,
    GET_MY_BOOKINGS,
    GET_BOOKINGS,
    GET_BOOKING,
    CHANGE_BOOKING_STATUS,
    PAY_BOOKING,
    QUOTE_CANCEL,
    CANCEL_BOOKING,
    ADD_REVIEW,
    GET_HOTEL_REVIEWS,

    // уведомления
    GET_NOTIFICATIONS,
    MARK_NOTIFICATIONS_READ,

    // панель управления и аналитика
    GET_DASHBOARD,
    GET_ANALYTICS,

    // администрирование
    GET_USERS,
    SET_USER_ROLE,
    SET_USER_BLOCKED,
    GET_AUDIT
}
