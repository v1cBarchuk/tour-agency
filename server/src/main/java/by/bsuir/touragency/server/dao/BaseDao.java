package by.bsuir.touragency.server.dao;

import by.bsuir.touragency.server.db.ConnectionPool;
import by.bsuir.touragency.server.db.DataAccessException;
import by.bsuir.touragency.server.db.PooledConnection;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Общая основа JDBC-реализаций DAO: соединение из пула и типовые операции.
 */
public abstract class BaseDao {

    /** Преобразование строки результата в объект. */
    @FunctionalInterface
    protected interface RowMapper<T> {
        T map(ResultSet rs) throws SQLException;
    }

    /** Действие внутри транзакции. */
    @FunctionalInterface
    protected interface TxAction<T> {
        T run(Connection connection) throws SQLException;
    }

    protected PooledConnection connection() {
        return ConnectionPool.getInstance().acquire();
    }

    protected <T> List<T> query(String sql, RowMapper<T> mapper, Object... params) {
        try (PooledConnection pc = connection();
             PreparedStatement ps = pc.get().prepareStatement(sql)) {
            bind(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                List<T> result = new ArrayList<>();
                while (rs.next()) {
                    result.add(mapper.map(rs));
                }
                return result;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Ошибка чтения данных", e);
        }
    }

    protected int update(String sql, Object... params) {
        try (PooledConnection pc = connection();
             PreparedStatement ps = pc.get().prepareStatement(sql)) {
            bind(ps, params);
            return ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Ошибка изменения данных", e);
        }
    }

    /** INSERT с возвратом сгенерированного ключа. */
    protected int insert(String sql, Object... params) {
        try (PooledConnection pc = connection()) {
            return insert(pc.get(), sql, params);
        } catch (SQLException e) {
            throw new DataAccessException("Ошибка сохранения данных", e);
        }
    }

    // Перегрузка: вставка в рамках уже открытой транзакции
    protected static int insert(Connection c, String sql, Object... params) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bind(ps, params);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return keys.getInt(1);
            }
        }
    }

    protected static int update(Connection c, String sql, Object... params) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            bind(ps, params);
            return ps.executeUpdate();
        }
    }

    /** Выполняет действие в одной транзакции: commit при успехе, rollback при ошибке. */
    protected <T> T inTransaction(TxAction<T> action) {
        try (PooledConnection pc = connection()) {
            Connection c = pc.get();
            c.setAutoCommit(false);
            try {
                T result = action.run(c);
                c.commit();
                return result;
            } catch (SQLException | RuntimeException e) {
                c.rollback();
                throw e;
            } finally {
                c.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new DataAccessException("Ошибка транзакции", e);
        }
    }

    protected static void bind(PreparedStatement ps, Object... params) throws SQLException {
        for (int i = 0; i < params.length; i++) {
            Object p = params[i];
            if (p instanceof LocalDate d) {
                ps.setDate(i + 1, Date.valueOf(d));
            } else if (p instanceof LocalDateTime dt) {
                ps.setTimestamp(i + 1, Timestamp.valueOf(dt));
            } else if (p instanceof Enum<?> en) {
                ps.setString(i + 1, en.name());
            } else {
                ps.setObject(i + 1, p);
            }
        }
    }

    protected static Integer getInteger(ResultSet rs, String column) throws SQLException {
        int value = rs.getInt(column);
        return rs.wasNull() ? null : value;
    }

    protected static Double getDouble(ResultSet rs, String column) throws SQLException {
        double value = rs.getDouble(column);
        return rs.wasNull() ? null : value;
    }

    protected static LocalDate getDate(ResultSet rs, String column) throws SQLException {
        Date date = rs.getDate(column);
        return date == null ? null : date.toLocalDate();
    }

    protected static LocalDateTime getDateTime(ResultSet rs, String column) throws SQLException {
        Timestamp ts = rs.getTimestamp(column);
        return ts == null ? null : ts.toLocalDateTime();
    }

    protected static BigDecimal getMoney(ResultSet rs, String column) throws SQLException {
        return rs.getBigDecimal(column);
    }
}
