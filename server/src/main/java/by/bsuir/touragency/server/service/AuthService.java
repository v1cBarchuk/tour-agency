package by.bsuir.touragency.server.service;

import by.bsuir.touragency.common.exception.AppException;
import by.bsuir.touragency.common.exception.AuthException;
import by.bsuir.touragency.common.exception.ValidationException;
import by.bsuir.touragency.common.model.Lang;
import by.bsuir.touragency.common.model.Role;
import by.bsuir.touragency.common.model.User;
import by.bsuir.touragency.server.dao.AuditDao;
import by.bsuir.touragency.server.dao.UserCredentials;
import by.bsuir.touragency.server.dao.UserDao;
import by.bsuir.touragency.server.util.PasswordHasher;
import by.bsuir.touragency.server.util.Validator;

/**
 * Бизнес-логика авторизации и регистрации.
 */
public class AuthService {

    /** После стольких неудачных попыток вход по логину временно блокируется. */
    static final int MAX_FAILED_ATTEMPTS = 5;
    static final java.time.Duration LOCK_TIME = java.time.Duration.ofMinutes(5);

    private final UserDao userDao;
    private final AuditDao auditDao;
    /** Счётчики неудачных попыток по логину (общие для всех клиентских потоков). */
    private final java.util.concurrent.ConcurrentHashMap<String, Attempts> attempts =
            new java.util.concurrent.ConcurrentHashMap<>();
    private java.time.Clock clock = java.time.Clock.systemDefaultZone();

    private static final class Attempts {
        int failed;
        java.time.Instant lockedUntil;
    }

    /** Для тестов: подменяемые часы. */
    void setClock(java.time.Clock clock) {
        this.clock = clock;
    }

    public AuthService(UserDao userDao, AuditDao auditDao) {
        this.userDao = userDao;
        this.auditDao = auditDao;
    }

    public User login(String login, String password) throws AppException {
        Validator.requireNotBlank(login, "field.login");
        Validator.requireNotBlank(password, "field.password");

        String key = login.trim().toLowerCase();
        checkNotLocked(key);

        UserCredentials credentials = userDao.findCredentialsByLogin(login.trim()).orElse(null);
        if (credentials == null) {
            registerFailure(key, null);
            throw new AuthException("auth.invalid");
        }
        if (!PasswordHasher.verify(password, credentials.salt(), credentials.passwordHash())) {
            auditDao.log(credentials.user().getId(), "LOGIN_FAILED", "Неверный пароль");
            registerFailure(key, credentials.user().getId());
            throw new AuthException("auth.invalid");
        }
        attempts.remove(key);
        if (credentials.user().isBlocked()) {
            throw new AuthException("auth.blocked");
        }
        auditDao.log(credentials.user().getId(), "LOGIN", "Вход в систему");
        return credentials.user();
    }

    private void checkNotLocked(String key) throws AuthException {
        Attempts a = attempts.get(key);
        if (a == null) return;
        synchronized (a) {
            if (a.lockedUntil == null) return;
            java.time.Instant now = clock.instant();
            if (now.isBefore(a.lockedUntil)) {
                long minutes = Math.max(1, (java.time.Duration.between(now, a.lockedUntil).toSeconds() + 59) / 60);
                throw new AuthException("auth.locked", minutes);
            }
            attempts.remove(key);          // время блокировки истекло
        }
    }

    private void registerFailure(String key, Integer userId) throws AuthException {
        Attempts a = attempts.computeIfAbsent(key, k -> new Attempts());
        synchronized (a) {
            a.failed++;
            if (a.failed >= MAX_FAILED_ATTEMPTS) {
                a.lockedUntil = clock.instant().plus(LOCK_TIME);
                a.failed = 0;
                if (userId != null) {
                    auditDao.log(userId, "LOGIN_LOCKED", "Вход заблокирован на " + LOCK_TIME.toMinutes() + " мин");
                }
                throw new AuthException("auth.locked", LOCK_TIME.toMinutes());
            }
        }
    }

