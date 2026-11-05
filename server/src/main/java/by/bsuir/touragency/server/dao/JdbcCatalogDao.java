package by.bsuir.touragency.server.dao;

import by.bsuir.touragency.common.model.City;
import by.bsuir.touragency.common.model.Country;
import by.bsuir.touragency.common.model.DictionaryItem;
import by.bsuir.touragency.common.model.Hotel;
import by.bsuir.touragency.common.model.Lang;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Имена столбцов с языковым суффиксом подставляются из enum Lang
 * (не из пользовательского ввода), поэтому SQL-инъекция невозможна.
 */
public class JdbcCatalogDao extends BaseDao implements CatalogDao {

    /** Будущие активные туры, не считая отменённые брони. */
    private static final String ACTIVE_FUTURE_TOUR = "t.is_active = TRUE AND t.start_date > CURRENT_DATE";

    @Override
    public List<DictionaryItem> findVacationTypes(Lang lang) {
        return dictionary("vacation_types", "vacation_type_id", lang, "vacation_type_id");
    }

    @Override
    public List<DictionaryItem> findMealTypes(Lang lang) {
        return dictionary("meal_types", "meal_type_id", lang, "sort_order");
    }

    @Override
    public List<DictionaryItem> findAmenities(Lang lang) {
        return dictionary("amenities", "amenity_id", lang, lang.col("name"));
    }

    private List<DictionaryItem> dictionary(String table, String idColumn, Lang lang, String orderBy) {
        String sql = "SELECT " + idColumn + " id, code, " + lang.col("name") + " name FROM " + table
                + " ORDER BY " + orderBy;
        return query(sql, rs -> new DictionaryItem(rs.getInt("id"), rs.getString("code"), rs.getString("name")));
    }

    @Override
    public List<Country> findCountries(Lang lang) {
        return query(countrySql(lang) + " GROUP BY co.country_id ORDER BY tours_count DESC, name",
                rs -> mapCountry(rs, lang));
    }

    @Override
    public Optional<Country> findCountry(int id, Lang lang) {
        return query(countrySql(lang) + " WHERE co.country_id = ? GROUP BY co.country_id",
                rs -> mapCountry(rs, lang), id).stream().findFirst();
    }

    private static String countrySql(Lang lang) {
        return "SELECT co.*, co." + lang.col("name") + " name, "
                + "COALESCE(co." + lang.col("description") + ", co.description_ru) description, "
                + "(SELECT COUNT(*) FROM cities ci2 WHERE ci2.country_id = co.country_id) cities_count, "
                + "COUNT(t.tour_id) tours_count, MIN(t.price_per_person) min_price "
                + "FROM countries co "
                + "LEFT JOIN cities ci ON ci.country_id = co.country_id "
                + "LEFT JOIN hotels h ON h.city_id = ci.city_id "
                + "LEFT JOIN tours t ON t.hotel_id = h.hotel_id AND " + ACTIVE_FUTURE_TOUR;
    }

    private static Country mapCountry(ResultSet rs, Lang lang) throws SQLException {
        Country c = new Country(rs.getInt("country_id"), rs.getString("name_ru"), rs.getString("name_en"));
        c.setName(rs.getString("name"));
        c.setDescription(rs.getString("description"));
        c.setDescriptionRu(rs.getString("description_ru"));
        c.setDescriptionEn(rs.getString("description_en"));
        c.setImagePath(rs.getString("image_path"));
        c.setCitiesCount(rs.getInt("cities_count"));
        c.setToursCount(rs.getInt("tours_count"));
        Integer min = getInteger(rs, "min_price");
        c.setMinPrice(min);
        return c;
    }

    @Override
    public List<City> findCities(Lang lang) {
        String sql = "SELECT ci.*, ci." + lang.col("name") + " name, co." + lang.col("name") + " country_name, "
                + "(SELECT COUNT(*) FROM hotels h WHERE h.city_id = ci.city_id) hotels_count "
                + "FROM cities ci JOIN countries co ON co.country_id = ci.country_id "
                + "ORDER BY country_name, name";
        return query(sql, rs -> {
            City c = new City(rs.getInt("city_id"), rs.getInt("country_id"),
                    rs.getString("name_ru"), rs.getString("name_en"));
            c.setName(rs.getString("name"));
            c.setCountryName(rs.getString("country_name"));
            c.setHotelsCount(rs.getInt("hotels_count"));
            return c;
        });
    }

    @Override
    public int saveCountry(Country c) {
        if (c.getId() == null) {
            return insert("INSERT INTO countries (name_ru, name_en, description_ru, description_en, image_path) "
                    + "VALUES (?, ?, ?, ?, ?)", c.getNameRu(), c.getNameEn(), c.getDescriptionRu(),
                    c.getDescriptionEn(), c.getImagePath());
        }
        update("UPDATE countries SET name_ru = ?, name_en = ?, description_ru = ?, description_en = ?, "
                + "image_path = ? WHERE country_id = ?", c.getNameRu(), c.getNameEn(), c.getDescriptionRu(),
                c.getDescriptionEn(), c.getImagePath(), c.getId());
        return c.getId();
    }

    @Override
    public int saveCity(City c) {
        if (c.getId() == null) {
            return insert("INSERT INTO cities (country_id, name_ru, name_en) VALUES (?, ?, ?)",
                    c.getCountryId(), c.getNameRu(), c.getNameEn());
        }
        update("UPDATE cities SET country_id = ?, name_ru = ?, name_en = ? WHERE city_id = ?",
                c.getCountryId(), c.getNameRu(), c.getNameEn(), c.getId());
        return c.getId();
    }

