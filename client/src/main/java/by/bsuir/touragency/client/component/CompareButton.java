package by.bsuir.touragency.client.component;

import by.bsuir.touragency.client.util.Alerts;
import by.bsuir.touragency.client.util.CompareList;
import by.bsuir.touragency.client.util.I18n;
import by.bsuir.touragency.common.model.Tour;
import javafx.collections.ListChangeListener;
import javafx.scene.Cursor;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;

/** Кнопка «Сравнить» на карточке тура. Щелчок не открывает карточку. */
public class CompareButton extends Label {

    private final Tour tour;
    private final ListChangeListener<Tour> listener = c -> render();

    public CompareButton(Tour tour) {
        this.tour = tour;
        getStyleClass().add("compare-button");
        setCursor(Cursor.HAND);
        render();
        setOnMouseClicked(e -> {
            e.consume();
            if (!CompareList.toggle(tour)) {
                Alerts.toast(I18n.t("compare.full", CompareList.MAX));
            }
        });
        // подписка только пока кнопка на экране, чтобы старые карточки не копились в памяти
        sceneProperty().addListener((o, old, scene) -> {
            if (scene != null) {
                CompareList.tours().addListener(listener);
                render();
            } else {
                CompareList.tours().removeListener(listener);
            }
        });
    }

    private void render() {
        boolean on = CompareList.contains(tour);
        setText(on ? "✓ ⇄" : "⇄");
        getStyleClass().remove("compare-on");
        if (on) getStyleClass().add("compare-on");
        setTooltip(new Tooltip(I18n.t(on ? "compare.remove" : "compare.add")));
    }
}
