package by.bsuir.touragency.client.component;

import by.bsuir.touragency.client.util.I18n;
import by.bsuir.touragency.client.util.Theme;
import javafx.scene.control.Button;
import javafx.scene.control.Tooltip;

/** Кнопка переключения светлой и тёмной темы (рядом с RU / EN). */
public class ThemeSwitch extends Button {

    public ThemeSwitch() {
        getStyleClass().add("theme-switch");
        update();
        setOnAction(e -> {
            Theme.toggle();
            update();
        });
    }

    private void update() {
        boolean dark = Theme.isDark();
        setText(dark ? "☀" : "☾");
        setTooltip(new Tooltip(I18n.t(dark ? "theme.light" : "theme.dark")));
    }
}
