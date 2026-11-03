package by.bsuir.touragency.server.service.event;

import by.bsuir.touragency.common.model.Tour;

/** Подписчик на события каталога (паттерн Observer). */
public interface TourEventListener {

    void tourCreated(Tour tour);
}
