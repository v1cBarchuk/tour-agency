package by.bsuir.touragency.client.view.client;

import by.bsuir.touragency.client.component.EmptyState;
import by.bsuir.touragency.client.component.MatchCard;
import by.bsuir.touragency.client.util.Async;
import by.bsuir.touragency.client.view.BaseView;
import by.bsuir.touragency.client.view.Shell;
import by.bsuir.touragency.common.model.MatchResult;
import by.bsuir.touragency.common.model.Recommendations;
import by.bsuir.touragency.common.protocol.CommandType;
import by.bsuir.touragency.common.protocol.Request;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.List;

/**
 * «Мои рекомендации»: подборки по анкете, по истории поездок, популярное и новинки.
 */
public class RecommendationsView extends BaseView {

    private final Shell shell;

    public RecommendationsView(Shell shell) {
        this.shell = shell;
    }

    @Override
    public String getTitle() {
        return t("menu.recommendations");
    }

    @Override
    protected Parent build() {
        Button edit = new Button("✎  " + t("rec.editPrefs"));
        edit.getStyleClass().add("btn-outline");
        edit.setOnAction(e -> shell.openSection("menu.preferences"));
        VBox page = new VBox(28, new HBox(pageHeader(t("rec.title"), t("rec.subtitle")),
                by.bsuir.touragency.client.util.Ui.hSpacer(), edit));
        page.getStyleClass().add("page");
        Label loading = new Label(t("common.loading"));
        page.getChildren().add(loading);

        Async.<Recommendations>load(new Request(CommandType.GET_RECOMMENDATIONS), rec -> {
            page.getChildren().remove(loading);
            if (!rec.isHasPreferences()) {
                VBox banner = new VBox(10);
                banner.getStyleClass().addAll("card", "cta-card");
                Label title = new Label(t("rec.noPrefs.title"));
                title.getStyleClass().add("section-title");
                Label text = new Label(t("rec.noPrefs.text"));
                text.setWrapText(true);
                Button go = new Button(t("rec.noPrefs.button"));
                go.getStyleClass().add("btn-accent");
                go.setOnAction(e -> shell.openSection("menu.preferences"));
                banner.getChildren().addAll(title, text, go);
                page.getChildren().add(banner);
            } else {
                page.getChildren().add(section(t("rec.forYou"), t("rec.forYou.hint"), rec.getForYou(), true));
            }
            if (rec.getVisitedCountry() != null && !rec.getBasedOnTrips().isEmpty()) {
                page.getChildren().add(section(t("rec.trips"), t("rec.trips.hint", rec.getVisitedCountry()),
                        rec.getBasedOnTrips(), rec.isHasPreferences()));
            }
            page.getChildren().add(section(t("rec.popular"), t("rec.popular.hint"), rec.getPopular(),
                    rec.isHasPreferences()));
            page.getChildren().add(section(t("rec.new"), t("rec.new.hint"), rec.getNewMatching(),
                    rec.isHasPreferences()));
        });
        return page;
    }

    private Node section(String title, String hint, List<MatchResult> results, boolean showPercent) {
        Label h = new Label(title);
        h.getStyleClass().add("section-title");
        Label sub = new Label(hint);
        sub.getStyleClass().add("muted");
        sub.setWrapText(true);
        FlowPane grid = new FlowPane(18, 18);
        if (results.isEmpty()) {
            grid.getChildren().add(new EmptyState("★", t("common.empty"), t("rec.empty")));
        }
        results.forEach(r -> grid.getChildren().add(new MatchCard(r, showPercent,
                tour -> shell.open(new TourDetailsView(shell, tour.getId())))));
        return new VBox(12, new VBox(2, h, sub), grid);
    }
}
