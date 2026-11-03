package by.bsuir.touragency.server.dao;

import by.bsuir.touragency.common.model.AuditEntry;
import by.bsuir.touragency.server.db.DataAccessException;
import by.bsuir.touragency.server.db.PooledConnection;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

public class JdbcAuditDao extends BaseDao implements AuditDao {

    @Override
    public List<AuditEntry> find(AuditFilter f) {
        StringBuilder sql = new StringBuilder("SELECT a.*, u.full_name, u.login FROM audit_log a "
                + "LEFT JOIN users u ON u.user_id = a.user_id WHERE 1 = 1 ");
        List<Object> p = new ArrayList<>();
        if (f.userId() != null) {
            sql.append("AND a.user_id = ? ");
            p.add(f.userId());
        }
        if (f.action() != null && !f.action().isBlank()) {
            sql.append("AND a.action = ? ");
            p.add(f.action());
        }
        if (f.objectType() != null && !f.objectType().isBlank()) {
            sql.append("AND a.object_type = ? ");
            p.add(f.objectType());
        }
        if (f.from() != null) {
            sql.append("AND DATE(a.created_at) >= ? ");
            p.add(f.from());
        }
        if (f.to() != null) {
            sql.append("AND DATE(a.created_at) <= ? ");
            p.add(f.to());
        }
        sql.append("ORDER BY a.created_at DESC, a.log_id DESC LIMIT 500");
        return query(sql.toString(), rs -> new AuditEntry(rs.getLong("log_id"), getInteger(rs, "user_id"),
                rs.getString("full_name"), rs.getString("login"), rs.getString("action"),
                rs.getString("object_type"), getInteger(rs, "object_id"), rs.getString("details"),
                getDateTime(rs, "created_at")), p.toArray());
    }

    @Override
    public void log(Integer userId, String action, String objectType, Integer objectId, String details) {
        String sql = "INSERT INTO audit_log (user_id, action, object_type, object_id, details) "
                + "VALUES (?, ?, ?, ?, ?)";
        try (PooledConnection pc = connection();
             PreparedStatement ps = pc.get().prepareStatement(sql)) {
            setNullableInt(ps, 1, userId);
            ps.setString(2, action);
            ps.setString(3, objectType);
            setNullableInt(ps, 4, objectId);
            ps.setString(5, details);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Ошибка записи в журнал", e);
        }
    }

    private static void setNullableInt(PreparedStatement ps, int index, Integer value) throws SQLException {
        if (value == null) {
            ps.setNull(index, Types.INTEGER);
        } else {
            ps.setInt(index, value);
        }
    }
}
