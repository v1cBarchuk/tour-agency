package by.bsuir.touragency.server.dao;

import by.bsuir.touragency.common.model.City;
import by.bsuir.touragency.common.model.Country;
import by.bsuir.touragency.common.model.DictionaryItem;
import by.bsuir.touragency.common.model.Hotel;
import by.bsuir.touragency.common.model.Lang;

import java.util.List;
import java.util.Optional;

/** Справочники, география и отели. */
public interface CatalogDao {

    List<DictionaryItem> findVacationTypes(Lang lang);

    List<DictionaryItem> findMealTypes(Lang lang);

    List<DictionaryItem> findAmenities(Lang lang);

    /** Страны со статистикой: число городов, будущих туров и минимальная цена. */
    List<Country> findCountries(Lang lang);

    Optional<Country> findCountry(int id, Lang lang);

    List<City> findCities(Lang lang);

    int saveCountry(Country country);

    int saveCity(City city);

    List<Hotel> findHotels(Integer countryId, Integer cityId, Lang lang);

    Optional<Hotel> findHotel(int id, Lang lang);

    int saveHotel(Hotel hotel);
}
