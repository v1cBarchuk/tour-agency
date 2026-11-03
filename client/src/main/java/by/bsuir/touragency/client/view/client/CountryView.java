package by.bsuir.touragency.client.view.client;

import by.bsuir.touragency.client.component.Badge;
import by.bsuir.touragency.client.component.HotelCard;
import by.bsuir.touragency.client.component.TourCard;
import by.bsuir.touragency.client.util.Async;
import by.bsuir.touragency.client.util.ClientContext;
import by.bsuir.touragency.client.util.Images;
import by.bsuir.touragency.client.view.BaseView;
import by.bsuir.touragency.client.view.Shell;
import by.bsuir.touragency.common.model.City;
import by.bsuir.touragency.common.model.Country;
import by.bsuir.touragency.common.model.Hotel;
import by.bsuir.touragency.common.model.Tour;
import by.bsuir.touragency.common.model.TourFilter;
import by.bsuir.touragency.common.protocol.CommandType;
import by.bsuir.touragency.common.protocol.Request;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.ArrayList;

/** Страница страны: описание, города, отели и доступные туры. */
public class CountryView extends BaseView {

    private final Shell shell;
    private final Country country;

    public CountryView(Shell shell, Country country) {
        this.shell = shell;
        this.country = country;
    }

    @Override
    public String getTitle() {
        return country.getName();
    }

    @Override
    protected Parent build() {
        // баннер
        StackPane banner = new StackPane(Images.cover(country.getImagePath(), null, 940, 240, 18));
        banner.setMaxWidth(940);
        banner.setAlignment(Pos.TOP_LEFT);
        banner.getStyleClass().add("banner");
        Region shade = new Region();
        shade.getStyleClass().add("country-shade");
        Label name = new Label(country.getName());
        name.getStyleClass().add("hero-title");
        Label desc = new Label(country.getDescription());
        desc.getStyleClass().add("hero-subtitle");
        desc.setWrapText(true);
        VBox text = new VBox(6, name, desc);
        text.setPadding(new Insets(28));
        text.setAlignment(Pos.BOTTOM_LEFT);
        banner.getChildren().addAll(shade, text);

        FlowPane cities = new FlowPane(8, 8);
        for (City c : ClientContext.ref().citiesOf(country.getId())) {
            cities.getChildren().add(new Badge(c.getName(), Badge.Kind.NEUTRAL));
        }

        Label hotelsTitle = new Label(t("dest.hotels"));
        hotelsTitle.getStyleClass().add("section-title");
        VBox hotels = new VBox(14, new Label(t("common.loading")));

        Label toursTitle = new Label(t("dest.toursIn"));
        toursTitle.getStyleClass().add("section-title");
        FlowPane tours = new FlowPane(18, 18);

        Async.<ArrayList<Hotel>>load(new Request(CommandType.GET_HOTELS).with("countryId", country.getId()), list -> {
            hotelsTitle.setText(t("dest.hotelsIn", list.size()));
            hotels.getChildren().clear();
            list.forEach(h -> hotels.getChildren().add(new HotelCard(h)));
        });
        Async.<ArrayList<Tour>>load(new Request(CommandType.GET_TOURS)
                .with("filter", new TourFilter().setCountryId(country.getId())), list -> {
            if (list.isEmpty()) {
                Label none = new Label(t("dest.noTours"));
                none.getStyleClass().add("muted");
                tours.getChildren().add(none);
            }
            list.forEach(tour -> tours.getChildren().add(
                    new TourCard(tour, x -> shell.open(new TourDetailsView(shell, x.getId())))));
        });

        VBox page = new VBox(22, banner, cities, toursTitle, tours, hotelsTitle, hotels);
        page.getStyleClass().add("page");
        return page;
    }
}
