package by.bsuir.touragency.client.util;

import by.bsuir.touragency.client.view.Navigator;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;

public final class Alerts {

    private Alerts() {
    }

    public static void error(String message) {
        show(Alert.AlertType.ERROR, I18n.t("common.error"), message);
    }

    // Перегрузка: со своим заголовком
    public static void error(String title, String message) {
        show(Alert.AlertType.ERROR, title, message);
    }

    public static void info(String message) {
        show(Alert.AlertType.INFORMATION, I18n.t("common.info"), message);
    }

    public static void info(String title, String message) {
        show(Alert.AlertType.INFORMATION, title, message);
    }

    public static boolean confirm(String message) {
        ButtonType yes = new ButtonType(I18n.t("common.yes"), ButtonBar.ButtonData.OK_DONE);
        ButtonType no = new ButtonType(I18n.t("common.no"), ButtonBar.ButtonData.CANCEL_CLOSE);
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, message, yes, no);
        prepare(alert, I18n.t("common.confirm"));
        return alert.showAndWait().orElse(no) == yes;
    }

    private static void show(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setContentText(message);
        prepare(alert, title);
        alert.showAndWait();
    }

    /**
     * Подписи стандартных кнопок (OK / Отмена) на языке интерфейса — по умолчанию
     * JavaFX берёт их из языка операционной системы. Также оформление и владелец окна.
     */
    public static void localize(javafx.scene.control.Dialog<?> dialog) {
        enterApplies(dialog);
        javafx.scene.Node ok = dialog.getDialogPane().lookupButton(ButtonType.OK);
        if (ok instanceof javafx.scene.control.Button b) b.setText(I18n.t("common.apply"));
        javafx.scene.Node cancel = dialog.getDialogPane().lookupButton(ButtonType.CANCEL);
        if (cancel instanceof javafx.scene.control.Button b) b.setText(I18n.t("common.cancel"));
        if (Navigator.getStage() != null && Navigator.getStage().getScene() != null) {
            dialog.initOwner(Navigator.getStage());
            dialog.getDialogPane().getStylesheets().addAll(Navigator.getStage().getScene().getStylesheets());
        }
    }

    /**
     * Короткое всплывающее сообщение в правом нижнем углу окна; исчезает само через 4 с
     * и не требует нажатия «ОК» (подтверждение действия, новое уведомление).
     */
    public static void toast(String message) {
        javafx.stage.Stage stage = Navigator.getStage();
        if (stage == null || !stage.isShowing()) return;
        javafx.scene.control.Label label = new javafx.scene.control.Label(message);
        label.setWrapText(true);
        label.setMaxWidth(360);
        label.getStyleClass().add("toast");
        if (stage.getScene() != null) label.getStylesheets().addAll(stage.getScene().getStylesheets());
        javafx.stage.Popup popup = new javafx.stage.Popup();
        popup.getContent().add(label);
        popup.setAutoHide(true);
        popup.show(stage);
        popup.setX(stage.getX() + stage.getWidth() - label.getWidth() - 30);
        popup.setY(stage.getY() + stage.getHeight() - label.getHeight() - 30);
        javafx.animation.PauseTransition pause = new javafx.animation.PauseTransition(javafx.util.Duration.seconds(4));
        pause.setOnFinished(e -> popup.hide());
        pause.play();
    }

    /**
     * Enter в любом месте диалога нажимает основную кнопку (OK / «Применить»), даже если фокус
     * в выпадающем списке — раньше список «съедал» Enter. В многострочном поле Enter переносит строку.
     */
    public static void enterApplies(javafx.scene.control.Dialog<?> dialog) {
        dialog.getDialogPane().addEventFilter(javafx.scene.input.KeyEvent.KEY_PRESSED, e -> {
            if (e.getCode() != javafx.scene.input.KeyCode.ENTER) return;
            javafx.scene.Node focus = dialog.getDialogPane().getScene().getFocusOwner();
            if (focus instanceof javafx.scene.control.TextArea) return;
            if (focus instanceof javafx.scene.control.ComboBoxBase<?> c && c.isShowing()) return;
            javafx.scene.Node ok = dialog.getDialogPane().lookupButton(ButtonType.OK);
            if (ok == null) {
                ok = dialog.getDialogPane().getButtonTypes().stream()
                        .filter(b -> b.getButtonData().isDefaultButton())
                        .map(dialog.getDialogPane()::lookupButton).findFirst().orElse(null);
            }
            if (ok instanceof javafx.scene.control.Button b && !b.isDisabled()) {
                b.fire();
                e.consume();
            }
        });
    }

    private static void prepare(Alert alert, String title) {
        alert.setTitle(title);
        alert.setHeaderText(null);
        // длинный текст переносится, окно растёт по высоте
        alert.getDialogPane().setPrefWidth(480);
        alert.getDialogPane().setMinHeight(javafx.scene.layout.Region.USE_PREF_SIZE);
        alert.setResizable(true);
        if (Navigator.getStage() != null && Navigator.getStage().getScene() != null) {
            alert.initOwner(Navigator.getStage());
            alert.getDialogPane().getStylesheets().addAll(Navigator.getStage().getScene().getStylesheets());
        }
    }
}
