package by.bsuir.touragency.client.view.client;

import by.bsuir.touragency.client.component.Badge;
import by.bsuir.touragency.client.component.FavoriteButton;
import by.bsuir.touragency.client.component.RatingLabel;
import by.bsuir.touragency.client.component.TourCard;
import by.bsuir.touragency.client.util.ClientContext;
import by.bsuir.touragency.client.util.Texts;
import by.bsuir.touragency.client.view.dialog.MatchDialog;
import by.bsuir.touragency.common.model.MatchResult;
import by.bsuir.touragency.common.model.Review;
import by.bsuir.touragency.common.model.Role;
import javafx.scene.control.Hyperlink;
import by.bsuir.touragency.client.util.Async;
import by.bsuir.touragency.client.util.Fmt;
import by.bsuir.touragency.client.util.Images;
import by.bsuir.touragency.client.view.BaseView;
import by.bsuir.touragency.client.view.Shell;
import by.bsuir.touragency.common.model.Hotel;
import by.bsuir.touragency.common.model.Tour;
import by.bsuir.touragency.common.protocol.CommandType;
import by.bsuir.touragency.common.protocol.Request;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/** Подробная страница тура: фото, параметры, отель, удобства и цена. */
public class TourDetailsView extends BaseView {

    private final Shell shell;
    private final int tourId;
    private String title;

    public TourDetailsView(Shell shell, int tourId) {
        this.shell = shell;
        this.tourId = tourId;
        this.title = t("tour.details");
    }

    @Override
    public String getTitle() {
        return title;
    }

    @Override
    protected Parent build() {
        StackPane holder = new StackPane(new Label(t("common.loading")));
        holder.getStyleClass().add("page");
        Async.<Tour>load(new Request(CommandType.GET_TOUR).with("tourId", tourId), tour -> {
            title = tour.getHotelName();
            Async.<Hotel>load(new Request(CommandType.GET_HOTEL).with("hotelId", tour.getHotelId()),
                    hotel -> holder.getChildren().setAll(content(tour, hotel)));
        });
        return holder;
    }

    private Node content(Tour tour, Hotel hotel) {
        // левая колонка: фото и описание
        StackPane photo = new StackPane(Images.cover(tour.getImagePath(), tour.getHotelName(), 600, 320, 18));
        photo.setMaxSize(600, 320);
        StackPane.setAlignment(photo, Pos.TOP_LEFT);
        FlowPane badges = new FlowPane(6, 6);
        badges.setPadding(new Insets(14));
        if (tour.isHot()) badges.getChildren().add(new Badge(t("badge.hot"), Badge.Kind.HOT));
        if (tour.isFeatured()) badges.getChildren().add(new Badge(t("badge.featured"), Badge.Kind.FEATURED));
        if (tour.isNewTour()) badges.getChildren().add(new Badge(t("badge.new"), Badge.Kind.NEW));
        if (tour.isPopular()) badges.getChildren().add(new Badge(t("badge.popular"), Badge.Kind.POPULAR));
        StackPane.setAlignment(badges, Pos.TOP_LEFT);
        photo.getChildren().add(badges);

        Label location = new Label(tour.getLocation());
        location.getStyleClass().add("card-overline");
        Label name = new Label(tour.getHotelName());
        name.getStyleClass().add("page-title");
        Label stars = new Label(Fmt.stars(tour.getHotelStars()));
        stars.getStyleClass().addAll("stars", "stars-large");
        Label tourTitle = new Label(tour.getTitle());
        tourTitle.getStyleClass().add("page-subtitle");

        GridPane facts = new GridPane();
        facts.setHgap(28);
        facts.setVgap(14);
        facts.add(fact(t("details.dates"), Fmt.dateRange(tour.getStartDate(), tour.getEndDate())), 0, 0);
        facts.add(fact(t("details.duration"), t("common.nights", tour.getNights())), 1, 0);
        facts.add(fact(t("details.meal"), tour.getMealTypeName()), 2, 0);
        facts.add(fact(t("details.type"), tour.getVacationTypeName()), 0, 1);
        facts.add(fact(t("details.seats"), tour.getFreeSeats() + " / " + tour.getTotalSeats()), 1, 1);
        facts.add(factNode(t("details.rating"), new RatingLabel(tour.getRating(), tour.getReviewsCount())), 2, 1);
        facts.getStyleClass().addAll("card", "facts");

        Label descTitle = new Label(t("details.description"));
        descTitle.getStyleClass().add("section-title");
        Label desc = new Label(tour.getDescription());
        desc.setWrapText(true);
        desc.getStyleClass().add("body-text");

        Label hotelTitle = new Label(t("details.hotel"));
        hotelTitle.getStyleClass().add("section-title");
        Label hotelDesc = new Label(hotel.getDescription());
        hotelDesc.setWrapText(true);
        hotelDesc.getStyleClass().add("body-text");
        FlowPane amenities = new FlowPane(8, 8);
        hotel.getAmenityNames().forEach(a -> amenities.getChildren().add(new Badge("✓  " + a, Badge.Kind.NEUTRAL)));

        Label reviewsTitle = new Label(t("details.reviews"));
        reviewsTitle.getStyleClass().add("section-title");
        VBox reviews = new VBox(10);
        Async.<java.util.ArrayList<Review>>load(new Request(CommandType.GET_HOTEL_REVIEWS)
                .with("hotelId", tour.getHotelId()), list -> {
            if (list.isEmpty()) {
                Label none = new Label(t("details.noReviews"));
                none.getStyleClass().add("muted");
                reviews.getChildren().add(none);
            }
            list.forEach(r -> reviews.getChildren().add(reviewCard(r)));
        });

        VBox left = new VBox(16, photo, new VBox(4, location, new HBox(10, name, stars), tourTitle), facts,
                descTitle, desc, hotelTitle, hotelDesc, amenities, reviewsTitle, reviews);
        HBox.setHgrow(left, Priority.ALWAYS);
        left.setMinWidth(0);
        left.setMaxWidth(640);
        desc.setMaxWidth(620);
        hotelDesc.setMaxWidth(620);

        // правая колонка: цена и бронирование
        Label priceLabel = new Label(t("details.price"));
        priceLabel.getStyleClass().add("muted");
        Label price = new Label(Fmt.money(tour.getPricePerPerson()));
        price.getStyleClass().add("price-large");
        Label per = new Label(t("common.perPerson"));
        per.getStyleClass().add("muted");
        Label seats = new Label(t("common.seatsFree", tour.getFreeSeats()));
        seats.getStyleClass().add(tour.getFreeSeats() <= 3 ? "seats-low" : "muted");
        Button book = new Button(t("details.book"));
        book.getStyleClass().addAll("btn-accent", "btn-large");
        book.setMaxWidth(Double.MAX_VALUE);
        boolean client = ClientContext.getUser().getRole() == Role.CLIENT;
        boolean bookable = tour.isActive() && tour.getFreeSeats() > 0
                && tour.getStartDate().isAfter(java.time.LocalDate.now());
        book.setDisable(!client || !bookable);
        book.setOnAction(e -> shell.open(new BookingView(shell, tour)));
        Label hint = new Label(!bookable ? t("details.notBookable") : client ? t("details.bookHint") : t("details.staffHint"));
        hint.getStyleClass().add("muted");
        hint.setWrapText(true);
        VBox priceCard = new VBox(8, priceLabel, price, per, seats, book, hint);
        if (client) {
            FavoriteButton fav = new FavoriteButton(tour);
            Label favText = new Label(t("fav.add"));
            HBox favRow = new HBox(8, fav, favText);
            favRow.setAlignment(Pos.CENTER_LEFT);
            priceCard.getChildren().add(favRow);
        }
        priceCard.getStyleClass().addAll("card", "price-card");
        priceCard.setPrefWidth(280);
        priceCard.setMinWidth(260);

        VBox right = new VBox(16, priceCard);
        if (ClientContext.getUser().getRole() == Role.CLIENT) {
            VBox matchBox = new VBox(8);
            matchBox.getStyleClass().addAll("card", "match-panel");
            matchBox.setPrefWidth(280);
            right.getChildren().add(matchBox);
            Async.<MatchResult>load(new Request(CommandType.GET_MATCH).with("tourId", tour.getId()),
                    r -> fillMatch(matchBox, r, tour.getMatchPercent() != null));
        }
        HBox layout = new HBox(24, left, right);
        layout.setAlignment(Pos.TOP_LEFT);
        return layout;
    }

