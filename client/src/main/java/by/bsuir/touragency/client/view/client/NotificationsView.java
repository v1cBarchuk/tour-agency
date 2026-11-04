package by.bsuir.touragency.client.view.client;

import by.bsuir.touragency.client.component.EmptyState;
import by.bsuir.touragency.client.util.Async;
import by.bsuir.touragency.client.util.Texts;
import by.bsuir.touragency.client.view.BaseView;
import by.bsuir.touragency.client.view.Shell;
import by.bsuir.touragency.common.model.Notification;
import by.bsuir.touragency.common.protocol.CommandType;
import by.bsuir.touragency.common.protocol.Request;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;

/** Все уведомления клиента: щелчок отмечает прочитанным и открывает тур или бронирование. */
public class NotificationsView extends BaseView {

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
    private final Shell shell;
    private final Runnable onChanged;
    private VBox list;

    public NotificationsView(Shell shell, Runnable onChanged) {
        this.shell = shell;
        this.onChanged = onChanged;
    }

    @Override
    public String getTitle() {
        return t("menu.notifications");
    }

    @Override
    protected Parent build() {
        Button readAll = new Button("✓  " + t("notif.readAll"));
        readAll.getStyleClass().add("btn-outline");
        readAll.setOnAction(e -> Async.send(new Request(CommandType.MARK_NOTIFICATIONS_READ), r -> reload()));
        list = new VBox(10);
        VBox page = new VBox(20, new HBox(pageHeader(t("notif.title"), t("notif.subtitle")),
                by.bsuir.touragency.client.util.Ui.hSpacer(), readAll), list);
        page.getStyleClass().add("page");
        reload();
        by.bsuir.touragency.client.util.Ui.autoRefresh(page, 10,
                () -> Async.<ArrayList<Notification>>quiet(new Request(CommandType.GET_NOTIFICATIONS), this::show));
        return page;
    }

    private Object lastSignature;

    private void reload() {
        lastSignature = null;
        Async.<ArrayList<Notification>>load(new Request(CommandType.GET_NOTIFICATIONS), this::show);
    }

    private void show(ArrayList<Notification> items) {
        Object signature = items.stream().map(n -> n.id() + ":" + n.read()).toList();
        if (signature.equals(lastSignature)) return;
        lastSignature = signature;
        {
            list.getChildren().clear();
            if (items.isEmpty()) {
                list.getChildren().add(new EmptyState("✉", t("notif.empty"), t("notif.emptyHint")));
            }
            items.forEach(n -> list.getChildren().add(row(n)));
            onChanged.run();
        }
    }

    private Node row(Notification n) {
        Label icon = new Label(Texts.notificationIcon(n));
        icon.getStyleClass().addAll("notif-icon", "notif-" + n.type().toLowerCase());
        Label text = new Label(Texts.notification(n));
        text.setWrapText(true);
        text.getStyleClass().add(n.read() ? "notif-text" : "notif-text-unread");
        Label time = new Label(n.createdAt() == null ? "" : n.createdAt().format(TIME));
        time.getStyleClass().add("muted");
        VBox body = new VBox(4, text, time);
        HBox.setHgrow(body, Priority.ALWAYS);
        HBox row = new HBox(14, icon, body);
        if (!n.read()) {
            Label dot = new Label("●");
            dot.getStyleClass().add("unread-dot");
            row.getChildren().add(dot);
        }
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().addAll("card", "notif-row");
        row.setCursor(Cursor.HAND);
        row.setOnMouseClicked(e -> Async.send(new Request(CommandType.MARK_NOTIFICATIONS_READ)
                .with("notificationId", n.id()), r -> {
            onChanged.run();
            if (n.tourId() != null) {
                shell.open(new TourDetailsView(shell, n.tourId()));
            } else if (n.bookingId() != null) {
                shell.openSection("menu.bookings");
            } else {
                reload();
            }
        }));
        return row;
    }
}
