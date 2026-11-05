package by.bsuir.touragency.client.component;

import by.bsuir.touragency.client.util.Images;
import javafx.scene.layout.StackPane;

/**
 * Фото фиксированной высоты, которое растягивается на всю ширину контейнера
 * (обрезка по центру и скругление как у {@link Images#cover}).
 */
public class FluidCover extends StackPane {

    private final String path;
    private final String caption;
    private final double height;
    private final double radius;
    private double builtFor = -1;

    public FluidCover(String path, String caption, double height, double radius) {
        this.path = path;
        this.caption = caption;
        this.height = height;
        this.radius = radius;
        setMinHeight(height);
        setPrefHeight(height);
        setMaxHeight(height);
        setMinWidth(0);
        setPrefWidth(200);
        setMaxWidth(Double.MAX_VALUE);
    }

    @Override
    protected void layoutChildren() {
        double w = Math.floor(getWidth());
        if (w > 0 && Math.abs(w - builtFor) >= 1) {
            builtFor = w;
            getChildren().setAll(Images.cover(path, caption, w, height, radius));
        }
        super.layoutChildren();
    }
}
