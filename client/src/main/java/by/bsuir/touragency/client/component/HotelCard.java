package by.bsuir.touragency.client.component;

import by.bsuir.touragency.client.util.Fmt;
import by.bsuir.touragency.client.util.I18n;
import by.bsuir.touragency.client.util.Images;
import by.bsuir.touragency.common.model.Hotel;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/** Карточка отеля: фото, звёзды, рейтинг, удобства, минимальная цена. */
public class HotelCard extends HBox {

    public HotelCard(Hotel hotel) {
        super(16);
        getStyleClass().addAll("card", "hotel-card");
        setPadding(new Insets(12));

        Label location = new Label(hotel.getLocation());
        location.getStyleClass().add("card-overline");
        Label name = new Label(hotel.getName());
        name.getStyleClass().add("card-title");
        Label stars = new Label(Fmt.stars(hotel.getStars()));
        stars.getStyleClass().add("stars");
        Label description = new Label(hotel.getDescription());
        description.setWrapText(true);
        description.getStyleClass().add("card-info");

        FlowPane amenities = new FlowPane(6, 6);
        hotel.getAmenityNames().forEach(a -> amenities.getChildren().add(new Badge(a, Badge.Kind.NEUTRAL)));

        Label price = new Label(hotel.getMinPrice() == null ? I18n.t("dest.noTours")
                : I18n.t("tour.priceFrom", Fmt.money(hotel.getMinPrice())));
        price.getStyleClass().add(hotel.getMinPrice() == null ? "muted" : "price-small");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox footer = new HBox(8, new RatingLabel(hotel.getRating(), hotel.getReviewsCount()), spacer, price);
        footer.setAlignment(Pos.CENTER_LEFT);

        VBox body = new VBox(6, location, new HBox(6, name, stars), description, amenities, footer);
        HBox.setHgrow(body, Priority.ALWAYS);
        getChildren().addAll(Images.cover(hotel.getMainPhoto(), hotel.getName(), 220, 150, 12), body);
    }
}
