package by.bsuir.touragency.client.util;

import by.bsuir.touragency.client.view.Navigator;
import javafx.scene.Scene;

import java.util.Objects;
import java.util.prefs.Preferences;

/**
 * Светлая и тёмная тема оформления. Тёмная тема — отдельный файл стилей app-dark.css,
 * который подключается поверх основного и переопределяет только цвета.
 * Выбор сохраняется в настройках пользователя Windows (java.util.prefs).
 */
public final class Theme {

    private static final Preferences PREFS = Preferences.userNodeForPackage(Theme.class);
    private static final String KEY = "theme.dark";
    private static final String BASE = css("/css/app.css");
    private static final String DARK = css("/css/app-dark.css");

    private Theme() {
    }

    private static String css(String path) {
        return Objects.requireNonNull(Theme.class.getResource(path)).toExternalForm();
    }

    public static boolean isDark() {
        return PREFS.getBoolean(KEY, false);
    }

    /** Подключает стили к сцене в соответствии с выбранной темой. */
    public static void apply(Scene scene) {
        scene.getStylesheets().setAll(BASE);
        if (isDark()) scene.getStylesheets().add(DARK);
    }

    public static void toggle() {
        PREFS.putBoolean(KEY, !isDark());
        if (Navigator.getStage() != null && Navigator.getStage().getScene() != null) {
            apply(Navigator.getStage().getScene());
        }
    }
}
