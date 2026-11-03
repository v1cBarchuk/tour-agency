package by.bsuir.touragency.server.dao;

import by.bsuir.touragency.common.model.BookingStatus;

/** Фильтр списка заявок для менеджера. */
public record BookingFilter(Integer userId, BookingStatus status, String search) {
}
