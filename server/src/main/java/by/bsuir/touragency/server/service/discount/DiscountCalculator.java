package by.bsuir.touragency.server.service.discount;

import by.bsuir.touragency.common.model.Discount;
import by.bsuir.touragency.common.model.DiscountType;

import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Выбор скидки для бронирования. Скидки разных типов несовместимы:
 * применяется одна — наибольшая из подходящих, поэтому двойного применения не бывает.
 */
public class DiscountCalculator {

    /** Выбранная скидка вместе со стратегией, которая её одобрила. */
    public record Applied(Discount discount, DiscountStrategy strategy, DiscountContext context) {
        public String[] explanationArgs() {
            return strategy.explanationArgs(discount, context);
        }
    }

    private final Map<DiscountType, DiscountStrategy> strategies = new EnumMap<>(DiscountType.class);

    public DiscountCalculator() {
        strategies.put(DiscountType.SEASONAL, new SeasonalDiscountStrategy());
        strategies.put(DiscountType.EARLY_BOOKING, new EarlyBookingDiscountStrategy());
        strategies.put(DiscountType.LOYALTY, new LoyaltyDiscountStrategy());
    }

    public Optional<Applied> best(List<Discount> activeDiscounts, DiscountContext context) {
        return activeDiscounts.stream()
                .filter(d -> strategies.get(d.getType()).applies(d, context))
                .max(Comparator.comparing(Discount::getPercent))
                .map(d -> new Applied(d, strategies.get(d.getType()), context));
    }
}
