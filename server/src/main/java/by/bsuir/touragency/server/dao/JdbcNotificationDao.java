package by.bsuir.touragency.server.dao;

import by.bsuir.touragency.common.model.Lang;
import by.bsuir.touragency.common.model.Notification;

import java.util.List;

public class JdbcNotificationDao extends BaseDao implements NotificationDao {

    @Override
    public void create(int userId, String type, Integer tourId, Integer bookingId, String message) {
        update("INSERT INTO notifications (user_id, type, tour_id, booking_id, message) VALUES (?, ?, ?, ?, ?)",
                userId, type, tourId, bookingId, message);
    }

    @Override
    public List<Integer> findPriceDropSubscribers(int tourId) {
        return query("SELECT f.user_id FROM favorites f JOIN users u ON u.user_id = f.user_id "
                + "WHERE f.tour_id = ? AND u.notify_price_drop = TRUE AND u.is_blocked = FALSE",
                rs -> rs.getInt("user_id"), tourId);
    }

    @Override
    public List<Notification> findByUser(int userId, Lang lang) {
        return query("SELECT n.*, COALESCE(t." + lang.col("title") + ", t.title_ru) tour_title FROM notifications n "
                        + "LEFT JOIN tours t ON t.tour_id = n.tour_id WHERE n.user_id = ? "
                        + "ORDER BY n.created_at DESC, n.notification_id DESC LIMIT 100",
                rs -> new Notification(rs.getInt("notification_id"), rs.getString("type"),
                        getInteger(rs, "tour_id"), rs.getString("tour_title"), getInteger(rs, "booking_id"),
                        rs.getString("message"), rs.getBoolean("is_read"), getDateTime(rs, "created_at")), userId);
    }

    @Override
    public void markRead(int userId, Integer notificationId) {
        if (notificationId == null) {
            update("UPDATE notifications SET is_read = TRUE WHERE user_id = ?", userId);
        } else {
            update("UPDATE notifications SET is_read = TRUE WHERE user_id = ? AND notification_id = ?",
                    userId, notificationId);
        }
    }
}
