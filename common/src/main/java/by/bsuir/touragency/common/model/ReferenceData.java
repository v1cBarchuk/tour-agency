package by.bsuir.touragency.common.model;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Справочники, которые клиент загружает один раз после входа
 * (и заново при смене языка).
 */
public class ReferenceData implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final ArrayList<Country> countries;
    private final ArrayList<City> cities;
    private final ArrayList<DictionaryItem> vacationTypes;
    private final ArrayList<DictionaryItem> mealTypes;
    private final ArrayList<DictionaryItem> amenities;

    public ReferenceData(List<Country> countries, List<City> cities, List<DictionaryItem> vacationTypes,
                         List<DictionaryItem> mealTypes, List<DictionaryItem> amenities) {
        this.countries = new ArrayList<>(countries);
        this.cities = new ArrayList<>(cities);
        this.vacationTypes = new ArrayList<>(vacationTypes);
        this.mealTypes = new ArrayList<>(mealTypes);
        this.amenities = new ArrayList<>(amenities);
    }

    public List<Country> getCountries() { return countries; }
    public List<City> getCities() { return cities; }
    public List<DictionaryItem> getVacationTypes() { return vacationTypes; }
    public List<DictionaryItem> getMealTypes() { return mealTypes; }
    public List<DictionaryItem> getAmenities() { return amenities; }

    public List<City> citiesOf(Integer countryId) {
        return cities.stream().filter(c -> c.getCountryId().equals(countryId)).toList();
    }
}
