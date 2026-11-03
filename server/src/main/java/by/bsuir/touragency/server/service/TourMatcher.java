package by.bsuir.touragency.server.service;

import by.bsuir.touragency.common.model.Criterion;
import by.bsuir.touragency.common.model.Criterion.Code;
import by.bsuir.touragency.common.model.Criterion.Result;
import by.bsuir.touragency.common.model.MatchResult;
import by.bsuir.touragency.common.model.Preferences;
import by.bsuir.touragency.common.model.Tour;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Алгоритм оценки тура по анкете предпочтений (ключевая бизнес-логика).
 *
 * <pre>
 * Критерий          Максимум   Правило
 * Страна               25      страна в списке предпочтений — 25, иначе 0
 * Бюджет               25      цена в [min; max] — 25; дешевле min — 20;
 *                              дороже max — 25 × (1 − 2 × превышение/max), не меньше 0
 * Тип отдыха           15      тип в списке — 15, иначе 0
 * Даты поездки         15      начало тура в желаемом окне — 15;
 *                              вне окна — 15 × (1 − дней_отклонения / 30), не меньше 0
 * Количество ночей     10      в [min; max] — 10; иначе 10 − 3 × отклонение, не меньше 0
 * Звёздность отеля     10      звёзд ≥ желаемых — 10; иначе 10 − 5 × нехватка, не меньше 0
 * </pre>
 *
 * Незаполненный критерий пропускается и не снижает оценку: итог нормализуется
 * по сумме максимумов только заполненных критериев — процент = баллы / максимум × 100.
 * Сверху добавляются бонусы (итог не больше 100): +5, если клиент уже ездил в эту страну
 * и оценил поездку на 4–5, и +5, если тур или его страна есть в избранном.
 * Тип питания баллов не даёт, но показывается в объяснении.
 */
public final class TourMatcher {

    public static final double MAX_COUNTRY = 25;
    public static final double MAX_BUDGET = 25;
    public static final double MAX_TYPE = 15;
    public static final double MAX_DATES = 15;
    public static final double MAX_NIGHTS = 10;
    public static final double MAX_STARS = 10;
    public static final double BONUS_HISTORY = 5;
    public static final double BONUS_FAVORITES = 5;

    /** Что известно о клиенте помимо анкеты. */
    public record ClientContext(Map<Integer, String> likedCountries, Set<Integer> favoriteTourIds,
                                Set<Integer> favoriteCountryIds) {
        public static ClientContext empty() {
            return new ClientContext(Map.of(), Set.of(), Set.of());
        }
    }

    private TourMatcher() {
    }

    public static MatchResult evaluate(Preferences p, Tour t, ClientContext ctx, String mealName) {
        List<Criterion> list = new ArrayList<>();
        list.add(country(p, t));
        list.add(budget(p, t));
        list.add(vacationType(p, t));
        list.add(dates(p, t));
        list.add(nights(p, t));
        list.add(stars(p, t));
        list.add(meal(p, t, mealName));

        double score = 0;
        double max = 0;
        for (Criterion c : list) {
            if (c.isCounted()) {
                score += c.getPoints();
                max += c.getMax();
            }
        }

        double bonus = 0;
        String liked = ctx.likedCountries().get(t.getCountryId());
        if (liked != null) {
            list.add(new Criterion(Code.HISTORY, BONUS_HISTORY, 0, Result.BONUS, liked));
            bonus += BONUS_HISTORY;
        }
        if (ctx.favoriteTourIds().contains(t.getId()) || ctx.favoriteCountryIds().contains(t.getCountryId())) {
            list.add(new Criterion(Code.FAVORITES, BONUS_FAVORITES, 0, Result.BONUS));
            bonus += BONUS_FAVORITES;
        }

        int percent = max == 0 ? (int) Math.min(100, 50 + bonus)
                : (int) Math.min(100, Math.round(score / max * 100) + bonus);
        return new MatchResult(t, list, round(score), max, bonus, percent);
    }

    private static Criterion country(Preferences p, Tour t) {
        if (p.getCountryIds().isEmpty()) return skipped(Code.COUNTRY, MAX_COUNTRY);
        boolean ok = p.getCountryIds().contains(t.getCountryId());
        return new Criterion(Code.COUNTRY, ok ? MAX_COUNTRY : 0, MAX_COUNTRY, ok ? Result.MATCH : Result.MISS,
                t.getCountryName());
    }

