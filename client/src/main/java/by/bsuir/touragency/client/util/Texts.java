package by.bsuir.touragency.client.util;

import by.bsuir.touragency.common.model.BookingStatus;
import by.bsuir.touragency.common.model.Criterion;
import by.bsuir.touragency.common.model.Notification;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Тексты, которые собираются из кодов, присланных сервером:
 * пояснения к оценке тура, уведомления, статусы.
 */
public final class Texts {

    private Texts() {
    }

    public static String status(BookingStatus status) {
        return I18n.t("status." + status.name());
    }

    /** Текст критерия: ключ crit.CODE.RESULT, аргументы — даты и суммы форматируются. */
    public static String criterion(Criterion c) {
        String[] raw = c.getArgs();
        Object[] args = new Object[raw.length];
        for (int i = 0; i < raw.length; i++) {
            boolean moneyArg = c.getCode() == Criterion.Code.BUDGET && i < 2;
            args[i] = moneyArg ? money(raw[i]) : pretty(raw[i]);
        }
        return I18n.t("crit." + c.getCode().name() + "." + c.getResult().name(), args);
    }

    public static String criterionName(Criterion c) {
        return I18n.t("crit." + c.getCode().name());
    }

    private static Object pretty(String arg) {
        if (arg == null) return "";
        if (arg.matches("\\d{4}-\\d{2}-\\d{2}")) {
            return Fmt.date(LocalDate.parse(arg));
        }
        return arg;
    }

    public static String money(String raw) {
        try {
            return Fmt.money(new BigDecimal(raw));
        } catch (NumberFormatException e) {
            return raw;
        }
    }

    /** Текст уведомления на текущем языке. */
    public static String notification(Notification n) {
        String tour = n.tourTitle() == null ? "" : n.tourTitle();
        return switch (n.type()) {
            case "NEW_MATCHING_TOUR" -> I18n.t("notif.NEW_MATCHING_TOUR", tour, n.message() == null ? "" : n.message());
            case "DISCOUNT_OFFER" -> I18n.t("notif.DISCOUNT_OFFER", tour, n.message() == null ? "" : n.message());
            case "BOOKING_STATUS" -> {
                try {
                    yield I18n.t("notif.BOOKING_STATUS", n.bookingId(), status(BookingStatus.valueOf(n.message())));
                } catch (IllegalArgumentException | NullPointerException e) {
                    yield n.message();
                }
            }
            default -> I18n.t("notif.SYSTEM");
        };
    }

    public static String notificationIcon(Notification n) {
        return switch (n.type()) {
            case "NEW_MATCHING_TOUR" -> "★";
            case "DISCOUNT_OFFER" -> "%";
            case "BOOKING_STATUS" -> "✓";
            default -> "i";
        };
    }
}
