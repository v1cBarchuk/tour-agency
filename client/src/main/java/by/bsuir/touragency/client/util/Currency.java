package by.bsuir.touragency.client.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;
import java.util.prefs.Preferences;

/**
 * Валюта отображения цен. Все суммы хранятся и считаются на сервере в BYN;
 * здесь они только пересчитываются для показа по курсу из настроек системы.
 */
public final class Currency {

    public static final String BASE = "BYN";
    public static final String[] CODES = {"BYN", "USD", "EUR"};
    private static final Preferences PREFS = Preferences.userNodeForPackage(Currency.class);
    private static final Map<String, BigDecimal> RATES = new HashMap<>();   // 1 USD = 3.27 BYN …
    private static String code = PREFS.get("currency", BASE);

    private Currency() {
    }

    public static String code() {
        return RATES.containsKey(code) || BASE.equals(code) ? code : BASE;
    }

    /** Выбранная пользователем валюта (даже если курсы ещё не загружены). */
    public static String selected() {
        return code;
    }

    public static void set(String newCode) {
        code = newCode;
        try {
            PREFS.put("currency", newCode);
        } catch (Exception ignored) {
            // выбор просто не запомнится
        }
    }

    public static void setRates(Map<String, BigDecimal> rates) {
        RATES.clear();
        RATES.putAll(rates);
    }

    public static boolean isBase() {
        return BASE.equals(code());
    }

    /** Сумма в BYN → сумма в выбранной валюте. */
    public static BigDecimal convert(BigDecimal byn) {
        if (byn == null || isBase()) return byn;
        return byn.divide(RATES.get(code()), 2, RoundingMode.HALF_UP);
    }

    /** Курс для подсказки: «1 USD = 3,27 BYN». */
    public static String rateText() {
        if (isBase()) return "";
        return "1 " + code() + " = " + RATES.get(code()).stripTrailingZeros().toPlainString().replace('.', ',') + " " + BASE;
    }
}
