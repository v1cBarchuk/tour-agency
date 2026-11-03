package by.bsuir.touragency.client.view.dialog;

import by.bsuir.touragency.client.util.Async;
import by.bsuir.touragency.client.util.I18n;
import by.bsuir.touragency.client.view.Navigator;
import by.bsuir.touragency.common.exception.ValidationException;
import by.bsuir.touragency.common.protocol.Request;
import javafx.event.ActionEvent;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

/**
 * Базовый диалог создания/редактирования (шаблонный метод):
 * наследник строит форму, собирает объект и формирует запрос;
 * сохранение идёт на сервер, ошибка показывается в самом диалоге.
 *
 * @param <T> редактируемая сущность
 */
public abstract class EditDialog<T> extends Dialog<Boolean> {

    protected final GridPane form = new GridPane();
    private final Label error = new Label();
    private final ButtonType saveType;

    protected EditDialog(String title) {
        setTitle(title);
        setHeaderText(null);
        if (Navigator.getStage() != null) {
            initOwner(Navigator.getStage());
            getDialogPane().getStylesheets().addAll(Navigator.getStage().getScene().getStylesheets());
        }
        getDialogPane().getStyleClass().add("edit-dialog");

        form.setHgap(14);
        form.setVgap(12);
        for (int i = 0; i < 2; i++) {
            ColumnConstraints cc = new ColumnConstraints();
            cc.setPercentWidth(50);
            form.getColumnConstraints().add(cc);
        }
        error.getStyleClass().add("error");
        error.setWrapText(true);
        Label heading = new Label(title);
        heading.getStyleClass().add("dialog-title");
        VBox content = new VBox(16, heading, form, error);
        content.setPrefWidth(620);
        content.getStyleClass().add("dialog-content");
        getDialogPane().setContent(content);

        saveType = new ButtonType(I18n.t("common.save"), ButtonBar.ButtonData.OK_DONE);
        ButtonType cancel = new ButtonType(I18n.t("common.cancel"), ButtonBar.ButtonData.CANCEL_CLOSE);
        getDialogPane().getButtonTypes().addAll(saveType, cancel);
        Button save = (Button) getDialogPane().lookupButton(saveType);
        save.getStyleClass().add("btn-primary");
        save.addEventFilter(ActionEvent.ACTION, e -> {
            e.consume();          // закрываем сами — только после успешного ответа сервера
            submit(save);
        });
        setResultConverter(bt -> bt == saveType);
    }

    /** Добавить поле в форму: на всю ширину или в одну из колонок. */
    protected void add(Node field, int col, int row, int colSpan) {
        form.add(field, col, row, colSpan, 1);
    }

    /** Собрать объект из полей формы (с проверками на стороне клиента). */
    protected abstract T collect() throws ValidationException;

    /** Запрос на сохранение объекта. */
    protected abstract Request saveRequest(T value);

    private void submit(Button save) {
        error.setText("");
        T value;
        try {
            value = collect();
        } catch (ValidationException e) {
            error.setText(I18n.t(e.getMessageKey()));
            return;
        } catch (NumberFormatException e) {
            error.setText(I18n.t("tf.badNumber"));
            return;
        }
        save.setDisable(true);
        Async.send(saveRequest(value), response -> {
            save.setDisable(false);
            if (response.isOk()) {
                setResult(Boolean.TRUE);
                close();
            } else {
                error.setText(response.getMessage());
            }
        }, ex -> {
            save.setDisable(false);
            error.setText(ex.getMessage());
        });
    }
}
