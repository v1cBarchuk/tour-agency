package by.bsuir.touragency.client.component;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

/** Заглушка для пустого списка или раздела, который ещё не готов. */
public class EmptyState extends VBox {

    public EmptyState(String icon, String title, String hint) {
        super(6);
        setAlignment(Pos.CENTER);
        getStyleClass().add("empty-state");
        Label i = new Label(icon);
        i.getStyleClass().add("empty-icon");
        Label t = new Label(title);
        t.getStyleClass().add("empty-title");
        Label h = new Label(hint);
        h.getStyleClass().add("muted");
        h.setWrapText(true);
        getChildren().addAll(i, t, h);
    }

    /** Кнопка-подсказка «что сделать дальше» (например, «Изменить анкету»). */
    public EmptyState action(String text, Runnable onClick) {
        javafx.scene.control.Button b = new javafx.scene.control.Button(text);
        b.getStyleClass().add("btn-primary");
        b.setOnAction(e -> onClick.run());
        javafx.scene.layout.VBox.setMargin(b, new javafx.geometry.Insets(8, 0, 0, 0));
        getChildren().add(b);
        return this;
    }
}
