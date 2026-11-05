package by.bsuir.touragency.common.model;

import java.io.Serial;
import java.math.BigDecimal;
import java.time.LocalDate;

public class Discount extends Entity {

    @Serial
    private static final long serialVersionUID = 1L;

    private String nameRu;
    private String nameEn;
    private DiscountType type;
    private BigDecimal percent;
    private LocalDate validFrom;
    private LocalDate validTo;
    private Integer minDaysBefore;
    private Integer minCompletedTrips;
    private boolean active = true;
    private int usedCount;          // вычисляемое: сколько броней со скидкой

    public Discount() {
    }

    @Override
    public String getDisplayName() {
        return nameRu;
    }

    public String getNameRu() { return nameRu; }
    public void setNameRu(String nameRu) { this.nameRu = nameRu; }
    public String getNameEn() { return nameEn; }
    public void setNameEn(String nameEn) { this.nameEn = nameEn; }
    public String getName(Lang lang) { return lang == Lang.EN ? nameEn : nameRu; }
    public DiscountType getType() { return type; }
    public void setType(DiscountType type) { this.type = type; }
    public BigDecimal getPercent() { return percent; }
    public void setPercent(BigDecimal percent) { this.percent = percent; }
    public LocalDate getValidFrom() { return validFrom; }
    public void setValidFrom(LocalDate validFrom) { this.validFrom = validFrom; }
    public LocalDate getValidTo() { return validTo; }
    public void setValidTo(LocalDate validTo) { this.validTo = validTo; }
    public Integer getMinDaysBefore() { return minDaysBefore; }
    public void setMinDaysBefore(Integer minDaysBefore) { this.minDaysBefore = minDaysBefore; }
    public Integer getMinCompletedTrips() { return minCompletedTrips; }
    public void setMinCompletedTrips(Integer minCompletedTrips) { this.minCompletedTrips = minCompletedTrips; }
    private String promoCode;     // для PROMO_CODE
    private Integer maxUses;      // лимит использований промокода, null — без лимита

    public String getPromoCode() { return promoCode; }
    public void setPromoCode(String promoCode) { this.promoCode = promoCode; }
    public Integer getMaxUses() { return maxUses; }
    public void setMaxUses(Integer maxUses) { this.maxUses = maxUses; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public int getUsedCount() { return usedCount; }
    public void setUsedCount(int usedCount) { this.usedCount = usedCount; }
}
