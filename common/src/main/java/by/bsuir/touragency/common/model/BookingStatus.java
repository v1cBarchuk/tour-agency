package by.bsuir.touragency.common.model;

import java.util.EnumSet;
import java.util.Set;

/**
 * Статусы бронирования и допустимые переходы между ними.
 * Идентификаторы совпадают с таблицей booking_statuses.
 */
public enum BookingStatus {
    NEW(1),
    PENDING_CONFIRMATION(2),
    CONFIRMED(3),
    AWAITING_PAYMENT(4),
    PAID(5),
    COMPLETED(6),
    CANCELLED(7);

    private final int id;

    BookingStatus(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    /** Следующие статусы, в которые менеджер может перевести заявку. */
    public Set<BookingStatus> next() {
        return switch (this) {
            case NEW -> EnumSet.of(PENDING_CONFIRMATION, CANCELLED);
            case PENDING_CONFIRMATION -> EnumSet.of(CONFIRMED, CANCELLED);
            case CONFIRMED -> EnumSet.of(AWAITING_PAYMENT, CANCELLED);
            case AWAITING_PAYMENT -> EnumSet.of(PAID, CANCELLED);
            case PAID -> EnumSet.of(COMPLETED, CANCELLED);
            case COMPLETED, CANCELLED -> EnumSet.noneOf(BookingStatus.class);
        };
    }

    public boolean canMoveTo(BookingStatus target) {
        return next().contains(target);
    }

    public boolean isFinal() {
        return this == COMPLETED || this == CANCELLED;
    }

    /** Шаги основной цепочки для временной шкалы (без отмены). */
    public static BookingStatus[] flow() {
        return new BookingStatus[]{NEW, PENDING_CONFIRMATION, CONFIRMED, AWAITING_PAYMENT, PAID, COMPLETED};
    }

    public static BookingStatus fromId(int id) {
        for (BookingStatus s : values()) {
            if (s.id == id) return s;
        }
        throw new IllegalArgumentException("Unknown status: " + id);
    }
}
