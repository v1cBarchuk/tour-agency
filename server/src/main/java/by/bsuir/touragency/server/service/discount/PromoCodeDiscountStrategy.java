package by.bsuir.touragency.server.service.discount;

import by.bsuir.touragency.common.model.Discount;

/**
 * Промокод: клиент ввёл код скидки, срок действия не истёк и лимит использований не исчерпан.
 * Четвёртая реализация {@link DiscountStrategy} — новая скидка добавлена без изменения калькулятора.
 */
public class PromoCodeDiscountStrategy implements DiscountStrategy {

    @Override
    public boolean applies(Discount d, DiscountContext ctx) {
        if (d.getPromoCode() == null || !ctx.hasPromoCode()) return false;
        if (!d.getPromoCode().equalsIgnoreCase(ctx.promoCode().trim())) return false;
        if (d.getValidFrom() != null && ctx.bookingDate().isBefore(d.getValidFrom())) return false;
        if (d.getValidTo() != null && ctx.bookingDate().isAfter(d.getValidTo())) return false;
        return d.getMaxUses() == null || d.getUsedCount() < d.getMaxUses();
    }

    @Override
    public String explanationKey() {
        return "discount.why.PROMO_CODE";
    }

    @Override
    public String[] explanationArgs(Discount d, DiscountContext ctx) {
        return new String[]{d.getPromoCode().toUpperCase()};
    }
}
