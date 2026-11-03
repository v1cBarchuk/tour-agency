package by.bsuir.touragency.server.dao;

import by.bsuir.touragency.common.model.Preferences;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public class JdbcPreferenceDao extends BaseDao implements PreferenceDao {

    private static final String SELECT = "SELECT p.* FROM client_preferences p ";

    @Override
    public Optional<Preferences> find(int userId) {
        return query(SELECT + "WHERE p.user_id = ?", JdbcPreferenceDao::map, userId).stream().findFirst()
                .map(p -> withLists(userId, p));
    }

    @Override
    public Map<Integer, Preferences> findAllClients() {
        Map<Integer, Preferences> result = new LinkedHashMap<>();
        query(SELECT + "JOIN users u ON u.user_id = p.user_id WHERE u.is_blocked = FALSE",
                rs -> result.put(rs.getInt("user_id"), map(rs)));
        result.forEach(this::withLists);
        return result;
    }

    private Preferences withLists(int userId, Preferences p) {
        p.setCountryIds(query("SELECT country_id FROM preference_countries WHERE user_id = ?",
                rs -> rs.getInt(1), userId));
        p.setVacationTypeIds(query("SELECT vacation_type_id FROM preference_vacation_types WHERE user_id = ?",
                rs -> rs.getInt(1), userId));
        return p;
    }

    private static Preferences map(ResultSet rs) throws SQLException {
        Preferences p = new Preferences();
        p.setMinBudget(rs.getBigDecimal("min_budget"));
        p.setMaxBudget(rs.getBigDecimal("max_budget"));
        p.setDesiredFrom(getDate(rs, "desired_from"));
        p.setDesiredTo(getDate(rs, "desired_to"));
        p.setMinNights(getInteger(rs, "min_nights"));
        p.setMaxNights(getInteger(rs, "max_nights"));
        p.setTouristsCount(getInteger(rs, "tourists_count"));
        p.setMinStars(getInteger(rs, "min_hotel_stars"));
        p.setMealTypeId(getInteger(rs, "meal_type_id"));
        return p;
    }

    /** Анкета и её списки сохраняются в одной транзакции. */
    @Override
    public void save(int userId, Preferences p) {
        inTransaction(c -> {
            update(c, "INSERT INTO client_preferences (user_id, min_budget, max_budget, desired_from, desired_to, "
                            + "min_nights, max_nights, tourists_count, min_hotel_stars, meal_type_id) "
                            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE "
                            + "min_budget = VALUES(min_budget), max_budget = VALUES(max_budget), "
                            + "desired_from = VALUES(desired_from), desired_to = VALUES(desired_to), "
                            + "min_nights = VALUES(min_nights), max_nights = VALUES(max_nights), "
                            + "tourists_count = VALUES(tourists_count), min_hotel_stars = VALUES(min_hotel_stars), "
                            + "meal_type_id = VALUES(meal_type_id)",
                    userId, p.getMinBudget(), p.getMaxBudget(), p.getDesiredFrom(), p.getDesiredTo(),
                    p.getMinNights(), p.getMaxNights(), p.getTouristsCount(), p.getMinStars(), p.getMealTypeId());
            update(c, "DELETE FROM preference_countries WHERE user_id = ?", userId);
            for (Integer id : p.getCountryIds()) {
                update(c, "INSERT INTO preference_countries (user_id, country_id) VALUES (?, ?)", userId, id);
            }
            update(c, "DELETE FROM preference_vacation_types WHERE user_id = ?", userId);
            for (Integer id : p.getVacationTypeIds()) {
                update(c, "INSERT INTO preference_vacation_types (user_id, vacation_type_id) VALUES (?, ?)", userId, id);
            }
            return null;
        });
    }
}
