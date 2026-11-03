package by.bsuir.touragency.common.model;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/** Запись истории статусов — точка на временной шкале заявки. */
public record StatusChange(BookingStatus status, LocalDateTime changedAt, String changedBy, String comment)
        implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
}
