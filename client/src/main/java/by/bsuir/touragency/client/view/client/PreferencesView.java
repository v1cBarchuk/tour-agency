package by.bsuir.touragency.client.view.client;

import by.bsuir.touragency.client.util.Alerts;
import by.bsuir.touragency.client.util.Async;
import by.bsuir.touragency.client.util.ClientContext;
import by.bsuir.touragency.client.util.Fmt;
import by.bsuir.touragency.client.util.Ui;
import by.bsuir.touragency.client.view.BaseView;
import by.bsuir.touragency.client.view.Shell;
import by.bsuir.touragency.common.model.Country;
import by.bsuir.touragency.common.model.DictionaryItem;
import by.bsuir.touragency.common.model.Preferences;
import by.bsuir.touragency.common.protocol.CommandType;
import by.bsuir.touragency.common.protocol.Request;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.Spinner;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Пошаговая анкета предпочтений: 8 шагов с карточками выбора.
 * Любой шаг можно пропустить — незаполненный критерий не снижает оценку туров.
 */
public class PreferencesView extends BaseView {

    /** Шаг анкеты: заголовок, подсказка, содержимое. */
    private record Step(String titleKey, String hintKey, Supplier<Node> content) {
    }

    private final Shell shell;
    private Preferences prefs = new Preferences();
    private final List<Step> steps = new ArrayList<>();
    private int index;

    // элементы ввода
    private final Map<Integer, ToggleButton> countryChips = new LinkedHashMap<>();
    private final Map<Integer, ToggleButton> typeChips = new LinkedHashMap<>();
    private final TextField minBudget = Ui.numberField("1500");
    private final TextField maxBudget = Ui.numberField("3500");
    private final DatePicker from = Ui.date(null);
    private final DatePicker to = Ui.date(null);
    private final TextField minNights = Ui.numberField("7");
    private final TextField maxNights = Ui.numberField("10");
    private final Spinner<Integer> tourists = new Spinner<>(1, 9, 2);
    private final ToggleGroup starsGroup = new ToggleGroup();
    private final ToggleGroup mealGroup = new ToggleGroup();

    private StackPane stepBody;
    private Label stepTitle;
    private Label stepHint;
    private Label stepCounter;
    private ProgressBar progress;
    private Button back;
    private Button next;
    private Button skip;

    public PreferencesView(Shell shell) {
        this.shell = shell;
    }

    @Override
    public String getTitle() {
        return t("menu.preferences");
    }

    @Override
    protected Parent build() {
        VBox page = new VBox(20, pageHeader(t("prefs.title"), t("prefs.subtitle")));
        page.getStyleClass().add("page");
        Label loading = new Label(t("common.loading"));
        page.getChildren().add(loading);
        Async.<Preferences>load(new Request(CommandType.GET_PREFERENCES), p -> {
            prefs = p;
            page.getChildren().remove(loading);
            page.getChildren().add(wizard());
        });
        return page;
    }

    private Node wizard() {
        defineSteps();
        stepCounter = new Label();
        stepCounter.getStyleClass().add("field-label");
        progress = new ProgressBar(0);
        progress.setMaxWidth(Double.MAX_VALUE);
        progress.getStyleClass().add("wizard-progress");
        stepTitle = new Label();
        stepTitle.getStyleClass().add("wizard-title");
        stepHint = new Label();
        stepHint.getStyleClass().add("muted");
        stepHint.setWrapText(true);
        stepBody = new StackPane();
        stepBody.setAlignment(Pos.TOP_LEFT);
        stepBody.setMinHeight(170);

        back = new Button("←  " + t("common.back"));
        back.getStyleClass().add("btn-outline");
        back.setOnAction(e -> show(index - 1));
        skip = new Button(t("prefs.skip"));
        skip.getStyleClass().add("btn-ghost");
        skip.setOnAction(e -> {
            clearStep(index);
            if (index == steps.size() - 1) save();
            else show(index + 1);
        });
        next = new Button();
        next.setOnAction(e -> {
            if (index == steps.size() - 1) save();
            else show(index + 1);
        });
        HBox buttons = Ui.row(10, back, Ui.hSpacer(), skip, next);

        VBox card = new VBox(16, stepCounter, progress, stepTitle, stepHint, stepBody, buttons);
        card.getStyleClass().addAll("card", "wizard");
        card.setMaxWidth(1080);
        show(0);
        return card;
    }

