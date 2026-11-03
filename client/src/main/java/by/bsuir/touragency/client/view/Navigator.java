package by.bsuir.touragency.client.view;

import by.bsuir.touragency.client.util.I18n;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.util.Objects;

/**
 * Переключение экранов в главном окне. При смене языка текущий экран
 * пересоздаётся (подписка на I18n — паттерн Observer).
 */
public final class Navigator {

    private static Stage stage;
    private static BaseView current;

    private Navigator() {
    }

    public static void init(Stage primaryStage) {
        stage = primaryStage;
        I18n.addListener(() -> {
            if (current != null) {
                show(current.fresh());
            }
        });
    }

    public static Stage getStage() {
        return stage;
    }

    public static void show(BaseView view) {
        current = view;
        Scene scene = stage.getScene();
        if (scene == null) {
            scene = new Scene(view.getRoot(), 1280, 800);
            scene.getStylesheets().add(Objects.requireNonNull(
                    Navigator.class.getResource("/css/app.css")).toExternalForm());
            stage.setScene(scene);
        } else {
            scene.setRoot(view.getRoot());
        }
        stage.setTitle(I18n.t("app.name") + " — " + view.getTitle());
    }
}
