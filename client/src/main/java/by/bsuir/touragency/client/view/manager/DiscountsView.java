package by.bsuir.touragency.client.view.manager;

import by.bsuir.touragency.client.component.Badge;
import by.bsuir.touragency.client.util.Alerts;
import by.bsuir.touragency.client.util.Async;
import by.bsuir.touragency.client.util.Fmt;
import by.bsuir.touragency.client.util.I18n;
import by.bsuir.touragency.client.util.Ui;
import by.bsuir.touragency.client.view.BaseView;
import by.bsuir.touragency.client.view.dialog.DiscountDialog;
import by.bsuir.touragency.common.model.Discount;
import by.bsuir.touragency.common.protocol.CommandType;
import by.bsuir.touragency.common.protocol.Request;
import javafx.scene.Parent;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.layout.VBox;

import java.util.ArrayList;

/** Управление скидками трёх типов. */
public class DiscountsView extends BaseView {

    private TablePanel<Discount> panel;

    @Override
    public String getTitle() {
        return t("disc.title");
    }

    @Override
    protected Parent build() {
        panel = new TablePanel<>();
        panel.button("+  " + t("disc.new"), "btn-primary", () -> edit(null));
        panel.action(t("common.edit"), "btn-outline", this::edit);
        panel.action(t("disc.toggle"), "btn-outline", this::toggle);
        panel.onDoubleClick(this::edit);

        var table = panel.table();
        table.getColumns().add(Ui.column(t("disc.col.name"), d -> d.getName(I18n.getLang()), 220));
        table.getColumns().add(Ui.column(t("disc.col.type"), d -> t("discountType." + d.getType().name()), 170));
        table.getColumns().add(Ui.column(t("disc.col.percent"),
                d -> "−" + d.getPercent().stripTrailingZeros().toPlainString() + "%", 90));
        table.getColumns().add(Ui.column(t("disc.col.condition"), DiscountsView::condition, 220));
        table.getColumns().add(Ui.column(t("disc.col.used"), Discount::getUsedCount, 90));
        TableColumn<Discount, Object> status = Ui.column(t("disc.col.status"), d -> d, 110);
        status.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(Object item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty || item == null ? null : ((Discount) item).isActive()
                        ? new Badge(t("disc.active"), Badge.Kind.SUCCESS)
                        : new Badge(t("disc.inactive"), Badge.Kind.NEUTRAL));
            }
        });
        table.getColumns().add(status);

        VBox page = new VBox(20, pageHeader(t("disc.title"), t("disc.subtitle")), panel);
        page.getStyleClass().add("page");
        reload();
        return page;
    }

    private static String condition(Discount d) {
        return switch (d.getType()) {
            case SEASONAL -> t("disc.cond.SEASONAL", Fmt.shortDate(d.getValidFrom()), Fmt.date(d.getValidTo()));
            case EARLY_BOOKING -> t("disc.cond.EARLY_BOOKING", d.getMinDaysBefore());
            case LOYALTY -> t("disc.cond.LOYALTY", d.getMinCompletedTrips());
            case PROMO_CODE -> t("disc.cond.PROMO_CODE", d.getPromoCode())
                    + (d.getMaxUses() == null ? "" : " · " + t("disc.cond.uses", d.getUsedCount(), d.getMaxUses()))
                    + (d.getValidTo() == null ? "" : " · " + t("disc.cond.until", Fmt.date(d.getValidTo())));
        };
    }

    private void reload() {
        Async.<ArrayList<Discount>>load(new Request(CommandType.GET_DISCOUNTS), panel::setItems);
    }

    private void edit(Discount d) {
        if (new DiscountDialog(d).showAndWait().orElse(false)) {
            reload();
        }
    }

    private void toggle(Discount d) {
        Async.send(new Request(CommandType.SET_DISCOUNT_ACTIVE).with("discountId", d.getId())
                .with("active", !d.isActive()), response -> {
            if (response.isOk()) {
                reload();
            } else {
                Alerts.error(response.getMessage());
            }
        });
    }
}
