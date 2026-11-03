package by.bsuir.touragency.server.service.discount;

import by.bsuir.touragency.common.model.Discount;

/** Сезонная скидка: дата начала тура попадает в период действия. */
public class SeasonalDiscountStrategy implements DiscountStrategy {

    @Override
    public boolean applies(Discount d, DiscountContext ctx) {
        return d.getValidFrom() != null && d.getValidTo() != null
                && !ctx.tourStart().isBefore(d.getValidFrom()) && !ctx.tourStart().isAfter(d.getValidTo());
    }

    @Override
    public String explanationKey() {
        return "discount.why.SEASONAL";
    }

    @Override
    public String[] explanationArgs(Discount d, DiscountContext ctx) {
        return new String[]{d.getValidFrom().toString(), d.getValidTo().toString()};
    }
}
