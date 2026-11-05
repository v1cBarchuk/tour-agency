package by.bsuir.touragency.client.network;

import by.bsuir.touragency.client.util.I18n;
import by.bsuir.touragency.common.protocol.CommandType;
import by.bsuir.touragency.common.protocol.Request;
import by.bsuir.touragency.common.protocol.Response;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;

/**
 * Соединение клиента с сервером. Паттерн Singleton: одно соединение
 * на всё приложение. Подключение выполняется лениво при первом запросе
 * и восстанавливается после обрыва.
 */
public final class ServerConnection {

    private static final int CONNECT_TIMEOUT_MS = 5000;
    private static ServerConnection instance;

    private final String host;
    private final int port;
    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;

    private ServerConnection(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public static synchronized void init(String host, int port) {
        if (instance == null) {
            instance = new ServerConnection(host, port);
        }
    }

    public static ServerConnection getInstance() {
        if (instance == null) {
            throw new IllegalStateException("Соединение не инициализировано");
        }
        return instance;
    }

    /** Сколько раз пробовать переподключиться после обрыва и пауза между попытками. */
    private static final int RECONNECT_ATTEMPTS = 3;
    private static final long RECONNECT_DELAY_MS = 1500;

    /** Последний успешный запрос входа: после переподключения сессия восстанавливается им же. */
    private Request loginRequest;
    private Runnable onRestored;

    /** Что сделать, когда связь восстановлена (клиент показывает уведомление). */
    public void setOnRestored(Runnable action) {
        this.onRestored = action;
    }

    /**
     * Отправляет запрос и ждёт ответ. Вызывать не из UI-потока (см. Async).
     * При обрыве соединение открывается заново, сессия восстанавливается
     * повторным входом, и исходный запрос отправляется ещё раз.
     */
    public synchronized Response send(Request request) throws IOException {
        if (!request.hasLang()) {
            request.setLang(I18n.getLang());
        }
        boolean hadSocket = socket != null;
        try {
            return attempt(request);
        } catch (IOException first) {
            disconnect();
            // без сессии и без прежнего соединения повторять нечего (сервер просто недоступен)
            if (request.getCommand() == CommandType.LOGOUT || (loginRequest == null && !hadSocket)) {
                throw first;
            }
            for (int i = 1; i <= RECONNECT_ATTEMPTS; i++) {
                try {
                    Thread.sleep(RECONNECT_DELAY_MS);
                    return attempt(request);
                } catch (IOException e) {
                    disconnect();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
            throw new IOException(I18n.t("net.lost"), first);
        }
    }

    /** Одна попытка: при новом соединении сначала восстанавливается сессия пользователя. */
    private Response attempt(Request request) throws IOException {
        boolean fresh = socket == null;
        ensureConnected();
        boolean restored = false;
        if (fresh && loginRequest != null && request.getCommand() != CommandType.LOGIN) {
            exchange(loginRequest);
            restored = true;
        }
        Response response = remember(request, exchange(request));
        if (restored && onRestored != null) onRestored.run();
        return response;
    }

    private Response exchange(Request request) throws IOException {
        try {
            out.writeObject(request);
            out.flush();
            out.reset();
            return (Response) in.readObject();
        } catch (ClassNotFoundException e) {
            throw new IOException(I18n.t("net.badResponse"), e);
        }
    }

    private Response remember(Request request, Response response) {
        if (request.getCommand() == CommandType.LOGIN && response.isOk()) {
            loginRequest = request;
        } else if (request.getCommand() == CommandType.LOGOUT) {
            loginRequest = null;
        }
        return response;
    }

    private void ensureConnected() throws IOException {
        if (socket != null && socket.isConnected() && !socket.isClosed()) {
            return;
        }
        try {
            socket = new Socket();
            socket.connect(new InetSocketAddress(host, port), CONNECT_TIMEOUT_MS);
            out = new ObjectOutputStream(socket.getOutputStream());
            out.flush();
            in = new ObjectInputStream(socket.getInputStream());
        } catch (IOException e) {
            disconnect();
            throw new IOException(I18n.t("net.unavailable", host + ":" + port), e);
        }
    }

    public synchronized void close() {
        if (out != null) {
            try {
                out.writeObject(new Request(CommandType.DISCONNECT));
                out.flush();
            } catch (IOException ignored) {
                // сервер уже недоступен
            }
        }
        disconnect();
    }

    private void disconnect() {
        try {
            if (socket != null) {
                socket.close();
            }
        } catch (IOException ignored) {
            // нечего закрывать
        }
        socket = null;
        out = null;
        in = null;
    }
}
