package by.bsuir.touragency.client.component;

import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

/**
 * «Скелетон» — серая пульсирующая карточка-заглушка, которая показывается, пока данные
 * загружаются с сервера. Повторяет форму настоящей карточки, поэтому страница не «прыгает».
 */
public final class Skeleton {

    private Skeleton() {
    }

    /** Карточка: блок под фото и несколько строк текста. */
    public static VBox card(double width, double imageHeight) {
        VBox card = new VBox(10, block(width, imageHeight), line(width * 0.45), line(width * 0.8), line(width * 0.6));
        card.getStyleClass().addAll("card", "skeleton-card", "grid-item");
        card.setPrefWidth(width);
        card.setMaxWidth(Double.MAX_VALUE);
        ((Region) card.getChildren().get(0)).setMaxWidth(Double.MAX_VALUE);
        pulse(card);
        return card;
    }

    /** Широкий блок (например, под фото или под карточку бронирования). */
    public static Region block(double width, double height) {
        Region r = new Region();
        r.getStyleClass().add("skeleton");
        r.setPrefSize(width, height);
        r.setMaxSize(width, height);
        return r;
    }

    private static Region line(double width) {
        Region r = block(width, 12);
        r.getStyleClass().add("skeleton-line");
        return r;
    }

    /** Заполнить контейнер n карточками-заглушками. */
    public static void fill(Pane pane, int count, double width, double imageHeight) {
        pane.getChildren().clear();
        for (int i = 0; i < count; i++) pane.getChildren().add(card(width, imageHeight));
    }

    private static void pulse(Region node) {
        FadeTransition t = new FadeTransition(Duration.millis(750), node);
        t.setFromValue(1);
        t.setToValue(0.45);
        t.setAutoReverse(true);
        t.setCycleCount(Animation.INDEFINITE);
        t.play();
        node.sceneProperty().addListener((o, old, scene) -> { if (scene == null) t.stop(); });
    }
}
