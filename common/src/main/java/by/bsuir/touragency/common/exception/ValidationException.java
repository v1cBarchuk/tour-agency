package by.bsuir.touragency.common.exception;

import java.io.Serial;

/** Некорректные входные данные (пустое поле, неверный формат и т.п.). */
public class ValidationException extends AppException {

    @Serial
    private static final long serialVersionUID = 1L;

    public ValidationException(String messageKey, Object... args) {
        super(messageKey, args);
    }
}
