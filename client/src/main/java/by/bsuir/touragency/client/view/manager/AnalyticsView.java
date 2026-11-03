package by.bsuir.touragency.client.view.manager;

import by.bsuir.touragency.client.component.KpiTile;
import by.bsuir.touragency.client.util.Alerts;
import by.bsuir.touragency.client.util.Async;
import by.bsuir.touragency.client.util.Charts;
import by.bsuir.touragency.client.util.Fmt;
import by.bsuir.touragency.client.util.PdfExporter;
import by.bsuir.touragency.client.util.Texts;
import by.bsuir.touragency.client.util.Ui;
import by.bsuir.touragency.client.util.XlsxExporter;
import by.bsuir.touragency.client.view.BaseView;
import by.bsuir.touragency.client.view.Navigator;
import by.bsuir.touragency.common.model.BookingStatus;
import by.bsuir.touragency.common.model.NamedValue;
import by.bsuir.touragency.common.model.Stats;
import by.bsuir.touragency.common.protocol.CommandType;
import by.bsuir.touragency.common.protocol.Request;
import javafx.collections.FXCollections;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import java.io.File;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Аналитика за период: показатели, графики, таблицы и экспорт в Excel и PDF.
 */
public class AnalyticsView extends BaseView {

    private final DatePicker from = Ui.date(LocalDate.now().withDayOfYear(1));
    private final DatePicker to = Ui.date(LocalDate.now());
    private VBox report;
    private Stats current;

    @Override
    public String getTitle() {
        return t("menu.analytics");
    }

    @Override
    protected Parent build() {
        Button month = preset(t("an.month"), LocalDate.now().withDayOfMonth(1));
        Button quarter = preset(t("an.quarter"), LocalDate.now().minusMonths(3));
        Button year = preset(t("an.year"), LocalDate.now().withDayOfYear(1));
        Button all = preset(t("an.all"), LocalDate.of(2020, 1, 1));
        Button apply = new Button(t("common.apply"));
        apply.getStyleClass().add("btn-primary");
        apply.setOnAction(e -> load());
        Button excel = new Button("⤓  Excel");
        excel.getStyleClass().add("btn-outline");
        excel.setOnAction(e -> exportExcel());
        Button pdf = new Button("⤓  PDF");
        pdf.getStyleClass().add("btn-outline");
        pdf.setOnAction(e -> exportPdf());
        from.setPrefWidth(160);
        to.setPrefWidth(160);
        from.setMinWidth(150);
        to.setMinWidth(150);
        HBox dates = Ui.row(10, Ui.field(t("an.from"), from), Ui.field(t("an.to"), to), apply, Ui.hSpacer(), excel, pdf);
        dates.setAlignment(Pos.BOTTOM_LEFT);
        HBox presets = Ui.row(8, month, quarter, year, all);
        VBox filters = new VBox(12, dates, presets);
        filters.getStyleClass().addAll("card", "filter-panel");
        for (Button b : new Button[]{apply, excel, pdf, month, quarter, year, all}) {
            b.setMinWidth(javafx.scene.layout.Region.USE_PREF_SIZE);
        }

        report = new VBox(18);
        report.getStyleClass().add("report");
        VBox page = new VBox(20, pageHeader(t("an.title"), t("an.subtitle")), filters, report);
        page.getStyleClass().add("page");
        load();
        return page;
    }

    private Button preset(String text, LocalDate start) {
        Button b = new Button(text);
        b.getStyleClass().add("chip-button");
        b.setOnAction(e -> {
            from.setValue(start);
            to.setValue(LocalDate.now());
            load();
        });
        return b;
    }

    private void load() {
        Async.send(new Request(CommandType.GET_ANALYTICS).with("from", from.getValue()).with("to", to.getValue()), r -> {
            if (!r.isOk()) {
                Alerts.error(r.getMessage());
                return;
            }
            current = r.getData();
            render(current);
        });
    }

