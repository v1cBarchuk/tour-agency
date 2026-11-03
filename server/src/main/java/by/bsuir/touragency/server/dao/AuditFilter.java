package by.bsuir.touragency.server.dao;

import java.time.LocalDate;

/** Фильтр журнала действий. Пустые поля не учитываются. */
public record AuditFilter(Integer userId, String action, String objectType, LocalDate from, LocalDate to) {
}
