package by.bsuir.touragency.server.service.discount;

import java.time.LocalDate;

/**
 * Данные, от которых зависит применимость скидки.
 *
 * @param promoCode промокод, введённый клиентом при бронировании (может быть null)
 */
public record DiscountContext(LocalDate tourStart, LocalDate bookingDate, int completedTrips, String promoCode) {

    public DiscountContext(LocalDate tourStart, LocalDate bookingDate, int completedTrips) {
        this(tourStart, bookingDate, completedTrips, null);
    }

    public long daysBeforeTour() {
        return java.time.temporal.ChronoUnit.DAYS.between(bookingDate, tourStart);
    }

    public boolean hasPromoCode() {
        return promoCode != null && !promoCode.isBlank();
    }
}
