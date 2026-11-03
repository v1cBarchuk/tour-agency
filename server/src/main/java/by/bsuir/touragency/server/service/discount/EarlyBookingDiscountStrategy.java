package by.bsuir.touragency.server.service.discount;

import by.bsuir.touragency.common.model.Discount;

/** Раннее бронирование: до начала тура не меньше заданного числа дней. */
public class EarlyBookingDiscountStrategy implements DiscountStrategy {

    @Override
    public boolean applies(Discount d, DiscountContext ctx) {
        return d.getMinDaysBefore() != null && ctx.daysBeforeTour() >= d.getMinDaysBefore();
    }

    @Override
    public String explanationKey() {
        return "discount.why.EARLY_BOOKING";
    }

    @Override
    public String[] explanationArgs(Discount d, DiscountContext ctx) {
        return new String[]{String.valueOf(ctx.daysBeforeTour()), String.valueOf(d.getMinDaysBefore())};
    }
}
