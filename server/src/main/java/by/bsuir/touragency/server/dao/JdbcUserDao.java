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
                    + "is_blocked, language, created_at, birth_date, home_city, notify_price_drop FROM users ";

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
                     + "AND a.action = 'LOGIN') last_login FROM users ") + "ORDER BY role_id, full_name, user_id");
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
    public void updateProfile(User u) {
        update("UPDATE users SET full_name = ?, email = ?, phone = ?, birth_date = ?, home_city = ?, "
                + "notify_price_drop = ? WHERE user_id = ?", u.getFullName(), u.getEmail(), u.getPhone(),
                u.getBirthDate(), u.getHomeCity(), u.isNotifyPriceDrop(), u.getId());
    }

    @Override
    public by.bsuir.touragency.common.model.ProfileStats profileStats(int userId) {
        return query("SELECT "
                + "(SELECT COUNT(*) FROM bookings WHERE user_id = ? AND status_id <> 7) bookings, "
                + "(SELECT COUNT(*) FROM bookings b JOIN tours t ON t.tour_id = b.tour_id WHERE b.user_id = ? "
                + "   AND b.status_id IN (3, 4, 5) AND t.start_date >= CURRENT_DATE) upcoming, "
                + "(SELECT COUNT(*) FROM bookings WHERE user_id = ? AND status_id = 6) completed, "
                + "(SELECT COALESCE(SUM(p.amount), 0) FROM payments p JOIN bookings b ON b.booking_id = p.booking_id "
                + "   WHERE b.user_id = ?) spent, "
                + "(SELECT COUNT(*) FROM favorites WHERE user_id = ?) favorites, "
                + "(SELECT COUNT(*) FROM reviews r JOIN bookings b ON b.booking_id = r.booking_id WHERE b.user_id = ?) reviews, "
                + "(SELECT COUNT(DISTINCT ci.country_id) FROM bookings b JOIN tours t ON t.tour_id = b.tour_id "
                + "   JOIN hotels h ON h.hotel_id = t.hotel_id JOIN cities ci ON ci.city_id = h.city_id "
                + "   WHERE b.user_id = ? AND b.status_id = 6) countries",
                rs -> new by.bsuir.touragency.common.model.ProfileStats(rs.getInt("bookings"), rs.getInt("upcoming"),
                        rs.getInt("completed"), rs.getBigDecimal("spent"), rs.getInt("favorites"),
                        rs.getInt("reviews"), rs.getInt("countries")),
                userId, userId, userId, userId, userId, userId, userId).get(0);
    }

    @Override
    public void updatePassword(int userId, String passwordHash, String salt) {
        update("UPDATE users SET password_hash = ?, salt = ? WHERE user_id = ?", passwordHash, salt, userId);
    }

    @Override
    public boolean emailTakenByOther(String email, int userId) {
        return !query("SELECT 1 FROM users WHERE email = ? AND user_id <> ?", rs -> 1, email, userId).isEmpty();
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
        java.sql.Date birth = rs.getDate("birth_date");
        if (birth != null) user.setBirthDate(birth.toLocalDate());
        user.setHomeCity(rs.getString("home_city"));
        user.setNotifyPriceDrop(rs.getBoolean("notify_price_drop"));
        Timestamp created = rs.getTimestamp("created_at");
        if (created != null) {
            user.setCreatedAt(created.toLocalDateTime());
        }
        return user;
    }
}