    private void render(Stats s) {
        Label period = new Label(t("an.period", Fmt.date(s.getFrom()), Fmt.date(s.getTo())));
        period.getStyleClass().add("section-title");

        GridPane kpi = new GridPane();
        kpi.setHgap(14);
        kpi.setVgap(14);
        Node[] tiles = {
                new KpiTile("▤", t("an.bookings"), Charts.number(s.kpi("bookings")), t("an.tourists", Charts.number(s.kpi("tourists")))),
                new KpiTile("BYN", t("an.revenue"), money(s.kpi("revenue")), t("an.avgCheck", money(s.kpi("avgCheck")))),
                new KpiTile("✕", t("an.cancellations"), Charts.number(s.kpi("cancellations")), t("an.penalties", money(s.kpi("penalties")))),
                new KpiTile("%", t("an.discounts"), money(s.kpi("discounts")), null),
        };
        for (int i = 0; i < tiles.length; i++) {
            kpi.add(tiles[i], i, 0);
            ColumnConstraints cc = new ColumnConstraints();
            cc.setPercentWidth(25);
            kpi.getColumnConstraints().add(cc);
        }

        GridPane charts = new GridPane();
        charts.setHgap(14);
        charts.setVgap(14);
        charts.add(Charts.bar(t("an.chart.revenue"), s.series("revenueByMonth"), Charts::month, "BYN"), 0, 0);
        charts.add(Charts.bar(t("an.chart.countries"), s.series("salesByCountry"), x -> x, "BYN"), 1, 0);
        charts.add(Charts.line(t("an.chart.bookings"), s.series("bookingsByMonth"), Charts::month, ""), 0, 1);
        charts.add(Charts.pie(t("an.chart.statuses"), s.series("bookingsByStatus"),
                code -> Texts.status(BookingStatus.valueOf(code))), 1, 1);
        for (int i = 0; i < 2; i++) {
            ColumnConstraints cc = new ColumnConstraints();
            cc.setPercentWidth(50);
            charts.getColumnConstraints().add(cc);
        }

        GridPane tables = new GridPane();
        tables.setHgap(14);
        tables.setVgap(14);
        tables.add(table(t("an.t.hotels"), s.series("popularHotels"), t("an.c.hotel"), t("an.c.bookings"),
                t("an.c.revenue"), true), 0, 0);
        tables.add(table(t("an.t.destinations"), s.series("salesByDestination"), t("an.c.destination"),
                t("an.c.bookings"), t("an.c.revenue"), true), 1, 0);
        tables.add(table(t("an.t.ratings"), s.series("destinationRatings"), t("an.c.country"), t("an.c.rating"),
                t("an.c.reviews"), false), 0, 1);
        tables.add(table(t("an.t.discounts"), s.series("discountsByName"), t("an.c.discount"), t("an.c.amount"),
                t("an.c.bookings"), false, true), 1, 1);
        for (int i = 0; i < 2; i++) {
            ColumnConstraints cc = new ColumnConstraints();
            cc.setPercentWidth(50);
            tables.getColumnConstraints().add(cc);
        }
        report.getChildren().setAll(period, kpi, charts, tables);
    }

    private Node table(String title, List<NamedValue> rows, String c1, String c2, String c3, boolean moneyExtra) {
        return table(title, rows, c1, c2, c3, moneyExtra, false);
    }

    // Перегрузка: второй столбец — денежная сумма (таблица скидок)
    private Node table(String title, List<NamedValue> rows, String c1, String c2, String c3, boolean moneyExtra,
                       boolean moneyValue) {
        TableView<NamedValue> table = new TableView<>(FXCollections.observableArrayList(rows));
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.getColumns().add(Ui.column(c1, NamedValue::name, 180));
        table.getColumns().add(Ui.column(c2, v -> moneyValue ? Fmt.number(BigDecimal.valueOf(v.value()))
                : Charts.number(v.value()), 80));
        table.getColumns().add(Ui.column(c3, v -> moneyExtra ? Fmt.number(BigDecimal.valueOf(v.extra()))
                : Charts.number(v.extra()), 90));
        table.setPrefHeight(Math.min(320, 48 + rows.size() * 42));
        table.setPlaceholder(new Label(t("common.empty")));
        Label h = new Label(title);
        h.getStyleClass().add("chart-title");
        VBox box = new VBox(8, h, table);
        box.getStyleClass().addAll("card", "chart-card");
        VBox.setVgrow(table, Priority.ALWAYS);
        return box;
    }

