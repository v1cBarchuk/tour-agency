package by.bsuir.touragency.server.dao;


import by.bsuir.touragency.common.model.AuditEntry;

import java.util.List;

public interface AuditDao {

    List<AuditEntry> find(AuditFilter filter);

    void log(Integer userId, String action, String objectType, Integer objectId, String details);

    /** Перегрузка для действий без объекта (вход, выход). */
    default void log(Integer userId, String action, String details) {
        log(userId, action, null, null, details);
    }
}
