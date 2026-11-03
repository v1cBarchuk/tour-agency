package by.bsuir.touragency.client.component;

import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/** Плитка показателя: подпись, крупное значение и пояснение. */
public class KpiTile extends VBox {

    public KpiTile(String icon, String label, String value, String hint) {
        super(6);
        getStyleClass().addAll("card", "kpi");
        Label i = new Label(icon);
        i.getStyleClass().add("kpi-icon");
        Label l = new Label(label);
        l.getStyleClass().add("kpi-label");
        l.setWrapText(true);
        Label v = new Label(value);
        v.getStyleClass().add("kpi-value");
        getChildren().addAll(new HBox(10, i, l), v);
        if (hint != null) {
            Label h = new Label(hint);
            h.getStyleClass().add("muted");
            getChildren().add(h);
        }
        HBox.setHgrow(this, Priority.ALWAYS);
        setMaxWidth(Double.MAX_VALUE);
        setMinWidth(150);
    }
}
