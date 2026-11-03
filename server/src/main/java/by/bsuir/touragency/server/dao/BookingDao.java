package by.bsuir.touragency.server.dao;

import by.bsuir.touragency.common.model.Booking;
import by.bsuir.touragency.common.model.BookingStatus;
import by.bsuir.touragency.common.model.Lang;
import by.bsuir.touragency.common.model.Tourist;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface BookingDao {

    /**
     * Создаёт бронирование атомарно: блокирует строку тура (SELECT … FOR UPDATE),
     * пересчитывает свободные места и только при их наличии вставляет бронь.
     *
     * @throws SeatsUnavailableException если мест не хватает
     */
    int createLocked(Booking draft, Integer discountId, List<Tourist> tourists, int actorId);

    List<Booking> find(BookingFilter filter, Lang lang);

    Optional<Booking> findById(int id, Lang lang);

    void changeStatus(int bookingId, BookingStatus status, Integer actorId, String comment);

    void pay(int bookingId, BigDecimal amount, String method, int actorId);

    void cancel(int bookingId, BigDecimal penalty, BigDecimal refund, int actorId, String comment);

    int countCompletedTrips(int userId);

    /** Завершённые поездки клиента с его оценкой (для рекомендаций). */
    List<TripRecord> findTrips(int userId, Lang lang);
}
