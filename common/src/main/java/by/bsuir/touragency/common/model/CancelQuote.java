package by.bsuir.touragency.common.model;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

/** Расчёт штрафа при отмене: сколько дней до тура, процент, штраф и возврат. */
public record CancelQuote(BigDecimal total, BigDecimal paid, long daysBefore, int percent,
                          BigDecimal penalty, BigDecimal refund) implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    public boolean wasPaid() {
        return paid != null && paid.signum() > 0;
    }
}
