package by.bsuir.touragency.server.dao;

import by.bsuir.touragency.common.model.Lang;
import by.bsuir.touragency.common.model.NamedValue;
import by.bsuir.touragency.common.model.Stats;

import java.time.LocalDate;
import java.util.List;

/**
 * Агрегирующие запросы для панели управления и аналитики.
 * Выручка — сумма оплаченных и завершённых бронирований (статусы 5 и 6).
 */
public class JdbcStatsDao extends BaseDao implements StatsDao {

    private static final String PAID = "b.status_id IN (5, 6)";
    private static final String NOT_CANCELLED = "b.status_id <> 7";
    private static final String JOIN_GEO = "JOIN tours t ON t.tour_id = b.tour_id "
            + "JOIN hotels h ON h.hotel_id = t.hotel_id JOIN cities ci ON ci.city_id = h.city_id "
            + "JOIN countries co ON co.country_id = ci.country_id ";

    private double scalar(String sql, Object... params) {
        Double v = query(sql, rs -> {
            double d = rs.getDouble(1);
            return rs.wasNull() ? 0.0 : d;
        }, params).get(0);
        return v == null ? 0 : v;
    }

    private List<NamedValue> series(String sql, Object... params) {
        return query(sql, rs -> new NamedValue(rs.getString(1), rs.getDouble(2),
                rs.getMetaData().getColumnCount() > 2 ? rs.getDouble(3) : 0), params);
    }

    @Override
    public Stats dashboard(Lang lang) {
        Stats s = new Stats();
        s.putKpi("tours", scalar("SELECT COUNT(*) FROM tours"));
        s.putKpi("activeTours", scalar("SELECT COUNT(*) FROM tours WHERE is_active = TRUE AND start_date > CURRENT_DATE"));
        s.putKpi("bookings", scalar("SELECT COUNT(*) FROM bookings b WHERE " + NOT_CANCELLED));
        s.putKpi("clients", scalar("SELECT COUNT(*) FROM users WHERE role_id = 3"));
        s.putKpi("revenue", scalar("SELECT COALESCE(SUM(total_price), 0) FROM bookings b WHERE " + PAID));
        s.putKpi("freeSeats", scalar("SELECT COALESCE(SUM(t.total_seats - COALESCE((SELECT SUM(b.persons_count) "
                + "FROM bookings b WHERE b.tour_id = t.tour_id AND " + NOT_CANCELLED + "), 0)), 0) FROM tours t "
                + "WHERE t.is_active = TRUE AND t.start_date > CURRENT_DATE"));
        s.putKpi("avgRating", scalar("SELECT COALESCE(AVG(rating), 0) FROM reviews"));
        s.putKpi("newRequests", scalar("SELECT COUNT(*) FROM bookings WHERE status_id IN (1, 2)"));

        String month = "DATE_FORMAT(b.created_at, '%Y-%m')";
        s.putSeries("revenueByMonth", series("SELECT " + month + " m, SUM(b.total_price) FROM bookings b WHERE "
                + PAID + " GROUP BY m ORDER BY m"));
        s.putSeries("bookingsByMonth", series("SELECT " + month + " m, COUNT(*) FROM bookings b WHERE "
                + NOT_CANCELLED + " GROUP BY m ORDER BY m"));
        String country = "co." + lang.col("name");
        s.putSeries("bookingsByCountry", series("SELECT " + country + " c, COUNT(*) n FROM bookings b " + JOIN_GEO
                + "WHERE " + NOT_CANCELLED + " GROUP BY c ORDER BY n DESC"));
        s.putSeries("revenueByCountry", series("SELECT " + country + " c, SUM(b.total_price) v FROM bookings b "
                + JOIN_GEO + "WHERE " + PAID + " GROUP BY c ORDER BY v DESC"));
        s.putSeries("toursByStars", series("SELECT CONCAT(h.stars, '★') s, COUNT(*) FROM tours t "
                + "JOIN hotels h ON h.hotel_id = t.hotel_id GROUP BY h.stars ORDER BY h.stars"));
        return s;
    }

