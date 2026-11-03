package by.bsuir.touragency.server.db;

import java.io.Serial;
import java.sql.SQLException;

/**
 * Непроверяемое исключение уровня доступа к данным (расширяет RuntimeException).
 * Оборачивает SQLException, чтобы детали SQL не уходили на клиент.
 */
public class DataAccessException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    private static final int MYSQL_DUPLICATE_KEY = 1062;

    public DataAccessException(String message, Throwable cause) {
        super(message, cause);
    }

    /** Нарушение уникального ключа (например, страна с таким названием уже есть). */
    public boolean isDuplicateKey() {
        return getCause() instanceof SQLException sql && sql.getErrorCode() == MYSQL_DUPLICATE_KEY;
    }
}
