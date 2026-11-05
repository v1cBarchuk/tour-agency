package by.bsuir.touragency.client.component;

import by.bsuir.touragency.client.util.Fmt;
import by.bsuir.touragency.client.util.I18n;
import by.bsuir.touragency.client.util.Images;
import by.bsuir.touragency.common.model.Tour;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.function.Consumer;

/**
 * Карточка тура в сетке каталога: фото, метки, отель, даты, питание,
 * рейтинг, свободные места и цена в BYN.
 */
public class TourCard extends VBox {

    public static final double WIDTH = 290;
    private static final double IMAGE_HEIGHT = 170;

    /** Цвет метки совпадения: зелёная от 80%, жёлтая от 50%, серая ниже. */
    public static String matchClass(int percent) {
        return percent >= 80 ? "match-high" : percent >= 50 ? "match-mid" : "match-low";
    }

    public TourCard(Tour tour, Consumer<Tour> onOpen) {
        this(tour, onOpen, by.bsuir.touragency.client.util.ClientContext.getUser() != null
                && by.bsuir.touragency.client.util.ClientContext.getUser().getRole()
                == by.bsuir.touragency.common.model.Role.CLIENT);
    }

    public TourCard(Tour tour, Consumer<Tour> onOpen, boolean showFavorite) {
        getStyleClass().addAll("card", "tour-card", "grid-item");
        // ширину задаёт сетка или карусель: карточка растягивается по колонке
        setPrefWidth(WIDTH);
        setMinWidth(240);
        setMaxWidth(Double.MAX_VALUE);
        // карточка появляется плавно
        setOpacity(0);
        javafx.animation.FadeTransition appear = new javafx.animation.FadeTransition(javafx.util.Duration.millis(260), this);
        appear.setToValue(1);
        appear.play();
        setCursor(Cursor.HAND);

        // фото с метками поверх
        StackPane media = new StackPane(new FluidCover(tour.getImagePath(), tour.getHotelName(), IMAGE_HEIGHT, 14));
        FlowPane badges = new FlowPane(6, 6);
        badges.setPadding(new Insets(10));
        badges.setPickOnBounds(false);
        if (tour.isHot()) badges.getChildren().add(new Badge(I18n.t("badge.hot"), Badge.Kind.HOT));
        if (tour.isFeatured()) badges.getChildren().add(new Badge(I18n.t("badge.featured"), Badge.Kind.FEATURED));
        if (tour.isNewTour()) badges.getChildren().add(new Badge(I18n.t("badge.new"), Badge.Kind.NEW));
        if (tour.isPopular()) badges.getChildren().add(new Badge(I18n.t("badge.popular"), Badge.Kind.POPULAR));
        if (tour.isAlmostSoldOut()) badges.getChildren().add(new Badge(I18n.t("badge.almostSold"), Badge.Kind.DANGER));
        if (tour.getOldPrice() != null) badges.getChildren().add(new Badge(I18n.t("badge.priceDrop"), Badge.Kind.SUCCESS));
        // метки переносятся на новую строку и не заходят под процент совпадения и «сердечко»
        double reserved = (tour.getMatchPercent() != null ? 62 : 0) + (showFavorite ? 84 : 0);
        badges.setPrefWrapLength(WIDTH - 20 - reserved);
        badges.setMaxSize(WIDTH - reserved, Region.USE_PREF_SIZE);
        widthProperty().addListener((o, a, w) -> {
            badges.setPrefWrapLength(Math.max(80, w.doubleValue() - 20 - reserved));
            badges.setMaxSize(Math.max(100, w.doubleValue() - reserved), Region.USE_PREF_SIZE);
        });
        StackPane.setAlignment(badges, Pos.TOP_LEFT);
        media.getChildren().add(badges);

        // процент совпадения с анкетой и «сердечко» — только для клиента
        HBox corner = new HBox(6);
        corner.setPadding(new Insets(10));
        corner.setAlignment(Pos.TOP_RIGHT);
        corner.setPickOnBounds(false);
        corner.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        if (tour.getMatchPercent() != null) {
            Label match = new Label(I18n.t("match.short", tour.getMatchPercent()));
            match.getStyleClass().addAll("match-chip", matchClass(tour.getMatchPercent()));
            corner.getChildren().add(match);
        }
        if (showFavorite) {
            corner.getChildren().addAll(new CompareButton(tour), new FavoriteButton(tour));
        }
        StackPane.setAlignment(corner, Pos.TOP_RIGHT);
        media.getChildren().add(corner);

        // описание
        Label location = new Label(tour.getLocation());
        Label stars = new Label(Fmt.stars(tour.getHotelStars()));
        stars.getStyleClass().add("stars");
        HBox top = new HBox(8, location, spacer(), stars);
        top.setAlignment(Pos.CENTER_LEFT);
        location.getStyleClass().add("card-overline");
        Label hotel = new Label(tour.getHotelName());
        hotel.getStyleClass().add("card-title");
        hotel.setWrapText(true);

        Label dates = info(Fmt.dateRange(tour.getStartDate(), tour.getEndDate())
                + " · " + I18n.t("common.nights", tour.getNights()));
        Label meal = info(tour.getMealTypeName() + " · " + tour.getVacationTypeName());

        Label seats = new Label(I18n.t("common.seatsLeft", tour.getFreeSeats()));
        seats.getStyleClass().add(tour.getFreeSeats() <= 3 ? "seats-low" : "muted");
        Integer wanted = by.bsuir.touragency.client.util.ClientContext.getDesiredPersons();
        if (wanted != null && wanted > tour.getFreeSeats()) {
            // клиент ищет тур на большее число туристов, чем осталось мест
            seats.setText(I18n.t("card.notEnough", tour.getFreeSeats(), wanted));
            seats.getStyleClass().setAll("label", "card-warning");
        }

        HBox meta = new HBox(8, new RatingLabel(tour.getRating(), tour.getReviewsCount()), spacer(), seats);
        meta.setAlignment(Pos.CENTER_LEFT);

        Label price = new Label(Fmt.money(tour.getPricePerPerson()));
        price.getStyleClass().add("price");
        Label perPerson = new Label(I18n.t("common.perPerson"));
        perPerson.getStyleClass().add("muted");
        VBox priceBox = new VBox(0, price, perPerson);
        if (tour.getOldPrice() != null) {
            Label old = new Label(Fmt.money(tour.getOldPrice()));
            old.getStyleClass().add("price-old");
            priceBox.getChildren().add(0, old);
        }
        Label more = new Label(I18n.t("tour.details") + "  →");
        more.getStyleClass().add("link-strong");
        HBox footer = new HBox(priceBox, spacer(), more);
        footer.setAlignment(Pos.BOTTOM_LEFT);
        footer.getStyleClass().add("card-footer");

        VBox body = new VBox(6, top, hotel, dates, meal, meta, footer);
        body.setPadding(new Insets(14, 16, 16, 16));

        getChildren().addAll(media, body);
        setOnMouseClicked(e -> onOpen.accept(tour));
    }

    private static Label info(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("card-info");
        return label;
    }

    private static Region spacer() {
        Region r = new Region();
        HBox.setHgrow(r, Priority.ALWAYS);
        return r;
    }
}
