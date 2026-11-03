package by.bsuir.touragency.server.service.discount;

import by.bsuir.touragency.common.model.Discount;

/** Постоянный клиент: не меньше заданного числа завершённых поездок. */
public class LoyaltyDiscountStrategy implements DiscountStrategy {

    @Override
    public boolean applies(Discount d, DiscountContext ctx) {
        return d.getMinCompletedTrips() != null && ctx.completedTrips() >= d.getMinCompletedTrips();
    }

    @Override
    public String explanationKey() {
        return "discount.why.LOYALTY";
    }

    @Override
    public String[] explanationArgs(Discount d, DiscountContext ctx) {
        return new String[]{String.valueOf(ctx.completedTrips())};
    }
}
