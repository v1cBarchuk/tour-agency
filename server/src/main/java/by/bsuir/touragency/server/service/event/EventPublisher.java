package by.bsuir.touragency.server.service.event;

import by.bsuir.touragency.common.model.BookingStatus;
import by.bsuir.touragency.common.model.Tour;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Издатель событий (паттерн Observer). Сервисы сообщают о событиях,
 * не зная, кто на них подписан; ошибка подписчика не ломает основную операцию.
 * Списки потокобезопасны: события приходят из разных потоков-обработчиков.
 */
public class EventPublisher {

    private static final Logger LOG = Logger.getLogger(EventPublisher.class.getName());

    private final List<TourEventListener> tourListeners = new CopyOnWriteArrayList<>();
    private final List<BookingEventListener> bookingListeners = new CopyOnWriteArrayList<>();

    public void subscribe(TourEventListener listener) {
        tourListeners.add(listener);
    }

    // Перегрузка: подписка на события бронирований
    public void subscribe(BookingEventListener listener) {
        bookingListeners.add(listener);
    }

    public void tourCreated(Tour tour) {
        for (TourEventListener l : tourListeners) {
            try {
                l.tourCreated(tour);
            } catch (RuntimeException e) {
                LOG.log(Level.WARNING, "Подписчик не обработал событие нового тура", e);
            }
        }
    }

    public void bookingStatusChanged(int bookingId, int clientId, BookingStatus status, Integer actorId) {
        for (BookingEventListener l : bookingListeners) {
            try {
                l.statusChanged(bookingId, clientId, status, actorId);
            } catch (RuntimeException e) {
                LOG.log(Level.WARNING, "Подписчик не обработал смену статуса", e);
            }
        }
    }
}