    @Override
    public Stats analytics(LocalDate from, LocalDate to, Lang lang) {
        Stats s = new Stats();
        s.setFrom(from);
        s.setTo(to);
        String period = "DATE(b.created_at) BETWEEN ? AND ? ";
        s.putKpi("bookings", scalar("SELECT COUNT(*) FROM bookings b WHERE " + period + "AND " + NOT_CANCELLED, from, to));
        s.putKpi("revenue", scalar("SELECT COALESCE(SUM(b.total_price), 0) FROM bookings b WHERE " + period
                + "AND " + PAID, from, to));
        s.putKpi("avgCheck", scalar("SELECT COALESCE(AVG(b.total_price), 0) FROM bookings b WHERE " + period
                + "AND " + PAID, from, to));
        s.putKpi("cancellations", scalar("SELECT COUNT(*) FROM bookings b WHERE " + period + "AND b.status_id = 7",
                from, to));
        s.putKpi("penalties", scalar("SELECT COALESCE(SUM(b.cancel_penalty), 0) FROM bookings b WHERE " + period
                + "AND b.status_id = 7", from, to));
        s.putKpi("discounts", scalar("SELECT COALESCE(SUM(b.discount_amount), 0) FROM bookings b WHERE " + period
                + "AND " + NOT_CANCELLED, from, to));
        s.putKpi("tourists", scalar("SELECT COALESCE(SUM(b.persons_count), 0) FROM bookings b WHERE " + period
                + "AND " + NOT_CANCELLED, from, to));

        String country = "co." + lang.col("name");
        String city = "CONCAT(ci." + lang.col("name") + ", ', ', " + country + ")";
        s.putSeries("revenueByMonth", series("SELECT DATE_FORMAT(b.created_at, '%Y-%m') m, SUM(b.total_price), "
                + "COUNT(*) FROM bookings b WHERE " + period + "AND " + PAID + " GROUP BY m ORDER BY m", from, to));
        s.putSeries("bookingsByMonth", series("SELECT DATE_FORMAT(b.created_at, '%Y-%m') m, COUNT(*) FROM bookings b "
                + "WHERE " + period + "AND " + NOT_CANCELLED + " GROUP BY m ORDER BY m", from, to));
        s.putSeries("salesByCountry", series("SELECT " + country + " c, COALESCE(SUM(CASE WHEN " + PAID
                + " THEN b.total_price END), 0) v, COUNT(*) FROM bookings b " + JOIN_GEO + "WHERE " + period
                + "AND " + NOT_CANCELLED + " GROUP BY c ORDER BY v DESC", from, to));
        s.putSeries("salesByDestination", series("SELECT " + city + " c, COUNT(*) n, COALESCE(SUM(CASE WHEN " + PAID
                + " THEN b.total_price END), 0) FROM bookings b " + JOIN_GEO + "WHERE " + period + "AND "
                + NOT_CANCELLED + " GROUP BY c ORDER BY n DESC", from, to));
        s.putSeries("popularHotels", series("SELECT h.name, COUNT(*) n, COALESCE(SUM(CASE WHEN " + PAID
                + " THEN b.total_price END), 0) FROM bookings b JOIN tours t ON t.tour_id = b.tour_id "
                + "JOIN hotels h ON h.hotel_id = t.hotel_id WHERE " + period + "AND " + NOT_CANCELLED
                + " GROUP BY h.hotel_id, h.name ORDER BY n DESC LIMIT 10", from, to));
        s.putSeries("destinationRatings", series("SELECT " + country + " c, AVG(r.rating), COUNT(*) FROM reviews r "
                + "JOIN bookings b ON b.booking_id = r.booking_id " + JOIN_GEO + "GROUP BY c ORDER BY AVG(r.rating) DESC"));
        s.putSeries("bookingsByStatus", series("SELECT s.code, COUNT(*) FROM bookings b JOIN booking_statuses s "
                + "ON s.status_id = b.status_id WHERE " + period + "GROUP BY s.status_id, s.code ORDER BY s.status_id",
                from, to));
        s.putSeries("discountsByName", series("SELECT d." + lang.col("name") + ", SUM(b.discount_amount), COUNT(*) "
                + "FROM bookings b JOIN discounts d ON d.discount_id = b.discount_id WHERE " + period + "AND "
                + NOT_CANCELLED + " GROUP BY d.discount_id, d." + lang.col("name") + " ORDER BY 2 DESC", from, to));
        return s;
    }
}
