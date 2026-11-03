package by.bsuir.touragency.client.view.manager;

import by.bsuir.touragency.client.component.Badge;
import by.bsuir.touragency.client.util.Alerts;
import by.bsuir.touragency.client.util.Async;
import by.bsuir.touragency.client.util.Fmt;
import by.bsuir.touragency.client.util.Ui;
import by.bsuir.touragency.client.view.BaseView;
import by.bsuir.touragency.client.view.Shell;
import by.bsuir.touragency.client.view.client.TourDetailsView;
import by.bsuir.touragency.client.view.dialog.TourDialog;
import by.bsuir.touragency.common.model.Hotel;
import by.bsuir.touragency.common.model.Tour;
import by.bsuir.touragency.common.model.TourFilter;
import by.bsuir.touragency.common.protocol.CommandType;
import by.bsuir.touragency.common.protocol.Request;
import javafx.animation.PauseTransition;
import javafx.scene.Parent;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.time.LocalDate;
import java.util.ArrayList;

/**
 * Управление турами: поиск, создание, изменение, снятие с продажи и удаление.
 */
public class ManageToursView extends BaseView {

    private final Shell shell;
    private TablePanel<Tour> panel;
    private TextField search;
    private CheckBox showAll;

    public ManageToursView(Shell shell) {
        this.shell = shell;
    }

    @Override
    public String getTitle() {
        return t("mt.title");
    }

    @Override
    protected Parent build() {
        panel = new TablePanel<>();
        search = new TextField();
        search.setPromptText(t("mt.searchPrompt"));
        search.setPrefWidth(240);
        PauseTransition debounce = new PauseTransition(Duration.millis(350));
        debounce.setOnFinished(e -> reload());
        search.textProperty().addListener((o, a, b) -> debounce.playFromStart());
        showAll = new CheckBox(t("mt.showAll"));
        showAll.setSelected(true);
        showAll.setOnAction(e -> reload());
        panel.left(search, showAll);

        panel.button("+  " + t("common.add"), "btn-primary", () -> edit(null));
        panel.action(t("common.edit"), "btn-outline", this::edit);
        panel.action(t("mt.toggle"), "btn-outline", this::toggleActive);
        panel.action(t("common.delete"), "btn-danger", this::delete);
        panel.onDoubleClick(tour -> shell.open(new TourDetailsView(shell, tour.getId())));

        var table = panel.table();
        table.getColumns().add(Ui.column(t("mt.col.id"), Tour::getId, 44));
        table.getColumns().add(Ui.column(t("mt.col.title"), Tour::getTitle, 220));
        table.getColumns().add(Ui.column(t("mt.col.dates"),
                x -> Fmt.dateRange(x.getStartDate(), x.getEndDate()), 190));
        table.getColumns().add(Ui.column(t("mt.col.nights"), Tour::getNights, 62));
        table.getColumns().add(Ui.column(t("mt.col.price"), x -> Fmt.number(x.getPricePerPerson()), 90));
        table.getColumns().add(Ui.column(t("mt.col.seats"),
                x -> x.getFreeSeats() + " / " + x.getTotalSeats(), 90));
        table.getColumns().add(Ui.column(t("mt.col.bookings"), Tour::getBookingsCount, 62));
        TableColumn<Tour, Object> status = Ui.column(t("mt.col.status"), x -> x, 110);
        status.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(Object item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    return;
                }
                Tour tour = (Tour) item;
                if (!tour.getStartDate().isAfter(LocalDate.now())) {
                    setGraphic(new Badge(t("mt.status.past"), Badge.Kind.NEUTRAL));
                } else if (tour.isActive()) {
                    setGraphic(new Badge(t("mt.status.active"), Badge.Kind.SUCCESS));
                } else {
                    setGraphic(new Badge(t("mt.status.inactive"), Badge.Kind.WARNING));
                }
            }
        });
        table.getColumns().add(status);

        VBox page = new VBox(20, pageHeader(t("mt.title"), t("mt.subtitle")), panel);
        page.getStyleClass().add("page");
        reload();
        return page;
    }

    private void reload() {
        TourFilter filter = new TourFilter()
                .setOnlyAvailable(!showAll.isSelected())
                .setSearch(search.getText())
                .setSort(TourFilter.Sort.DATE);
        Async.<ArrayList<Tour>>load(new Request(CommandType.GET_TOURS).with("filter", filter), panel::setItems);
    }

    private void edit(Tour tour) {
        Async.<ArrayList<Hotel>>load(new Request(CommandType.GET_HOTELS), hotels -> {
            if (new TourDialog(tour, hotels).showAndWait().orElse(false)) {
                reload();
            }
        });
    }

    private void toggleActive(Tour tour) {
        Async.send(new Request(CommandType.SET_TOUR_ACTIVE).with("tourId", tour.getId())
                .with("active", !tour.isActive()), response -> {
            if (response.isOk()) {
                reload();
            } else {
                Alerts.error(response.getMessage());
            }
        });
    }

    private void delete(Tour tour) {
        if (!Alerts.confirm(t("mt.confirmDelete", tour.getTitle()))) {
            return;
        }
        Async.send(new Request(CommandType.DELETE_TOUR).with("tourId", tour.getId()), response -> {
            if (response.isOk()) {
                reload();
            } else {
                Alerts.error(response.getMessage());
            }
        });
    }
}
