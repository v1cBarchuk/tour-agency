package by.bsuir.touragency.common.protocol;

import java.io.Serial;
import java.io.Serializable;

/**
 * Ответ сервера: статус, сообщение для пользователя и полезная нагрузка.
 */
public class Response implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final ResponseStatus status;
    private final String message;
    private final Serializable data;

    private Response(ResponseStatus status, String message, Serializable data) {
        this.status = status;
        this.message = message;
        this.data = data;
    }

    // Статические фабричные методы с перегрузкой
    public static Response ok() {
        return new Response(ResponseStatus.OK, null, null);
    }

    public static Response ok(Serializable data) {
        return new Response(ResponseStatus.OK, null, data);
    }

    public static Response ok(String message, Serializable data) {
        return new Response(ResponseStatus.OK, message, data);
    }

    public static Response error(String message) {
        return new Response(ResponseStatus.ERROR, message, null);
    }

    public static Response of(ResponseStatus status, String message) {
        return new Response(status, message, null);
    }

    public boolean isOk() {
        return status == ResponseStatus.OK;
    }

    public ResponseStatus getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }

    @SuppressWarnings("unchecked")
    public <T extends Serializable> T getData() {
        return (T) data;
    }

    @Override
    public String toString() {
        return "Response{" + status + ", " + message + "}";
    }
}
