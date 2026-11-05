package by.bsuir.touragency.client.view.client;

import by.bsuir.touragency.client.component.CountryCard;
import by.bsuir.touragency.client.util.Async;
import by.bsuir.touragency.client.view.BaseView;
import by.bsuir.touragency.client.view.Shell;
import by.bsuir.touragency.common.model.Country;
import by.bsuir.touragency.common.protocol.CommandType;
import by.bsuir.touragency.common.protocol.Request;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;

import java.util.ArrayList;

/** Каталог направлений: карточки стран. */
public class DestinationsView extends BaseView {

    private final Shell shell;

    public DestinationsView(Shell shell) {
        this.shell = shell;
    }

    @Override
    public String getTitle() {
        return t("dest.title");
    }

    @Override
    protected Parent build() {
        Label subtitle = new Label(t("common.loading"));
        subtitle.getStyleClass().add("page-subtitle");
        Label title = new Label(t("dest.title"));
        title.getStyleClass().add("page-title");
        by.bsuir.touragency.client.component.CardGrid grid = new by.bsuir.touragency.client.component.CardGrid(260, 18);

        Async.<ArrayList<Country>>load(new Request(CommandType.GET_COUNTRIES), countries -> {
            subtitle.setText(t("dest.subtitle", countries.size()));
            countries.forEach(c -> grid.getChildren().add(
                    new CountryCard(c, 300, 220, x -> shell.open(new CountryView(shell, x)))));
        });
        VBox page = new VBox(20, new VBox(2, title, subtitle), grid);
        page.getStyleClass().add("page");
        return page;
    }
}
