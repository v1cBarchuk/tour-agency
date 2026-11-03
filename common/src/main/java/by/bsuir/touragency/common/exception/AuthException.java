package by.bsuir.touragency.common.exception;

import java.io.Serial;

/** Ошибка авторизации: неверный логин/пароль, блокировка, нет прав. */
public class AuthException extends AppException {

    @Serial
    private static final long serialVersionUID = 1L;

    public AuthException(String messageKey, Object... args) {
        super(messageKey, args);
    }
}
