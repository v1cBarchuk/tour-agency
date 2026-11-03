package by.bsuir.touragency.client.util;

import by.bsuir.touragency.common.model.City;
import by.bsuir.touragency.common.model.Country;
import by.bsuir.touragency.common.model.DictionaryItem;
import by.bsuir.touragency.common.model.ReferenceData;
import by.bsuir.touragency.common.model.User;

import java.util.List;
import java.util.Optional;

/**
 * Данные текущего сеанса клиента: пользователь и справочники.
 */
public final class ClientContext {

    private static User user;
    private static ReferenceData reference;

    private ClientContext() {
    }

    public static User getUser() {
        return user;
    }

    public static void setUser(User u) {
        user = u;
    }

    public static ReferenceData ref() {
        return reference;
    }

    public static void setReference(ReferenceData data) {
        reference = data;
    }

    public static void clear() {
        user = null;
        reference = null;
    }

    public static List<Country> countries() {
        return reference == null ? List.of() : reference.getCountries();
    }

    public static List<City> cities() {
        return reference == null ? List.of() : reference.getCities();
    }

    public static List<DictionaryItem> vacationTypes() {
        return reference == null ? List.of() : reference.getVacationTypes();
    }

    public static List<DictionaryItem> mealTypes() {
        return reference == null ? List.of() : reference.getMealTypes();
    }

    public static List<DictionaryItem> amenities() {
        return reference == null ? List.of() : reference.getAmenities();
    }

    public static Optional<Country> country(Integer id) {
        return countries().stream().filter(c -> c.getId().equals(id)).findFirst();
    }
}
