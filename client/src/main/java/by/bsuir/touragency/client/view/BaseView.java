package by.bsuir.touragency.client.view;

import by.bsuir.touragency.client.util.I18n;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

/**
 * Базовый класс всех экранов и разделов клиента.
 */
public abstract class BaseView {

    private Parent root;

    /** Строит содержимое экрана. Вызывается один раз при первом показе. */
    protected abstract Parent build();

    public abstract String getTitle();

    public final Parent getRoot() {
        if (root == null) {
            root = build();
        }
        return root;
    }

    /**
     * Новый экземпляр экрана с тем же состоянием — используется при смене языка,
     * чтобы все тексты построились заново.
     */
    public BaseView fresh() {
        return this;
    }

    protected static String t(String key) {
        return I18n.t(key);
    }

    protected static String t(String key, Object... args) {
        return I18n.t(key, args);
    }

    protected static Label heading(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("heading");
        return label;
    }

    /** Заголовок раздела с подзаголовком. */
    protected static VBox pageHeader(String title, String subtitle) {
        Label h = new Label(title);
        h.getStyleClass().add("page-title");
        VBox box = new VBox(2, h);
        if (subtitle != null) {
            Label s = new Label(subtitle);
            s.getStyleClass().add("page-subtitle");
            box.getChildren().add(s);
        }
        return box;
    }
}
