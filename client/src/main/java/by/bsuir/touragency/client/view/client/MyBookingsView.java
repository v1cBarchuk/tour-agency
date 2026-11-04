package by.bsuir.touragency.client.view.client;

import by.bsuir.touragency.client.component.Badge;
import by.bsuir.touragency.client.component.EmptyState;
import by.bsuir.touragency.client.component.StatusTimeline;
import by.bsuir.touragency.client.util.Alerts;
import by.bsuir.touragency.client.util.Async;
import by.bsuir.touragency.client.util.Fmt;
import by.bsuir.touragency.client.util.Images;
import by.bsuir.touragency.client.util.Texts;
import by.bsuir.touragency.client.util.Ui;
import by.bsuir.touragency.client.view.BaseView;
import by.bsuir.touragency.client.view.Shell;
import by.bsuir.touragency.client.view.dialog.BookingActions;
import by.bsuir.touragency.client.view.dialog.BookingDetailsDialog;
import by.bsuir.touragency.client.view.dialog.ReviewDialog;
import by.bsuir.touragency.common.model.Booking;
import by.bsuir.touragency.common.model.BookingStatus;
import by.bsuir.touragency.common.protocol.CommandType;
import by.bsuir.touragency.common.protocol.Request;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.ArrayList;

/**
 * Мои бронирования: ближайшие поездки и история, шкала статусов,
 * оплата, отмена со штрафом, отзыв после поездки.
 */
public class MyBookingsView extends BaseView {

    private final Shell shell;
    private VBox active;
    private VBox past;

    public MyBookingsView(Shell shell) {
        this.shell = shell;
    }

    @Override
    public String getTitle() {
        return t("menu.bookings");
    }

    @Override
    protected Parent build() {
        active = new VBox(14);
        past = new VBox(14);
        Label a = new Label(t("bk.upcoming"));
        a.getStyleClass().add("section-title");
        Label p = new Label(t("bk.history"));
        p.getStyleClass().add("section-title");
        VBox page = new VBox(20, pageHeader(t("bk.title"), t("bk.subtitle")), a, active, p, past);
        page.getStyleClass().add("page");
        reload();
        // новый статус, выставленный менеджером, появляется сам — без перехода в другой раздел
        by.bsuir.touragency.client.util.Ui.autoRefresh(page, 10,
                () -> Async.<ArrayList<Booking>>quiet(new Request(CommandType.GET_MY_BOOKINGS), this::show));
        return page;
    }

    private Object lastSignature;

    private void reload() {
        lastSignature = null;
        Async.<ArrayList<Booking>>load(new Request(CommandType.GET_MY_BOOKINGS), this::show);
    }

    private void show(ArrayList<Booking> list) {
        Object signature = list.stream().map(b -> b.getId() + ":" + b.getStatus() + ":" + b.getPaidAmount()
                + ":" + b.canReview()).toList();
        if (signature.equals(lastSignature)) return;   // ничего не изменилось — не перестраиваем страницу
        lastSignature = signature;
        {
            active.getChildren().clear();
            past.getChildren().clear();
            for (Booking b : list) {
                (b.getStatus().isFinal() ? past : active).getChildren().add(card(b));
            }
            if (active.getChildren().isEmpty()) {
                EmptyState empty = new EmptyState("✈", t("bk.empty"), t("bk.emptyHint"));
                Button go = new Button(t("menu.tours"));
                go.getStyleClass().add("btn-primary");
                go.setOnAction(e -> shell.openSection("menu.tours"));
                empty.getChildren().add(go);
                active.getChildren().add(empty);
            }
            if (past.getChildren().isEmpty()) {
                Label none = new Label(t("common.empty"));
                none.getStyleClass().add("muted");
                past.getChildren().add(none);
            }
        }
    }

