package by.bsuir.touragency.server.dao;

import java.util.Map;

public interface SettingsDao {

    Map<String, String> findAll();

    String get(String key, String defaultValue);

    // Перегрузка: числовая настройка
    default int getInt(String key, int defaultValue) {
        String value = get(key, null);
        try {
            return value == null ? defaultValue : Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    void set(String key, String value);
}
