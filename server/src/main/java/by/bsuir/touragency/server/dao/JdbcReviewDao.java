package by.bsuir.touragency.server.dao;

import by.bsuir.touragency.common.model.Review;

import java.util.List;

public class JdbcReviewDao extends BaseDao implements ReviewDao {

    @Override
    public int add(int bookingId, int rating, String comment) {
        return insert("INSERT INTO reviews (booking_id, rating, comment) VALUES (?, ?, ?)", bookingId, rating, comment);
    }

    @Override
    public List<Review> findByHotel(int hotelId) {
        return query("SELECT r.*, u.full_name, h.name hotel_name, t.title_ru FROM reviews r "
                        + "JOIN bookings b ON b.booking_id = r.booking_id JOIN users u ON u.user_id = b.user_id "
                        + "JOIN tours t ON t.tour_id = b.tour_id JOIN hotels h ON h.hotel_id = t.hotel_id "
                        + "WHERE t.hotel_id = ? ORDER BY r.created_at DESC",
                rs -> new Review(rs.getInt("review_id"), rs.getInt("booking_id"), rs.getInt("rating"),
                        rs.getString("comment"), getDateTime(rs, "created_at"), rs.getString("full_name"),
                        rs.getString("hotel_name"), rs.getString("title_ru")), hotelId);
    }
}