    private void defineSteps() {
        // 1. страны
        for (Country c : ClientContext.countries()) {
            ToggleButton chip = chip(c.getName());
            chip.setSelected(prefs.getCountryIds().contains(c.getId()));
            countryChips.put(c.getId(), chip);
        }
        steps.add(new Step("prefs.step.countries", "prefs.hint.countries", () -> flow(countryChips.values())));

        // 2. бюджет
        if (prefs.getMinBudget() != null) minBudget.setText(prefs.getMinBudget().stripTrailingZeros().toPlainString());
        if (prefs.getMaxBudget() != null) maxBudget.setText(prefs.getMaxBudget().stripTrailingZeros().toPlainString());
        steps.add(new Step("prefs.step.budget", "prefs.hint.budget", () -> {
            FlowPane presets = new FlowPane(8, 8);
            int[][] ranges = {{0, 1500}, {1500, 3000}, {3000, 5000}, {5000, 15000}};
            for (int[] r : ranges) {
                Button b = new Button(r[0] == 0 ? t("prefs.upTo", Fmt.money(r[1]))
                        : Fmt.money(r[0]) + " – " + Fmt.money(r[1]));
                b.getStyleClass().add("chip-button");
                b.setOnAction(e -> {
                    minBudget.setText(r[0] == 0 ? "" : String.valueOf(r[0]));
                    maxBudget.setText(String.valueOf(r[1]));
                });
                presets.getChildren().add(b);
            }
            HBox fields = Ui.row(12, Ui.field(t("prefs.minBudget"), minBudget), Ui.field(t("prefs.maxBudget"), maxBudget));
            return new VBox(14, presets, fields);
        }));

        // 3. типы отдыха
        for (DictionaryItem v : ClientContext.vacationTypes()) {
            ToggleButton chip = chip(vacationIcon(v.getCode()) + "  " + v.getName());
            chip.setSelected(prefs.getVacationTypeIds().contains(v.getId()));
            typeChips.put(v.getId(), chip);
        }
        steps.add(new Step("prefs.step.types", "prefs.hint.types", () -> flow(typeChips.values())));

        // 4. даты
        from.setValue(prefs.getDesiredFrom());
        to.setValue(prefs.getDesiredTo());
        steps.add(new Step("prefs.step.dates", "prefs.hint.dates", () ->
                Ui.row(12, Ui.field(t("prefs.from"), from), Ui.field(t("prefs.to"), to))));

        // 5. ночи
        if (prefs.getMinNights() != null) minNights.setText(prefs.getMinNights().toString());
        if (prefs.getMaxNights() != null) maxNights.setText(prefs.getMaxNights().toString());
        steps.add(new Step("prefs.step.nights", "prefs.hint.nights", () -> {
            FlowPane presets = new FlowPane(8, 8);
            int[][] ranges = {{3, 5}, {6, 8}, {9, 11}, {12, 14}};
            for (int[] r : ranges) {
                Button b = new Button(t("prefs.nightsRange", r[0], r[1]));
                b.getStyleClass().add("chip-button");
                b.setOnAction(e -> {
                    minNights.setText(String.valueOf(r[0]));
                    maxNights.setText(String.valueOf(r[1]));
                });
                presets.getChildren().add(b);
            }
            return new VBox(14, presets, Ui.row(12, Ui.field(t("prefs.minNights"), minNights),
                    Ui.field(t("prefs.maxNights"), maxNights)));
        }));

        // 6. туристы
        tourists.setEditable(true);
        if (prefs.getTouristsCount() != null) tourists.getValueFactory().setValue(prefs.getTouristsCount());
        steps.add(new Step("prefs.step.tourists", "prefs.hint.tourists", () ->
                Ui.row(12, Ui.field(t("prefs.tourists"), tourists))));

        // 7. звёзды
        List<ToggleButton> starChips = new ArrayList<>();
        for (int s = 3; s <= 5; s++) {
            ToggleButton chip = chip(t("prefs.starsFrom", "★".repeat(s)));
            chip.setUserData(s);
            chip.setToggleGroup(starsGroup);
            chip.setSelected(prefs.getMinStars() != null && prefs.getMinStars() == s);
            starChips.add(chip);
        }
        steps.add(new Step("prefs.step.stars", "prefs.hint.stars", () -> flow(starChips)));

        // 8. питание
        List<ToggleButton> mealChips = new ArrayList<>();
        for (DictionaryItem m : ClientContext.mealTypes()) {
            ToggleButton chip = chip(m.getCode() + " · " + m.getName());
            chip.setUserData(m.getId());
            chip.setToggleGroup(mealGroup);
            chip.setSelected(m.getId().equals(prefs.getMealTypeId()));
            mealChips.add(chip);
        }
        steps.add(new Step("prefs.step.meal", "prefs.hint.meal", () -> flow(mealChips)));
    }

