package by.bsuir.touragency.server.db;

import java.sql.Connection;

/**
 * Обёртка над соединением из пула для try-with-resources.
 */
public final class PooledConnection implements AutoCloseable {

    private final Connection connection;
    private final ConnectionPool pool;
    private boolean released;

    PooledConnection(Connection connection, ConnectionPool pool) {
        this.connection = connection;
        this.pool = pool;
    }

    public Connection get() {
        return connection;
    }

    @Override
    public void close() {
        if (!released) {
            released = true;
            pool.release(connection);
        }
    }
}