    private static String money(double v) {
        return Fmt.money(BigDecimal.valueOf(v));
    }

    // ------------------------------------------------------------ экспорт

    private File chooseFile(String ext, String description) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle(t("an.export"));
        chooser.setInitialFileName("voyago-report-" + from.getValue() + "_" + to.getValue() + "." + ext);
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(description, "*." + ext));
        return chooser.showSaveDialog(Navigator.getStage());
    }

    private void exportExcel() {
        if (current == null) return;
        File file = chooseFile("xlsx", "Excel");
        if (file == null) return;
        Stats s = current;
        List<XlsxExporter.Sheet> sheets = new ArrayList<>();
        List<List<Object>> kpiRows = new ArrayList<>();
        kpiRows.add(List.of(t("an.from"), s.getFrom().toString()));
        kpiRows.add(List.of(t("an.to"), s.getTo().toString()));
        kpiRows.add(List.of(t("an.bookings"), s.kpi("bookings")));
        kpiRows.add(List.of(t("an.revenue") + ", BYN", s.kpi("revenue")));
        kpiRows.add(List.of(t("an.avgCheckShort") + ", BYN", round(s.kpi("avgCheck"))));
        kpiRows.add(List.of(t("an.cancellations"), s.kpi("cancellations")));
        kpiRows.add(List.of(t("an.penaltiesShort") + ", BYN", s.kpi("penalties")));
        kpiRows.add(List.of(t("an.discounts") + ", BYN", s.kpi("discounts")));
        sheets.add(new XlsxExporter.Sheet(t("an.sheet.summary"), List.of(t("an.c.metric"), t("an.c.value")), kpiRows));
        sheets.add(sheet(t("an.sheet.months"), s.series("revenueByMonth"), t("an.c.month"), t("an.c.revenue"),
                t("an.c.bookings"), Charts::month));
        sheets.add(sheet(t("an.sheet.countries"), s.series("salesByCountry"), t("an.c.country"), t("an.c.revenue"),
                t("an.c.bookings"), x -> x));
        sheets.add(sheet(t("an.t.destinations"), s.series("salesByDestination"), t("an.c.destination"),
                t("an.c.bookings"), t("an.c.revenue"), x -> x));
        sheets.add(sheet(t("an.t.hotels"), s.series("popularHotels"), t("an.c.hotel"), t("an.c.bookings"),
                t("an.c.revenue"), x -> x));
        sheets.add(sheet(t("an.t.ratings"), s.series("destinationRatings"), t("an.c.country"), t("an.c.rating"),
                t("an.c.reviews"), x -> x));
        sheets.add(sheet(t("an.t.discounts"), s.series("discountsByName"), t("an.c.discount"), t("an.c.amount"),
                t("an.c.bookings"), x -> x));
        sheets.add(sheet(t("an.chart.statuses"), s.series("bookingsByStatus"), t("an.c.status"), t("an.c.bookings"),
                "", code -> Texts.status(BookingStatus.valueOf(code))));
        try {
            XlsxExporter.write(file.toPath(), sheets);
            Alerts.info(t("an.saved", file.getName()));
        } catch (Exception e) {
            Alerts.error(e.getMessage());
        }
    }

    private static XlsxExporter.Sheet sheet(String name, List<NamedValue> rows, String c1, String c2, String c3,
                                            Function<String, String> label) {
        List<List<Object>> data = new ArrayList<>();
        for (NamedValue v : rows) {
            data.add(c3.isEmpty() ? List.of(label.apply(v.name()), round(v.value()))
                    : List.of(label.apply(v.name()), round(v.value()), round(v.extra())));
        }
        return new XlsxExporter.Sheet(name, c3.isEmpty() ? List.of(c1, c2) : List.of(c1, c2, c3), data);
    }

    private static double round(double v) {
        return Math.round(v * 100) / 100.0;
    }

    private void exportPdf() {
        if (current == null) return;
        File file = chooseFile("pdf", "PDF");
        if (file == null) return;
        try {
            PdfExporter.write(file.toPath(), report);
            Alerts.info(t("an.saved", file.getName()));
        } catch (Exception e) {
            Alerts.error(e.getMessage());
        }
    }
}