    private static String vacationIcon(String code) {
        return switch (code) {
            case "BEACH" -> "☀";
            case "FAMILY" -> "☺";
            case "ROMANTIC" -> "♥";
            case "SIGHTSEEING" -> "⌂";
            case "ACTIVE" -> "⚑";
            case "SKI" -> "❄";
            default -> "★";
        };
    }

    private static ToggleButton chip(String text) {
        ToggleButton b = new ToggleButton(text);
        b.getStyleClass().add("choice-chip");
        return b;
    }

    private static FlowPane flow(java.util.Collection<ToggleButton> chips) {
        FlowPane pane = new FlowPane(10, 10);
        pane.getChildren().addAll(chips);
        return pane;
    }

    private void show(int i) {
        index = Math.max(0, Math.min(i, steps.size() - 1));
        Step step = steps.get(index);
        stepCounter.setText(t("prefs.stepOf", index + 1, steps.size()));
        progress.setProgress((index + 1) / (double) steps.size());
        stepTitle.setText(t(step.titleKey()));
        stepHint.setText(t(step.hintKey()));
        stepBody.getChildren().setAll(step.content().get());
        back.setDisable(index == 0);
        boolean last = index == steps.size() - 1;
        next.setText(last ? t("prefs.finish") : t("prefs.next") + "  →");
        next.getStyleClass().removeAll("btn-primary", "btn-accent");
        next.getStyleClass().add(last ? "btn-accent" : "btn-primary");
    }

    /** «Пропустить» очищает ответ текущего шага. */
    private void clearStep(int i) {
        switch (i) {
            case 0 -> countryChips.values().forEach(c -> c.setSelected(false));
            case 1 -> { minBudget.clear(); maxBudget.clear(); }
            case 2 -> typeChips.values().forEach(c -> c.setSelected(false));
            case 3 -> { from.setValue(null); to.setValue(null); }
            case 4 -> { minNights.clear(); maxNights.clear(); }
            case 5 -> tourists.getValueFactory().setValue(1);
            case 6 -> starsGroup.selectToggle(null);
            case 7 -> mealGroup.selectToggle(null);
            default -> { }
        }
        if (i == 5) touristsSkipped = true;
    }

    private boolean touristsSkipped;

    private Preferences collect() {
        Preferences p = new Preferences();
        List<Integer> countries = new ArrayList<>();
        countryChips.forEach((id, chip) -> { if (chip.isSelected()) countries.add(id); });
        p.setCountryIds(countries);
        List<Integer> types = new ArrayList<>();
        typeChips.forEach((id, chip) -> { if (chip.isSelected()) types.add(id); });
        p.setVacationTypeIds(types);
        BigDecimal min = Ui.decimal(minBudget);
        BigDecimal max = Ui.decimal(maxBudget);
        p.setMinBudget(min);
        p.setMaxBudget(max);
        p.setDesiredFrom(from.getValue());
        p.setDesiredTo(to.getValue());
        p.setMinNights(Ui.integer(minNights));
        p.setMaxNights(Ui.integer(maxNights));
        p.setTouristsCount(touristsSkipped ? null : tourists.getValue());
        p.setMinStars(starsGroup.getSelectedToggle() == null ? null : (Integer) starsGroup.getSelectedToggle().getUserData());
        p.setMealTypeId(mealGroup.getSelectedToggle() == null ? null : (Integer) mealGroup.getSelectedToggle().getUserData());
        return p;
    }

    private void save() {
        next.setDisable(true);
        Async.send(new Request(CommandType.SAVE_PREFERENCES).with("preferences", collect()), response -> {
            next.setDisable(false);
            if (response.isOk()) {
                shell.openSection("menu.recommendations");
            } else {
                Alerts.error(response.getMessage());
            }
        });
    }
}
