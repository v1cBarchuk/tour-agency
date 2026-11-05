package by.bsuir.touragency.client.view.client;

import by.bsuir.touragency.client.component.EmptyState;
import by.bsuir.touragency.client.component.FluidCover;
import by.bsuir.touragency.client.util.Async;
import by.bsuir.touragency.client.util.ClientContext;
import by.bsuir.touragency.client.util.CompareList;
import by.bsuir.touragency.client.util.Fmt;
import by.bsuir.touragency.client.util.I18n;
import by.bsuir.touragency.client.view.BaseView;
import by.bsuir.touragency.client.view.Shell;
import by.bsuir.touragency.common.model.DictionaryItem;
import by.bsuir.touragency.common.model.Tour;
import by.bsuir.touragency.common.protocol.CommandType;
import by.bsuir.touragency.common.protocol.Request;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import java.util.function.ToDoubleFunction;

/**
 * Сравнение 2–3 туров: параметры в одной таблице, лучшее значение в каждой строке выделено.
 * Данные запрашиваются заново, чтобы цена, места и процент совпадения были актуальными.
 */
public class CompareView extends BaseView {

    private final Shell shell;
    private final VBox page = new VBox(20);

    public CompareView(Shell shell) {
        this.shell = shell;
    }

    @Override
    public String getTitle() {
        return t("menu.compare");
    }

    @Override
    protected Parent build() {
        page.getStyleClass().add("page");
        reload();
        return page;
    }

    private void reload() {
        page.getChildren().setAll(pageHeader(t("compare.title"), t("compare.subtitle", CompareList.MAX)));
        List<Tour> selected = new ArrayList<>(CompareList.tours());
        if (selected.size() < 2) {
            EmptyState empty = new EmptyState("⇄", t(selected.isEmpty() ? "compare.empty" : "compare.one"),
                    t("compare.emptyHint")).action(t("menu.tours"), () -> shell.openSection("menu.tours"));
            page.getChildren().add(empty);
            if (selected.isEmpty()) return;
        }
        List<Tour> fresh = new ArrayList<>();
        for (Tour s : selected) {
            Async.<Tour>quiet(new Request(CommandType.GET_TOUR).with("tourId", s.getId()).with("noView", true), t -> {
                fresh.add(t);
                if (fresh.size() == selected.size()) {
                    fresh.sort(Comparator.comparingInt(x -> indexOf(selected, x)));
                    page.getChildren().add(table(fresh));
                }
            });
        }
    }

    private static int indexOf(List<Tour> list, Tour t) {
        for (int i = 0; i < list.size(); i++) if (list.get(i).getId().equals(t.getId())) return i;
        return 0;
    }

