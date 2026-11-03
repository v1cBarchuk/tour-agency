package by.bsuir.touragency.client.view.manager;

import by.bsuir.touragency.client.util.Async;
import by.bsuir.touragency.client.util.ClientContext;
import by.bsuir.touragency.client.util.Fmt;
import by.bsuir.touragency.client.util.Ui;
import by.bsuir.touragency.client.view.BaseView;
import by.bsuir.touragency.client.view.dialog.CityDialog;
import by.bsuir.touragency.client.view.dialog.CountryDialog;
import by.bsuir.touragency.client.view.dialog.EditDialog;
import by.bsuir.touragency.client.view.dialog.HotelDialog;
import by.bsuir.touragency.common.model.City;
import by.bsuir.touragency.common.model.Country;
import by.bsuir.touragency.common.model.Hotel;
import by.bsuir.touragency.common.model.ReferenceData;
import by.bsuir.touragency.common.protocol.CommandType;
import by.bsuir.touragency.common.protocol.Request;
import javafx.scene.Parent;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.VBox;

import java.util.ArrayList;

/**
 * Страны, города и отели. После сохранения справочники клиента
 * перезагружаются, чтобы новые записи сразу появились в формах и фильтрах.
 */
public class GeoView extends BaseView {

    private TablePanel<Country> countries;
    private TablePanel<City> cities;
    private TablePanel<Hotel> hotels;
    private ComboBox<Country> hotelCountry;

    @Override
    public String getTitle() {
        return t("geo.title");
    }

    @Override
    protected Parent build() {
        countries = new TablePanel<>();
        countries.button("+  " + t("geo.newCountry"), "btn-primary", () -> edit(new CountryDialog(null)));
        countries.action(t("common.edit"), "btn-outline", c -> edit(new CountryDialog(c)));
        countries.onDoubleClick(c -> edit(new CountryDialog(c)));
        var ct = countries.table();
        ct.getColumns().add(Ui.column(t("geo.col.nameRu"), Country::getNameRu, 200));
        ct.getColumns().add(Ui.column(t("geo.col.nameEn"), Country::getNameEn, 200));
        ct.getColumns().add(Ui.column(t("geo.col.cities"), Country::getCitiesCount, 100));
        ct.getColumns().add(Ui.column(t("geo.col.tours"), Country::getToursCount, 100));
        ct.getColumns().add(Ui.column(t("geo.col.minPrice"), c -> Fmt.money(c.getMinPrice()), 140));

        cities = new TablePanel<>();
        cities.button("+  " + t("geo.newCity"), "btn-primary", () -> edit(new CityDialog(null)));
        cities.action(t("common.edit"), "btn-outline", c -> edit(new CityDialog(c)));
        cities.onDoubleClick(c -> edit(new CityDialog(c)));
        var cit = cities.table();
        cit.getColumns().add(Ui.column(t("geo.col.country"), City::getCountryName, 180));
        cit.getColumns().add(Ui.column(t("geo.col.nameRu"), City::getNameRu, 200));
        cit.getColumns().add(Ui.column(t("geo.col.nameEn"), City::getNameEn, 200));
        cit.getColumns().add(Ui.column(t("geo.col.hotels"), City::getHotelsCount, 100));

        hotels = new TablePanel<>();
        hotelCountry = Ui.combo(ClientContext.countries(), Country::getName, t("common.all"));
        hotelCountry.setPrefWidth(220);
        hotelCountry.setOnAction(e -> loadHotels());
        hotels.left(hotelCountry);
        hotels.button("+  " + t("geo.newHotel"), "btn-primary", () -> edit(new HotelDialog(null)));
        hotels.action(t("common.edit"), "btn-outline", h -> edit(new HotelDialog(h)));
        hotels.onDoubleClick(h -> edit(new HotelDialog(h)));
        var ht = hotels.table();
        ht.getColumns().add(Ui.column(t("geo.col.hotel"), Hotel::getName, 240));
        ht.getColumns().add(Ui.column(t("geo.col.city"), Hotel::getLocation, 200));
        ht.getColumns().add(Ui.column(t("geo.col.stars"), h -> Fmt.stars(h.getStars()), 90));
        ht.getColumns().add(Ui.column(t("geo.col.rating"),
                h -> h.getRating() == null ? "—" : Fmt.rating(h.getRating()) + " (" + h.getReviewsCount() + ")", 100));
        ht.getColumns().add(Ui.column(t("geo.col.minPrice"), h -> Fmt.money(h.getMinPrice()), 130));
        ht.getColumns().add(Ui.column(t("geo.amenities"), h -> String.join(", ", h.getAmenityNames()), 220));

        TabPane tabs = new TabPane(
                new Tab(t("geo.countries"), countries),
                new Tab(t("geo.cities"), cities),
                new Tab(t("geo.hotels"), hotels));
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabs.getStyleClass().add("tabs");

        VBox page = new VBox(20, pageHeader(t("geo.title"), t("geo.subtitle")), tabs);
        page.getStyleClass().add("page");
        reloadAll();
        return page;
    }

    private void edit(EditDialog<?> dialog) {
        if (dialog.showAndWait().orElse(false)) {
            reloadAll();
        }
    }

    private void reloadAll() {
        Async.<ReferenceData>load(new Request(CommandType.GET_REFERENCE_DATA), ref -> {
            ClientContext.setReference(ref);
            countries.setItems(ref.getCountries());
            cities.setItems(ref.getCities());
        });
        loadHotels();
    }

    private void loadHotels() {
        Country c = hotelCountry.getValue();
        Async.<ArrayList<Hotel>>load(new Request(CommandType.GET_HOTELS)
                .with("countryId", c == null ? null : c.getId()), hotels::setItems);
    }
}
