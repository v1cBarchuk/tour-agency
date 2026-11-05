package by.bsuir.touragency.client.view.client;

import by.bsuir.touragency.client.component.Badge;
import by.bsuir.touragency.client.component.FavoriteButton;
import by.bsuir.touragency.client.component.RatingLabel;
import by.bsuir.touragency.client.component.TourCard;
import by.bsuir.touragency.client.util.ClientContext;
import by.bsuir.touragency.client.util.Texts;
import by.bsuir.touragency.client.util.Ui;
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
        javafx.scene.layout.VBox sk = new javafx.scene.layout.VBox(14, by.bsuir.touragency.client.component.Skeleton.block(600, 320),
                by.bsuir.touragency.client.component.Skeleton.block(360, 22), by.bsuir.touragency.client.component.Skeleton.block(520, 14), by.bsuir.touragency.client.component.Skeleton.block(460, 14));
        StackPane holder = new StackPane(sk);
        StackPane.setAlignment(sk, Pos.TOP_LEFT);
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
        // галерея: собственное фото тура (если есть) и все фото отеля из hotel_photos
        java.util.List<String> paths = new java.util.ArrayList<>();
        if (tour.getImagePath() != null) paths.add(tour.getImagePath());
        hotel.getPhotos().stream().filter(p -> !paths.contains(p)).forEach(paths::add);
        StackPane photo = new StackPane(new by.bsuir.touragency.client.component.Gallery(paths, tour.getHotelName(), 600, 420, 18));
        photo.setMaxSize(Double.MAX_VALUE, 420);
        StackPane.setAlignment(photo, Pos.TOP_LEFT);
        FlowPane badges = new FlowPane(6, 6);
        badges.setPadding(new Insets(14));
        if (tour.isHot()) badges.getChildren().add(new Badge(t("badge.hot"), Badge.Kind.HOT));
        if (tour.isFeatured()) badges.getChildren().add(new Badge(t("badge.featured"), Badge.Kind.FEATURED));
        if (tour.isNewTour()) badges.getChildren().add(new Badge(t("badge.new"), Badge.Kind.NEW));
        if (tour.isPopular()) badges.getChildren().add(new Badge(t("badge.popular"), Badge.Kind.POPULAR));
        if (tour.isAlmostSoldOut()) badges.getChildren().add(new Badge(t("badge.almostSold"), Badge.Kind.DANGER));
        if (tour.getOldPrice() != null) badges.getChildren().add(new Badge(t("badge.priceDrop"), Badge.Kind.SUCCESS));
        StackPane.setAlignment(badges, Pos.TOP_LEFT);
        badges.setPickOnBounds(false);            // клики проходят к стрелкам галереи
        badges.setMaxSize(javafx.scene.layout.Region.USE_PREF_SIZE, javafx.scene.layout.Region.USE_PREF_SIZE);
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

        Label weatherTitle = new Label(t("weather.title", tour.getCityName()));
        weatherTitle.getStyleClass().add("section-title");
        VBox weather = new VBox(10, new Label(t("common.loading")));
        Async.<by.bsuir.touragency.common.model.WeatherForecast>quiet(new Request(CommandType.GET_WEATHER)
                .with("tourId", tour.getId()), f -> fillWeather(weather, f), error -> {
            Label l = new Label(error);
            l.getStyleClass().add("muted");
            l.setWrapText(true);
            weather.getChildren().setAll(l);
        });

        VBox left = new VBox(16, photo, new VBox(4, location, new HBox(10, name, stars), tourTitle), facts,
                weatherTitle, weather, descTitle, desc, hotelTitle, hotelDesc, amenities, reviewsTitle, reviews);
        HBox.setHgrow(left, Priority.ALWAYS);
        left.setMinWidth(0);
        left.setMaxWidth(1100);

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

        // число туристов → итог за всех и проверка остатка мест
        int wanted = ClientContext.getDesiredPersons() == null ? 1 : ClientContext.getDesiredPersons();
        javafx.scene.control.Spinner<Integer> travellers = new javafx.scene.control.Spinner<>(1, 9, Math.min(9, wanted));
        travellers.setPrefWidth(90);
        Label travellersLabel = new Label(t("details.travellers"));
        travellersLabel.getStyleClass().add("field-label");
        HBox travellersRow = new HBox(10, travellersLabel, Ui.hSpacer(), travellers);
        travellersRow.setAlignment(Pos.CENTER_LEFT);
        Label total = new Label();
        total.getStyleClass().add("total-line");
        total.setWrapText(true);
        Label warning = new Label();
        warning.getStyleClass().add("seats-warning");
        warning.setWrapText(true);
        warning.setMaxWidth(Double.MAX_VALUE);
        Runnable recalc = () -> {
            int n = travellers.getValue();
            total.setText(t("details.totalFor", n, Fmt.money(tour.getPricePerPerson().multiply(java.math.BigDecimal.valueOf(n)))));
            boolean notEnough = bookable && n > tour.getFreeSeats();
            warning.setText(t("details.notEnoughSeats", n, tour.getFreeSeats()));
            warning.setVisible(notEnough);
            warning.setManaged(notEnough);
            book.setDisable(!client || !bookable || notEnough);
        };
        travellers.valueProperty().addListener((o, a, b) -> recalc.run());
        recalc.run();
        book.setOnAction(e -> shell.open(new BookingView(shell, tour, travellers.getValue())));
        Label hint = new Label(!bookable ? t("details.notBookable") : client ? t("details.bookHint") : t("details.staffHint"));
        hint.getStyleClass().add("muted");
        hint.setWrapText(true);
        VBox priceCard = new VBox(8, priceLabel, price, per, seats, new javafx.scene.control.Separator(),
                travellersRow, total, warning, book, hint);
        if (!by.bsuir.touragency.client.util.Currency.isBase()) {
            Label rate = new Label(by.bsuir.touragency.client.util.Currency.rateText() + " · " + t("currency.payInByn"));
            rate.getStyleClass().add("rate-hint");
            rate.setWrapText(true);
            priceCard.getChildren().add(rate);
        }
        if (tour.getOldPrice() != null) {
            Label old = new Label(Fmt.money(tour.getOldPrice()));
            old.getStyleClass().add("price-old");
            Label drop = new Label(t("details.priceDropped", Fmt.money(tour.getOldPrice().subtract(tour.getPricePerPerson()))));
            drop.getStyleClass().add("reason-ok");
            priceCard.getChildren().add(1, new HBox(10, old, drop));
        }
        if (tour.isAlmostSoldOut()) {
            Label sold = new Label(t("details.almostSold", tour.getOccupancyPercent()));
            sold.getStyleClass().add("seats-low");
            sold.setWrapText(true);
            priceCard.getChildren().add(priceCard.getChildren().indexOf(seats) + 1, sold);
        }
        if (client) {
            FavoriteButton fav = new FavoriteButton(tour);
            Label favText = new Label(t("fav.add"));
            HBox favRow = new HBox(8, fav, favText);
            favRow.setAlignment(Pos.CENTER_LEFT);
            by.bsuir.touragency.client.component.CompareButton cmp = new by.bsuir.touragency.client.component.CompareButton(tour);
            Hyperlink openCompare = new Hyperlink(t("compare.open"));
            openCompare.setOnAction(e -> shell.openSection("menu.compare"));
            HBox cmpRow = new HBox(8, cmp, openCompare);
            cmpRow.setAlignment(Pos.CENTER_LEFT);
            priceCard.getChildren().addAll(favRow, cmpRow);
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
        HBox layout = new HBox(28, left, right);
        layout.setAlignment(Pos.TOP_LEFT);
        right.setMinWidth(300);
        right.setPrefWidth(320);
        right.setMaxHeight(javafx.scene.layout.Region.USE_PREF_SIZE);
        Ui.sticky(right, layout, 24);          // цена и «Забронировать» всегда перед глазами

        // похожие туры — карусель под основной частью страницы
        Label similarTitle = new Label(t("details.similar"));
        similarTitle.getStyleClass().add("section-title");
        by.bsuir.touragency.client.component.Carousel similar =
                new by.bsuir.touragency.client.component.Carousel(TourCard.WIDTH, 18);
        VBox similarBox = new VBox(14, similarTitle, similar);
        similarBox.setVisible(false);
        similarBox.setManaged(false);
        Async.<java.util.ArrayList<Tour>>quiet(new Request(CommandType.GET_SIMILAR_TOURS).with("tourId", tour.getId()),
                list -> {
                    if (list.isEmpty()) return;
                    list.forEach(x -> similar.add(new TourCard(x, y -> shell.open(new TourDetailsView(shell, y.getId())))));
                    similarBox.setVisible(true);
                    similarBox.setManaged(true);
                });
        return new VBox(36, layout, similarBox);
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

    /** Плитки прогноза: день недели, значок погоды, температура днём/ночью, осадки. */
    private void fillWeather(VBox box, by.bsuir.touragency.common.model.WeatherForecast f) {
        Label hint = new Label(f.forTourDates() ? t("weather.forTour") : t("weather.nextWeek"));
        hint.getStyleClass().add("muted");
        hint.setWrapText(true);
        FlowPane days = new FlowPane(10, 10);
        for (var d : f.days()) {
            Label day = new Label(Texts.weekday(d.date()) + ", " + d.date().getDayOfMonth() + "."
                    + String.format("%02d", d.date().getMonthValue()));
            day.getStyleClass().add("weather-day");
            Label icon = new Label(Texts.weatherIcon(d.code()));
            icon.getStyleClass().add("weather-icon");
            Label temp = new Label(deg(d.tempMax()));
            temp.getStyleClass().add("weather-temp");
            Label night = new Label(deg(d.tempMin()));
            night.getStyleClass().add("muted");
            Label rain = new Label(!Double.isNaN(d.precipitation()) && d.precipitation() > 0.2
                    ? "☂ " + Math.round(d.precipitation()) + " " + t("weather.mm") : " ");
            rain.getStyleClass().add("weather-rain");
            VBox tile = new VBox(4, day, icon, new HBox(6, temp, night), rain);
            tile.setAlignment(Pos.TOP_CENTER);
            ((HBox) tile.getChildren().get(2)).setAlignment(Pos.BASELINE_CENTER);
            tile.getStyleClass().addAll("card", "weather-tile");
            javafx.scene.control.Tooltip.install(tile, new javafx.scene.control.Tooltip(Texts.weatherText(d.code())
                    + (Double.isNaN(d.windMax()) ? "" : "\n" + t("weather.wind", Math.round(d.windMax())))));
            days.getChildren().add(tile);
        }
        box.getChildren().setAll(hint, days);
    }

    /** Температура или прочерк, если сервис не дал значения. */
    private static String deg(double value) {
        return Double.isNaN(value) ? "—" : Math.round(value) + "°";
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
