package by.bsuir.touragency.client.component;

import javafx.scene.control.Label;

/** Цветная метка: «Горящий», «Новинка», статус и т.п. */
public class Badge extends Label {

    public enum Kind { HOT, NEW, POPULAR, FEATURED, NEUTRAL, SUCCESS, WARNING, DANGER }

    public Badge(String text, Kind kind) {
        super(text);
        getStyleClass().addAll("badge", "badge-" + kind.name().toLowerCase());
    }
}
