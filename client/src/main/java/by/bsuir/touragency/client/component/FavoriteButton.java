package by.bsuir.touragency.client.component;

import by.bsuir.touragency.client.util.Alerts;
import by.bsuir.touragency.client.util.Async;
import by.bsuir.touragency.client.util.I18n;
import by.bsuir.touragency.common.model.Tour;
import by.bsuir.touragency.common.protocol.CommandType;
import by.bsuir.touragency.common.protocol.Request;
import javafx.scene.Cursor;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;

/** Кнопка-сердечко «В избранное». Щелчок не открывает карточку тура. */
public class FavoriteButton extends Label {

    private final Tour tour;

    public FavoriteButton(Tour tour) {
        this.tour = tour;
        getStyleClass().add("fav-button");
        setCursor(Cursor.HAND);
        render();
        setOnMouseClicked(e -> {
            e.consume();
            Async.send(new Request(CommandType.TOGGLE_FAVORITE).with("tourId", tour.getId()), response -> {
                if (response.isOk()) {
                    tour.setFavorite(response.<Boolean>getData());
                    render();
                } else {
                    Alerts.error(response.getMessage());
                }
            });
        });
    }

    private void render() {
        setText(tour.isFavorite() ? "♥" : "♡");
        getStyleClass().remove("fav-on");
        if (tour.isFavorite()) getStyleClass().add("fav-on");
        setTooltip(new Tooltip(I18n.t(tour.isFavorite() ? "fav.remove" : "fav.add")));
    }
}
