package by.bsuir.touragency.client.util;

import by.bsuir.touragency.common.model.NamedValue;
import javafx.collections.FXCollections;
import javafx.scene.Node;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.VBox;

import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.Function;

/** Построение графиков JavaFX из рядов NamedValue. */
public final class Charts {

    private Charts() {
    }

    /** «2026-03» → «мар 2026» / «Mar 2026». */
    public static String month(String ym) {
        try {
            return YearMonth.parse(ym).format(DateTimeFormatter.ofPattern("LLL yyyy", I18n.getLang().getLocale()));
        } catch (Exception e) {
            return ym;
        }
    }

    public static Node bar(String title, List<NamedValue> data, Function<String, String> label, String seriesName) {
        CategoryAxis x = new CategoryAxis();
        NumberAxis y = new NumberAxis();
        BarChart<String, Number> chart = new BarChart<>(x, y);
        chart.setLegendVisible(false);
        chart.setAnimated(false);
        chart.setCategoryGap(18);
        XYChart.Series<String, Number> s = new XYChart.Series<>();
        s.setName(seriesName);
        for (NamedValue v : data) {
            s.getData().add(new XYChart.Data<>(label.apply(v.name()), v.value()));
        }
        chart.setData(FXCollections.observableArrayList(List.of(s)));
        s.getData().forEach(d -> Tooltip.install(d.getNode(), new Tooltip(d.getXValue() + ": " + number(d.getYValue()))));
        return wrap(title, chart, data.isEmpty());
    }

    public static Node line(String title, List<NamedValue> data, Function<String, String> label, String seriesName) {
        CategoryAxis x = new CategoryAxis();
        NumberAxis y = new NumberAxis();
        LineChart<String, Number> chart = new LineChart<>(x, y);
        chart.setLegendVisible(false);
        chart.setAnimated(false);
        chart.setCreateSymbols(true);
        XYChart.Series<String, Number> s = new XYChart.Series<>();
        s.setName(seriesName);
        for (NamedValue v : data) {
            s.getData().add(new XYChart.Data<>(label.apply(v.name()), v.value()));
        }
        chart.setData(FXCollections.observableArrayList(List.of(s)));
        return wrap(title, chart, data.isEmpty());
    }

    public static Node pie(String title, List<NamedValue> data, Function<String, String> label) {
        PieChart chart = new PieChart();
        chart.setAnimated(false);
        // длинные подписи делают круг крошечным — тогда оставляем только легенду
        boolean shortLabels = data.stream().allMatch(v -> label.apply(v.name()).length() <= 12) && data.size() <= 8;
        chart.setLabelsVisible(shortLabels);
        chart.setLegendVisible(true);
        chart.setLabelLineLength(10);
        for (NamedValue v : data) {
            chart.getData().add(new PieChart.Data(label.apply(v.name()) + " (" + number(v.value()) + ")", v.value()));
        }
        return wrap(title, chart, data.isEmpty());
    }

    private static Node wrap(String title, javafx.scene.chart.Chart chart, boolean empty) {
        chart.setPrefHeight(280);
        chart.setMinHeight(240);
        Label t = new Label(title);
        t.getStyleClass().add("chart-title");
        VBox box = new VBox(6, t, chart);
        box.getStyleClass().addAll("card", "chart-card");
        if (empty) {
            Label none = new Label(I18n.t("common.empty"));
            none.getStyleClass().add("muted");
            box.getChildren().set(1, none);
        }
        return box;
    }

    public static String number(Number n) {
        double d = n.doubleValue();
        return d == Math.rint(d) ? String.format("%,d", (long) d).replace(',', ' ')
                : String.format(I18n.getLang().getLocale(), "%.1f", d);
    }
}
