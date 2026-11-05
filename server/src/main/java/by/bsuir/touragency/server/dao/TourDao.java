package by.bsuir.touragency.server.dao;

import by.bsuir.touragency.common.model.Lang;
import by.bsuir.touragency.common.model.Tour;
import by.bsuir.touragency.common.model.TourFilter;

import java.util.List;
import java.util.Optional;

public interface TourDao {

    /** Запись в историю цен (по ней показывается прежняя цена и рассылаются уведомления). */
    void addPriceHistory(int tourId, java.math.BigDecimal oldPrice, java.math.BigDecimal newPrice, int changedBy);

    /** Отметить, что клиент открыл тур (карусель «Вы недавно смотрели»). */
    void registerView(int userId, int tourId);

    /** Недавно просмотренные туры клиента, новые первыми. */
    List<Tour> findRecentlyViewed(int userId, int limit, Lang lang);

    /** Похожие туры: та же страна или тот же тип отдыха, ближе по цене — выше. */
    List<Tour> findSimilar(Tour tour, int limit, Lang lang);

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
