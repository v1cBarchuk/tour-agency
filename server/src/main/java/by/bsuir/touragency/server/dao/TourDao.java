package by.bsuir.touragency.server.dao;

import by.bsuir.touragency.common.model.Lang;
import by.bsuir.touragency.common.model.Tour;
import by.bsuir.touragency.common.model.TourFilter;

import java.util.List;
import java.util.Optional;

public interface TourDao {

    List<Tour> find(TourFilter filter, Lang lang);

    Optional<Tour> findById(int id, Lang lang);

    /** Создаёт тур и возвращает его идентификатор. */
    int create(Tour tour, int managerId);

    void update(Tour tour);

    void setActive(int tourId, boolean active);

    void delete(int tourId);

    /** Количество неотменённых бронирований тура. */
    int countBookings(int tourId);

    /** Сумма мест в неотменённых бронированиях тура. */
    int countBookedSeats(int tourId);
}