    // ------------------------------------------------------------- hotels

    private static String hotelSql(Lang lang) {
        return "SELECT h.*, ci." + lang.col("name") + " city_name, co.country_id, co." + lang.col("name")
                + " country_name, COALESCE(h." + lang.col("description") + ", h.description_ru) description, "
                + "(SELECT image_path FROM hotel_photos p WHERE p.hotel_id = h.hotel_id ORDER BY photo_no LIMIT 1) photo, "
                + "(SELECT ROUND(AVG(r.rating), 1) FROM reviews r JOIN bookings b ON b.booking_id = r.booking_id "
                + "   JOIN tours tr ON tr.tour_id = b.tour_id WHERE tr.hotel_id = h.hotel_id) rating, "
                + "(SELECT COUNT(*) FROM reviews r JOIN bookings b ON b.booking_id = r.booking_id "
                + "   JOIN tours tr ON tr.tour_id = b.tour_id WHERE tr.hotel_id = h.hotel_id) reviews_count, "
                + "(SELECT MIN(t.price_per_person) FROM tours t WHERE t.hotel_id = h.hotel_id AND "
                + ACTIVE_FUTURE_TOUR + ") min_price, "
                + "(SELECT GROUP_CONCAT(a.amenity_id ORDER BY a.amenity_id) FROM hotel_amenities a "
                + "   WHERE a.hotel_id = h.hotel_id) amenity_ids, "
                + "(SELECT GROUP_CONCAT(am." + lang.col("name") + " ORDER BY am.amenity_id SEPARATOR '|') "
                + "   FROM hotel_amenities a JOIN amenities am ON am.amenity_id = a.amenity_id "
                + "   WHERE a.hotel_id = h.hotel_id) amenity_names "
                + "FROM hotels h JOIN cities ci ON ci.city_id = h.city_id "
                + "JOIN countries co ON co.country_id = ci.country_id ";
    }

    @Override
    public List<Hotel> findHotels(Integer countryId, Integer cityId, Lang lang) {
        StringBuilder sql = new StringBuilder(hotelSql(lang)).append("WHERE 1 = 1 ");
        List<Object> params = new ArrayList<>();
        if (countryId != null) {
            sql.append("AND co.country_id = ? ");
            params.add(countryId);
        }
        if (cityId != null) {
            sql.append("AND ci.city_id = ? ");
            params.add(cityId);
        }
        sql.append("ORDER BY country_name, city_name, h.stars DESC, h.name");
        return query(sql.toString(), JdbcCatalogDao::mapHotel, params.toArray());
    }

    @Override
    public Optional<Hotel> findHotel(int id, Lang lang) {
        Optional<Hotel> hotel = query(hotelSql(lang) + "WHERE h.hotel_id = ?", JdbcCatalogDao::mapHotel, id)
                .stream().findFirst();
        hotel.ifPresent(h -> h.setPhotos(query("SELECT image_path FROM hotel_photos WHERE hotel_id = ? ORDER BY photo_no",
                rs -> rs.getString(1), id)));
        return hotel;
    }

    private static Hotel mapHotel(ResultSet rs) throws SQLException {
        Hotel h = new Hotel();
        h.setId(rs.getInt("hotel_id"));
        h.setCityId(rs.getInt("city_id"));
        h.setCityName(rs.getString("city_name"));
        h.setCountryId(rs.getInt("country_id"));
        h.setCountryName(rs.getString("country_name"));
        h.setName(rs.getString("name"));
        h.setStars(rs.getInt("stars"));
        h.setAddress(rs.getString("address"));
        h.setDescription(rs.getString("description"));
        h.setDescriptionRu(rs.getString("description_ru"));
        h.setDescriptionEn(rs.getString("description_en"));
        h.setMainPhoto(rs.getString("photo"));
        h.setRating(getDouble(rs, "rating"));
        h.setReviewsCount(rs.getInt("reviews_count"));
        h.setMinPrice(getInteger(rs, "min_price"));
        String ids = rs.getString("amenity_ids");
        if (ids != null) {
            h.setAmenityIds(Arrays.stream(ids.split(",")).map(Integer::valueOf).toList());
        }
        String names = rs.getString("amenity_names");
        if (names != null) {
            h.setAmenityNames(List.of(names.split("\\|")));
        }
        return h;
    }

    /** Отель и его удобства сохраняются в одной транзакции. */
    @Override
    public int saveHotel(Hotel h) {
        return inTransaction(c -> {
            int id;
            if (h.getId() == null) {
                id = insert(c, "INSERT INTO hotels (city_id, name, stars, address, description_ru, description_en) "
                        + "VALUES (?, ?, ?, ?, ?, ?)", h.getCityId(), h.getName(), h.getStars(), h.getAddress(),
                        h.getDescriptionRu(), h.getDescriptionEn());
            } else {
                id = h.getId();
                update(c, "UPDATE hotels SET city_id = ?, name = ?, stars = ?, address = ?, description_ru = ?, "
                        + "description_en = ? WHERE hotel_id = ?", h.getCityId(), h.getName(), h.getStars(),
                        h.getAddress(), h.getDescriptionRu(), h.getDescriptionEn(), id);
                update(c, "DELETE FROM hotel_amenities WHERE hotel_id = ?", id);
            }
            for (Integer amenityId : h.getAmenityIds()) {
                update(c, "INSERT INTO hotel_amenities (hotel_id, amenity_id) VALUES (?, ?)", id, amenityId);
            }
            return id;
        });
    }
}
