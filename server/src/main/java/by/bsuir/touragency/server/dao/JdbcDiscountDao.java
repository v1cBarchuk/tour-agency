package by.bsuir.touragency.server.dao;

import by.bsuir.touragency.common.model.Discount;
import by.bsuir.touragency.common.model.DiscountType;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public class JdbcDiscountDao extends BaseDao implements DiscountDao {

    private static final String SELECT = "SELECT d.*, (SELECT COUNT(*) FROM bookings b "
            + "WHERE b.discount_id = d.discount_id) used_count FROM discounts d ";

    @Override
    public List<Discount> findAll() {
        return query(SELECT + "ORDER BY d.is_active DESC, d.discount_type, d.percent DESC", JdbcDiscountDao::map);
    }

    @Override
    public List<Discount> findActive() {
        return query(SELECT + "WHERE d.is_active = TRUE", JdbcDiscountDao::map);
    }

    @Override
    public Optional<Discount> findById(int id) {
        return query(SELECT + "WHERE d.discount_id = ?", JdbcDiscountDao::map, id).stream().findFirst();
    }

    private static Discount map(ResultSet rs) throws SQLException {
        Discount d = new Discount();
        d.setId(rs.getInt("discount_id"));
        d.setNameRu(rs.getString("name_ru"));
        d.setNameEn(rs.getString("name_en"));
        d.setType(DiscountType.valueOf(rs.getString("discount_type")));
        d.setPercent(rs.getBigDecimal("percent"));
        d.setValidFrom(getDate(rs, "valid_from"));
        d.setValidTo(getDate(rs, "valid_to"));
        d.setMinDaysBefore(getInteger(rs, "min_days_before"));
        d.setMinCompletedTrips(getInteger(rs, "min_completed_trips"));
        d.setActive(rs.getBoolean("is_active"));
        d.setUsedCount(rs.getInt("used_count"));
        d.setPromoCode(rs.getString("promo_code"));
        d.setMaxUses(getInteger(rs, "max_uses"));
        return d;
    }

    @Override
    public int save(Discount d) {
        Object[] values = {d.getNameRu(), d.getNameEn(), d.getType(), d.getPercent(), d.getValidFrom(),
                d.getValidTo(), d.getMinDaysBefore(), d.getMinCompletedTrips(), d.getPromoCode(), d.getMaxUses(),
                d.isActive()};
        if (d.getId() == null) {
            return insert("INSERT INTO discounts (name_ru, name_en, discount_type, percent, valid_from, valid_to, "
                    + "min_days_before, min_completed_trips, promo_code, max_uses, is_active) "
                    + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)", values);
        }
        Object[] withId = java.util.Arrays.copyOf(values, values.length + 1);
        withId[values.length] = d.getId();
        update("UPDATE discounts SET name_ru = ?, name_en = ?, discount_type = ?, percent = ?, valid_from = ?, "
                + "valid_to = ?, min_days_before = ?, min_completed_trips = ?, promo_code = ?, max_uses = ?, "
                + "is_active = ? WHERE discount_id = ?",
                withId);
        return d.getId();
    }

    @Override
    public void setActive(int id, boolean active) {
        update("UPDATE discounts SET is_active = ? WHERE discount_id = ?", active, id);
    }
}
