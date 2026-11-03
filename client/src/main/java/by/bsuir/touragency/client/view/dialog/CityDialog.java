package by.bsuir.touragency.client.view.dialog;

import by.bsuir.touragency.client.util.ClientContext;
import by.bsuir.touragency.client.util.I18n;
import by.bsuir.touragency.client.util.Ui;
import by.bsuir.touragency.common.model.City;
import by.bsuir.touragency.common.model.Country;
import by.bsuir.touragency.common.protocol.CommandType;
import by.bsuir.touragency.common.protocol.Request;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;

public class CityDialog extends EditDialog<City> {

    private final City city;
    private final ComboBox<Country> country;
    private final TextField nameRu = new TextField();
    private final TextField nameEn = new TextField();

    public CityDialog(City existing) {
        super(existing == null ? I18n.t("geo.newCity") : existing.getName());
        this.city = existing == null ? new City() : existing;
        country = Ui.combo(ClientContext.countries(), Country::getName, null);
        if (existing != null) {
            Ui.select(country, Country::getId, city.getCountryId());
            nameRu.setText(city.getNameRu());
            nameEn.setText(city.getNameEn());
        }
        add(Ui.field(I18n.t("geo.col.country"), country), 0, 0, 2);
        add(Ui.field(I18n.t("geo.col.nameRu"), nameRu), 0, 1, 1);
        add(Ui.field(I18n.t("geo.col.nameEn"), nameEn), 1, 1, 1);
    }

    @Override
    protected City collect() {
        city.setCountryId(country.getValue() == null ? null : country.getValue().getId());
        city.setNameRu(nameRu.getText());
        city.setNameEn(nameEn.getText());
        return city;
    }

    @Override
    protected Request saveRequest(City value) {
        return new Request(CommandType.SAVE_CITY).with("city", value);
    }
}
