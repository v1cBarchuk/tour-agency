package by.bsuir.touragency.server.service;

import by.bsuir.touragency.common.model.BookingStatus;
import by.bsuir.touragency.common.model.Lang;
import by.bsuir.touragency.common.model.MatchResult;
import by.bsuir.touragency.common.model.Notification;
import by.bsuir.touragency.common.model.Preferences;
import by.bsuir.touragency.common.model.Tour;
import by.bsuir.touragency.server.dao.NotificationDao;
import by.bsuir.touragency.server.dao.PreferenceDao;
import by.bsuir.touragency.server.dao.SettingsDao;
import by.bsuir.touragency.server.service.event.BookingEventListener;
import by.bsuir.touragency.server.service.event.TourEventListener;

import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Уведомления клиентов — подписчик событий (паттерн Observer):
 * новый тур сверяется с анкетами всех клиентов, о смене статуса брони сообщается её владельцу.
 */
public class NotificationService implements TourEventListener, BookingEventListener {

    private static final Logger LOG = Logger.getLogger(NotificationService.class.getName());

    private final NotificationDao notificationDao;
    private final PreferenceDao preferenceDao;
    private final SettingsDao settings;

    public NotificationService(NotificationDao notificationDao, PreferenceDao preferenceDao, SettingsDao settings) {
        this.notificationDao = notificationDao;
        this.preferenceDao = preferenceDao;
        this.settings = settings;
    }

    @Override
    public void tourCreated(Tour tour) {
        int threshold = settings.getInt("match.notify.percent", 60);
        int sent = 0;
        for (Map.Entry<Integer, Preferences> e : preferenceDao.findAllClients().entrySet()) {
            if (e.getValue().isEmpty()) continue;
            MatchResult r = TourMatcher.evaluate(e.getValue(), tour, TourMatcher.ClientContext.empty(), null);
            if (r.getPercent() >= threshold) {
                notificationDao.create(e.getKey(), "NEW_MATCHING_TOUR", tour.getId(), null,
                        String.valueOf(r.getPercent()));
                sent++;
            }
        }
        LOG.info("Новый тур #" + tour.getId() + ": уведомлено клиентов — " + sent);
    }

    @Override
    public void statusChanged(int bookingId, int clientId, BookingStatus status, Integer actorId) {
        // клиент сам создал или оплатил бронь — уведомлять его об этом не нужно
        if (actorId != null && actorId == clientId && (status == BookingStatus.NEW)) {
            return;
        }
        notificationDao.create(clientId, "BOOKING_STATUS", null, bookingId, status.name());
    }

    public List<Notification> list(int userId, Lang lang) {
        return notificationDao.findByUser(userId, lang);
    }

    public void markRead(int userId, Integer notificationId) {
        notificationDao.markRead(userId, notificationId);
    }
}
