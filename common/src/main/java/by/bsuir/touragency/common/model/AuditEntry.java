package by.bsuir.touragency.common.model;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/** Запись журнала действий. */
public record AuditEntry(long id, Integer userId, String userName, String login, String action,
                         String objectType, Integer objectId, String details, LocalDateTime createdAt)
        implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
}
