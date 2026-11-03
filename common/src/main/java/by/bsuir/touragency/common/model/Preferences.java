package by.bsuir.touragency.common.model;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Анкета предпочтений клиента. Любое поле может быть не заполнено —
 * такой критерий не участвует в оценке тура.
 */
public class Preferences implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private BigDecimal minBudget;
    private BigDecimal maxBudget;
    private LocalDate desiredFrom;
    private LocalDate desiredTo;
    private Integer minNights;
    private Integer maxNights;
    private Integer touristsCount;
    private Integer minStars;
    private Integer mealTypeId;
    private ArrayList<Integer> countryIds = new ArrayList<>();
    private ArrayList<Integer> vacationTypeIds = new ArrayList<>();

    /** Анкета ещё не заполнялась ни по одному критерию. */
    public boolean isEmpty() {
        return minBudget == null && maxBudget == null && desiredFrom == null && desiredTo == null
                && minNights == null && maxNights == null && minStars == null && mealTypeId == null
                && touristsCount == null && countryIds.isEmpty() && vacationTypeIds.isEmpty();
    }

    public BigDecimal getMinBudget() { return minBudget; }
    public void setMinBudget(BigDecimal v) { minBudget = v; }
    public BigDecimal getMaxBudget() { return maxBudget; }
    public void setMaxBudget(BigDecimal v) { maxBudget = v; }
    public LocalDate getDesiredFrom() { return desiredFrom; }
    public void setDesiredFrom(LocalDate v) { desiredFrom = v; }
    public LocalDate getDesiredTo() { return desiredTo; }
    public void setDesiredTo(LocalDate v) { desiredTo = v; }
    public Integer getMinNights() { return minNights; }
    public void setMinNights(Integer v) { minNights = v; }
    public Integer getMaxNights() { return maxNights; }
    public void setMaxNights(Integer v) { maxNights = v; }
    public Integer getTouristsCount() { return touristsCount; }
    public void setTouristsCount(Integer v) { touristsCount = v; }
    public Integer getMinStars() { return minStars; }
    public void setMinStars(Integer v) { minStars = v; }
    public Integer getMealTypeId() { return mealTypeId; }
    public void setMealTypeId(Integer v) { mealTypeId = v; }
    public List<Integer> getCountryIds() { return countryIds; }
    public void setCountryIds(List<Integer> v) { countryIds = new ArrayList<>(v); }
    public List<Integer> getVacationTypeIds() { return vacationTypeIds; }
    public void setVacationTypeIds(List<Integer> v) { vacationTypeIds = new ArrayList<>(v); }
}
