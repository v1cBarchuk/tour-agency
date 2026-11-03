package by.bsuir.touragency.server.service.discount;

import by.bsuir.touragency.common.model.Discount;

/**
 * Стратегия скидки (паттерн Strategy): каждая реализация знает,
 * при каком условии применяется скидка своего типа и как это объяснить клиенту.
 */
public interface DiscountStrategy {

    boolean applies(Discount discount, DiscountContext context);

    /** Ключ текста-объяснения (i18n) для клиента. */
    String explanationKey();

    /** Аргументы для текста-объяснения. */
    String[] explanationArgs(Discount discount, DiscountContext context);
}
