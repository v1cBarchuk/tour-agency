package by.bsuir.touragency.common.exception;

import java.io.Serial;

/** Запрошенный объект не найден (тур, отель, страна…). */
public class NotFoundException extends AppException {

    @Serial
    private static final long serialVersionUID = 1L;

    public NotFoundException(String messageKey, Object... args) {
        super(messageKey, args);
    }
}
