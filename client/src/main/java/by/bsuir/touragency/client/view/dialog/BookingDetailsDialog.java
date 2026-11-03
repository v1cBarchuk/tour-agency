package by.bsuir.touragency.client.view.dialog;

import by.bsuir.touragency.client.component.StatusTimeline;
import by.bsuir.touragency.client.util.Fmt;
import by.bsuir.touragency.client.util.I18n;
import by.bsuir.touragency.client.util.Texts;
import by.bsuir.touragency.client.view.Navigator;
import by.bsuir.touragency.common.model.Booking;
import by.bsuir.touragency.common.model.StatusChange;
import by.bsuir.touragency.common.model.Tourist;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

import java.time.format.DateTimeFormatter;

/** Подробности заявки: шкала статусов, туристы, стоимость, история изменений. */
public class BookingDetailsDialog extends Dialog<Void> {

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    public BookingDetailsDialog(Booking b) {
        setTitle(I18n.t("bk.number", b.getId()));
        if (Navigator.getStage() != null) {
            initOwner(Navigator.getStage());
            getDialogPane().getStylesheets().addAll(Navigator.getStage().getScene().getStylesheets());
        }
        getDialogPane().getStyleClass().add("edit-dialog");

        Label title = new Label(I18n.t("bk.number", b.getId()) + " · " + b.getHotelName());
        title.getStyleClass().add("dialog-title");
        Label sub = new Label(b.getLocation() + " · " + Fmt.dateRange(b.getStartDate(), b.getEndDate())
                + " · " + I18n.t("common.nights", b.getNights()));
        sub.getStyleClass().add("muted");

        GridPane info = new GridPane();
        info.setHgap(24);
        info.setVgap(8);
        int r = 0;
        row(info, r++, I18n.t("bk.client"), b.getClientName() + " (" + b.getClientLogin() + ")");
        row(info, r++, I18n.t("bk.contacts"), b.getContactPhone() + " · " + b.getContactEmail());
        row(info, r++, I18n.t("bk.created"), b.getCreatedAt() == null ? "—" : b.getCreatedAt().format(TIME));
        row(info, r++, I18n.t("book.base"), Fmt.money(b.getBasePrice()));
        row(info, r++, I18n.t("book.discount"), b.getDiscountName() == null ? "—"
                : "−" + Fmt.money(b.getDiscountAmount()) + " (" + b.getDiscountName() + ")");
        row(info, r++, I18n.t("book.total"), Fmt.money(b.getTotalPrice()));
        row(info, r++, I18n.t("bk.paid"), Fmt.money(b.getPaidAmount()));
        if (b.getCancelPenalty() != null) {
            row(info, r++, I18n.t("cancel.penalty"), Fmt.money(b.getCancelPenalty()));
            row(info, r++, I18n.t("cancel.refund"), Fmt.money(b.getRefundAmount()));
        }

        VBox tourists = new VBox(4);
        for (Tourist t : b.getTourists()) {
            tourists.getChildren().add(new Label(t.number() + ". " + t.fullName() + " — " + Fmt.date(t.birthDate())));
        }
        VBox history = new VBox(4);
        for (StatusChange h : b.getHistory()) {
            Label l = new Label((h.changedAt() == null ? "" : h.changedAt().format(TIME)) + "  ·  "
                    + Texts.status(h.status()) + (h.changedBy() == null ? "" : "  ·  " + h.changedBy())
                    + (h.comment() == null ? "" : "  ·  " + h.comment()));
            l.setWrapText(true);
            l.getStyleClass().add("card-info");
            history.getChildren().add(l);
        }

        VBox content = new VBox(16, new VBox(2, title, sub), new StatusTimeline(b.getStatus(), b.getHistory()),
                info, section(I18n.t("book.tourists")), tourists, section(I18n.t("bk.history")), history);
        content.getStyleClass().add("dialog-content");
        content.setPrefWidth(760);
        getDialogPane().setContent(content);
        getDialogPane().getButtonTypes().add(new ButtonType(I18n.t("common.close"), ButtonBar.ButtonData.CANCEL_CLOSE));
    }

    private static Label section(String text) {
        Label l = new Label(text);
        l.getStyleClass().add("field-label");
        return l;
    }

    private static void row(GridPane grid, int row, String label, String value) {
        Label l = new Label(label);
        l.getStyleClass().add("muted");
        Label v = new Label(value);
        v.getStyleClass().add("fact-value");
        grid.add(l, 0, row);
        grid.add(v, 1, row);
    }
}
