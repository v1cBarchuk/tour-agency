package by.bsuir.touragency.server.service.discount;

import java.time.LocalDate;

/** Данные, от которых зависит применимость скидки. */
public record DiscountContext(LocalDate tourStart, LocalDate bookingDate, int completedTrips) {

    public long daysBeforeTour() {
        return java.time.temporal.ChronoUnit.DAYS.between(bookingDate, tourStart);
    }
}
