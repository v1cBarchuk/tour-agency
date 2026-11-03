package by.bsuir.touragency.server.dao;

import by.bsuir.touragency.common.model.Review;

import java.util.List;

public interface ReviewDao {

    int add(int bookingId, int rating, String comment);

    List<Review> findByHotel(int hotelId);
}
