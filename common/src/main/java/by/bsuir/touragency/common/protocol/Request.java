package by.bsuir.touragency.common.protocol;

import by.bsuir.touragency.common.model.Lang;

import java.io.Serial;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Запрос клиента к серверу: тип команды и именованные параметры.
 */
public class Request implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final CommandType command;
    private final HashMap<String, Serializable> params = new HashMap<>();
    private Lang lang;              // null — клиент подставит язык интерфейса

    public Request(CommandType command) {
        this.command = command;
    }

    /** Добавляет параметр и возвращает этот же запрос (удобно строить цепочкой). */
    public Request with(String name, Serializable value) {
        params.put(name, value);
        return this;
    }

    /** Язык, на котором клиент ждёт сообщения и данные. */
    public Lang getLang() {
        return lang == null ? Lang.RU : lang;
    }

    public boolean hasLang() {
        return lang != null;
    }

    public Request setLang(Lang lang) {
        this.lang = lang;
        return this;
    }

    public CommandType getCommand() {
        return command;
    }

    public Serializable get(String name) {
        return params.get(name);
    }

    public String getString(String name) {
        Serializable value = params.get(name);
        return value == null ? null : value.toString();
    }

    // Перегрузка: значение по умолчанию, если параметр не передан
    public String getString(String name, String defaultValue) {
        String value = getString(name);
        return value == null ? defaultValue : value;
    }

    public Integer getInt(String name) {
        Serializable value = params.get(name);
        if (value == null) return null;
        if (value instanceof Number number) return number.intValue();
        return Integer.valueOf(value.toString());
    }

    public boolean getBool(String name) {
        Serializable value = params.get(name);
        return value instanceof Boolean b ? b : Boolean.parseBoolean(String.valueOf(value));
    }

    @SuppressWarnings("unchecked")
    public <T extends Serializable> T getObject(String name) {
        return (T) params.get(name);
    }

    public Map<String, Serializable> getParams() {
        return Map.copyOf(params);
    }

    @Override
    public String toString() {
        // Пароли в логи не попадают
        return "Request{" + command + ", params=" + params.keySet() + "}";
    }
}
