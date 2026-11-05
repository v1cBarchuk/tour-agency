package by.bsuir.touragency.client.component;

import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

import java.util.Collection;

/**
 * Горизонтальная карусель карточек: стрелки «‹ ›» листают ленту примерно на ширину экрана
 * с плавной анимацией; Shift + колесо или жест тачпада прокручивают её по горизонтали.
 */
public class Carousel extends StackPane {

    private final HBox track = new HBox();
    private final ScrollPane scroll = new ScrollPane(track);
    private final Button prev = arrow("‹");
    private final Button next = arrow("›");
    private final double itemWidth;
    private Timeline animation;

    public Carousel(double itemWidth, double gap) {
        this.itemWidth = itemWidth;
        getStyleClass().add("carousel");
        track.setSpacing(gap);
        track.setPadding(new Insets(4, 2, 14, 2));          // место под тень карточек
        track.setAlignment(Pos.TOP_LEFT);
        scroll.getStyleClass().add("carousel-scroll");
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setFitToHeight(true);
        scroll.setPannable(true);
        // обычное колесо прокручивает страницу; Shift + колесо (или жест тачпада) — ленту
        scroll.addEventFilter(ScrollEvent.SCROLL, e -> {
            if (overflow() <= 0) return;
            double delta = e.isShiftDown() ? (e.getDeltaX() != 0 ? e.getDeltaX() : e.getDeltaY()) : e.getDeltaX();
            if (delta == 0) {
                // вертикальная прокрутка: передаём странице, а не ленте
                e.consume();
                if (getParent() != null) getParent().fireEvent(e.copyFor(getParent(), getParent()));
                return;
            }
            scroll.setHvalue(clamp(scroll.getHvalue() - delta / overflow()));
            e.consume();
        });
        prev.setOnAction(e -> page(-1));
        next.setOnAction(e -> page(1));
        StackPane.setAlignment(prev, Pos.CENTER_LEFT);
        StackPane.setAlignment(next, Pos.CENTER_RIGHT);
        StackPane.setMargin(prev, new Insets(0, 0, 0, -8));
        StackPane.setMargin(next, new Insets(0, -8, 0, 0));
        getChildren().addAll(scroll, prev, next);
        scroll.hvalueProperty().addListener((o, a, b) -> updateArrows());
        scroll.viewportBoundsProperty().addListener((o, a, b) -> updateArrows());
        track.widthProperty().addListener((o, a, b) -> updateArrows());
        updateArrows();
    }

    private static Button arrow(String text) {
        Button b = new Button(text);
        b.getStyleClass().addAll("gallery-arrow", "carousel-arrow");
        b.setFocusTraversable(false);
        return b;
    }

    public Carousel set(Collection<? extends Node> items) {
        track.getChildren().clear();
        items.forEach(this::add);
        scroll.setHvalue(0);
        return this;
    }

    public void add(Node item) {
        if (item instanceof Region r && !item.getStyleClass().contains("empty-state")) {
            r.setMinWidth(itemWidth);
            r.setPrefWidth(itemWidth);
            r.setMaxWidth(itemWidth);
        }
        track.getChildren().add(item);
    }

    public void clear() {
        track.getChildren().clear();
    }

    public boolean isEmpty() {
        return track.getChildren().isEmpty();
    }

    private double overflow() {
        return track.getWidth() - scroll.getViewportBounds().getWidth();
    }

    private void page(int direction) {
        double over = overflow();
        if (over <= 0) return;
        double step = Math.max(itemWidth, scroll.getViewportBounds().getWidth() * 0.85) / over;
        double target = clamp(scroll.getHvalue() + direction * step);
        if (animation != null) animation.stop();
        animation = new Timeline(new KeyFrame(Duration.millis(320),
                new KeyValue(scroll.hvalueProperty(), target, Interpolator.EASE_BOTH)));
        animation.play();
    }

    private void updateArrows() {
        boolean scrollable = overflow() > 1;
        prev.setVisible(scrollable && scroll.getHvalue() > 0.001);
        next.setVisible(scrollable && scroll.getHvalue() < 0.999);
    }

    private static double clamp(double v) {
        return Math.max(0, Math.min(1, v));
    }
}
