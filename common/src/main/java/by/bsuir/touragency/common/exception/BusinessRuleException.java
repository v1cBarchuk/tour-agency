package by.bsuir.touragency.common.exception;

import java.io.Serial;

/** Нарушение бизнес-правила: нет мест, нельзя удалить тур с бронями и т.п. */
public class BusinessRuleException extends AppException {

    @Serial
    private static final long serialVersionUID = 1L;

    public BusinessRuleException(String messageKey, Object... args) {
        super(messageKey, args);
    }
}
