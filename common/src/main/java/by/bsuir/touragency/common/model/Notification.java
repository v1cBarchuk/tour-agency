package by.bsuir.touragency.common.model;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/** Уведомление клиенту. Текст строится на клиенте по type (с переводом), message — запасной. */
public record Notification(int id, String type, Integer tourId, String tourTitle, Integer bookingId,
                           String message, boolean read, LocalDateTime createdAt) implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
}
