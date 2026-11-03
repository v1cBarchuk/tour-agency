package by.bsuir.touragency.server.dao;

import by.bsuir.touragency.common.model.Preferences;

import java.util.Map;
import java.util.Optional;

public interface PreferenceDao {

    Optional<Preferences> find(int userId);

    void save(int userId, Preferences preferences);

    /** Анкеты всех активных клиентов: для рассылки уведомлений о новых турах. */
    Map<Integer, Preferences> findAllClients();
}
