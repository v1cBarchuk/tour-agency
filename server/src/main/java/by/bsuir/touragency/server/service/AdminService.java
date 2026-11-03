package by.bsuir.touragency.server.service;

import by.bsuir.touragency.common.exception.AppException;
import by.bsuir.touragency.common.exception.BusinessRuleException;
import by.bsuir.touragency.common.exception.NotFoundException;
import by.bsuir.touragency.common.model.AuditEntry;
import by.bsuir.touragency.common.model.Lang;
import by.bsuir.touragency.common.model.Role;
import by.bsuir.touragency.common.model.Stats;
import by.bsuir.touragency.common.model.User;
import by.bsuir.touragency.server.dao.AuditDao;
import by.bsuir.touragency.server.dao.AuditFilter;
import by.bsuir.touragency.server.dao.StatsDao;
import by.bsuir.touragency.server.dao.UserDao;

import java.time.LocalDate;
import java.util.List;

/** Панель управления, аналитика, пользователи и журнал действий. */
public class AdminService {

    private final UserDao userDao;
    private final AuditDao auditDao;
    private final StatsDao statsDao;

    public AdminService(UserDao userDao, AuditDao auditDao, StatsDao statsDao) {
        this.userDao = userDao;
        this.auditDao = auditDao;
        this.statsDao = statsDao;
    }

    public Stats dashboard(Lang lang) {
        return statsDao.dashboard(lang);
    }

    /** Отчёт за период; по умолчанию — с начала года по сегодня. */
    public Stats analytics(LocalDate from, LocalDate to, Lang lang) throws AppException {
        LocalDate f = from == null ? LocalDate.now().withDayOfYear(1) : from;
        LocalDate t = to == null ? LocalDate.now() : to;
        if (t.isBefore(f)) {
            throw new BusinessRuleException("validation.datesOrder");
        }
        return statsDao.analytics(f, t, lang);
    }

    public List<User> users() {
        return userDao.findAll();
    }

    public void setRole(int userId, Role role, User admin) throws AppException {
        User target = userDao.findById(userId).orElseThrow(() -> new NotFoundException("catalog.notFound"));
        if (target.getId().equals(admin.getId())) {
            throw new BusinessRuleException("admin.self");
        }
        userDao.updateRole(userId, role);
        auditDao.log(admin.getId(), "UPDATE", "USER", userId, "role: " + target.getRole() + " → " + role);
    }

    public void setBlocked(int userId, boolean blocked, User admin) throws AppException {
        User target = userDao.findById(userId).orElseThrow(() -> new NotFoundException("catalog.notFound"));
        if (target.getId().equals(admin.getId())) {
            throw new BusinessRuleException("admin.self");
        }
        userDao.setBlocked(userId, blocked);
        auditDao.log(admin.getId(), blocked ? "BLOCK" : "UNBLOCK", "USER", userId,
                "is_blocked: " + target.isBlocked() + " → " + blocked);
    }

    public List<AuditEntry> audit(AuditFilter filter) {
        return auditDao.find(filter);
    }
}
