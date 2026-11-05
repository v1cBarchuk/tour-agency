package by.bsuir.touragency.server.dao;

import by.bsuir.touragency.common.model.BookingStatus;
import by.bsuir.touragency.common.model.Lang;
import by.bsuir.touragency.common.model.Tour;
import by.bsuir.touragency.common.model.TourFilter;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class JdbcTourDao extends BaseDao implements TourDao {

    private static final int CANCELLED = BookingStatus.CANCELLED.getId();

    private final SettingsDao settings;

    public JdbcTourDao(SettingsDao settings) {
        this.settings = settings;
    }

    /**
     * Свободные места, рейтинг, число броней и главное фото вычисляются подзапросами —
     * в таблице tours эти значения не хранятся (3НФ).
     */
    private static String baseSql(Lang lang) {
        return "SELECT * FROM (SELECT t.*, "
                + "COALESCE(t." + lang.col("title") + ", t.title_ru) title, "
                + "COALESCE(t." + lang.col("description") + ", t.description_ru) description, "
                + "h.name hotel_name, h.stars hotel_stars, ci.city_id, ci." + lang.col("name") + " city_name, "
                + "ci.latitude, ci.longitude, "
                + "(SELECT ph.old_price FROM tour_price_history ph WHERE ph.tour_id = t.tour_id "
                + "   AND ph.changed_at >= NOW() - INTERVAL 30 DAY ORDER BY ph.changed_at DESC, ph.history_id DESC "
                + "   LIMIT 1) old_price, "
                + "co.country_id, co." + lang.col("name") + " country_name, co.visa_free, "
                + "vt." + lang.col("name") + " vacation_type_name, mt." + lang.col("name") + " meal_type_name, "
                + "DATEDIFF(t.end_date, t.start_date) nights, "
                + "t.total_seats - COALESCE((SELECT SUM(b.persons_count) FROM bookings b "
                + "   WHERE b.tour_id = t.tour_id AND b.status_id <> " + CANCELLED + "), 0) free_seats, "
                + "(SELECT COUNT(*) FROM bookings b WHERE b.tour_id = t.tour_id AND b.status_id <> "
                + CANCELLED + ") bookings_count, "
                + "(SELECT ROUND(AVG(r.rating), 1) FROM reviews r JOIN bookings b ON b.booking_id = r.booking_id "
                + "   JOIN tours tr ON tr.tour_id = b.tour_id WHERE tr.hotel_id = t.hotel_id) rating, "
                + "(SELECT COUNT(*) FROM reviews r JOIN bookings b ON b.booking_id = r.booking_id "
                + "   JOIN tours tr ON tr.tour_id = b.tour_id WHERE tr.hotel_id = t.hotel_id) reviews_count, "
                + "COALESCE(t.image_path, (SELECT p.image_path FROM hotel_photos p WHERE p.hotel_id = t.hotel_id "
                + "   ORDER BY p.photo_no LIMIT 1)) photo "
                + "FROM tours t "
                + "JOIN hotels h ON h.hotel_id = t.hotel_id "
                + "JOIN cities ci ON ci.city_id = h.city_id "
                + "JOIN countries co ON co.country_id = ci.country_id "
                + "JOIN vacation_types vt ON vt.vacation_type_id = t.vacation_type_id "
                + "JOIN meal_types mt ON mt.meal_type_id = t.meal_type_id) x ";
    }

    @Override
    public List<Tour> find(TourFilter f, Lang lang) {
        StringBuilder sql = new StringBuilder(baseSql(lang)).append("WHERE 1 = 1 ");
        List<Object> p = new ArrayList<>();
        if (f.isOnlyAvailable()) {
            sql.append("AND x.is_active = TRUE AND x.start_date > CURRENT_DATE AND x.free_seats > 0 ");
        }
        addEq(sql, p, "x.country_id", f.getCountryId());
        addEq(sql, p, "x.city_id", f.getCityId());
        addEq(sql, p, "x.hotel_id", f.getHotelId());
        addEq(sql, p, "x.vacation_type_id", f.getVacationTypeId());
        addEq(sql, p, "x.meal_type_id", f.getMealTypeId());
        if (f.getMaxPrice() != null) {
            sql.append("AND x.price_per_person <= ? ");
            p.add(f.getMaxPrice());
        }
        if (f.getDateFrom() != null) {
            sql.append("AND x.start_date >= ? ");
            p.add(f.getDateFrom());
        }
        if (f.getDateTo() != null) {
            sql.append("AND x.start_date <= ? ");
            p.add(f.getDateTo());
        }
        if (f.getMinNights() != null) {
            sql.append("AND x.nights >= ? ");
            p.add(f.getMinNights());
        }
        if (f.getMaxNights() != null) {
            sql.append("AND x.nights <= ? ");
            p.add(f.getMaxNights());
        }
        if (f.getMinStars() != null) {
            sql.append("AND x.hotel_stars >= ? ");
            p.add(f.getMinStars());
        }
        if (f.isOnlyHot()) {
            sql.append("AND x.start_date <= CURRENT_DATE + INTERVAL ? DAY ");
            p.add(settings.getInt("tour.hot.days", 14));
        }
        if (f.isOnlyVisaFree()) {
            sql.append("AND x.visa_free = TRUE ");
        }
        if (f.getPersons() != null) {
            sql.append("AND x.free_seats >= ? ");
            p.add(f.getPersons());
        }
        if (f.getSearch() != null && !f.getSearch().isBlank()) {
            sql.append("AND (x.title LIKE ? OR x.hotel_name LIKE ? OR x.city_name LIKE ? OR x.country_name LIKE ?) ");
            String like = "%" + f.getSearch().trim() + "%";
            p.add(like); p.add(like); p.add(like); p.add(like);
        }
        sql.append(switch (f.getSort()) {
            case PRICE_ASC -> "ORDER BY x.price_per_person, x.start_date";
            case PRICE_DESC -> "ORDER BY x.price_per_person DESC, x.start_date";
            case RATING -> "ORDER BY x.rating IS NULL, x.rating DESC, x.reviews_count DESC";
            case POPULARITY -> "ORDER BY x.bookings_count DESC, x.rating DESC";
            case NEWEST -> "ORDER BY x.created_at DESC";
            case DATE, MATCH -> "ORDER BY x.start_date, x.tour_id";
        });
        return query(sql.toString(), this::map, p.toArray());
    }

    private static void addEq(StringBuilder sql, List<Object> params, String column, Object value) {
        if (value != null) {
            sql.append("AND ").append(column).append(" = ? ");
            params.add(value);
        }
    }

    @Override
    public Optional<Tour> findById(int id, Lang lang) {
        return query(baseSql(lang) + "WHERE x.tour_id = ?", this::map, id).stream().findFirst();
    }

    private Tour map(ResultSet rs) throws SQLException {
        Tour t = new Tour();
        t.setId(rs.getInt("tour_id"));
        t.setHotelId(rs.getInt("hotel_id"));
        t.setVacationTypeId(rs.getInt("vacation_type_id"));
        t.setMealTypeId(rs.getInt("meal_type_id"));
        t.setTitleRu(rs.getString("title_ru"));
        t.setTitleEn(rs.getString("title_en"));
        t.setTitle(rs.getString("title"));
        t.setDescription(rs.getString("description"));
        t.setDescriptionRu(rs.getString("description_ru"));
        t.setDescriptionEn(rs.getString("description_en"));
        t.setStartDate(getDate(rs, "start_date"));
        t.setEndDate(getDate(rs, "end_date"));
        t.setPricePerPerson(getMoney(rs, "price_per_person"));
        t.setTotalSeats(rs.getInt("total_seats"));
        t.setImagePath(rs.getString("photo"));
        t.setFeatured(rs.getBoolean("is_featured"));
        t.setActive(rs.getBoolean("is_active"));
        t.setCreatedAt(getDateTime(rs, "created_at"));
        t.setHotelName(rs.getString("hotel_name"));
        t.setHotelStars(rs.getInt("hotel_stars"));
        t.setCityName(rs.getString("city_name"));
        t.setCountryId(rs.getInt("country_id"));
        t.setCountryName(rs.getString("country_name"));
        t.setVacationTypeName(rs.getString("vacation_type_name"));
        t.setMealTypeName(rs.getString("meal_type_name"));
        t.setFreeSeats(rs.getInt("free_seats"));
        t.setBookingsCount(rs.getInt("bookings_count"));
        t.setRating(getDouble(rs, "rating"));
        t.setReviewsCount(rs.getInt("reviews_count"));
        t.setCityId(rs.getInt("city_id"));
        t.setVisaFree(rs.getBoolean("visa_free"));
        t.setLatitude(getDouble(rs, "latitude"));
        t.setLongitude(getDouble(rs, "longitude"));
        java.math.BigDecimal old = getMoney(rs, "old_price");
        if (old != null && old.compareTo(t.getPricePerPerson()) > 0) t.setOldPrice(old);   // только снижение

        // метки карточки
        LocalDate today = LocalDate.now();
        t.setHot(t.isActive() && t.getFreeSeats() > 0 && t.getStartDate().isAfter(today)
                && !t.getStartDate().isAfter(today.plusDays(settings.getInt("tour.hot.days", 14))));
        t.setNewTour(t.getCreatedAt() != null
                && t.getCreatedAt().isAfter(LocalDateTime.now().minusDays(settings.getInt("tour.new.days", 10))));
        t.setPopular(t.getBookingsCount() >= settings.getInt("tour.popular.bookings", 2));
        t.setAlmostSoldOut(t.getFreeSeats() > 0
                && t.getOccupancyPercent() > settings.getInt("tour.almostSold.percent", 80));
        return t;
    }

    @Override
    public int create(Tour t, int managerId) {
        return insert("INSERT INTO tours (hotel_id, vacation_type_id, meal_type_id, created_by, title_ru, title_en, "
                        + "description_ru, description_en, start_date, end_date, price_per_person, total_seats, "
                        + "image_path, is_featured, is_active) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                t.getHotelId(), t.getVacationTypeId(), t.getMealTypeId(), managerId, t.getTitleRu(), t.getTitleEn(),
                t.getDescriptionRu(), t.getDescriptionEn(), t.getStartDate(), t.getEndDate(), t.getPricePerPerson(),
                t.getTotalSeats(), t.getImagePath(), t.isFeatured(), t.isActive());
    }

    @Override
    public void update(Tour t) {
        update("UPDATE tours SET hotel_id = ?, vacation_type_id = ?, meal_type_id = ?, title_ru = ?, title_en = ?, "
                        + "description_ru = ?, description_en = ?, start_date = ?, end_date = ?, price_per_person = ?, "
                        + "total_seats = ?, is_featured = ?, is_active = ? WHERE tour_id = ?",
                t.getHotelId(), t.getVacationTypeId(), t.getMealTypeId(), t.getTitleRu(), t.getTitleEn(),
                t.getDescriptionRu(), t.getDescriptionEn(), t.getStartDate(), t.getEndDate(), t.getPricePerPerson(),
                t.getTotalSeats(), t.isFeatured(), t.isActive(), t.getId());
    }

    @Override
    public void addPriceHistory(int tourId, java.math.BigDecimal oldPrice, java.math.BigDecimal newPrice, int changedBy) {
        update("INSERT INTO tour_price_history (tour_id, old_price, new_price, changed_by) VALUES (?, ?, ?, ?)",
                tourId, oldPrice, newPrice, changedBy);
    }

    @Override
    public void registerView(int userId, int tourId) {
        update("INSERT INTO tour_views (user_id, tour_id) VALUES (?, ?) "
                + "ON DUPLICATE KEY UPDATE viewed_at = CURRENT_TIMESTAMP, views_count = views_count + 1", userId, tourId);
    }

    @Override
    public List<Tour> findRecentlyViewed(int userId, int limit, Lang lang) {
        return query(baseSql(lang) + "JOIN tour_views v ON v.tour_id = x.tour_id "
                + "WHERE v.user_id = ? AND x.is_active = TRUE ORDER BY v.viewed_at DESC LIMIT ?", this::map, userId, limit);
    }

    @Override
    public List<Tour> findSimilar(Tour t, int limit, Lang lang) {
        return query(baseSql(lang) + "WHERE x.tour_id <> ? AND x.is_active = TRUE AND x.start_date > CURRENT_DATE "
                        + "AND x.free_seats > 0 AND (x.country_id = ? OR x.vacation_type_id = ?) "
                        + "ORDER BY (x.country_id = ?) DESC, ABS(x.price_per_person - ?), x.start_date LIMIT ?",
                this::map, t.getId(), t.getCountryId(), t.getVacationTypeId(), t.getCountryId(),
                t.getPricePerPerson(), limit);
    }

    @Override
    public void setActive(int tourId, boolean active) {
        update("UPDATE tours SET is_active = ? WHERE tour_id = ?", active, tourId);
    }

    @Override
    public void delete(int tourId) {
        update("DELETE FROM tours WHERE tour_id = ?", tourId);
    }

    @Override
    public int countBookings(int tourId) {
        return query("SELECT COUNT(*) n FROM bookings WHERE tour_id = ?", rs -> rs.getInt("n"), tourId).get(0);
    }

    @Override
    public int countBookedSeats(int tourId) {
        return query("SELECT COALESCE(SUM(persons_count), 0) n FROM bookings WHERE tour_id = ? AND status_id <> ?",
                rs -> rs.getInt("n"), tourId, CANCELLED).get(0);
    }
}
