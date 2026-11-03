package by.bsuir.touragency.common.model;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Параметры поиска туров. Незаполненное поле (null) не участвует в фильтрации.
 */
public class TourFilter implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    public enum Sort { DATE, MATCH, PRICE_ASC, PRICE_DESC, RATING, POPULARITY, NEWEST }

    private Integer countryId;
    private Integer cityId;
    private Integer hotelId;
    private BigDecimal maxPrice;
    private LocalDate dateFrom;
    private LocalDate dateTo;
    private Integer minNights;
    private Integer maxNights;
    private Integer minStars;
    private Integer vacationTypeId;
    private Integer mealTypeId;
    private Integer persons;          // нужно столько свободных мест
    private String search;            // по названию тура/отеля (для менеджера)
    private boolean onlyAvailable = true;   // активные, будущие, есть места
    private Sort sort = Sort.DATE;

    public Integer getCountryId() { return countryId; }
    public TourFilter setCountryId(Integer v) { countryId = v; return this; }
    public Integer getCityId() { return cityId; }
    public TourFilter setCityId(Integer v) { cityId = v; return this; }
    public Integer getHotelId() { return hotelId; }
    public TourFilter setHotelId(Integer v) { hotelId = v; return this; }
    public BigDecimal getMaxPrice() { return maxPrice; }
    public TourFilter setMaxPrice(BigDecimal v) { maxPrice = v; return this; }
    public LocalDate getDateFrom() { return dateFrom; }
    public TourFilter setDateFrom(LocalDate v) { dateFrom = v; return this; }
    public LocalDate getDateTo() { return dateTo; }
    public TourFilter setDateTo(LocalDate v) { dateTo = v; return this; }
    public Integer getMinNights() { return minNights; }
    public TourFilter setMinNights(Integer v) { minNights = v; return this; }
    public Integer getMaxNights() { return maxNights; }
    public TourFilter setMaxNights(Integer v) { maxNights = v; return this; }
    public Integer getMinStars() { return minStars; }
    public TourFilter setMinStars(Integer v) { minStars = v; return this; }
    public Integer getVacationTypeId() { return vacationTypeId; }
    public TourFilter setVacationTypeId(Integer v) { vacationTypeId = v; return this; }
    public Integer getMealTypeId() { return mealTypeId; }
    public TourFilter setMealTypeId(Integer v) { mealTypeId = v; return this; }
    public Integer getPersons() { return persons; }
    public TourFilter setPersons(Integer v) { persons = v; return this; }
    public String getSearch() { return search; }
    public TourFilter setSearch(String v) { search = v; return this; }
    public boolean isOnlyAvailable() { return onlyAvailable; }
    public TourFilter setOnlyAvailable(boolean v) { onlyAvailable = v; return this; }
    public Sort getSort() { return sort; }
    public TourFilter setSort(Sort v) { sort = v == null ? Sort.DATE : v; return this; }
}
