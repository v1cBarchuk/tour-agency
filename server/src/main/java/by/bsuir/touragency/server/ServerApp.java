package by.bsuir.touragency.server;

import by.bsuir.touragency.server.command.CommandFactory;
import by.bsuir.touragency.server.config.ServerConfig;
import by.bsuir.touragency.server.db.ConnectionPool;
import by.bsuir.touragency.server.network.Server;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Точка входа сервера.
 * Пример: java -jar tour-server.jar --server.port=5555 --db.password=secret
 */
public final class ServerApp {

    private static final Logger LOG = Logger.getLogger(ServerApp.class.getName());

    private ServerApp() {
    }

    public static void main(String[] args) {
        System.setProperty("java.util.logging.SimpleFormatter.format",
                "%1$tH:%1$tM:%1$tS %4$s %5$s%6$s%n");
        try {
            ServerConfig config = ServerConfig.load(args);
            ConnectionPool.init(config);

            Server server = new Server(config.getPort(), config.getThreads(), new CommandFactory());
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                server.stop();
                ConnectionPool.getInstance().shutdown();
            }));
            server.start();
        } catch (java.sql.SQLException e) {
            LOG.log(Level.SEVERE, "Не удалось подключиться к БД. Проверьте db.url, db.user, "
                    + "db.password в server.properties: " + e.getMessage());
            System.exit(1);
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Сервер остановлен с ошибкой", e);
            System.exit(1);
        }
    }
}
