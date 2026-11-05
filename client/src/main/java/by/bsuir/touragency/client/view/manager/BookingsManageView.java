package by.bsuir.touragency.client.view.manager;

import by.bsuir.touragency.client.component.Badge;
import by.bsuir.touragency.client.util.Alerts;
import by.bsuir.touragency.client.util.Async;
import by.bsuir.touragency.client.util.Fmt;
import by.bsuir.touragency.client.util.I18n;
import by.bsuir.touragency.client.util.Texts;
import by.bsuir.touragency.client.util.Ui;
import by.bsuir.touragency.client.view.BaseView;
import by.bsuir.touragency.client.view.client.MyBookingsView;
import by.bsuir.touragency.client.view.dialog.BookingActions;
import by.bsuir.touragency.client.view.dialog.BookingDetailsDialog;
import by.bsuir.touragency.common.model.Booking;
import by.bsuir.touragency.common.model.BookingStatus;
import by.bsuir.touragency.common.protocol.CommandType;
import by.bsuir.touragency.common.protocol.Request;
import javafx.animation.PauseTransition;
import javafx.scene.Parent;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Управление бронированиями: фильтр по статусу и поиск, подробности,
 * смена статуса в рамках допустимых переходов, отмена с расчётом штрафа.
 */
public class BookingsManageView extends BaseView {

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("dd.MM.yy");
    private TablePanel<Booking> panel;
    private ComboBox<BookingStatus> status;
    private TextField search;

    @Override
    public String getTitle() {
        return t("menu.requests");
    }

    @Override
    protected Parent build() {
        panel = new TablePanel<>();
        status = Ui.combo(Arrays.asList(BookingStatus.values()), Texts::status, t("common.all"));
        status.setPrefWidth(210);
        status.setOnAction(e -> reload());
        search = new TextField();
        search.getStyleClass().add("search-field");   // Ctrl+F
        search.setPromptText(t("bm.search"));
        search.setPrefWidth(230);
        PauseTransition debounce = new PauseTransition(Duration.millis(350));
        debounce.setOnFinished(e -> reload());
        search.textProperty().addListener((o, a, b) -> debounce.playFromStart());
        panel.left(status, search);

        panel.action(t("bk.details"), "btn-outline", this::details);
        panel.action(t("bm.nextStatus"), "btn-primary", this::nextStatus);
        panel.action(t("bk.cancel"), "btn-danger", b -> {
            if (!b.canCancel()) {
                Alerts.info(t("bm.cannotCancel"));
                return;
            }
            BookingActions.cancel(b, this::reload);
        });
        panel.onDoubleClick(this::details);

        var table = panel.table();
        table.getColumns().add(Ui.column(t("bm.col.id"), b -> "#" + b.getId(), 50));
        table.getColumns().add(Ui.column(t("bm.col.client"), Booking::getClientName, 130));
        table.getColumns().add(Ui.column(t("bm.col.tour"), b -> b.getHotelName() + " · " + b.getLocation(), 150));
        table.getColumns().add(Ui.column(t("bm.col.dates"), b -> Fmt.dateRange(b.getStartDate(), b.getEndDate()), 172));
        table.getColumns().add(Ui.column(t("bm.col.persons"), Booking::getPersonsCount, 46));
        table.getColumns().add(Ui.column(t("bm.col.total"), b -> Fmt.number(b.getTotalPrice()), 80));
        table.getColumns().add(Ui.column(t("bm.col.created"),
                b -> b.getCreatedAt() == null ? "" : b.getCreatedAt().format(TIME), 80));
        TableColumn<Booking, Object> st = Ui.column(t("bm.col.status"), b -> b, 205);
        st.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(Object item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    return;
                }
                Booking b = (Booking) item;
                setGraphic(new Badge(Texts.status(b.getStatus()), MyBookingsView.badgeKind(b.getStatus())));
            }
        });
        table.getColumns().add(st);

        VBox page = new VBox(20, pageHeader(t("bm.title"), t("bm.subtitle")), panel);
        page.getStyleClass().add("page");
        panel.setChangeKey(b -> b.getId() + ":" + b.getStatus() + ":" + b.getPaidAmount());
        // новые и ожидающие подтверждения заявки выделяются цветом всей строки
        panel.setRowStyle(b -> switch (b.getStatus()) {
            case NEW -> "row-new";
            case PENDING_CONFIRMATION -> "row-pending";
            case CANCELLED -> "row-cancelled";
            default -> null;
        });
        reload();
        // заявки, созданные клиентами и изменённые другими менеджерами, появляются без перезахода в раздел
        Ui.autoRefresh(page, 10, this::refreshQuietly);
        return page;
    }

    private Request listRequest() {
        BookingStatus s = status.getValue();
        return new Request(CommandType.GET_BOOKINGS).with("status", s == null ? null : s.name())
                .with("search", search.getText());
    }

    private void reload() {
        Async.<ArrayList<Booking>>load(listRequest(), panel::setItems);
    }

    private void refreshQuietly() {
        Async.<ArrayList<Booking>>quiet(listRequest(), panel::setItems);
    }

    private void details(Booking b) {
        Async.<Booking>load(new Request(CommandType.GET_BOOKING).with("bookingId", b.getId()),
                full -> new BookingDetailsDialog(full).showAndWait());
    }

    /** Выбор следующего статуса только из допустимых переходов. */
    private void nextStatus(Booking b) {
        List<BookingStatus> options = b.getStatus().next().stream()
                .filter(s -> s != BookingStatus.CANCELLED).toList();
        if (options.isEmpty()) {
            Alerts.info(t("bm.final"));
            return;
        }
        List<String> labels = options.stream().map(Texts::status).toList();
        ChoiceDialog<String> dialog = new ChoiceDialog<>(labels.get(0), labels);
        dialog.setTitle(t("bm.nextStatus"));
        dialog.setHeaderText(I18n.t("bm.current", b.getId(), Texts.status(b.getStatus())));
        dialog.setContentText(t("bm.newStatus"));
        Alerts.localize(dialog);
        dialog.showAndWait().ifPresent(label -> {
            BookingStatus target = options.get(labels.indexOf(label));
            Async.send(new Request(CommandType.CHANGE_BOOKING_STATUS).with("bookingId", b.getId())
                    .with("status", target.name()), r -> {
                if (r.isOk()) {
                    reload();
                    Alerts.toast(I18n.t("bm.changed", b.getId(), Texts.status(target)));
                } else {
                    Alerts.error(r.getMessage());
                }
            });
        });
    }
}
