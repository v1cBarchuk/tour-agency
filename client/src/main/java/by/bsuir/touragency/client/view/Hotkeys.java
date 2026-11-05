package by.bsuir.touragency.client.view;

import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.TextInputControl;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyCodeCombination;

/**
 * Горячие клавиши главного окна:
 * F5 — обновить текущий раздел, Ctrl+F — перейти в поле поиска, Esc — назад.
 */
public final class Hotkeys {

    private static final KeyCombination SEARCH = new KeyCodeCombination(KeyCode.F, KeyCombination.SHORTCUT_DOWN);

    private Hotkeys() {
    }

    public static void install(Scene scene) {
        scene.addEventFilter(javafx.scene.input.KeyEvent.KEY_PRESSED, e -> {
            Object current = Navigator.getCurrent();
            if (!(current instanceof Shell shell)) return;
            if (e.getCode() == KeyCode.F5) {
                shell.refresh();
                e.consume();
            } else if (SEARCH.match(e)) {
                Node field = scene.getRoot().lookup(".search-field");
                if (field instanceof TextInputControl input) {
                    input.requestFocus();
                    input.selectAll();
                }
                e.consume();
            } else if (e.getCode() == KeyCode.ESCAPE && !(scene.getFocusOwner() instanceof TextInputControl)) {
                shell.back();
                e.consume();
            }
        });
    }
}
