package by.bsuir.touragency.server.dao;

import by.bsuir.touragency.common.model.Lang;
import by.bsuir.touragency.common.model.Role;
import by.bsuir.touragency.common.model.User;
import by.bsuir.touragency.server.db.DataAccessException;
import by.bsuir.touragency.server.db.PooledConnection;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.Optional;

public class JdbcUserDao extends BaseDao implements UserDao {

    private static final String SELECT_FIELDS =
            "SELECT user_id, role_id, login, password_hash, salt, full_name, email, phone, "
                    + "is_blocked, language, created_at FROM users ";

    @Override
    public Optional<UserCredentials> findCredentialsByLogin(String login) {
        try (PooledConnection pc = connection();
             PreparedStatement ps = pc.get().prepareStatement(SELECT_FIELDS + "WHERE login = ?")) {
            ps.setString(1, login);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(new UserCredentials(
                            mapUser(rs), rs.getString("password_hash"), rs.getString("salt")));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new DataAccessException("Ошибка поиска пользователя", e);
        }
    }

    @Override
    public Optional<User> findById(int id) {
        try (PooledConnection pc = connection();
             PreparedStatement ps = pc.get().prepareStatement(SELECT_FIELDS + "WHERE user_id = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapUser(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DataAccessException("Ошибка поиска пользователя", e);
        }
    }

    @Override
    public boolean existsByLogin(String login) {
        return exists("SELECT 1 FROM users WHERE login = ?", login);
    }

    @Override
    public boolean existsByEmail(String email) {
        return exists("SELECT 1 FROM users WHERE email = ?", email);
    }

    @Override
    public int create(User user, String passwordHash, String salt) {
        String sql = "INSERT INTO users (role_id, login, password_hash, salt, full_name, email, phone, language) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (PooledConnection pc = connection();
             PreparedStatement ps = pc.get().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, user.getRole().getId());
            ps.setString(2, user.getLogin());
            ps.setString(3, passwordHash);
            ps.setString(4, salt);
            ps.setString(5, user.getFullName());
            ps.setString(6, user.getEmail());
            ps.setString(7, user.getPhone());
            ps.setString(8, user.getLanguage().name());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return keys.getInt(1);
            }
        } catch (SQLException e) {
            throw new DataAccessException("Ошибка сохранения пользователя", e);
        }
    }

    @Override
    public void updateLanguage(int userId, Lang language) {
        try (PooledConnection pc = connection();
             PreparedStatement ps = pc.get().prepareStatement("UPDATE users SET language = ? WHERE user_id = ?")) {
            ps.setString(1, language.name());
            ps.setInt(2, userId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Ошибка изменения языка", e);
        }
    }

    @Override
    public java.util.List<User> findAll() {
        java.util.List<User> users = new java.util.ArrayList<>();
        try (PooledConnection pc = connection();
             PreparedStatement ps = pc.get().prepareStatement(SELECT_FIELDS.replace("FROM users ",
                     ", (SELECT COUNT(*) FROM bookings b WHERE b.user_id = users.user_id) bookings_count, "
                     + "(SELECT MAX(a.created_at) FROM audit_log a WHERE a.user_id = users.user_id "
                     + "AND a.action = 'LOGIN') last_login FROM users ") + "ORDER BY role_id, full_name");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                User u = mapUser(rs);
                u.setBookingsCount(rs.getInt("bookings_count"));
                Timestamp last = rs.getTimestamp("last_login");
                if (last != null) u.setLastLogin(last.toLocalDateTime());
                users.add(u);
            }
            return users;
        } catch (SQLException e) {
            throw new DataAccessException("Ошибка чтения пользователей", e);
        }
    }

    @Override
    public void updateRole(int userId, Role role) {
        simpleUpdate("UPDATE users SET role_id = ? WHERE user_id = ?", role.getId(), userId);
    }

    @Override
    public void setBlocked(int userId, boolean blocked) {
        simpleUpdate("UPDATE users SET is_blocked = ? WHERE user_id = ?", blocked, userId);
    }

    private void simpleUpdate(String sql, Object a, Object b) {
        try (PooledConnection pc = connection();
             PreparedStatement ps = pc.get().prepareStatement(sql)) {
            ps.setObject(1, a);
            ps.setObject(2, b);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Ошибка изменения пользователя", e);
        }
    }

    private boolean exists(String sql, String value) {
        try (PooledConnection pc = connection();
             PreparedStatement ps = pc.get().prepareStatement(sql)) {
            ps.setString(1, value);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new DataAccessException("Ошибка проверки данных пользователя", e);
        }
    }

    private static User mapUser(ResultSet rs) throws SQLException {
        User user = new User(
                rs.getInt("user_id"),
                rs.getString("login"),
                rs.getString("full_name"),
                rs.getString("email"),
                Role.fromId(rs.getInt("role_id")));
        user.setPhone(rs.getString("phone"));
        user.setBlocked(rs.getBoolean("is_blocked"));
        user.setLanguage(Lang.parse(rs.getString("language")));
        Timestamp created = rs.getTimestamp("created_at");
        if (created != null) {
            user.setCreatedAt(created.toLocalDateTime());
        }
        return user;
    }
}
