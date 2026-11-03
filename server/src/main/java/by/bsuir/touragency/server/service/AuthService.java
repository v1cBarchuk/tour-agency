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

    private final UserDao userDao;
    private final AuditDao auditDao;

    public AuthService(UserDao userDao, AuditDao auditDao) {
        this.userDao = userDao;
        this.auditDao = auditDao;
    }

    public User login(String login, String password) throws AppException {
        Validator.requireNotBlank(login, "field.login");
        Validator.requireNotBlank(password, "field.password");

        UserCredentials credentials = userDao.findCredentialsByLogin(login.trim())
                .orElseThrow(() -> new AuthException("auth.invalid"));

        if (!PasswordHasher.verify(password, credentials.salt(), credentials.passwordHash())) {
            auditDao.log(credentials.user().getId(), "LOGIN_FAILED", "Неверный пароль");
            throw new AuthException("auth.invalid");
        }
        if (credentials.user().isBlocked()) {
            throw new AuthException("auth.blocked");
        }
        auditDao.log(credentials.user().getId(), "LOGIN", "Вход в систему");
        return credentials.user();
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
