package by.bsuir.touragency.common.model;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * Предварительный расчёт стоимости: цена без скидки, применённая скидка
 * и её объяснение (ключ explanationKey с аргументами).
 */
public record BookingQuote(int persons, BigDecimal pricePerPerson, BigDecimal basePrice,
                           Integer discountId, String discountName, BigDecimal discountPercent,
                           BigDecimal discountAmount, BigDecimal total, int freeSeats,
                           String explanationKey, String[] explanationArgs) implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
}
