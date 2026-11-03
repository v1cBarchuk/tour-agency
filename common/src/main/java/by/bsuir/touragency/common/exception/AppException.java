package by.bsuir.touragency.common.exception;

import java.io.Serial;

/**
 * Базовое проверяемое исключение приложения (расширяет java.lang.Exception).
 * Вместо готового текста хранит ключ сообщения и аргументы: текст на нужном
 * языке подставляет сервер (по языку запроса) или клиент.
 * Аргумент, начинающийся с «field.», тоже переводится (название поля).
 */
public class AppException extends Exception {

    @Serial
    private static final long serialVersionUID = 1L;

    private final transient Object[] args;

    public AppException(String messageKey, Object... args) {
        super(messageKey);
        this.args = args;
    }

    public AppException(String messageKey, Throwable cause) {
        super(messageKey, cause);
        this.args = new Object[0];
    }

    public String getMessageKey() {
        return getMessage();
    }

    public Object[] getArgs() {
        return args == null ? new Object[0] : args;
    }
}
