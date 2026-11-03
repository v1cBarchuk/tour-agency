package by.bsuir.touragency.client.util;

import by.bsuir.touragency.common.model.Lang;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.prefs.Preferences;

/**
 * Локализация интерфейса: тексты из i18n/messages_ru|en.properties.
 * При смене языка уведомляются подписчики (Observer) — главное окно перестраивается.
 */
public final class I18n {

    private static final Preferences PREFS = Preferences.userNodeForPackage(I18n.class);
    private static final List<Runnable> LISTENERS = new ArrayList<>();

    private static Lang lang = Lang.parse(PREFS.get("lang", "RU"));
    private static ResourceBundle bundle = load(lang);

    static {
        applyFormatLocale(lang);
    }

    /** Форматы дат и чисел стандартных компонентов (DatePicker) — по языку интерфейса. */
    private static void applyFormatLocale(Lang l) {
        java.util.Locale.setDefault(java.util.Locale.Category.FORMAT,
                l == Lang.RU ? java.util.Locale.of("ru", "BY") : java.util.Locale.UK);
    }

    private I18n() {
    }

    private static ResourceBundle load(Lang l) {
        return ResourceBundle.getBundle("i18n.messages", l.getLocale());
    }

    public static Lang getLang() {
        return lang;
    }

    public static void setLang(Lang newLang) {
        if (newLang == lang) {
            return;
        }
        lang = newLang;
        bundle = load(newLang);
        applyFormatLocale(newLang);
        PREFS.put("lang", newLang.name());
        new ArrayList<>(LISTENERS).forEach(Runnable::run);
    }

    public static void addListener(Runnable listener) {
        LISTENERS.add(listener);
    }

    public static void removeListener(Runnable listener) {
        LISTENERS.remove(listener);
    }

    /** Текст по ключу. */
    public static String t(String key) {
        try {
            return bundle.getString(key);
        } catch (MissingResourceException e) {
            return key;
        }
    }

    // Перегрузка: текст с подстановкой параметров {0}, {1}…
    public static String t(String key, Object... args) {
        return new MessageFormat(t(key), lang.getLocale()).format(args);
    }
}
