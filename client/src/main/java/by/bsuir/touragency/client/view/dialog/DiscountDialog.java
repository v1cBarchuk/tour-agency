package by.bsuir.touragency.client.view.dialog;

import by.bsuir.touragency.client.util.I18n;
import by.bsuir.touragency.client.util.Ui;
import by.bsuir.touragency.common.model.Discount;
import by.bsuir.touragency.common.model.DiscountType;
import by.bsuir.touragency.common.protocol.CommandType;
import by.bsuir.touragency.common.protocol.Request;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

import java.util.Arrays;

/** Скидка: поля условий меняются в зависимости от типа. */
public class DiscountDialog extends EditDialog<Discount> {

    private final Discount discount;
    private final TextField nameRu = new TextField();
    private final TextField nameEn = new TextField();
    private final ComboBox<DiscountType> type;
    private final TextField percent = Ui.numberField("10");
    private final DatePicker from = Ui.date(null);
    private final DatePicker to = Ui.date(null);
    private final TextField minDays = Ui.numberField("30");
    private final TextField minTrips = Ui.numberField("2");
    private final CheckBox active = new CheckBox(I18n.t("disc.active"));
    private final VBox fromField;
    private final VBox toField;
    private final VBox daysField;
    private final VBox tripsField;

    public DiscountDialog(Discount existing) {
        super(existing == null ? I18n.t("disc.new") : existing.getName(I18n.getLang()));
        this.discount = existing == null ? new Discount() : existing;
        type = Ui.combo(Arrays.asList(DiscountType.values()), d -> I18n.t("discountType." + d.name()), null);
        fromField = Ui.field(I18n.t("disc.validFrom"), from);
        toField = Ui.field(I18n.t("disc.validTo"), to);
        daysField = Ui.field(I18n.t("disc.minDays"), minDays);
        tripsField = Ui.field(I18n.t("disc.minTrips"), minTrips);

        if (existing != null) {
            nameRu.setText(discount.getNameRu());
            nameEn.setText(discount.getNameEn());
            type.getSelectionModel().select(discount.getType());
            percent.setText(discount.getPercent().stripTrailingZeros().toPlainString());
            from.setValue(discount.getValidFrom());
            to.setValue(discount.getValidTo());
            if (discount.getMinDaysBefore() != null) minDays.setText(discount.getMinDaysBefore().toString());
            if (discount.getMinCompletedTrips() != null) minTrips.setText(discount.getMinCompletedTrips().toString());
            active.setSelected(discount.isActive());
        } else {
            type.getSelectionModel().select(DiscountType.SEASONAL);
            active.setSelected(true);
        }
        type.setOnAction(e -> updateVisibility());

        add(Ui.field(I18n.t("disc.nameRu"), nameRu), 0, 0, 1);
        add(Ui.field(I18n.t("disc.nameEn"), nameEn), 1, 0, 1);
        add(Ui.field(I18n.t("disc.type"), type), 0, 1, 1);
        add(Ui.field(I18n.t("disc.percent"), percent), 1, 1, 1);
        add(fromField, 0, 2, 1);
        add(toField, 1, 2, 1);
        add(daysField, 0, 3, 1);
        add(tripsField, 1, 3, 1);
        add(active, 0, 4, 2);
        updateVisibility();
    }

    private void updateVisibility() {
        DiscountType t = type.getValue();
        show(fromField, t == DiscountType.SEASONAL);
        show(toField, t == DiscountType.SEASONAL);
        show(daysField, t == DiscountType.EARLY_BOOKING);
        show(tripsField, t == DiscountType.LOYALTY);
        if (getDialogPane().getScene() != null && getDialogPane().getScene().getWindow() != null) {
            getDialogPane().getScene().getWindow().sizeToScene();
        }
    }

    private static void show(VBox field, boolean visible) {
        field.setVisible(visible);
        field.setManaged(visible);
    }

    @Override
    protected Discount collect() {
        discount.setNameRu(nameRu.getText());
        discount.setNameEn(nameEn.getText());
        discount.setType(type.getValue());
        discount.setPercent(Ui.decimal(percent));
        discount.setValidFrom(from.getValue());
        discount.setValidTo(to.getValue());
        discount.setMinDaysBefore(Ui.integer(minDays));
        discount.setMinCompletedTrips(Ui.integer(minTrips));
        discount.setActive(active.isSelected());
        return discount;
    }

    @Override
    protected Request saveRequest(Discount value) {
        return new Request(CommandType.SAVE_DISCOUNT).with("discount", value);
    }
}
