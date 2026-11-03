package by.bsuir.touragency.server.dao;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Настройки из таблицы settings. Кэшируются: читаются при каждом
 * построении карточки тура, а меняются редко.
 */
public class JdbcSettingsDao extends BaseDao implements SettingsDao {

    private final Map<String, String> cache = new ConcurrentHashMap<>();
    private volatile boolean loaded;

    private void ensureLoaded() {
        if (!loaded) {
            synchronized (this) {
                if (!loaded) {
                    query("SELECT setting_key, setting_value FROM settings",
                            rs -> cache.put(rs.getString(1), rs.getString(2)));
                    loaded = true;
                }
            }
        }
    }

    @Override
    public Map<String, String> findAll() {
        ensureLoaded();
        return new LinkedHashMap<>(cache);
    }

    @Override
    public String get(String key, String defaultValue) {
        ensureLoaded();
        return cache.getOrDefault(key, defaultValue);
    }

    @Override
    public void set(String key, String value) {
        update("INSERT INTO settings (setting_key, setting_value) VALUES (?, ?) "
                + "ON DUPLICATE KEY UPDATE setting_value = VALUES(setting_value)", key, value);
        cache.put(key, value);
    }
}
