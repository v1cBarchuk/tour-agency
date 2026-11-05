package by.bsuir.touragency.client.util;

import by.bsuir.touragency.client.network.ServerConnection;
import by.bsuir.touragency.common.protocol.Request;
import by.bsuir.touragency.common.protocol.Response;
import javafx.concurrent.Task;

import java.util.function.Consumer;

/**
 * Выполнение запросов к серверу в фоновом потоке, чтобы окно не «зависало».
 * Результат возвращается в UI-поток JavaFX.
 */
public final class Async {

    private Async() {
    }

    /** Любой ответ передаётся в onResult; ошибка соединения показывается окном. */
    public static void send(Request request, Consumer<Response> onResult) {
        send(request, onResult, error -> Alerts.error(I18n.t("common.connectionError"), error.getMessage()));
    }

    // Перегрузка: собственная обработка ошибки соединения
    public static void send(Request request, Consumer<Response> onResult, Consumer<Throwable> onError) {
        Task<Response> task = new Task<>() {
            @Override
            protected Response call() throws Exception {
                return ServerConnection.getInstance().send(request);
            }
        };
        task.setOnSucceeded(e -> onResult.accept(task.getValue()));
        task.setOnFailed(e -> onError.accept(task.getException()));
        Thread thread = new Thread(task, "request-" + request.getCommand());
        thread.setDaemon(true);
        thread.start();
    }

    /**
     * Запрос, у которого важен только успешный результат: данные передаются в onData,
     * сообщение об ошибке сервера показывается окном.
     */
    /**
     * Тихая загрузка для фонового автообновления: при ошибке сервера или соединения
     * ничего не показывается, чтобы не засыпать пользователя окнами.
     */
    public static <T extends java.io.Serializable> void quiet(Request request, Consumer<T> onData) {
        send(request, response -> {
            if (response.isOk()) onData.accept(response.getData());
        }, error -> { });
    }

    // Перегрузка: текст ошибки передаётся в onError, а не показывается окном
    public static <T extends java.io.Serializable> void quiet(Request request, Consumer<T> onData,
                                                              Consumer<String> onError) {
        send(request, response -> {
            if (response.isOk()) {
                onData.accept(response.getData());
            } else {
                onError.accept(response.getMessage());
            }
        }, error -> onError.accept(I18n.t("net.lost")));
    }

    public static <T extends java.io.Serializable> void load(Request request, Consumer<T> onData) {
        send(request, response -> {
            if (response.isOk()) {
                onData.accept(response.getData());
            } else {
                Alerts.error(response.getMessage());
            }
        });
    }
}
