package by.bsuir.touragency.client.view.client;

import by.bsuir.touragency.client.component.EmptyState;
import by.bsuir.touragency.client.component.TourCard;
import by.bsuir.touragency.client.util.Async;
import by.bsuir.touragency.client.view.BaseView;
import by.bsuir.touragency.client.view.Shell;
import by.bsuir.touragency.common.model.Tour;
import by.bsuir.touragency.common.protocol.CommandType;
import by.bsuir.touragency.common.protocol.Request;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;

import java.util.ArrayList;

/** Избранные туры клиента. */
public class FavoritesView extends BaseView {

    private final Shell shell;

    public FavoritesView(Shell shell) {
        this.shell = shell;
    }

    @Override
    public String getTitle() {
        return t("menu.favorites");
    }

    @Override
    protected Parent build() {
        by.bsuir.touragency.client.component.CardGrid grid = new by.bsuir.touragency.client.component.CardGrid(260, 18);
        VBox page = new VBox(20, pageHeader(t("fav.title"), t("fav.subtitle")), grid);
        page.getStyleClass().add("page");
        by.bsuir.touragency.client.component.Skeleton.fill(grid, 3, 290, 170);
        Async.<ArrayList<Tour>>load(new Request(CommandType.GET_FAVORITES), tours -> {
            grid.getChildren().clear();
            if (tours.isEmpty()) {
                EmptyState empty = new EmptyState("♡", t("fav.empty"), t("fav.emptyHint"));
                Button go = new Button(t("menu.tours"));
                go.getStyleClass().add("btn-primary");
                go.setOnAction(e -> shell.openSection("menu.tours"));
                empty.getChildren().add(go);
                empty.setPrefWidth(900);
                grid.getChildren().add(empty);
                return;
            }
            tours.forEach(t -> grid.getChildren().add(new TourCard(t, x -> shell.open(new TourDetailsView(shell, x.getId())))));
        });
        return page;
    }
}
