package by.bsuir.touragency.client.view.dialog;

import by.bsuir.touragency.client.util.ClientContext;
import by.bsuir.touragency.client.util.I18n;
import by.bsuir.touragency.client.util.Ui;
import by.bsuir.touragency.common.model.City;
import by.bsuir.touragency.common.model.DictionaryItem;
import by.bsuir.touragency.common.model.Hotel;
import by.bsuir.touragency.common.protocol.CommandType;
import by.bsuir.touragency.common.protocol.Request;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Spinner;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class HotelDialog extends EditDialog<Hotel> {

    private final Hotel hotel;
    private final ComboBox<City> city;
    private final TextField name = new TextField();
    private final Spinner<Integer> stars = new Spinner<>(1, 5, 4);
    private final TextField address = new TextField();
    private final TextArea descRu = new TextArea();
    private final TextArea descEn = new TextArea();
    private final Map<Integer, CheckBox> amenities = new LinkedHashMap<>();

    public HotelDialog(Hotel existing) {
        super(existing == null ? I18n.t("geo.newHotel") : existing.getName());
        this.hotel = existing == null ? new Hotel() : existing;
        city = Ui.combo(ClientContext.cities(), City::getDisplayName, null);
        descRu.setPrefRowCount(3);
        descEn.setPrefRowCount(3);
        descRu.setWrapText(true);
        descEn.setWrapText(true);

        FlowPane checks = new FlowPane(14, 8);
        for (DictionaryItem a : ClientContext.amenities()) {
            CheckBox cb = new CheckBox(a.getName());
            amenities.put(a.getId(), cb);
            checks.getChildren().add(cb);
        }
        if (existing != null) {
            Ui.select(city, City::getId, hotel.getCityId());
            name.setText(hotel.getName());
            stars.getValueFactory().setValue(hotel.getStars());
            address.setText(hotel.getAddress());
            descRu.setText(hotel.getDescriptionRu());
            descEn.setText(hotel.getDescriptionEn());
            hotel.getAmenityIds().forEach(id -> {
                CheckBox cb = amenities.get(id);
                if (cb != null) cb.setSelected(true);
            });
        }
        add(Ui.field(I18n.t("geo.col.hotel"), name), 0, 0, 1);
        add(Ui.field(I18n.t("geo.col.city"), city), 1, 0, 1);
        add(Ui.field(I18n.t("geo.col.stars"), stars), 0, 1, 1);
        add(Ui.field(I18n.t("geo.address"), address), 1, 1, 1);
        add(Ui.field(I18n.t("geo.descRu"), descRu), 0, 2, 1);
        add(Ui.field(I18n.t("geo.descEn"), descEn), 1, 2, 1);
        add(Ui.field(I18n.t("geo.amenities"), checks), 0, 3, 2);
    }

    @Override
    protected Hotel collect() {
        hotel.setCityId(city.getValue() == null ? null : city.getValue().getId());
        hotel.setName(name.getText());
        hotel.setStars(stars.getValue());
        hotel.setAddress(address.getText());
        hotel.setDescriptionRu(descRu.getText());
        hotel.setDescriptionEn(descEn.getText());
        List<Integer> selected = new ArrayList<>();
        amenities.forEach((id, cb) -> {
            if (cb.isSelected()) selected.add(id);
        });
        hotel.setAmenityIds(selected);
        return hotel;
    }

    @Override
    protected Request saveRequest(Hotel value) {
        return new Request(CommandType.SAVE_HOTEL).with("hotel", value);
    }
}
