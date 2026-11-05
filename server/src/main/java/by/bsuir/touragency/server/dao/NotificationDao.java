package by.bsuir.touragency.server.dao;

import by.bsuir.touragency.common.model.Lang;
import by.bsuir.touragency.common.model.Notification;

import java.util.List;

public interface NotificationDao {

    void create(int userId, String type, Integer tourId, Integer bookingId, String message);

    /** Клиенты, добавившие тур в избранное и согласные получать уведомления о снижении цены. */
    List<Integer> findPriceDropSubscribers(int tourId);

    List<Notification> findByUser(int userId, Lang lang);

    /** Отметить прочитанными: одно уведомление или все (id == null). */
    void markRead(int userId, Integer notificationId);
}
