package by.bsuir.touragency.common.model;

import java.io.Serial;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Бронирование тура. */
public class Booking extends Entity {

    @Serial
    private static final long serialVersionUID = 1L;

    private Integer userId;
    private String clientName;
    private String clientLogin;
    private Integer tourId;
    private String tourTitle;
    private String hotelName;
    private int hotelStars;
    private String location;
    private String imagePath;
    private LocalDate startDate;
    private LocalDate endDate;
    private BookingStatus status;
    private String discountName;
    private BigDecimal discountPercent;
    private int personsCount;
    private BigDecimal basePrice;
    private BigDecimal discountAmount;
    private BigDecimal totalPrice;
    private BigDecimal paidAmount = BigDecimal.ZERO;
    private String contactPhone;
    private String contactEmail;
    private BigDecimal cancelPenalty;
    private BigDecimal refundAmount;
    private LocalDateTime createdAt;
    private Integer reviewRating;            // null — отзыва нет
    private ArrayList<Tourist> tourists = new ArrayList<>();
    private ArrayList<StatusChange> history = new ArrayList<>();

    @Override
    public String getDisplayName() {
        return "#" + getId() + " " + tourTitle;
    }

    public long getNights() {
        return startDate == null || endDate == null ? 0 : java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate);
    }

    public boolean canReview() {
        return status == BookingStatus.COMPLETED && reviewRating == null;
    }

    public boolean canCancel() {
        return !status.isFinal();
    }

    public Integer getUserId() { return userId; }
    public void setUserId(Integer v) { userId = v; }
    public String getClientName() { return clientName; }
    public void setClientName(String v) { clientName = v; }
    public String getClientLogin() { return clientLogin; }
    public void setClientLogin(String v) { clientLogin = v; }
    public Integer getTourId() { return tourId; }
    public void setTourId(Integer v) { tourId = v; }
    public String getTourTitle() { return tourTitle; }
    public void setTourTitle(String v) { tourTitle = v; }
    public String getHotelName() { return hotelName; }
    public void setHotelName(String v) { hotelName = v; }
    public int getHotelStars() { return hotelStars; }
    public void setHotelStars(int v) { hotelStars = v; }
    public String getLocation() { return location; }
    public void setLocation(String v) { location = v; }
    public String getImagePath() { return imagePath; }
    public void setImagePath(String v) { imagePath = v; }
    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate v) { startDate = v; }
    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate v) { endDate = v; }
    public BookingStatus getStatus() { return status; }
    public void setStatus(BookingStatus v) { status = v; }
    public String getDiscountName() { return discountName; }
    public void setDiscountName(String v) { discountName = v; }
    public BigDecimal getDiscountPercent() { return discountPercent; }
    public void setDiscountPercent(BigDecimal v) { discountPercent = v; }
    public int getPersonsCount() { return personsCount; }
    public void setPersonsCount(int v) { personsCount = v; }
    public BigDecimal getBasePrice() { return basePrice; }
    public void setBasePrice(BigDecimal v) { basePrice = v; }
    public BigDecimal getDiscountAmount() { return discountAmount; }
    public void setDiscountAmount(BigDecimal v) { discountAmount = v; }
    public BigDecimal getTotalPrice() { return totalPrice; }
    public void setTotalPrice(BigDecimal v) { totalPrice = v; }
    public BigDecimal getPaidAmount() { return paidAmount; }
    public void setPaidAmount(BigDecimal v) { paidAmount = v == null ? BigDecimal.ZERO : v; }
    public String getContactPhone() { return contactPhone; }
    public void setContactPhone(String v) { contactPhone = v; }
    public String getContactEmail() { return contactEmail; }
    public void setContactEmail(String v) { contactEmail = v; }
    public BigDecimal getCancelPenalty() { return cancelPenalty; }
    public void setCancelPenalty(BigDecimal v) { cancelPenalty = v; }
    public BigDecimal getRefundAmount() { return refundAmount; }
    public void setRefundAmount(BigDecimal v) { refundAmount = v; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime v) { createdAt = v; }
    public Integer getReviewRating() { return reviewRating; }
    public void setReviewRating(Integer v) { reviewRating = v; }
    public List<Tourist> getTourists() { return tourists; }
    public void setTourists(List<Tourist> v) { tourists = new ArrayList<>(v); }
    public List<StatusChange> getHistory() { return history; }
    public void setHistory(List<StatusChange> v) { history = new ArrayList<>(v); }
}
