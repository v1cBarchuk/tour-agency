package by.bsuir.touragency.server.service.discount;

import by.bsuir.touragency.common.model.Discount;
import by.bsuir.touragency.common.model.DiscountType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Проверка выбора наибольшей подходящей скидки (паттерн Strategy). */
class DiscountCalculatorTest {

    private final DiscountCalculator calculator = new DiscountCalculator();

    private static Discount seasonal(String percent, LocalDate from, LocalDate to) {
        Discount d = new Discount();
        d.setType(DiscountType.SEASONAL);
        d.setPercent(new BigDecimal(percent));
        d.setValidFrom(from);
        d.setValidTo(to);
        return d;
    }

    private static Discount early(String percent, int days) {
        Discount d = new Discount();
        d.setType(DiscountType.EARLY_BOOKING);
        d.setPercent(new BigDecimal(percent));
        d.setMinDaysBefore(days);
        return d;
    }

    private static Discount loyalty(String percent, int trips) {
        Discount d = new Discount();
        d.setType(DiscountType.LOYALTY);
        d.setPercent(new BigDecimal(percent));
        d.setMinCompletedTrips(trips);
        return d;
    }

    private static final LocalDate TOUR = LocalDate.of(2027, 7, 10);

    @Test
    @DisplayName("Из нескольких подходящих скидок выбирается наибольшая")
    void picksTheLargest() {
        DiscountContext ctx = new DiscountContext(TOUR, TOUR.minusDays(90), 3);
        List<Discount> all = List.of(early("7", 60), loyalty("10", 2),
                seasonal("5", LocalDate.of(2027, 6, 1), LocalDate.of(2027, 8, 31)));
        Optional<DiscountCalculator.Applied> best = calculator.best(all, ctx);
        assertTrue(best.isPresent());
        assertEquals(DiscountType.LOYALTY, best.get().discount().getType());
    }

    @Test
    @DisplayName("Скидка раннего бронирования не действует, если до тура мало дней")
    void earlyBookingNeedsEnoughDays() {
        DiscountContext ctx = new DiscountContext(TOUR, TOUR.minusDays(30), 0);
        assertTrue(calculator.best(List.of(early("7", 60)), ctx).isEmpty());
    }

    @Test
    @DisplayName("Сезонная скидка действует только для туров внутри периода")
    void seasonalChecksPeriod() {
        Discount summer = seasonal("5", LocalDate.of(2027, 6, 1), LocalDate.of(2027, 6, 30));
        DiscountContext july = new DiscountContext(TOUR, TOUR.minusDays(10), 0);
        assertTrue(calculator.best(List.of(summer), july).isEmpty());
        DiscountContext june = new DiscountContext(LocalDate.of(2027, 6, 30), TOUR.minusDays(60), 0);
        assertTrue(calculator.best(List.of(summer), june).isPresent());
    }

    @Test
    @DisplayName("Скидка постоянного клиента требует нужного числа поездок")
    void loyaltyNeedsTrips() {
        DiscountContext ctx = new DiscountContext(TOUR, TOUR.minusDays(10), 1);
        assertTrue(calculator.best(List.of(loyalty("10", 2)), ctx).isEmpty());
    }

    private static Discount promo(String percent, String code, Integer maxUses, int used) {
        Discount d = new Discount();
        d.setType(DiscountType.PROMO_CODE);
        d.setPercent(new BigDecimal(percent));
        d.setPromoCode(code);
        d.setMaxUses(maxUses);
        d.setUsedCount(used);
        return d;
    }

    @Test
    @DisplayName("Промокод применяется без учёта регистра и выигрывает, если он выгоднее")
    void promoCodeApplies() {
        DiscountContext ctx = new DiscountContext(TOUR, TOUR.minusDays(90), 0, " student15 ");
        Optional<DiscountCalculator.Applied> best = calculator.best(
                List.of(early("7", 60), promo("15", "STUDENT15", 50, 3)), ctx);
        assertEquals(DiscountType.PROMO_CODE, best.get().discount().getType());
        assertTrue(calculator.promoAccepted(List.of(promo("15", "STUDENT15", 50, 3)), ctx));
    }

    @Test
    @DisplayName("Исчерпанный или чужой промокод не действует")
    void promoCodeLimitAndMismatch() {
        DiscountContext ctx = new DiscountContext(TOUR, TOUR.minusDays(10), 0, "STUDENT15");
        assertTrue(calculator.best(List.of(promo("15", "STUDENT15", 50, 50)), ctx).isEmpty());
        assertTrue(calculator.best(List.of(promo("15", "WELCOME8", null, 0)), ctx).isEmpty());
        DiscountContext noCode = new DiscountContext(TOUR, TOUR.minusDays(10), 0);
        assertTrue(calculator.best(List.of(promo("15", "STUDENT15", null, 0)), noCode).isEmpty());
    }
}
