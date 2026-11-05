package by.bsuir.touragency.client.component;

import by.bsuir.touragency.client.util.Fmt;
import by.bsuir.touragency.client.util.I18n;
import by.bsuir.touragency.client.util.Images;
import by.bsuir.touragency.common.model.Country;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Label;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.function.Consumer;

/** Карточка направления: большое фото с затемнением и подписью. */
public class CountryCard extends StackPane {

    public CountryCard(Country country, double width, double height, Consumer<Country> onOpen) {
        getStyleClass().addAll("card", "country-card", "grid-item");
        setPrefSize(width, height);
        setMinHeight(height);
        setMaxSize(Double.MAX_VALUE, height);
        setCursor(Cursor.HAND);

        Region shade = new Region();
        shade.getStyleClass().add("country-shade");
        shade.setMaxSize(Double.MAX_VALUE, height);

        Label name = new Label(country.getName());
        name.getStyleClass().add("country-name");
        Label stats = new Label(I18n.t("home.toursCount", country.getToursCount())
                + (country.getMinPrice() != null ? " · " + I18n.t("tour.priceFrom", Fmt.money(country.getMinPrice())) : ""));
        stats.getStyleClass().add("country-stats");
        VBox text = new VBox(2, name, stats);
        text.setPadding(new Insets(16));
        text.setAlignment(Pos.BOTTOM_LEFT);
        text.setMaxSize(Double.MAX_VALUE, height);

        getChildren().addAll(new FluidCover(country.getImagePath(), null, height, 16), shade, text);
        setOnMouseClicked(e -> onOpen.accept(country));
    }
}