    private void fillMatch(VBox box, MatchResult r, boolean hasPreferences) {
        if (!hasPreferences) {
            Label title = new Label(t("details.matchNoPrefs"));
            title.setWrapText(true);
            Button go = new Button(t("rec.noPrefs.button"));
            go.getStyleClass().add("btn-outline");
            go.setOnAction(e -> shell.openSection("menu.preferences"));
            box.getChildren().addAll(title, go);
            return;
        }
        Label percent = new Label(r.getPercent() + "%");
        percent.getStyleClass().addAll("match-big", TourCard.matchClass(r.getPercent()) + "-text");
        Label title = new Label(t("match.title", r.getPercent()));
        title.setWrapText(true);
        title.getStyleClass().add("fact-value");
        box.getChildren().addAll(percent, title);
        r.matched().stream().limit(4).forEach(c -> {
            Label l = new Label("✓  " + Texts.criterion(c));
            l.setWrapText(true);
            l.getStyleClass().add("reason-ok");
            box.getChildren().add(l);
        });
        r.differs().stream().limit(3).forEach(c -> {
            Label l = new Label("•  " + Texts.criterion(c));
            l.setWrapText(true);
            l.getStyleClass().add("reason-diff");
            box.getChildren().add(l);
        });
        Hyperlink how = new Hyperlink(t("match.how"));
        how.setOnAction(e -> new MatchDialog(r).showAndWait());
        box.getChildren().add(how);
    }

    private static Node reviewCard(Review r) {
        Label stars = new Label("★".repeat(r.rating()) + "☆".repeat(5 - r.rating()));
        stars.getStyleClass().add("stars");
        Label author = new Label(r.authorName());
        author.getStyleClass().add("fact-value");
        Label date = new Label(r.createdAt() == null ? "" : Fmt.date(r.createdAt().toLocalDate()));
        date.getStyleClass().add("muted");
        Label text = new Label(r.comment() == null ? "" : r.comment());
        text.setWrapText(true);
        text.getStyleClass().add("body-text");
        VBox card = new VBox(6, new HBox(10, author, stars, date), text);
        card.getStyleClass().addAll("card", "review-card");
        card.setMaxWidth(640);
        return card;
    }

    private static Node fact(String label, String value) {
        Label v = new Label(value);
        v.getStyleClass().add("fact-value");
        return factNode(label, v);
    }

    private static Node factNode(String label, Node value) {
        Label l = new Label(label);
        l.getStyleClass().add("field-label");
        return new VBox(4, l, value);
    }
}
