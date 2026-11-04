package by.bsuir.touragency.client.util;

import javafx.collections.FXCollections;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.util.StringConverter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

/**
 * Небольшие помощники построения интерфейса.
 */
public final class Ui {

    /**
     * Периодически выполняет action, пока узел показан на экране. Когда раздел закрывают
     * (узел убирается из сцены), таймер останавливается сам — лишних запросов нет.
     */
    public static void autoRefresh(Node node, double seconds, Runnable action) {
        javafx.animation.Timeline timer = new javafx.animation.Timeline(
                new javafx.animation.KeyFrame(javafx.util.Duration.seconds(seconds), e -> action.run()));
        timer.setCycleCount(javafx.animation.Animation.INDEFINITE);
        node.sceneProperty().addListener((obs, old, scene) -> {
            if (scene == null) timer.stop(); else timer.play();
        });
        if (node.getScene() != null) timer.play();
    }

    private Ui() {
    }

    /** Поле формы: подпись сверху, элемент управления снизу. */
    public static VBox field(String label, Node control) {
        Label l = new Label(label);
        l.getStyleClass().add("field-label");
        VBox box = new VBox(4, l, control);
        if (control instanceof Region r) {
            r.setMaxWidth(Double.MAX_VALUE);
        }
        return box;
    }

    public static Region hSpacer() {
        Region r = new Region();
        HBox.setHgrow(r, Priority.ALWAYS);
        return r;
    }

    public static HBox row(double spacing, Node... nodes) {
        HBox box = new HBox(spacing, nodes);
        box.setAlignment(Pos.CENTER_LEFT);
        return box;
    }

    /**
     * Выпадающий список. При anyText != null первым элементом идёт null с этой подписью
     * («Любой») — удобно для фильтров.
     */
    public static <T> ComboBox<T> combo(List<T> items, Function<T, String> text, String anyText) {
        List<T> all = new ArrayList<>();
        if (anyText != null) all.add(null);
        all.addAll(items);
        ComboBox<T> box = new ComboBox<>(FXCollections.observableArrayList(all));
        box.setMaxWidth(Double.MAX_VALUE);
        StringConverter<T> converter = new StringConverter<>() {
            @Override
            public String toString(T value) {
                return value == null ? (anyText == null ? "" : anyText) : text.apply(value);
            }

            @Override
            public T fromString(String s) {
                return null;
            }
        };
        box.setConverter(converter);
        box.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(T item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : converter.toString(item));
            }
        });
        if (anyText != null) box.getSelectionModel().selectFirst();
        return box;
    }

    /** Выбор элемента по идентификатору (для форм редактирования). */
    public static <T> void select(ComboBox<T> box, Function<T, Object> id, Object value) {
        box.getItems().stream().filter(i -> i != null && Objects.equals(id.apply(i), value))
                .findFirst().ifPresent(box.getSelectionModel()::select);
    }

    /**
     * Поле даты, которое сохраняет набранный вручную текст при уходе из поля
     * (в некоторых версиях JavaFX значение фиксируется только по Enter).
     */
    public static javafx.scene.control.DatePicker date(java.time.LocalDate value) {
        javafx.scene.control.DatePicker picker = new javafx.scene.control.DatePicker(value);
        picker.focusedProperty().addListener((o, was, now) -> {
            if (!now) {
                String text = picker.getEditor().getText();
                try {
                    picker.setValue(text == null || text.isBlank() ? null
                            : picker.getConverter().fromString(text.trim()));
                } catch (Exception e) {
                    picker.getEditor().setText(picker.getConverter().toString(picker.getValue()));
                }
            }
        });
        return picker;
    }

    public static TextField numberField(String prompt) {
        TextField f = new TextField();
        f.setPromptText(prompt);
        f.setTextFormatter(new javafx.scene.control.TextFormatter<String>(change ->
                change.getControlNewText().matches("[0-9]*([.,][0-9]{0,2})?") ? change : null));
        return f;
    }

    public static BigDecimal decimal(TextField f) {
        String text = f.getText() == null ? "" : f.getText().trim().replace(',', '.');
        return text.isEmpty() ? null : new BigDecimal(text);
    }

    public static Integer integer(TextField f) {
        String text = f.getText() == null ? "" : f.getText().trim();
        if (text.isEmpty()) return null;
        return new BigDecimal(text.replace(',', '.')).intValue();
    }

    /** Столбец таблицы по функции-получателю значения. */
    public static <T> TableColumn<T, Object> column(String title, Function<T, Object> value, double width) {
        TableColumn<T, Object> col = new TableColumn<>(title);
        col.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(value.apply(c.getValue())));
        col.setPrefWidth(width);
        col.setMinWidth(width);
        return col;
    }
}
