package by.bsuir.touragency.server.dao;

import by.bsuir.touragency.common.model.Lang;
import by.bsuir.touragency.common.model.User;

import java.util.Optional;

public interface UserDao {

    Optional<UserCredentials> findCredentialsByLogin(String login);

    Optional<User> findById(int id);

    boolean existsByLogin(String login);

    boolean existsByEmail(String email);

    /** Сохраняет пользователя и возвращает присвоенный идентификатор. */
    int create(User user, String passwordHash, String salt);

    void updateLanguage(int userId, Lang language);

    /** Все пользователи с числом бронирований и датой последнего входа. */
    java.util.List<User> findAll();

    void updateRole(int userId, by.bsuir.touragency.common.model.Role role);

    void setBlocked(int userId, boolean blocked);

    /** Личный кабинет: ФИО, контакты, дата рождения, город вылета, настройки уведомлений. */
    void updateProfile(User user);

    void updatePassword(int userId, String passwordHash, String salt);

    boolean emailTakenByOther(String email, int userId);

    by.bsuir.touragency.common.model.ProfileStats profileStats(int userId);
}
