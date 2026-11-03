package by.bsuir.touragency.common.model;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/** Отзыв о поездке. */
public record Review(int id, int bookingId, int rating, String comment, LocalDateTime createdAt,
                     String authorName, String hotelName, String tourTitle) implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
}
