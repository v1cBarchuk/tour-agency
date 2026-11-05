package by.bsuir.touragency.common.model;

public enum DiscountType {
    SEASONAL("Сезонная"),
    EARLY_BOOKING("Раннее бронирование"),
    LOYALTY("Постоянный клиент"),
    PROMO_CODE("Промокод");

    private final String title;

    DiscountType(String title) {
        this.title = title;
    }

    public String getTitle() {
        return title;
    }

    @Override
    public String toString() {
        return title;
    }
}
