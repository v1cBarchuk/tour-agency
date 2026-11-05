package by.bsuir.touragency.server.service;

import by.bsuir.touragency.common.model.MatchResult;
import by.bsuir.touragency.common.model.Preferences;
import by.bsuir.touragency.common.model.Tour;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Проверка алгоритма оценки соответствия тура анкете клиента. */
class TourMatcherTest {

    private static Tour tour() {
        Tour t = new Tour();
        t.setId(1);
        t.setCountryId(10);
        t.setCountryName("Турция");
        t.setVacationTypeId(2);
        t.setMealTypeId(3);
        t.setHotelStars(5);
        t.setPricePerPerson(new BigDecimal("1000"));
        t.setStartDate(LocalDate.of(2027, 7, 10));
        t.setEndDate(LocalDate.of(2027, 7, 17));
        return t;
    }

    private static Preferences fullMatch() {
        Preferences p = new Preferences();
        p.setCountryIds(List.of(10));
        p.setVacationTypeIds(List.of(2));
        p.setMaxBudget(new BigDecimal("1200"));
        p.setDesiredFrom(LocalDate.of(2027, 7, 1));
        p.setDesiredTo(LocalDate.of(2027, 7, 31));
        p.setMinNights(6);
        p.setMaxNights(8);
        p.setMinStars(4);
        return p;
    }

    @Test
    @DisplayName("Полное совпадение по всем критериям даёт 100 %")
    void fullMatchIs100() {
        MatchResult r = TourMatcher.evaluate(fullMatch(), tour(), TourMatcher.ClientContext.empty(), null);
        assertEquals(100, r.getPercent());
    }

    @Test
    @DisplayName("Пустая анкета даёт нейтральные 50 %")
    void emptyPreferencesGive50() {
        MatchResult r = TourMatcher.evaluate(new Preferences(), tour(), TourMatcher.ClientContext.empty(), null);
        assertEquals(50, r.getPercent());
    }

    @Test
    @DisplayName("Другая страна снижает процент на её вес")
    void otherCountryLosesCountryWeight() {
        Preferences p = fullMatch();
        p.setCountryIds(List.of(99));
        MatchResult r = TourMatcher.evaluate(p, tour(), TourMatcher.ClientContext.empty(), null);
        assertEquals(75, r.getPercent());                       // 75 из 100 баллов
    }

    @Test
    @DisplayName("Превышение бюджета на 25 % даёт половину баллов за бюджет")
    void budgetOverrunIsPartial() {
        Preferences p = new Preferences();
        p.setMaxBudget(new BigDecimal("800"));                 // цена 1000 — превышение 25 %
        MatchResult r = TourMatcher.evaluate(p, tour(), TourMatcher.ClientContext.empty(), null);
        assertEquals(50, r.getPercent());                       // 12,5 из 25
    }

    @Test
    @DisplayName("Бонусы за историю и избранное прибавляются, но не выше 100 %")
    void bonusesAreCapped() {
        TourMatcher.ClientContext ctx = new TourMatcher.ClientContext(Map.of(10, "Турция"), Set.of(1), Set.of());
        MatchResult empty = TourMatcher.evaluate(new Preferences(), tour(), ctx, null);
        assertEquals(60, empty.getPercent());                   // 50 + 5 + 5
        MatchResult full = TourMatcher.evaluate(fullMatch(), tour(), ctx, null);
        assertEquals(100, full.getPercent());
    }

    @Test
    @DisplayName("Отклонение дат на 15 дней даёт частичное совпадение")
    void datesOffsetIsPartial() {
        Preferences p = new Preferences();
        p.setDesiredTo(LocalDate.of(2027, 6, 25));             // тур начинается на 15 дней позже
        MatchResult r = TourMatcher.evaluate(p, tour(), TourMatcher.ClientContext.empty(), null);
        assertEquals(50, r.getPercent());
        assertTrue(r.differs().size() == 1);
    }
}