    private Node table(List<Tour> tours) {
        GridPane g = new GridPane();
        g.getStyleClass().addAll("card", "compare-table");
        g.setHgap(0);
        g.setVgap(0);
        ColumnConstraints first = new ColumnConstraints(190);
        g.getColumnConstraints().add(first);
        for (int i = 0; i < tours.size(); i++) {
            ColumnConstraints c = new ColumnConstraints();
            c.setHgrow(Priority.ALWAYS);
            c.setPercentWidth(-1);
            c.setFillWidth(true);
            c.setMinWidth(200);
            g.getColumnConstraints().add(c);
        }

        // заголовки колонок: фото, отель, место, кнопка «убрать»
        for (int i = 0; i < tours.size(); i++) {
            Tour t = tours.get(i);
            StackPane photo = new StackPane(new FluidCover(t.getImagePath(), t.getHotelName(), 150, 12));
            Button remove = new Button("✕");
            remove.getStyleClass().add("compare-remove");
            remove.setOnAction(e -> {
                CompareList.toggle(t);
                reload();
            });
            StackPane.setAlignment(remove, Pos.TOP_RIGHT);
            photo.getChildren().add(remove);
            Label hotel = new Label(t.getHotelName());
            hotel.getStyleClass().add("card-title");
            hotel.setWrapText(true);
            Label place = new Label(t.getLocation());
            place.getStyleClass().add("card-overline");
            VBox head = new VBox(8, photo, place, hotel);
            head.getStyleClass().add("compare-head");
            g.add(head, i + 1, 0);
        }

        int row = 1;
        row = add(g, row, t("compare.price"), tours, x -> Fmt.money(x.getPricePerPerson()),
                x -> -x.getPricePerPerson().doubleValue());
        row = add(g, row, t("compare.perNight"), tours, x -> Fmt.money(perNight(x)), x -> -perNight(x).doubleValue());
        row = add(g, row, t("compare.dates"), tours, x -> Fmt.dateRange(x.getStartDate(), x.getEndDate()), null);
        row = add(g, row, t("compare.nights"), tours, x -> t("common.nights", x.getNights()), x -> x.getNights());
        row = add(g, row, t("compare.meal"), tours, Tour::getMealTypeName, CompareView::mealRank);
        row = add(g, row, t("compare.type"), tours, Tour::getVacationTypeName, null);
        row = add(g, row, t("compare.stars"), tours, x -> Fmt.stars(x.getHotelStars()), x -> x.getHotelStars());
        row = add(g, row, t("compare.rating"), tours,
                x -> x.getRating() == null ? t("common.noReviews") : String.format("★ %.1f (%d)", x.getRating(), x.getReviewsCount()),
                x -> x.getRating() == null ? -1 : x.getRating());
        if (tours.stream().anyMatch(x -> x.getMatchPercent() != null)) {
            row = add(g, row, t("compare.match"), tours,
                    x -> x.getMatchPercent() == null ? "—" : x.getMatchPercent() + "%",
                    x -> x.getMatchPercent() == null ? -1 : x.getMatchPercent());
        }
        row = add(g, row, t("compare.seats"), tours, x -> x.getFreeSeats() + " / " + x.getTotalSeats(), null);

        // кнопки под колонками
        for (int i = 0; i < tours.size(); i++) {
            Tour t = tours.get(i);
            Button open = new Button(t("tour.details"));
            open.getStyleClass().add("btn-outline");
            open.setOnAction(e -> shell.open(new TourDetailsView(shell, t.getId())));
            Button book = new Button(t("details.book"));
            book.getStyleClass().add("btn-accent");
            book.setDisable(!t.isActive() || t.getFreeSeats() == 0);
            book.setOnAction(e -> shell.open(new BookingView(shell, t)));
            HBox buttons = new HBox(8, open, book);
            buttons.getStyleClass().add("compare-cell");
            g.add(buttons, i + 1, row);
        }
        Label legend = new Label(t("compare.legend"));
        legend.getStyleClass().add("muted");
        return new VBox(10, g, legend);
    }

    /**
     * Строка таблицы. score — «чем больше, тем лучше» (для цены передаётся со знаком минус);
     * null — строка без выделения лучшего значения.
     */
    private static int add(GridPane g, int row, String label, List<Tour> tours, Function<Tour, String> text,
                           ToDoubleFunction<Tour> score) {
        Label l = new Label(label);
        l.getStyleClass().add("field-label");
        VBox head = new VBox(l);
        head.getStyleClass().addAll("compare-cell", "compare-label");
        g.add(head, 0, row);
        double best = score == null ? 0 : tours.stream().mapToDouble(score).max().orElse(0);
        boolean allSame = score == null || tours.stream().mapToDouble(score).distinct().count() == 1;
        for (int i = 0; i < tours.size(); i++) {
            Tour t = tours.get(i);
            Label v = new Label(text.apply(t));
            v.getStyleClass().add("fact-value");
            v.setWrapText(true);
            VBox cell = new VBox(4, v);
            cell.getStyleClass().add("compare-cell");
            if (row % 2 == 0) cell.getStyleClass().add("compare-odd");
            if (!allSame && score.applyAsDouble(t) == best) {
                cell.getStyleClass().add("compare-best");
                Label mark = new Label("★ " + I18n.t("compare.best"));
                mark.getStyleClass().add("compare-best-mark");
                cell.getChildren().add(mark);
            }
            g.add(cell, i + 1, row);
        }
        if (row % 2 == 0) head.getStyleClass().add("compare-odd");
        return row + 1;
    }

    private static BigDecimal perNight(Tour t) {
        return t.getPricePerPerson().divide(BigDecimal.valueOf(Math.max(1, t.getNights())), 0, RoundingMode.HALF_UP);
    }

    /** Чем «полнее» питание, тем лучше: порядок справочника — от «без питания» до «ультра всё включено». */
    private static double mealRank(Tour t) {
        List<DictionaryItem> meals = ClientContext.mealTypes();
        for (int i = 0; i < meals.size(); i++) {
            if (meals.get(i).getId().equals(t.getMealTypeId())) return i;
        }
        return -1;
    }
}
