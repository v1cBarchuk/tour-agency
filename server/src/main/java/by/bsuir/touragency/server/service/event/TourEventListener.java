package by.bsuir.touragency.server.service.event;

import by.bsuir.touragency.common.model.Tour;

/** Подписчик на события каталога (паттерн Observer). */
public interface TourEventListener {

    void tourCreated(Tour tour);

    /**
     * Менеджер снизил цену тура. Метод по умолчанию пустой: подписчик
     * переопределяет только те события, которые ему нужны.
     */
    default void priceDropped(Tour tour, java.math.BigDecimal oldPrice) {
    }
}
