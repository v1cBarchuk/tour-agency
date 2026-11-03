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
}
