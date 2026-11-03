package by.bsuir.touragency.client.view.dialog;

import by.bsuir.touragency.client.util.I18n;
import by.bsuir.touragency.client.util.Ui;
import by.bsuir.touragency.common.model.Country;
import by.bsuir.touragency.common.protocol.CommandType;
import by.bsuir.touragency.common.protocol.Request;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

public class CountryDialog extends EditDialog<Country> {

    private final Country country;
    private final TextField nameRu = new TextField();
    private final TextField nameEn = new TextField();
    private final TextArea descRu = new TextArea();
    private final TextArea descEn = new TextArea();

    public CountryDialog(Country existing) {
        super(existing == null ? I18n.t("geo.newCountry") : existing.getName());
        this.country = existing == null ? new Country() : existing;
        descRu.setPrefRowCount(4);
        descEn.setPrefRowCount(4);
        descRu.setWrapText(true);
        descEn.setWrapText(true);
        if (existing != null) {
            nameRu.setText(country.getNameRu());
            nameEn.setText(country.getNameEn());
            descRu.setText(country.getDescriptionRu());
            descEn.setText(country.getDescriptionEn());
        }
        add(Ui.field(I18n.t("geo.col.nameRu"), nameRu), 0, 0, 1);
        add(Ui.field(I18n.t("geo.col.nameEn"), nameEn), 1, 0, 1);
        add(Ui.field(I18n.t("geo.descRu"), descRu), 0, 1, 1);
        add(Ui.field(I18n.t("geo.descEn"), descEn), 1, 1, 1);
    }

    @Override
    protected Country collect() {
        country.setNameRu(nameRu.getText());
        country.setNameEn(nameEn.getText());
        country.setDescriptionRu(descRu.getText());
        country.setDescriptionEn(descEn.getText());
        if (country.getImagePath() == null && !nameEn.getText().isBlank()) {
            country.setImagePath("images/countries/" + nameEn.getText().trim().toLowerCase().replace(' ', '-') + ".jpg");
        }
        return country;
    }

    @Override
    protected Request saveRequest(Country value) {
        return new Request(CommandType.SAVE_COUNTRY).with("country", value);
    }
}
