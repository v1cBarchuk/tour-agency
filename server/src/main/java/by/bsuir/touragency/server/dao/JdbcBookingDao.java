package by.bsuir.touragency.server.dao;

import by.bsuir.touragency.common.model.Booking;
import by.bsuir.touragency.common.model.BookingStatus;
import by.bsuir.touragency.common.model.Lang;
import by.bsuir.touragency.common.model.StatusChange;
import by.bsuir.touragency.common.model.Tourist;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class JdbcBookingDao extends BaseDao implements BookingDao {

    private static final int CANCELLED = BookingStatus.CANCELLED.getId();
    private static final int COMPLETED = BookingStatus.COMPLETED.getId();

    @Override
    public int createLocked(Booking d, Integer discountId, List<Tourist> tourists, int actorId) {
        return inTransaction(c -> {
            int totalSeats;
            // 1. Блокируем строку тура: параллельная транзакция будет ждать здесь до COMMIT
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT total_seats FROM tours WHERE tour_id = ? FOR UPDATE")) {
                ps.setInt(1, d.getTourId());
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) throw new SeatsUnavailableException(0);
                    totalSeats = rs.getInt(1);
                }
            }
            // 2. Пересчитываем занятые места уже под блокировкой
            int booked;
            try (PreparedStatement ps = c.prepareStatement("SELECT COALESCE(SUM(persons_count), 0) FROM bookings "
                    + "WHERE tour_id = ? AND status_id <> ?")) {
                ps.setInt(1, d.getTourId());
                ps.setInt(2, CANCELLED);
                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    booked = rs.getInt(1);
                }
            }
            int free = totalSeats - booked;
            if (free < d.getPersonsCount()) {
                throw new SeatsUnavailableException(free);   // откат транзакции
            }
            // 3. Вставляем бронь, туристов и первую запись истории
            int id = insert(c, "INSERT INTO bookings (user_id, tour_id, status_id, discount_id, persons_count, "
                            + "base_price, discount_amount, total_price, contact_phone, contact_email) "
                            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    d.getUserId(), d.getTourId(), BookingStatus.NEW.getId(), discountId, d.getPersonsCount(),
                    d.getBasePrice(), d.getDiscountAmount(), d.getTotalPrice(), d.getContactPhone(),
                    d.getContactEmail());
            for (Tourist t : tourists) {
                update(c, "INSERT INTO booking_tourists (booking_id, tourist_no, full_name, birth_date) "
                        + "VALUES (?, ?, ?, ?)", id, t.number(), t.fullName(), t.birthDate());
            }
            update(c, "INSERT INTO booking_status_history (booking_id, status_id, changed_by) VALUES (?, ?, ?)",
                    id, BookingStatus.NEW.getId(), actorId);
            return id;
        });
    }

    private static String baseSql(Lang lang) {
        return "SELECT b.*, u.full_name client_name, u.login client_login, "
                + "COALESCE(t." + lang.col("title") + ", t.title_ru) tour_title, t.start_date, t.end_date, "
                + "h.name hotel_name, h.stars hotel_stars, "
                + "CONCAT(ci." + lang.col("name") + ", ', ', co." + lang.col("name") + ") location, "
                + "COALESCE(t.image_path, (SELECT p.image_path FROM hotel_photos p WHERE p.hotel_id = h.hotel_id "
                + "   ORDER BY p.photo_no LIMIT 1)) photo, "
                + "d." + lang.col("name") + " discount_name, d.percent discount_percent, "
                + "(SELECT COALESCE(SUM(pm.amount), 0) FROM payments pm WHERE pm.booking_id = b.booking_id) paid, "
                + "(SELECT r.rating FROM reviews r WHERE r.booking_id = b.booking_id) review_rating "
                + "FROM bookings b "
                + "JOIN users u ON u.user_id = b.user_id "
                + "JOIN tours t ON t.tour_id = b.tour_id "
                + "JOIN hotels h ON h.hotel_id = t.hotel_id "
                + "JOIN cities ci ON ci.city_id = h.city_id "
                + "JOIN countries co ON co.country_id = ci.country_id "
                + "LEFT JOIN discounts d ON d.discount_id = b.discount_id ";
    }

    @Override
    public List<Booking> find(BookingFilter f, Lang lang) {
        StringBuilder sql = new StringBuilder(baseSql(lang)).append("WHERE 1 = 1 ");
        List<Object> p = new ArrayList<>();
        if (f.userId() != null) {
            sql.append("AND b.user_id = ? ");
            p.add(f.userId());
        }
        if (f.status() != null) {
            sql.append("AND b.status_id = ? ");
            p.add(f.status().getId());
        }
        if (f.search() != null && !f.search().isBlank()) {
            sql.append("AND (u.full_name LIKE ? OR u.login LIKE ? OR h.name LIKE ? OR CAST(b.booking_id AS CHAR) = ?) ");
            String like = "%" + f.search().trim() + "%";
            p.add(like);
            p.add(like);
            p.add(like);
            p.add(f.search().trim().replace("#", ""));
        }
        sql.append(f.userId() != null ? "ORDER BY (b.status_id IN (6, 7)), t.start_date, b.booking_id" : "ORDER BY b.created_at DESC, b.booking_id DESC");
        return query(sql.toString(), JdbcBookingDao::map, p.toArray());
    }

    @Override
    public Optional<Booking> findById(int id, Lang lang) {
        Optional<Booking> booking = query(baseSql(lang) + "WHERE b.booking_id = ?", JdbcBookingDao::map, id)
                .stream().findFirst();
        booking.ifPresent(b -> {
            b.setTourists(query("SELECT * FROM booking_tourists WHERE booking_id = ? ORDER BY tourist_no",
                    rs -> new Tourist(rs.getInt("tourist_no"), rs.getString("full_name"),
                            getDate(rs, "birth_date")), id));
            b.setHistory(query("SELECT h.*, u.full_name FROM booking_status_history h "
                            + "LEFT JOIN users u ON u.user_id = h.changed_by WHERE h.booking_id = ? "
                            + "ORDER BY h.changed_at, h.history_id",
                    rs -> new StatusChange(BookingStatus.fromId(rs.getInt("status_id")),
                            getDateTime(rs, "changed_at"), rs.getString("full_name"), rs.getString("comment")), id));
        });
        return booking;
    }

    private static Booking map(ResultSet rs) throws SQLException {
        Booking b = new Booking();
        b.setId(rs.getInt("booking_id"));
        b.setUserId(rs.getInt("user_id"));
        b.setClientName(rs.getString("client_name"));
        b.setClientLogin(rs.getString("client_login"));
        b.setTourId(rs.getInt("tour_id"));
        b.setTourTitle(rs.getString("tour_title"));
        b.setHotelName(rs.getString("hotel_name"));
        b.setHotelStars(rs.getInt("hotel_stars"));
        b.setLocation(rs.getString("location"));
        b.setImagePath(rs.getString("photo"));
        b.setStartDate(getDate(rs, "start_date"));
        b.setEndDate(getDate(rs, "end_date"));
        b.setStatus(BookingStatus.fromId(rs.getInt("status_id")));
        b.setDiscountName(rs.getString("discount_name"));
        b.setDiscountPercent(rs.getBigDecimal("discount_percent"));
        b.setPersonsCount(rs.getInt("persons_count"));
        b.setBasePrice(rs.getBigDecimal("base_price"));
        b.setDiscountAmount(rs.getBigDecimal("discount_amount"));
        b.setTotalPrice(rs.getBigDecimal("total_price"));
        b.setPaidAmount(rs.getBigDecimal("paid"));
        b.setContactPhone(rs.getString("contact_phone"));
        b.setContactEmail(rs.getString("contact_email"));
        b.setCancelPenalty(rs.getBigDecimal("cancel_penalty"));
        b.setRefundAmount(rs.getBigDecimal("refund_amount"));
        b.setCreatedAt(getDateTime(rs, "created_at"));
        b.setReviewRating(getInteger(rs, "review_rating"));
        return b;
    }

    @Override
    public void changeStatus(int bookingId, BookingStatus status, Integer actorId, String comment) {
        inTransaction(c -> {
            update(c, "UPDATE bookings SET status_id = ? WHERE booking_id = ?", status.getId(), bookingId);
            update(c, "INSERT INTO booking_status_history (booking_id, status_id, changed_by, comment) "
                    + "VALUES (?, ?, ?, ?)", bookingId, status.getId(), actorId, comment);
            return null;
        });
    }

    @Override
    public void pay(int bookingId, BigDecimal amount, String method, int actorId) {
        inTransaction(c -> {
            update(c, "INSERT INTO payments (booking_id, amount, method) VALUES (?, ?, ?)", bookingId, amount, method);
            update(c, "UPDATE bookings SET status_id = ? WHERE booking_id = ?", BookingStatus.PAID.getId(), bookingId);
            update(c, "INSERT INTO booking_status_history (booking_id, status_id, changed_by) VALUES (?, ?, ?)",
                    bookingId, BookingStatus.PAID.getId(), actorId);
            return null;
        });
    }

    @Override
    public void cancel(int bookingId, BigDecimal penalty, BigDecimal refund, int actorId, String comment) {
        inTransaction(c -> {
            update(c, "UPDATE bookings SET status_id = ?, cancel_penalty = ?, refund_amount = ? WHERE booking_id = ?",
                    CANCELLED, penalty, refund, bookingId);
            update(c, "INSERT INTO booking_status_history (booking_id, status_id, changed_by, comment) "
                    + "VALUES (?, ?, ?, ?)", bookingId, CANCELLED, actorId, comment);
            return null;
        });
    }

    @Override
    public int countCompletedTrips(int userId) {
        return query("SELECT COUNT(*) FROM bookings WHERE user_id = ? AND status_id = ?",
                rs -> rs.getInt(1), userId, COMPLETED).get(0);
    }

    @Override
    public List<TripRecord> findTrips(int userId, Lang lang) {
        return query("SELECT co.country_id, co." + lang.col("name") + " country_name, t.vacation_type_id, r.rating "
                        + "FROM bookings b JOIN tours t ON t.tour_id = b.tour_id "
                        + "JOIN hotels h ON h.hotel_id = t.hotel_id JOIN cities ci ON ci.city_id = h.city_id "
                        + "JOIN countries co ON co.country_id = ci.country_id "
                        + "LEFT JOIN reviews r ON r.booking_id = b.booking_id "
                        + "WHERE b.user_id = ? AND b.status_id = ? ORDER BY r.rating DESC, t.end_date DESC",
                rs -> new TripRecord(rs.getInt("country_id"), rs.getString("country_name"),
                        rs.getInt("vacation_type_id"), getInteger(rs, "rating")), userId, COMPLETED);
    }
}
