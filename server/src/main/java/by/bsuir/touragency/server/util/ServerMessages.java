package by.bsuir.touragency.server.util;

import by.bsuir.touragency.common.exception.AppException;
import by.bsuir.touragency.common.model.Lang;

import java.text.MessageFormat;
import java.util.EnumMap;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

/**
 * Тексты сообщений сервера на языке клиента (ресурсы i18n/server_*.properties).
 */
public final class ServerMessages {

    private static final Map<Lang, ResourceBundle> BUNDLES = new EnumMap<>(Lang.class);

    static {
        for (Lang lang : Lang.values()) {
            BUNDLES.put(lang, ResourceBundle.getBundle("i18n.server", lang.getLocale()));
        }
    }

    private ServerMessages() {
    }

    public static String get(Lang lang, String key, Object... args) {
        String pattern;
        try {
            pattern = BUNDLES.get(lang).getString(key);
        } catch (MissingResourceException e) {
            return key;
        }
        Object[] translated = new Object[args.length];
        for (int i = 0; i < args.length; i++) {
            Object arg = args[i];
            translated[i] = arg instanceof String s && s.startsWith("field.") ? get(lang, s) : arg;
        }
        return args.length == 0 ? pattern : new MessageFormat(pattern, lang.getLocale()).format(translated);
    }

    // Перегрузка: текст для исключения приложения
    public static String get(Lang lang, AppException e) {
        return get(lang, e.getMessageKey(), e.getArgs());
    }
}
