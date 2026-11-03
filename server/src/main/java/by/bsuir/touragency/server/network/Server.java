package by.bsuir.touragency.server.network;

import by.bsuir.touragency.server.command.CommandFactory;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

/**
 * Многопоточный TCP-сервер: каждое подключение обслуживается
 * отдельным потоком из фиксированного пула.
 */
public class Server {

    private static final Logger LOG = Logger.getLogger(Server.class.getName());

    private final int port;
    private final ExecutorService pool;
    private final CommandFactory commandFactory;
    private final AtomicInteger activeClients = new AtomicInteger();
    private volatile boolean running;
    private ServerSocket serverSocket;

    public Server(int port, int threads, CommandFactory commandFactory) {
        this.port = port;
        this.pool = Executors.newFixedThreadPool(threads);
        this.commandFactory = commandFactory;
    }

    public void start() throws IOException {
        serverSocket = new ServerSocket(port);
        running = true;
        LOG.info("Сервер запущен на порту " + port);

        while (running) {
            try {
                Socket socket = serverSocket.accept();
                int count = activeClients.incrementAndGet();
                LOG.info("Активных клиентов: " + count);
                pool.execute(new ClientHandler(socket, commandFactory, activeClients::decrementAndGet));
            } catch (SocketException e) {
                if (running) {
                    throw e;
                }
            }
        }
    }

    public void stop() {
        running = false;
        try {
            if (serverSocket != null) {
                serverSocket.close();
            }
        } catch (IOException ignored) {
            // сокет уже закрыт
        }
        pool.shutdownNow();
        try {
            pool.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        LOG.info("Сервер остановлен");
    }
}