    private static Criterion budget(Preferences p, Tour t) {
        BigDecimal min = p.getMinBudget();
        BigDecimal max = p.getMaxBudget();
        if (min == null && max == null) return skipped(Code.BUDGET, MAX_BUDGET);
        double price = t.getPricePerPerson().doubleValue();
        String priceArg = String.valueOf(Math.round(price));
        if (max != null && price > max.doubleValue()) {
            double over = (price - max.doubleValue()) / max.doubleValue();
            double pts = Math.max(0, MAX_BUDGET * (1 - 2 * over));
            return new Criterion(Code.BUDGET, round(pts), MAX_BUDGET, pts > 0 ? Result.PARTIAL : Result.MISS,
                    priceArg, String.valueOf(Math.round(max.doubleValue())), String.valueOf(Math.round(over * 100)));
        }
        if (min != null && price < min.doubleValue()) {
            return new Criterion(Code.BUDGET, 20, MAX_BUDGET, Result.MATCH, priceArg);
        }
        return new Criterion(Code.BUDGET, MAX_BUDGET, MAX_BUDGET, Result.MATCH, priceArg);
    }

    private static Criterion vacationType(Preferences p, Tour t) {
        if (p.getVacationTypeIds().isEmpty()) return skipped(Code.VACATION_TYPE, MAX_TYPE);
        boolean ok = p.getVacationTypeIds().contains(t.getVacationTypeId());
        return new Criterion(Code.VACATION_TYPE, ok ? MAX_TYPE : 0, MAX_TYPE, ok ? Result.MATCH : Result.MISS,
                t.getVacationTypeName());
    }

    private static Criterion dates(Preferences p, Tour t) {
        LocalDate from = p.getDesiredFrom();
        LocalDate to = p.getDesiredTo();
        if (from == null && to == null) return skipped(Code.DATES, MAX_DATES);
        LocalDate start = t.getStartDate();
        long off = 0;
        if (from != null && start.isBefore(from)) off = ChronoUnit.DAYS.between(start, from);
        if (to != null && start.isAfter(to)) off = ChronoUnit.DAYS.between(to, start);
        if (off == 0) {
            return new Criterion(Code.DATES, MAX_DATES, MAX_DATES, Result.MATCH, start.toString());
        }
        double pts = Math.max(0, MAX_DATES * (1 - off / 30.0));
        return new Criterion(Code.DATES, round(pts), MAX_DATES, pts > 0 ? Result.PARTIAL : Result.MISS,
                start.toString(), String.valueOf(off));
    }

    private static Criterion nights(Preferences p, Tour t) {
        Integer min = p.getMinNights();
        Integer max = p.getMaxNights();
        if (min == null && max == null) return skipped(Code.NIGHTS, MAX_NIGHTS);
        long n = t.getNights();
        long off = 0;
        if (min != null && n < min) off = min - n;
        if (max != null && n > max) off = n - max;
        if (off == 0) {
            return new Criterion(Code.NIGHTS, MAX_NIGHTS, MAX_NIGHTS, Result.MATCH, String.valueOf(n));
        }
        double pts = Math.max(0, MAX_NIGHTS - 3 * off);
        return new Criterion(Code.NIGHTS, pts, MAX_NIGHTS, pts > 0 ? Result.PARTIAL : Result.MISS,
                String.valueOf(n), String.valueOf(off));
    }

    private static Criterion stars(Preferences p, Tour t) {
        Integer min = p.getMinStars();
        if (min == null) return skipped(Code.STARS, MAX_STARS);
        int s = t.getHotelStars();
        if (s >= min) {
            return new Criterion(Code.STARS, MAX_STARS, MAX_STARS, Result.MATCH, String.valueOf(s));
        }
        double pts = Math.max(0, MAX_STARS - 5 * (min - s));
        return new Criterion(Code.STARS, pts, MAX_STARS, pts > 0 ? Result.PARTIAL : Result.MISS,
                String.valueOf(s), String.valueOf(min));
    }

    /** Питание баллов не даёт (максимум 0), но попадает в объяснение. */
    private static Criterion meal(Preferences p, Tour t, String preferredName) {
        if (p.getMealTypeId() == null) return skipped(Code.MEAL, 0);
        boolean ok = p.getMealTypeId().equals(t.getMealTypeId());
        return new Criterion(Code.MEAL, 0, 0, ok ? Result.MATCH : Result.MISS, t.getMealTypeName(),
                preferredName == null ? "" : preferredName);
    }

    private static Criterion skipped(Code code, double max) {
        return new Criterion(code, 0, max, Result.SKIPPED);
    }

    private static double round(double v) {
        return Math.round(v * 10) / 10.0;
    }
}
