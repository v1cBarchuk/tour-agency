package by.bsuir.touragency.server.db;

import by.bsuir.touragency.server.config.ServerConfig;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * Пул соединений с БД. Паттерн Singleton: один пул на весь сервер,
 * общий для всех потоков-обработчиков клиентов.
 */
public final class ConnectionPool {

    private static final Logger LOG = Logger.getLogger(ConnectionPool.class.getName());
    private static final long WAIT_SECONDS = 10;

    private static volatile ConnectionPool instance;

    private final BlockingQueue<Connection> free;
    private final String url;
    private final String user;
    private final String password;

    private ConnectionPool(ServerConfig config) throws SQLException {
        this.url = config.getDbUrl();
        this.user = config.getDbUser();
        this.password = config.getDbPassword();
        int size = config.getDbPoolSize();
        this.free = new ArrayBlockingQueue<>(size);
        for (int i = 0; i < size; i++) {
            free.add(createConnection());
        }
        LOG.info("Пул соединений создан: " + size + " шт.");
    }

    /** Инициализация при старте сервера. */
    public static synchronized void init(ServerConfig config) throws SQLException {
        if (instance == null) {
            instance = new ConnectionPool(config);
        }
    }

    public static ConnectionPool getInstance() {
        ConnectionPool local = instance;
        if (local == null) {
            throw new IllegalStateException("Пул соединений не инициализирован");
        }
        return local;
    }

    private Connection createConnection() throws SQLException {
        return DriverManager.getConnection(url, user, password);
    }

    /**
     * Выдаёт соединение. Используется в try-with-resources:
     * при close() соединение возвращается в пул.
     */
    public PooledConnection acquire() {
        try {
            Connection connection = free.poll(WAIT_SECONDS, TimeUnit.SECONDS);
            if (connection == null) {
                throw new DataAccessException("Нет свободных соединений с БД", null);
            }
            if (!connection.isValid(2)) {
                closeQuietly(connection);
                connection = createConnection();
            }
            return new PooledConnection(connection, this);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new DataAccessException("Ожидание соединения прервано", e);
        } catch (SQLException e) {
            throw new DataAccessException("Не удалось подключиться к БД", e);
        }
    }

    void release(Connection connection) {
        try {
            if (!connection.getAutoCommit()) {
                connection.rollback();
                connection.setAutoCommit(true);
            }
        } catch (SQLException e) {
            closeQuietly(connection);
            try {
                connection = createConnection();
            } catch (SQLException ex) {
                LOG.warning("Не удалось восстановить соединение: " + ex.getMessage());
                return;
            }
        }
        free.offer(connection);
    }

    public void shutdown() {
        Connection connection;
        while ((connection = free.poll()) != null) {
            closeQuietly(connection);
        }
    }

    private static void closeQuietly(Connection connection) {
        try {
            connection.close();
        } catch (SQLException ignored) {
            // соединение уже недоступно
        }
    }
}
