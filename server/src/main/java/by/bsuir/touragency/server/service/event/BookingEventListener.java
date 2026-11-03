package by.bsuir.touragency.server.service.event;

import by.bsuir.touragency.common.model.BookingStatus;

/** Подписчик на изменения бронирований (паттерн Observer). */
public interface BookingEventListener {

    void statusChanged(int bookingId, int clientId, BookingStatus status, Integer actorId);
}
