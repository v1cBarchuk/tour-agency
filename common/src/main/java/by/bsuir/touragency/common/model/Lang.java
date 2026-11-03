package by.bsuir.touragency.common.model;

import java.util.Locale;

/** Язык интерфейса. Совпадает со значениями users.language. */
public enum Lang {
    RU(Locale.of("ru")),
    EN(Locale.ENGLISH);

    private final Locale locale;

    Lang(Locale locale) {
        this.locale = locale;
    }

    public Locale getLocale() {
        return locale;
    }

    /** Имя столбца БД с суффиксом языка: name → name_ru / name_en. */
    public String col(String base) {
        return base + "_" + name().toLowerCase();
    }

    public static Lang parse(String value) {
        return "EN".equalsIgnoreCase(value) ? EN : RU;
    }
}
