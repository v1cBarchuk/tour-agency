package by.bsuir.touragency.client.view.dialog;

import by.bsuir.touragency.client.component.TourCard;
import by.bsuir.touragency.client.util.I18n;
import by.bsuir.touragency.client.util.Texts;
import by.bsuir.touragency.client.view.Navigator;
import by.bsuir.touragency.common.model.Criterion;
import by.bsuir.touragency.common.model.MatchResult;
import javafx.geometry.HPos;
import javafx.geometry.Pos;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.Locale;

/** «Как сформирована оценка»: баллы по каждому критерию и итоговая формула. */
public class MatchDialog extends Dialog<Void> {

    public MatchDialog(MatchResult r) {
        setTitle(I18n.t("match.how"));
        if (Navigator.getStage() != null) {
            initOwner(Navigator.getStage());
            getDialogPane().getStylesheets().addAll(Navigator.getStage().getScene().getStylesheets());
        }
        getDialogPane().getStyleClass().add("edit-dialog");

        Label percent = new Label(r.getPercent() + "%");
        percent.getStyleClass().addAll("match-big", TourCard.matchClass(r.getPercent()) + "-text");
        Label title = new Label(r.getTour().getTitle());
        title.getStyleClass().add("dialog-title");
        title.setWrapText(true);
        Label subtitle = new Label(I18n.t("match.title", r.getPercent()));
        subtitle.getStyleClass().add("muted");
        HBox head = new HBox(16, percent, new VBox(2, title, subtitle));
        head.setAlignment(Pos.CENTER_LEFT);

        GridPane grid = new GridPane();
        grid.setHgap(14);
        grid.setVgap(10);
        ColumnConstraints c0 = new ColumnConstraints(170);
        ColumnConstraints c1 = new ColumnConstraints(290);
        ColumnConstraints c2 = new ColumnConstraints(110);
        ColumnConstraints c3 = new ColumnConstraints(70);
        c3.setHalignment(HPos.RIGHT);
        grid.getColumnConstraints().addAll(c0, c1, c2, c3);
        grid.add(header(I18n.t("match.col.criterion")), 0, 0);
        grid.add(header(I18n.t("match.col.result")), 1, 0);
        grid.add(header(""), 2, 0);
        grid.add(header(I18n.t("match.col.points")), 3, 0);

        int row = 1;
        for (Criterion c : r.getCriteria()) {
            Label name = new Label(Texts.criterionName(c));
            name.getStyleClass().add("fact-value");
            Label text = new Label(c.getResult() == Criterion.Result.SKIPPED
                    ? I18n.t("match.skipped") : Texts.criterion(c));
            text.setWrapText(true);
            text.getStyleClass().add(switch (c.getResult()) {
                case MATCH, BONUS -> "reason-ok";
                case PARTIAL, MISS -> "reason-diff";
                case SKIPPED -> "muted";
            });
            grid.add(name, 0, row);
            grid.add(text, 1, row);
            if (c.getMax() > 0 && c.getResult() != Criterion.Result.SKIPPED) {
                ProgressBar bar = new ProgressBar(c.getPoints() / c.getMax());
                bar.setPrefWidth(100);
                bar.getStyleClass().add("score-bar");
                grid.add(bar, 2, row);
            }
            String pts = switch (c.getResult()) {
                case SKIPPED -> "—";
                case BONUS -> "+" + fmt(c.getPoints());
                default -> c.getMax() > 0 ? fmt(c.getPoints()) + " / " + fmt(c.getMax()) : "—";
            };
            grid.add(new Label(pts), 3, row);
            row++;
        }

        Label formula = new Label(I18n.t("match.formula", fmt(r.getScore()), fmt(r.getMaxScore()),
                Math.round(r.getMaxScore() == 0 ? 0 : r.getScore() / r.getMaxScore() * 100), fmt(r.getBonus()),
                r.getPercent()));
        formula.getStyleClass().add("formula");
        formula.setWrapText(true);
        formula.setMaxWidth(660);
        formula.setMinHeight(javafx.scene.layout.Region.USE_PREF_SIZE);
        Label note = new Label(I18n.t("match.note"));
        note.getStyleClass().add("muted");
        note.setWrapText(true);
        note.setMaxWidth(660);
        note.setMinHeight(javafx.scene.layout.Region.USE_PREF_SIZE);

        VBox content = new VBox(18, head, grid, formula, note);
        content.getStyleClass().add("dialog-content");
        content.setPrefWidth(700);
        getDialogPane().setContent(content);
        getDialogPane().getButtonTypes().add(new ButtonType(I18n.t("common.close"),
                javafx.scene.control.ButtonBar.ButtonData.CANCEL_CLOSE));
    }

    private static Label header(String text) {
        Label l = new Label(text);
        l.getStyleClass().add("field-label");
        return l;
    }

    private static String fmt(double v) {
        return v == Math.rint(v) ? String.valueOf((long) v) : String.format(Locale.ROOT, "%.1f", v);
    }
}
