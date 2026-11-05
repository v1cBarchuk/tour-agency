package by.bsuir.touragency.client.view.manager;

import by.bsuir.touragency.client.util.Alerts;
import by.bsuir.touragency.client.util.I18n;
import by.bsuir.touragency.client.util.Ui;
import javafx.collections.FXCollections;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Таблица с панелью инструментов: кнопки «Добавить / Изменить» и т.п.
 * Используется во всех разделах управления.
 *
 * @param <T> тип строки
 */
public class TablePanel<T> extends VBox {

    private final TableView<T> table = new TableView<>();
    private final HBox toolbar = new HBox(10);
    private final HBox extra = new HBox(10);

    public TablePanel() {
        super(14);
        getStyleClass().addAll("card", "table-card");
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setPlaceholder(new Label(I18n.t("common.empty")));
        table.setPrefHeight(560);
        VBox.setVgrow(table, Priority.ALWAYS);
        toolbar.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        extra.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        toolbar.getChildren().addAll(extra, Ui.hSpacer());
        getChildren().addAll(toolbar, table);
        // одна фабрика строк: двойной щелчок и цветовая подсветка строки по её данным
        table.setRowFactory(tv -> {
            javafx.scene.control.TableRow<T> row = new javafx.scene.control.TableRow<>() {
                @Override
                protected void updateItem(T item, boolean empty) {
                    super.updateItem(item, empty);
                    getStyleClass().removeIf(c -> c.startsWith("row-"));
                    if (!empty && item != null && rowStyle != null) {
                        String style = rowStyle.apply(item);
                        if (style != null) getStyleClass().add(style);
                    }
                }
            };
            row.setOnMouseClicked(e -> {
                if (doubleClick != null && e.getClickCount() == 2 && !row.isEmpty()) doubleClick.accept(row.getItem());
            });
            return row;
        });
    }

    private Consumer<T> doubleClick;
    private Function<T, String> rowStyle;

    /** CSS-класс строки (например, row-new для новых заявок) или null. */
    public void setRowStyle(Function<T, String> style) {
        this.rowStyle = style;
        table.refresh();
    }

    public TableView<T> table() {
        return table;
    }

    /** Элементы слева (поиск, фильтры). */
    public TablePanel<T> left(Node... nodes) {
        extra.getChildren().addAll(nodes);
        return this;
    }

    /** Кнопка действия над выбранной строкой; без выбора показывает подсказку. */
    public Button action(String text, String styleClass, Consumer<T> onSelected) {
        Button b = new Button(text);
        b.getStyleClass().add(styleClass);
        b.setMinWidth(javafx.scene.layout.Region.USE_PREF_SIZE);
        b.setOnAction(e -> {
            T selected = table.getSelectionModel().getSelectedItem();
            if (selected == null) {
                Alerts.info(I18n.t("common.selectRow"));
            } else {
                onSelected.accept(selected);
            }
        });
        toolbar.getChildren().add(b);
        return b;
    }

    /** Кнопка, не зависящая от выбора (например, «Добавить»). */
    public Button button(String text, String styleClass, Runnable onClick) {
        Button b = new Button(text);
        b.getStyleClass().add(styleClass);
        b.setMinWidth(javafx.scene.layout.Region.USE_PREF_SIZE);
        b.setOnAction(e -> onClick.run());
        toolbar.getChildren().add(b);
        return b;
    }

    /** Двойной щелчок по строке. */
    public void onDoubleClick(Consumer<T> action) {
        this.doubleClick = action;
    }

    private Function<T, Object> changeKey;
    private Object lastSignature;

    /**
     * Ключ, по которому определяется, изменилась ли запись (например, id + статус).
     * Нужен для автообновления: таблица перерисовывается только при реальных изменениях.
     */
    public void setChangeKey(Function<T, Object> key) {
        this.changeKey = key;
    }

    public void setItems(List<T> items) {
        if (changeKey != null) {
            Object signature = items.stream().map(changeKey).toList();
            if (signature.equals(lastSignature)) return;
            lastSignature = signature;
        }
        T selected = table.getSelectionModel().getSelectedItem();
        table.setItems(FXCollections.observableArrayList(items));
        if (selected != null) table.getSelectionModel().select(selected);   // строка остаётся выбранной
        // модели сравниваются по id, поэтому JavaFX не перерисовывает ячейки обновлённой записи
        // (например, новый статус заявки) — перерисовываем таблицу явно
        table.refresh();
    }
}
