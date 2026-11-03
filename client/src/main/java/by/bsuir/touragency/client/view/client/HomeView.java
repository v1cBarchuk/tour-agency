package by.bsuir.touragency.client.view.client;

import by.bsuir.touragency.client.component.CountryCard;
import by.bsuir.touragency.client.component.TourCard;
import by.bsuir.touragency.client.util.Async;
import by.bsuir.touragency.client.util.ClientContext;
import by.bsuir.touragency.client.util.Images;
import by.bsuir.touragency.client.util.Ui;
import by.bsuir.touragency.client.view.BaseView;
import by.bsuir.touragency.client.view.Shell;
import by.bsuir.touragency.common.model.Country;
import by.bsuir.touragency.common.model.Tour;
import by.bsuir.touragency.common.model.TourFilter;
import by.bsuir.touragency.common.protocol.CommandType;
import by.bsuir.touragency.common.protocol.Request;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;

/**
 * Главная страница клиента: большой баннер с поиском, популярные направления,
 * горящие и рекомендуемые туры, преимущества.
 */
public class HomeView extends BaseView {

    private final Shell shell;
    private FlowPane destinations;
    private FlowPane hot;
    private FlowPane featured;

    public HomeView(Shell shell) {
        this.shell = shell;
    }

    @Override
    public String getTitle() {
        return t("menu.home");
    }

    @Override
    protected Parent build() {
        destinations = grid();
        hot = grid();
        featured = grid();
        VBox page = new VBox(32,
                hero(),
                section(t("home.popularDestinations"), destinations, "menu.destinations"),
                section(t("home.hotOffers"), hot, "menu.tours"),
                section(t("home.featured"), featured, "menu.tours"),
                whyUs());
        page.getStyleClass().add("page");
        load();
        return page;
    }

    private Node hero() {
        StackPane hero = new StackPane();
        hero.getStyleClass().add("hero");
        hero.setMinHeight(340);
        var photo = Images.raw("images/hero/home.jpg");
        if (photo != null) {
            ImageView iv = new ImageView(photo);
            iv.setPreserveRatio(false);
            iv.fitWidthProperty().bind(hero.widthProperty());
            iv.fitHeightProperty().bind(hero.heightProperty());
            hero.getChildren().add(iv);
        } else {
            Region bg = new Region();
            bg.getStyleClass().add("hero-bg");
            hero.getChildren().add(bg);
        }
        Region shade = new Region();
        shade.getStyleClass().add("hero-shade");

        Label title = new Label(t("hero.title"));
        title.getStyleClass().add("hero-title");
        title.setWrapText(true);
        Label subtitle = new Label(t("hero.subtitle"));
        subtitle.getStyleClass().add("hero-subtitle");
        subtitle.setWrapText(true);
        VBox text = new VBox(10, title, subtitle, searchPanel());
        text.setPadding(new Insets(40, 40, 32, 40));
        text.setAlignment(Pos.BOTTOM_LEFT);
        hero.getChildren().addAll(shade, text);
        return hero;
    }

    /** Поисковая панель: страна, дата, ночи, туристы, бюджет → каталог с фильтром. */
    private Node searchPanel() {
        ComboBox<Country> country = Ui.combo(ClientContext.countries(), Country::getName, t("common.anyF"));
        DatePicker date = Ui.date(null);
        Spinner<Integer> nights = new Spinner<>(1, 21, 7);
        nights.setEditable(true);
        Spinner<Integer> persons = new Spinner<>(1, 9, 2);
        persons.setEditable(true);
        TextField budget = Ui.numberField("3000");
        Button find = new Button(t("home.find"));
        find.getStyleClass().addAll("btn-accent", "btn-large");
        find.setMinWidth(javafx.scene.layout.Region.USE_PREF_SIZE);
        find.setOnAction(e -> {
            TourFilter f = new TourFilter()
                    .setCountryId(country.getValue() == null ? null : country.getValue().getId())
                    .setDateFrom(date.getValue())
                    .setDateTo(date.getValue() == null ? null : date.getValue().plusDays(14))
                    .setMinNights(Math.max(1, nights.getValue() - 2))
                    .setMaxNights(nights.getValue() + 3)
                    .setPersons(persons.getValue())
                    .setMaxPrice(Ui.decimal(budget));
            shell.openSection("menu.tours", new ToursView(shell, f));
        });

        HBox panel = new HBox(12,
                grow(Ui.field(t("home.country"), country), 1.6),
                grow(Ui.field(t("home.date"), date), 1.2),
                grow(Ui.field(t("home.nights"), nights), 0.7),
                grow(Ui.field(t("home.persons"), persons), 0.7),
                grow(Ui.field(t("home.budget"), budget), 1),
                find);
        panel.setAlignment(Pos.BOTTOM_LEFT);
        panel.getStyleClass().add("search-panel");
        return panel;
    }

    private static Node grow(VBox field, double weight) {
        HBox.setHgrow(field, Priority.ALWAYS);
        field.setPrefWidth(140 * weight);
        return field;
    }

    private Node section(String title, FlowPane body, String sectionKey) {
        Label h = new Label(title);
        h.getStyleClass().add("section-title");
        Hyperlink all = new Hyperlink(t("home.viewAll") + "  →");
        all.setOnAction(e -> shell.openSection(sectionKey));
        HBox header = Ui.row(8, h, Ui.hSpacer(), all);
        return new VBox(14, header, body);
    }

    private static FlowPane grid() {
        FlowPane pane = new FlowPane(18, 18);
        pane.getChildren().add(new Label(t("common.loading")));
        return pane;
    }

    private Node whyUs() {
        Label h = new Label(t("home.whyUs"));
        h.getStyleClass().add("section-title");
        HBox cards = new HBox(18,
                why("◎", "home.why1"), why("BYN", "home.why2"), why("★", "home.why3"));
        return new VBox(14, h, cards);
    }

    private static Node why(String icon, String key) {
        Label i = new Label(icon);
        i.getStyleClass().add("why-icon");
        Label title = new Label(t(key + ".title"));
        title.getStyleClass().add("card-title");
        Label text = new Label(t(key + ".text"));
        text.setWrapText(true);
        text.getStyleClass().add("card-info");
        VBox card = new VBox(10, i, title, text);
        card.getStyleClass().addAll("card", "why-card");
        HBox.setHgrow(card, Priority.ALWAYS);
        card.setMaxWidth(Double.MAX_VALUE);
        return card;
    }

    private void load() {
        Async.<ArrayList<Country>>load(new Request(CommandType.GET_COUNTRIES), countries -> {
            destinations.getChildren().clear();
            countries.stream().filter(c -> c.getToursCount() > 0).limit(3).forEach(c -> destinations.getChildren()
                    .add(new CountryCard(c, 292, 200, x -> shell.open(new CountryView(shell, x)))));
        });
        Async.<ArrayList<Tour>>load(new Request(CommandType.GET_TOURS).with("filter", new TourFilter()), tours -> {
            fill(hot, tours.stream().filter(Tour::isHot).limit(3).toList());
            fill(featured, tours.stream().filter(Tour::isFeatured).limit(3).toList());
        });
    }

    private void fill(FlowPane pane, List<Tour> tours) {
        pane.getChildren().clear();
        if (tours.isEmpty()) {
            Label none = new Label(t("common.empty"));
            none.getStyleClass().add("muted");
            pane.getChildren().add(none);
        }
        tours.forEach(t -> pane.getChildren().add(new TourCard(t, x -> shell.open(new TourDetailsView(shell, x.getId())))));
    }
}
