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

    /** Отправляет запрос и ждёт ответ. Вызывать не из UI-потока (см. Async). */
    public synchronized Response send(Request request) throws IOException {
        if (!request.hasLang()) {
            request.setLang(I18n.getLang());
        }
        ensureConnected();
        try {
            out.writeObject(request);
            out.flush();
            out.reset();
            return (Response) in.readObject();
        } catch (IOException e) {
            disconnect();
            throw new IOException(I18n.t("net.lost"), e);
        } catch (ClassNotFoundException e) {
            throw new IOException(I18n.t("net.badResponse"), e);
        }
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
