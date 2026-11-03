package by.bsuir.touragency.server.util;

import by.bsuir.touragency.common.exception.ValidationException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.regex.Pattern;

/**
 * Проверка входных данных на сервере. Статические методы.
 * Сообщения — ключи из i18n/server_*.properties, поля — ключи «field.*».
 */
public final class Validator {

    private static final Pattern LOGIN = Pattern.compile("^[A-Za-z0-9_]{3,50}$");
    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    private static final Pattern PHONE = Pattern.compile("^\\+?[0-9 ()-]{7,20}$");

    private Validator() {
    }

    public static String requireNotBlank(String value, String fieldKey) throws ValidationException {
        if (value == null || value.isBlank()) {
            throw new ValidationException("validation.required", fieldKey);
        }
        return value.trim();
    }

    // Перегрузка для объектов (идентификаторы, даты)
    public static <T> T requireNotNull(T value, String fieldKey) throws ValidationException {
        if (value == null) {
            throw new ValidationException("validation.required", fieldKey);
        }
        return value;
    }

    public static String optional(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    public static int range(Integer value, int min, int max, String fieldKey) throws ValidationException {
        requireNotNull(value, fieldKey);
        if (value < min || value > max) {
            throw new ValidationException("validation.range", fieldKey, min, max);
        }
        return value;
    }

    public static BigDecimal positive(BigDecimal value, String fieldKey) throws ValidationException {
        requireNotNull(value, fieldKey);
        if (value.signum() <= 0) {
            throw new ValidationException("validation.positive", fieldKey);
        }
        return value;
    }

    public static void datesOrder(LocalDate from, LocalDate to) throws ValidationException {
        if (from != null && to != null && !to.isAfter(from)) {
            throw new ValidationException("validation.datesOrder");
        }
    }

    public static String login(String value) throws ValidationException {
        String login = requireNotBlank(value, "field.login");
        if (!LOGIN.matcher(login).matches()) {
            throw new ValidationException("validation.login");
        }
        return login;
    }

    public static String password(String value) throws ValidationException {
        if (value == null || value.length() < 6) {
            throw new ValidationException("validation.password");
        }
        return value;
    }

    public static String email(String value) throws ValidationException {
        String email = requireNotBlank(value, "field.email");
        if (!EMAIL.matcher(email).matches()) {
            throw new ValidationException("validation.email");
        }
        return email;
    }

    /** Телефон необязателен: пустое значение превращается в null. */
    public static String phone(String value) throws ValidationException {
        String phone = optional(value);
        if (phone != null && !PHONE.matcher(phone).matches()) {
            throw new ValidationException("validation.phone");
        }
        return phone;
    }
}
