package by.bsuir.touragency.client.view.dialog;

import by.bsuir.touragency.client.util.ClientContext;
import by.bsuir.touragency.client.util.I18n;
import by.bsuir.touragency.client.util.Ui;
import by.bsuir.touragency.common.model.DictionaryItem;
import by.bsuir.touragency.common.model.Hotel;
import by.bsuir.touragency.common.model.Tour;
import by.bsuir.touragency.common.protocol.CommandType;
import by.bsuir.touragency.common.protocol.Request;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Spinner;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

import java.util.List;

/** Создание и редактирование тура. */
public class TourDialog extends EditDialog<Tour> {

    private final Tour tour;
    private final ComboBox<Hotel> hotel;
    private final ComboBox<DictionaryItem> vacationType;
    private final ComboBox<DictionaryItem> mealType;
    private final TextField titleRu = new TextField();
    private final TextField titleEn = new TextField();
    private final DatePicker start = Ui.date(null);
    private final DatePicker end = Ui.date(null);
    private final TextField price = Ui.numberField("2500");
    private final Spinner<Integer> seats = new Spinner<>(1, 500, 20);
    private final CheckBox featured = new CheckBox(I18n.t("tf.featured"));
    private final CheckBox active = new CheckBox(I18n.t("tf.active"));
    private final TextArea descRu = new TextArea();
    private final TextArea descEn = new TextArea();

    public TourDialog(Tour existing, List<Hotel> hotels) {
        super(existing == null ? I18n.t("mt.new") : I18n.t("mt.editTitle", existing.getTitle()));
        this.tour = existing == null ? new Tour() : existing;

        hotel = Ui.combo(hotels, h -> h.getName() + " " + h.getStars() + "★ — " + h.getCityName(), null);
        vacationType = Ui.combo(ClientContext.vacationTypes(), DictionaryItem::getName, null);
        mealType = Ui.combo(ClientContext.mealTypes(), DictionaryItem::getName, null);
        seats.setEditable(true);
        titleEn.setPromptText(I18n.t("tf.titleEnHint"));
        descRu.setPrefRowCount(3);
        descEn.setPrefRowCount(3);
        descRu.setWrapText(true);
        descEn.setWrapText(true);

        // при выборе отеля подставляем название тура, если оно пустое
        hotel.setOnAction(e -> {
            Hotel h = hotel.getValue();
            if (h != null && titleRu.getText().isBlank()) {
                titleRu.setText(h.getCityName() + ": " + h.getName());
            }
        });

        if (existing != null) {
            Ui.select(hotel, Hotel::getId, tour.getHotelId());
            Ui.select(vacationType, DictionaryItem::getId, tour.getVacationTypeId());
            Ui.select(mealType, DictionaryItem::getId, tour.getMealTypeId());
            titleRu.setText(tour.getTitleRu());
            titleEn.setText(tour.getTitleEn());
            start.setValue(tour.getStartDate());
            end.setValue(tour.getEndDate());
            price.setText(tour.getPricePerPerson().stripTrailingZeros().toPlainString());
            seats.getValueFactory().setValue(tour.getTotalSeats());
            featured.setSelected(tour.isFeatured());
            active.setSelected(tour.isActive());
            descRu.setText(tour.getDescriptionRu());
            descEn.setText(tour.getDescriptionEn());
        } else {
            active.setSelected(true);
        }

        add(Ui.field(I18n.t("tf.hotel"), hotel), 0, 0, 2);
        add(Ui.field(I18n.t("tf.vacationType"), vacationType), 0, 1, 1);
        add(Ui.field(I18n.t("tf.mealType"), mealType), 1, 1, 1);
        add(Ui.field(I18n.t("tf.titleRu"), titleRu), 0, 2, 1);
        add(Ui.field(I18n.t("tf.titleEn"), titleEn), 1, 2, 1);
        add(Ui.field(I18n.t("tf.start"), start), 0, 3, 1);
        add(Ui.field(I18n.t("tf.end"), end), 1, 3, 1);
        add(Ui.field(I18n.t("tf.price"), price), 0, 4, 1);
        add(Ui.field(I18n.t("tf.seats"), seats), 1, 4, 1);
        add(Ui.row(24, featured, active), 0, 5, 2);
        add(Ui.field(I18n.t("tf.descRu"), descRu), 0, 6, 1);
        add(Ui.field(I18n.t("tf.descEn"), descEn), 1, 6, 1);
    }

    @Override
    protected Tour collect() {
        tour.setHotelId(hotel.getValue() == null ? null : hotel.getValue().getId());
        tour.setVacationTypeId(vacationType.getValue() == null ? null : vacationType.getValue().getId());
        tour.setMealTypeId(mealType.getValue() == null ? null : mealType.getValue().getId());
        tour.setTitleRu(titleRu.getText());
        tour.setTitleEn(titleEn.getText());
        tour.setStartDate(start.getValue());
        tour.setEndDate(end.getValue());
        tour.setPricePerPerson(Ui.decimal(price));
        tour.setTotalSeats(seats.getValue());
        tour.setFeatured(featured.isSelected());
        tour.setActive(active.isSelected());
        tour.setDescriptionRu(descRu.getText());
        tour.setDescriptionEn(descEn.getText());
        return tour;
    }

    @Override
    protected Request saveRequest(Tour value) {
        return new Request(CommandType.SAVE_TOUR).with("tour", value);
    }
}