    // ------------------------------------------------------------ личный кабинет

    public by.bsuir.touragency.common.model.ProfileStats profileStats(int userId) {
        return userDao.profileStats(userId);
    }

    /** Сохраняет данные профиля; логин и роль здесь не меняются. Возвращает обновлённого пользователя. */
    public User updateProfile(User actor, User changes) throws AppException {
        User u = userDao.findById(actor.getId()).orElseThrow(() -> new AuthException("auth.invalid"));
        u.setFullName(Validator.requireNotBlank(changes.getFullName(), "field.fullName"));
        String email = Validator.email(changes.getEmail());
        if (userDao.emailTakenByOther(email, u.getId())) {
            throw new ValidationException("validation.emailTaken");
        }
        u.setEmail(email);
        u.setPhone(Validator.phone(changes.getPhone()));
        if (changes.getBirthDate() != null && (changes.getBirthDate().isAfter(java.time.LocalDate.now().minusYears(14))
                || changes.getBirthDate().isBefore(java.time.LocalDate.of(1900, 1, 1)))) {
            throw new ValidationException("profile.badBirthDate");
        }
        u.setBirthDate(changes.getBirthDate());
        u.setHomeCity(Validator.optional(changes.getHomeCity()));
        u.setNotifyPriceDrop(changes.isNotifyPriceDrop());
        userDao.updateProfile(u);
        auditDao.log(u.getId(), "UPDATE", "USER", u.getId(), "Изменён профиль");
        return u;
    }

    public void changePassword(User actor, String oldPassword, String newPassword) throws AppException {
        Validator.requireNotBlank(oldPassword, "field.oldPassword");
        UserCredentials c = userDao.findCredentialsByLogin(actor.getLogin())
                .orElseThrow(() -> new AuthException("auth.invalid"));
        if (!PasswordHasher.verify(oldPassword, c.salt(), c.passwordHash())) {
            throw new ValidationException("profile.wrongPassword");
        }
        Validator.password(newPassword);
        if (newPassword.equals(oldPassword)) {
            throw new ValidationException("profile.samePassword");
        }
        String salt = PasswordHasher.generateSalt();
        userDao.updatePassword(actor.getId(), PasswordHasher.hash(newPassword, salt), salt);
        auditDao.log(actor.getId(), "UPDATE", "USER", actor.getId(), "Изменён пароль");
    }

    /** Самостоятельная регистрация доступна только для роли «Клиент». */
    public User registerClient(String login, String password, String fullName,
                               String email, String phone, Lang language) throws AppException {
        User user = new User(null,
                Validator.login(login),
                Validator.requireNotBlank(fullName, "field.fullName"),
                Validator.email(email),
                Role.CLIENT);
        user.setPhone(Validator.phone(phone));
        user.setLanguage(language);
        Validator.password(password);

        synchronized (this) {
            // Проверка и вставка под одной блокировкой: два одновременных запроса
            // с одинаковым логином не создадут дубликат (дополнительно страхует UNIQUE в БД)
            if (userDao.existsByLogin(user.getLogin())) {
                throw new ValidationException("validation.loginTaken");
            }
            if (userDao.existsByEmail(user.getEmail())) {
                throw new ValidationException("validation.emailTaken");
            }
            String salt = PasswordHasher.generateSalt();
            int id = userDao.create(user, PasswordHasher.hash(password, salt), salt);
            user.setId(id);
        }
        auditDao.log(user.getId(), "REGISTER", "Регистрация клиента");
        return user;
    }

    public void changeLanguage(User user, Lang language) {
        userDao.updateLanguage(user.getId(), language);
        user.setLanguage(language);
    }

    public void logout(User user) {
        if (user != null) {
            auditDao.log(user.getId(), "LOGOUT", "Выход из системы");
        }
    }
}
