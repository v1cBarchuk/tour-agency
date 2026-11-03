package by.bsuir.touragency.client.component;

import by.bsuir.touragency.client.util.Fmt;
import by.bsuir.touragency.client.util.I18n;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;

/** Рейтинг по отзывам: «★ 4,5 · 12 отз.». */
public class RatingLabel extends HBox {

    public RatingLabel(Double rating, int reviews) {
        super(4);
        setAlignment(Pos.CENTER_LEFT);
        getStyleClass().add("rating");
        if (rating == null) {
            Label none = new Label(I18n.t("common.noReviews"));
            none.getStyleClass().add("muted");
            getChildren().add(none);
            return;
        }
        Label star = new Label("★");
        star.getStyleClass().add("rating-star");
        Label value = new Label(Fmt.rating(rating));
        value.getStyleClass().add("rating-value");
        Label count = new Label("· " + I18n.t("common.reviews", reviews));
        count.getStyleClass().add("muted");
        getChildren().addAll(star, value, count);
    }
}