    private Node card(Booking b) {
        Label place = new Label(b.getLocation());
        place.getStyleClass().add("card-overline");
        Label hotel = new Label(b.getHotelName() + "  " + Fmt.stars(b.getHotelStars()));
        hotel.getStyleClass().add("card-title");
        Label info = new Label(t("bk.number", b.getId()) + " · " + Fmt.dateRange(b.getStartDate(), b.getEndDate())
                + " · " + t("common.persons", b.getPersonsCount()));
        info.getStyleClass().add("card-info");
        Badge status = new Badge(Texts.status(b.getStatus()), badgeKind(b.getStatus()));
        Label price = new Label(Fmt.money(b.getTotalPrice()));
        price.getStyleClass().add("price");
        VBox priceBox = new VBox(2, price);
        if (b.getDiscountName() != null) {
            Label d = new Label(t("bk.withDiscount", b.getDiscountName()));
            d.getStyleClass().add("muted");
            priceBox.getChildren().add(d);
        }
        priceBox.setAlignment(Pos.TOP_RIGHT);

        HBox head = new HBox(16, Images.cover(b.getImagePath(), b.getHotelName(), 150, 100, 12),
                new VBox(6, place, hotel, info, status), Ui.hSpacer(), priceBox);

        // действия
        HBox actions = new HBox(8);
        actions.setAlignment(Pos.CENTER_LEFT);
        Button details = button(t("bk.details"), "btn-outline");
        details.setOnAction(e -> Async.<Booking>load(new Request(CommandType.GET_BOOKING).with("bookingId", b.getId()),
                full -> new BookingDetailsDialog(full).showAndWait()));
        actions.getChildren().add(details);
        Button tour = button(t("bk.openTour"), "btn-ghost");
        tour.setOnAction(e -> shell.open(new TourDetailsView(shell, b.getTourId())));
        actions.getChildren().add(tour);
        actions.getChildren().add(Ui.hSpacer());
        if (b.getStatus() == BookingStatus.AWAITING_PAYMENT) {
            Button pay = button(t("bk.pay"), "btn-accent");
            pay.setOnAction(e -> BookingActions.pay(b, this::reload));
            actions.getChildren().add(pay);
        }
        if (b.canReview()) {
            Button review = button("★  " + t("bk.review"), "btn-primary");
            review.setOnAction(e -> {
                if (new ReviewDialog(b).showAndWait().orElse(false)) {
                    Alerts.info(t("review.thanks"));
                    reload();
                }
            });
            actions.getChildren().add(review);
        } else if (b.getReviewRating() != null) {
            Label rated = new Label(t("bk.rated", "★".repeat(b.getReviewRating())));
            rated.getStyleClass().add("stars");
            actions.getChildren().add(rated);
        }
        if (b.canCancel()) {
            Button cancel = button(t("bk.cancel"), "btn-danger");
            cancel.setOnAction(e -> BookingActions.cancel(b, this::reload));
            actions.getChildren().add(cancel);
        }

        VBox card = new VBox(16, head);
        if (b.getStatus() != BookingStatus.COMPLETED) {
            card.getChildren().add(new StatusTimeline(b.getStatus(), java.util.List.of()));
        }
        if (b.getStatus() == BookingStatus.CANCELLED && b.getCancelPenalty() != null) {
            Label c = new Label(t("bk.cancelInfo", Fmt.money(b.getCancelPenalty()), Fmt.money(b.getRefundAmount())));
            c.getStyleClass().add("muted");
            card.getChildren().add(c);
        }
        card.getChildren().add(actions);
        card.getStyleClass().addAll("card", "booking-card");
        HBox.setHgrow(card, Priority.ALWAYS);
        return card;
    }

    private static Button button(String text, String style) {
        Button b = new Button(text);
        b.getStyleClass().add(style);
        b.setMinWidth(javafx.scene.layout.Region.USE_PREF_SIZE);
        return b;
    }

    public static Badge.Kind badgeKind(BookingStatus s) {
        return switch (s) {
            case NEW, PENDING_CONFIRMATION -> Badge.Kind.WARNING;
            case CONFIRMED, AWAITING_PAYMENT -> Badge.Kind.FEATURED;
            case PAID, COMPLETED -> Badge.Kind.SUCCESS;
            case CANCELLED -> Badge.Kind.DANGER;
        };
    }
}
