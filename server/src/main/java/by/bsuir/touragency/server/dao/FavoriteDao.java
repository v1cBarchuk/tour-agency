package by.bsuir.touragency.server.dao;

import java.util.Set;

public interface FavoriteDao {

    /** Добавляет тур в избранное или убирает его; возвращает true, если теперь тур в избранном. */
    boolean toggle(int userId, int tourId);

    Set<Integer> findTourIds(int userId);
}
