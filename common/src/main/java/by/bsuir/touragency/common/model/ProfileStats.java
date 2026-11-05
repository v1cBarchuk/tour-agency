package by.bsuir.touragency.common.model;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * Сводка для личного кабинета клиента.
 *
 * @param bookings       все бронирования, кроме отменённых
 * @param upcoming       предстоящие поездки (подтверждённые и оплаченные, дата начала впереди)
 * @param completedTrips завершённые поездки (от них зависит скидка постоянного клиента)
 * @param totalSpent     оплачено всего, BYN
 * @param favorites      туров в избранном
 * @param reviews        оставлено отзывов
 * @param countries      посещено стран
 */
public record ProfileStats(int bookings, int upcoming, int completedTrips, BigDecimal totalSpent,
                           int favorites, int reviews, int countries) implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
}
