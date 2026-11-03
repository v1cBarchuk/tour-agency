package by.bsuir.touragency.client.view.manager;

import by.bsuir.touragency.client.component.KpiTile;
import by.bsuir.touragency.client.util.Async;
import by.bsuir.touragency.client.util.Charts;
import by.bsuir.touragency.client.util.Fmt;
import by.bsuir.touragency.client.view.BaseView;
import by.bsuir.touragency.client.view.Shell;
import by.bsuir.touragency.common.model.Stats;
import by.bsuir.touragency.common.protocol.CommandType;
import by.bsuir.touragency.common.protocol.Request;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.math.BigDecimal;

/** Панель управления: ключевые показатели и графики по всем данным. */
public class DashboardView extends BaseView {

    private final Shell shell;

    public DashboardView(Shell shell) {
        this.shell = shell;
    }

    @Override
    public String getTitle() {
        return t("menu.dashboard");
    }

    @Override
    protected Parent build() {
        VBox page = new VBox(22, pageHeader(t("dash.title"), t("dash.subtitle")));
        page.getStyleClass().add("page");
        Label loading = new Label(t("common.loading"));
        page.getChildren().add(loading);
        Async.<Stats>load(new Request(CommandType.GET_DASHBOARD), s -> {
            page.getChildren().remove(loading);
            page.getChildren().addAll(kpis(s), alert(s), charts(s));
        });
        return page;
    }

    private Node kpis(Stats s) {
        GridPane grid = new GridPane();
        grid.setHgap(16);
        grid.setVgap(16);
        Node[] tiles = {
                new KpiTile("✈", t("dash.tours"), num(s.kpi("tours")), t("dash.activeTours", num(s.kpi("activeTours")))),
                new KpiTile("▤", t("dash.bookings"), num(s.kpi("bookings")), null),
                new KpiTile("☺", t("dash.clients"), num(s.kpi("clients")), null),
                new KpiTile("BYN", t("dash.revenue"), Fmt.money(BigDecimal.valueOf(s.kpi("revenue"))), t("dash.revenueHint")),
                new KpiTile("◫", t("dash.freeSeats"), num(s.kpi("freeSeats")), t("dash.freeSeatsHint")),
                new KpiTile("★", t("dash.rating"), Fmt.rating(s.kpi("avgRating")), t("dash.ratingHint")),
        };
        for (int i = 0; i < tiles.length; i++) {
            grid.add(tiles[i], i % 3, i / 3);
            GridPane.setHgrow(tiles[i], Priority.ALWAYS);
        }
        for (int i = 0; i < 3; i++) {
            javafx.scene.layout.ColumnConstraints cc = new javafx.scene.layout.ColumnConstraints();
            cc.setPercentWidth(33.33);
            grid.getColumnConstraints().add(cc);
        }
        return grid;
    }

    private Node alert(Stats s) {
        int requests = (int) s.kpi("newRequests");
        Label text = new Label(requests > 0 ? t("dash.newRequests", requests) : t("dash.noRequests"));
        text.getStyleClass().add("fact-value");
        Button open = new Button(t("dash.openRequests") + "  →");
        open.getStyleClass().add(requests > 0 ? "btn-accent" : "btn-outline");
        open.setOnAction(e -> shell.openSection("menu.requests"));
        HBox box = new HBox(16, text, by.bsuir.touragency.client.util.Ui.hSpacer(), open);
        box.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        box.getStyleClass().addAll("card", requests > 0 ? "alert-card" : "info-card");
        return box;
    }

    private Node charts(Stats s) {
        GridPane grid = new GridPane();
        grid.setHgap(16);
        grid.setVgap(16);
        grid.add(Charts.bar(t("dash.chart.revenue"), s.series("revenueByMonth"), Charts::month, "BYN"), 0, 0);
        grid.add(Charts.line(t("dash.chart.bookings"), s.series("bookingsByMonth"), Charts::month, ""), 1, 0);
        grid.add(Charts.pie(t("dash.chart.destinations"), s.series("bookingsByCountry"), x -> x), 0, 1);
        grid.add(Charts.bar(t("dash.chart.revenueCountry"), s.series("revenueByCountry"), x -> x, "BYN"), 1, 1);
        grid.add(Charts.pie(t("dash.chart.stars"), s.series("toursByStars"), x -> x), 0, 2);
        for (int i = 0; i < 2; i++) {
            javafx.scene.layout.ColumnConstraints cc = new javafx.scene.layout.ColumnConstraints();
            cc.setPercentWidth(50);
            grid.getColumnConstraints().add(cc);
        }
        return grid;
    }

    private static String num(double v) {
        return Charts.number(v);
    }
}
