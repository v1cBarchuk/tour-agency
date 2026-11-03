package by.bsuir.touragency.client.view.client;

import by.bsuir.touragency.client.component.EmptyState;
import by.bsuir.touragency.client.component.TourCard;
import by.bsuir.touragency.client.util.Async;
import by.bsuir.touragency.client.util.ClientContext;
import by.bsuir.touragency.client.util.Ui;
import by.bsuir.touragency.client.view.BaseView;
import by.bsuir.touragency.client.view.Shell;
import by.bsuir.touragency.common.model.Country;
import by.bsuir.touragency.common.model.DictionaryItem;
import by.bsuir.touragency.common.model.Tour;
import by.bsuir.touragency.common.model.TourFilter;
import by.bsuir.touragency.common.protocol.CommandType;
import by.bsuir.touragency.common.protocol.Request;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Каталог туров: панель фильтров, сортировка и сетка карточек.
 */
public class ToursView extends BaseView {

    private final Shell shell;
    private final TourFilter initial;

    private ComboBox<Country> country;
    private TextField maxPrice;
    private DatePicker dateFrom;
    private DatePicker dateTo;
    private TextField minNights;
    private TextField maxNights;
    private ComboBox<Integer> stars;
    private ComboBox<DictionaryItem> vacationType;
    private ComboBox<DictionaryItem> mealType;
    private TextField persons;
    private ComboBox<TourFilter.Sort> sort;
    private FlowPane results;
    private Label found;

    public ToursView(Shell shell, TourFilter initial) {
        this.shell = shell;
        this.initial = initial == null ? new TourFilter() : initial;
    }

    @Override
    public String getTitle() {
        return t("tours.title");
    }

    @Override
    protected Parent build() {
        found = new Label(t("common.loading"));
        found.getStyleClass().add("page-subtitle");
        Label title = new Label(t("tours.title"));
        title.getStyleClass().add("page-title");

        sort = Ui.combo(Arrays.asList(TourFilter.Sort.values()), s -> t("sort." + s.name()), null);
        sort.getSelectionModel().select(initial.getSort());
        sort.setPrefWidth(200);
        sort.setOnAction(e -> search());
        HBox header = Ui.row(12, new VBox(2, title, found), Ui.hSpacer(), new Label(t("tours.sort")), sort);

        results = new FlowPane(18, 18);
        results.setAlignment(Pos.TOP_LEFT);

        VBox page = new VBox(20, header, filters(), results);
        page.getStyleClass().add("page");
        search();
        return page;
    }

    private Parent filters() {
        country = Ui.combo(ClientContext.countries(), Country::getName, t("common.anyF"));
        Ui.select(country, Country::getId, initial.getCountryId());
        maxPrice = Ui.numberField("—");
        if (initial.getMaxPrice() != null) maxPrice.setText(initial.getMaxPrice().toPlainString());
        dateFrom = Ui.date(initial.getDateFrom());
        dateTo = Ui.date(initial.getDateTo());
        minNights = Ui.numberField("—");
        if (initial.getMinNights() != null) minNights.setText(initial.getMinNights().toString());
        maxNights = Ui.numberField("—");
        if (initial.getMaxNights() != null) maxNights.setText(initial.getMaxNights().toString());
        stars = Ui.combo(List.of(3, 4, 5), s -> s + "★", t("common.any"));
        Ui.select(stars, s -> s, initial.getMinStars());
        vacationType = Ui.combo(ClientContext.vacationTypes(), DictionaryItem::getName, t("common.any"));
        Ui.select(vacationType, DictionaryItem::getId, initial.getVacationTypeId());
        mealType = Ui.combo(ClientContext.mealTypes(), DictionaryItem::getName, t("common.any"));
        Ui.select(mealType, DictionaryItem::getId, initial.getMealTypeId());
        persons = Ui.numberField("—");
        if (initial.getPersons() != null) persons.setText(initial.getPersons().toString());

        Button apply = new Button(t("common.apply"));
        apply.getStyleClass().add("btn-primary");
        apply.setOnAction(e -> search());
        Button reset = new Button(t("common.reset"));
        reset.getStyleClass().add("btn-outline");
        reset.setOnAction(e -> shell.openSection("menu.tours"));

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(12);
        for (javafx.scene.layout.Region r : new javafx.scene.layout.Region[]{country, dateFrom, dateTo, maxPrice,
                persons, vacationType, mealType, stars, minNights, maxNights}) {
            r.setPrefWidth(120);
            r.setMinWidth(60);
        }
        grid.add(Ui.field(t("home.country"), country), 0, 0);
        grid.add(Ui.field(t("tours.dateFrom"), dateFrom), 1, 0);
        grid.add(Ui.field(t("tours.dateTo"), dateTo), 2, 0);
        grid.add(Ui.field(t("tours.maxPrice"), maxPrice), 3, 0);
        grid.add(Ui.field(t("tours.persons"), persons), 4, 0);
        grid.add(Ui.field(t("tours.vacationType"), vacationType), 0, 1);
        grid.add(Ui.field(t("tours.mealType"), mealType), 1, 1);
        grid.add(Ui.field(t("tours.stars"), stars), 2, 1);
        grid.add(Ui.field(t("tours.minNights"), minNights), 3, 1);
        grid.add(Ui.field(t("tours.maxNights"), maxNights), 4, 1);
        for (int i = 0; i < 5; i++) {
            javafx.scene.layout.ColumnConstraints cc = new javafx.scene.layout.ColumnConstraints();
            cc.setPercentWidth(20);
            grid.getColumnConstraints().add(cc);
        }
        HBox buttons = Ui.row(8, Ui.hSpacer(), reset, apply);
        VBox box = new VBox(14, grid, buttons);
        box.getStyleClass().addAll("card", "filter-panel");
        return box;
    }

    private TourFilter collect() {
        return new TourFilter()
                .setCountryId(country.getValue() == null ? null : country.getValue().getId())
                .setMaxPrice(Ui.decimal(maxPrice))
                .setDateFrom(dateFrom.getValue())
                .setDateTo(dateTo.getValue())
                .setMinNights(Ui.integer(minNights))
                .setMaxNights(Ui.integer(maxNights))
                .setMinStars(stars.getValue())
                .setVacationTypeId(vacationType.getValue() == null ? null : vacationType.getValue().getId())
                .setMealTypeId(mealType.getValue() == null ? null : mealType.getValue().getId())
                .setPersons(Ui.integer(persons))
                .setSort(sort.getValue());
    }

    private void search() {
        if (results == null) return;
        Async.<ArrayList<Tour>>load(new Request(CommandType.GET_TOURS).with("filter", collect()), tours -> {
            found.setText(t("tours.subtitle", tours.size()));
            results.getChildren().clear();
            if (tours.isEmpty()) {
                EmptyState empty = new EmptyState("✈", t("common.empty"), t("common.emptyHint"));
                empty.setPrefWidth(900);
                results.getChildren().add(empty);
                return;
            }
            tours.forEach(tour -> results.getChildren().add(
                    new TourCard(tour, x -> shell.open(new TourDetailsView(shell, x.getId())))));
        });
    }
}
