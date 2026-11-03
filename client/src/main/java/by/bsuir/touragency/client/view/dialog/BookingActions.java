package by.bsuir.touragency.client.view.dialog;

import by.bsuir.touragency.client.util.Alerts;
import by.bsuir.touragency.client.util.Async;
import by.bsuir.touragency.client.util.Fmt;
import by.bsuir.touragency.client.util.I18n;
import by.bsuir.touragency.common.model.Booking;
import by.bsuir.touragency.common.model.CancelQuote;
import by.bsuir.touragency.common.protocol.CommandType;
import by.bsuir.touragency.common.protocol.Request;
import javafx.scene.control.ChoiceDialog;

import java.util.List;

/** Общие действия с заявкой для клиента и менеджера: оплата и отмена со штрафом. */
public final class BookingActions {

    private BookingActions() {
    }

    public static void pay(Booking b, Runnable onDone) {
        List<String> methods = List.of(I18n.t("pay.CARD"), I18n.t("pay.TRANSFER"), I18n.t("pay.CASH"));
        ChoiceDialog<String> dialog = new ChoiceDialog<>(methods.get(0), methods);
        dialog.setTitle(I18n.t("pay.title"));
        dialog.setHeaderText(I18n.t("pay.header", Fmt.money(b.getTotalPrice())));
        dialog.setContentText(I18n.t("pay.method"));
        Alerts.localize(dialog);
        dialog.showAndWait().ifPresent(choice -> {
            String method = choice.equals(methods.get(1)) ? "TRANSFER" : choice.equals(methods.get(2)) ? "CASH" : "CARD";
            Async.send(new Request(CommandType.PAY_BOOKING).with("bookingId", b.getId()).with("method", method), r -> {
                if (r.isOk()) {
                    Alerts.info(r.getMessage());
                    onDone.run();
                } else {
                    Alerts.error(r.getMessage());
                }
            });
        });
    }

    /** Отмена: сначала расчёт штрафа и подтверждение, затем отмена. */
    public static void cancel(Booking b, Runnable onDone) {
        Async.send(new Request(CommandType.QUOTE_CANCEL).with("bookingId", b.getId()), response -> {
            if (!response.isOk()) {
                Alerts.error(response.getMessage());
                return;
            }
            CancelQuote q = response.getData();
            String text = I18n.t("cancel.text",
                    Fmt.money(q.total()), q.daysBefore(), q.percent(),
                    q.wasPaid() ? Fmt.money(q.penalty()) : I18n.t("cancel.noPenalty"),
                    Fmt.money(q.refund()))
                    + (q.wasPaid() ? "" : "\n\n" + I18n.t("cancel.notPaid"));
            if (!Alerts.confirm(I18n.t("cancel.title", b.getId()) + "\n\n" + text)) {
                return;
            }
            Async.send(new Request(CommandType.CANCEL_BOOKING).with("bookingId", b.getId()), r -> {
                if (r.isOk()) {
                    onDone.run();
                } else {
                    Alerts.error(r.getMessage());
                }
            });
        });
    }
}
