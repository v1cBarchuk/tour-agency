package by.bsuir.touragency.client.component;

import by.bsuir.touragency.client.util.I18n;
import by.bsuir.touragency.client.util.Texts;
import by.bsuir.touragency.client.view.dialog.MatchDialog;
import by.bsuir.touragency.common.model.Criterion;
import by.bsuir.touragency.common.model.MatchResult;
import by.bsuir.touragency.common.model.Tour;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

import java.util.function.Consumer;

/**
 * Карточка рекомендации: карточка тура + «подходит вам на N%» и главные причины.
 */
public class MatchCard extends VBox {

    public MatchCard(MatchResult result, boolean showPercent, Consumer<Tour> onOpen) {
        super(0);
        getStyleClass().add("match-card");
        setMaxWidth(TourCard.WIDTH);
        getChildren().add(new TourCard(result.getTour(), onOpen));

        VBox why = new VBox(4);
        why.getStyleClass().add("match-why");
        if (showPercent) {
            Label title = new Label(I18n.t("match.title", result.getPercent()));
            title.getStyleClass().addAll("match-title", TourCard.matchClass(result.getPercent()) + "-text");
            title.setWrapText(true);
            why.getChildren().add(title);
        }
        result.matched().stream().filter(c -> c.getCode() != Criterion.Code.MEAL || showPercent).limit(3)
                .forEach(c -> {
                    Label l = new Label("✓  " + Texts.criterion(c));
                    l.getStyleClass().add("reason-ok");
                    l.setWrapText(true);
                    why.getChildren().add(l);
                });
        result.differs().stream().limit(1).forEach(c -> {
            Label l = new Label("•  " + Texts.criterion(c));
            l.getStyleClass().add("reason-diff");
            l.setWrapText(true);
            why.getChildren().add(l);
        });
        if (showPercent) {
            Hyperlink how = new Hyperlink(I18n.t("match.how"));
            how.setOnAction(e -> new MatchDialog(result).showAndWait());
            why.getChildren().add(how);
        }
        getChildren().add(why);
    }
}
