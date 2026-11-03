package by.bsuir.touragency.server.network;

import by.bsuir.touragency.common.exception.AppException;
import by.bsuir.touragency.common.model.Lang;
import by.bsuir.touragency.common.protocol.CommandType;
import by.bsuir.touragency.common.protocol.Request;
import by.bsuir.touragency.common.protocol.Response;
import by.bsuir.touragency.server.command.Command;
import by.bsuir.touragency.server.command.CommandFactory;
import by.bsuir.touragency.server.db.DataAccessException;
import by.bsuir.touragency.server.util.ServerMessages;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Обработчик одного клиента. Выполняется в отдельном потоке из пула:
 * читает запросы, передаёт их командам и отправляет ответы.
 */
public class ClientHandler implements Runnable {

    private static final Logger LOG = Logger.getLogger(ClientHandler.class.getName());

    private final Socket socket;
    private final CommandFactory commandFactory;
    private final ClientSession session;
    private final Runnable onClose;

    public ClientHandler(Socket socket, CommandFactory commandFactory, Runnable onClose) {
        this.socket = socket;
        this.commandFactory = commandFactory;
        this.session = new ClientSession(socket.getRemoteSocketAddress().toString());
        this.onClose = onClose;
    }

    @Override
    public void run() {
        LOG.info("Клиент подключён: " + session.getRemoteAddress());
        try (socket;
             ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream())) {
            out.flush();
            try (ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {
                while (!socket.isClosed()) {
                    Object received = in.readObject();
                    if (!(received instanceof Request request)) {
                        send(out, Response.error(ServerMessages.get(Lang.RU, "error.badRequest")));
                        continue;
                    }
                    if (request.getCommand() == CommandType.DISCONNECT) {
                        break;
                    }
                    send(out, handle(request));
                }
            }
        } catch (EOFException | SocketException e) {
            // клиент закрыл соединение
        } catch (IOException | ClassNotFoundException e) {
            LOG.log(Level.WARNING, "Ошибка обмена с клиентом " + session, e);
        } finally {
            if (session.isAuthenticated()) {
                commandFactory.create(CommandType.LOGOUT).ifPresent(this::silentLogout);
            }
            onClose.run();
            LOG.info("Клиент отключён: " + session);
        }
    }

    private Response handle(Request request) {
        LOG.fine(session + " -> " + request);
        Command command = commandFactory.create(request.getCommand()).orElse(null);
        if (command == null) {
            return Response.error(ServerMessages.get(request.getLang(), "error.unsupported", request.getCommand()));
        }
        try {
            return command.execute(request, session);
        } catch (AppException e) {
            // ожидаемая ошибка: сообщение показывается пользователю
            return Response.error(ServerMessages.get(request.getLang(), e));
        } catch (DataAccessException e) {
            LOG.log(Level.SEVERE, "Ошибка БД при выполнении " + request, e);
            return Response.error(ServerMessages.get(request.getLang(), "error.db"));
        } catch (RuntimeException e) {
            LOG.log(Level.SEVERE, "Непредвиденная ошибка при выполнении " + request, e);
            return Response.error(ServerMessages.get(request.getLang(), "error.internal"));
        }
    }

    private void send(ObjectOutputStream out, Response response) throws IOException {
        out.writeObject(response);
        out.flush();
        out.reset(); // сбрасываем кэш ссылок, иначе изменённые объекты не дойдут
    }

    private void silentLogout(Command logout) {
        try {
            logout.execute(new Request(CommandType.LOGOUT), session);
        } catch (Exception e) {
            LOG.fine("Не удалось записать выход: " + e.getMessage());
        }
    }
}
