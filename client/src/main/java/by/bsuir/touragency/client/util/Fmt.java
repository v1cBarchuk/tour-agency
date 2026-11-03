package by.bsuir.touragency.client.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Единое форматирование цен (BYN) и дат для всего интерфейса.
 */
public final class Fmt {

    public static final String CURRENCY = "BYN";

    private Fmt() {
    }

    private static DecimalFormat moneyFormat() {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols();
        symbols.setGroupingSeparator(' ');   // неразрывный пробел: 1 250 BYN
        symbols.setDecimalSeparator(',');
        return new DecimalFormat("#,##0", symbols);
    }

    /** 1250.00 → «1 250 BYN». */
    public static String money(BigDecimal amount) {
        if (amount == null) return "—";
        return moneyFormat().format(amount.setScale(0, RoundingMode.HALF_UP)) + " " + CURRENCY;
    }

    // Перегрузка для целых сумм
    public static String money(Integer amount) {
        return amount == null ? "—" : money(BigDecimal.valueOf(amount));
    }

    /** Число без валюты: для таблиц, где валюта указана в заголовке столбца. */
    public static String number(BigDecimal amount) {
        return amount == null ? "—" : moneyFormat().format(amount.setScale(0, RoundingMode.HALF_UP));
    }

    public static String date(LocalDate date) {
        if (date == null) return "—";
        return date.format(DateTimeFormatter.ofPattern("d MMM yyyy", I18n.getLang().getLocale()));
    }

    public static String shortDate(LocalDate date) {
        if (date == null) return "—";
        return date.format(DateTimeFormatter.ofPattern("d MMM", I18n.getLang().getLocale()));
    }

    public static String dateRange(LocalDate from, LocalDate to) {
        return shortDate(from) + " – " + date(to);
    }

    public static String rating(Double rating) {
        return rating == null ? "—" : String.format(I18n.getLang().getLocale(), "%.1f", rating);
    }

    public static String stars(int count) {
        return "★".repeat(Math.max(0, count));
    }
}
