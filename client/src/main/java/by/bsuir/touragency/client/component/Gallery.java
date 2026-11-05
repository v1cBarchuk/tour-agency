package by.bsuir.touragency.client.component;

import by.bsuir.touragency.client.util.Images;
import javafx.animation.FadeTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;

/**
 * Галерея фото отеля: стрелки «назад/вперёд», точки-индикаторы и счётчик «2 / 4».
 * Пока файла фото нет, вместо него показывается цветная заглушка с подписью.
 */
public class Gallery extends StackPane {

    private final List<String> photos;
    private final String caption;
    private final double width;
    private final double height;
    private final double radius;
    private final StackPane frame = new StackPane();
    private final HBox dots = new HBox(6);
    private final Label counter = new Label();
    private int index;

    public Gallery(List<String> paths, String caption, double width, double height, double radius) {
        this.photos = new ArrayList<>(paths.isEmpty() ? List.of("") : paths);
        this.caption = caption;
        this.width = width;
        this.height = height;
        this.radius = radius;
        getStyleClass().add("gallery");
        setMaxSize(Double.MAX_VALUE, height);
        setMinHeight(height);
        setPrefHeight(height);
        getChildren().add(frame);
        if (photos.size() > 1) {
            Button prev = arrow("‹", -1);
            Button next = arrow("›", 1);
            StackPane.setAlignment(prev, Pos.CENTER_LEFT);
            StackPane.setAlignment(next, Pos.CENTER_RIGHT);
            StackPane.setMargin(prev, new Insets(0, 0, 0, 12));
            StackPane.setMargin(next, new Insets(0, 12, 0, 0));
            dots.setAlignment(Pos.CENTER);
            dots.setPickOnBounds(false);
            dots.setFillHeight(false);
            dots.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
            StackPane.setAlignment(dots, Pos.BOTTOM_CENTER);
            StackPane.setMargin(dots, new Insets(0, 0, 14, 0));
            counter.getStyleClass().add("gallery-counter");
            StackPane.setAlignment(counter, Pos.BOTTOM_RIGHT);
            StackPane.setMargin(counter, new Insets(0, 14, 12, 0));
            getChildren().addAll(prev, next, dots, counter);
            setFocusTraversable(true);
            setOnKeyPressed(e -> {
                if (e.getCode() == KeyCode.LEFT) show(index - 1);
                if (e.getCode() == KeyCode.RIGHT) show(index + 1);
            });
        }
        show(0);
    }

    private Button arrow(String text, int step) {
        Button b = new Button(text);
        b.getStyleClass().add("gallery-arrow");
        b.setOnAction(e -> show(index + step));
        return b;
    }

    private void show(int i) {
        index = Math.floorMod(i, photos.size());
        String path = photos.get(index);
        Node image = new FluidCover(path.isEmpty() ? null : path, caption, height, radius);
        frame.getChildren().setAll(image);
        FadeTransition fade = new FadeTransition(Duration.millis(220), image);
        fade.setFromValue(0.35);
        fade.setToValue(1);
        fade.play();
        dots.getChildren().clear();
        for (int k = 0; k < photos.size(); k++) {
            Region dot = new Region();
            dot.setMinSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
            dot.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
            dot.getStyleClass().addAll("gallery-dot", k == index ? "gallery-dot-active" : "gallery-dot-idle");
            int target = k;
            dot.setOnMouseClicked(e -> show(target));
            dots.getChildren().add(dot);
        }
        counter.setText((index + 1) + " / " + photos.size());
    }
}
