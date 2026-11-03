package by.bsuir.touragency.client;

/**
 * Точка входа для исполняемого jar. Отдельный класс, не наследующий Application,
 * нужен, чтобы JavaFX запускался из jar без модульного пути.
 */
public final class Launcher {

    private Launcher() {
    }

    public static void main(String[] args) {
        ClientApp.main(args);
    }
}
