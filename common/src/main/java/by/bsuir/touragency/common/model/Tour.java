package by.bsuir.touragency.common.model;

import java.io.Serial;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * Тур. Поля после «вычисляемые» заполняет сервер при чтении,
 * в таблице tours они не хранятся.
 */
public class Tour extends Entity {

    @Serial
    private static final long serialVersionUID = 1L;

    private Integer hotelId;
    private Integer vacationTypeId;
    private Integer mealTypeId;
    private String titleRu;
    private String titleEn;
    private String description;       // на языке запроса
    private String descriptionRu;
    private String descriptionEn;
    private String title;             // на языке запроса
    private LocalDate startDate;
    private LocalDate endDate;
    private BigDecimal pricePerPerson;
    private int totalSeats;
    private String imagePath;
    private boolean featured;
    private boolean active = true;
    private LocalDateTime createdAt;

    // вычисляемые
    private String hotelName;
    private int hotelStars;
    private String cityName;
    private Integer countryId;
    private String countryName;
    private String vacationTypeName;
    private String mealTypeName;
    private int freeSeats;
    private int bookingsCount;
    private Double rating;
    private int reviewsCount;
    private boolean hot;
    private boolean newTour;
    private boolean popular;
    private boolean almostSoldOut;      // занято больше N % мест (настройка tour.almostSold.percent)
    private java.math.BigDecimal oldPrice;  // прежняя цена, если её снизили за последние 30 дней
    private Integer cityId;
    private boolean visaFree;
    private Double latitude;            // координаты города — для прогноза погоды
    private Double longitude;
    private Integer matchPercent;     // совпадение с анкетой клиента, null — анкеты нет
    private boolean favorite;         // тур в избранном текущего клиента

    public Tour() {
    }

    @Override
    public String getDisplayName() {
        return getTitle();
    }

    @Override
    public String toString() {
        return getTitle();
    }

    public long getNights() {
        return startDate == null || endDate == null ? 0 : ChronoUnit.DAYS.between(startDate, endDate);
    }

    public int getBookedSeats() {
        return totalSeats - freeSeats;
    }

    public String getLocation() {
        return cityName + ", " + countryName;
    }

    public Integer getHotelId() { return hotelId; }
    public void setHotelId(Integer hotelId) { this.hotelId = hotelId; }
    public Integer getVacationTypeId() { return vacationTypeId; }
    public void setVacationTypeId(Integer vacationTypeId) { this.vacationTypeId = vacationTypeId; }
    public Integer getMealTypeId() { return mealTypeId; }
    public void setMealTypeId(Integer mealTypeId) { this.mealTypeId = mealTypeId; }
    public String getTitleRu() { return titleRu; }
    public void setTitleRu(String titleRu) { this.titleRu = titleRu; }
    public String getTitleEn() { return titleEn; }
    public void setTitleEn(String titleEn) { this.titleEn = titleEn; }
    public String getDescriptionRu() { return descriptionRu; }
    public void setDescriptionRu(String v) { this.descriptionRu = v; }
    public String getDescriptionEn() { return descriptionEn; }
    public void setDescriptionEn(String v) { this.descriptionEn = v; }
    public String getTitle() { return title != null ? title : titleRu; }
    public void setTitle(String v) { this.title = v; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
    public BigDecimal getPricePerPerson() { return pricePerPerson; }
    public void setPricePerPerson(BigDecimal pricePerPerson) { this.pricePerPerson = pricePerPerson; }
    public int getTotalSeats() { return totalSeats; }
    public void setTotalSeats(int totalSeats) { this.totalSeats = totalSeats; }
    public String getImagePath() { return imagePath; }
    public void setImagePath(String imagePath) { this.imagePath = imagePath; }
    public boolean isFeatured() { return featured; }
    public void setFeatured(boolean featured) { this.featured = featured; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public String getHotelName() { return hotelName; }
    public void setHotelName(String hotelName) { this.hotelName = hotelName; }
    public int getHotelStars() { return hotelStars; }
    public void setHotelStars(int hotelStars) { this.hotelStars = hotelStars; }
    public String getCityName() { return cityName; }
    public void setCityName(String cityName) { this.cityName = cityName; }
    public Integer getCountryId() { return countryId; }
    public void setCountryId(Integer countryId) { this.countryId = countryId; }
    public String getCountryName() { return countryName; }
    public void setCountryName(String countryName) { this.countryName = countryName; }
    public String getVacationTypeName() { return vacationTypeName; }
    public void setVacationTypeName(String vacationTypeName) { this.vacationTypeName = vacationTypeName; }
    public String getMealTypeName() { return mealTypeName; }
    public void setMealTypeName(String mealTypeName) { this.mealTypeName = mealTypeName; }
    public int getFreeSeats() { return freeSeats; }
    public void setFreeSeats(int freeSeats) { this.freeSeats = freeSeats; }
    public int getBookingsCount() { return bookingsCount; }
    public void setBookingsCount(int bookingsCount) { this.bookingsCount = bookingsCount; }
    public Double getRating() { return rating; }
    public void setRating(Double rating) { this.rating = rating; }
    public int getReviewsCount() { return reviewsCount; }
    public void setReviewsCount(int reviewsCount) { this.reviewsCount = reviewsCount; }
    public boolean isHot() { return hot; }
    public void setHot(boolean hot) { this.hot = hot; }
    public boolean isNewTour() { return newTour; }
    public void setNewTour(boolean newTour) { this.newTour = newTour; }
    public Integer getMatchPercent() { return matchPercent; }
    public void setMatchPercent(Integer v) { matchPercent = v; }
    public boolean isFavorite() { return favorite; }
    public void setFavorite(boolean v) { favorite = v; }
    public boolean isPopular() { return popular; }
    public boolean isAlmostSoldOut() { return almostSoldOut; }
    public void setAlmostSoldOut(boolean almostSoldOut) { this.almostSoldOut = almostSoldOut; }
    public java.math.BigDecimal getOldPrice() { return oldPrice; }
    public void setOldPrice(java.math.BigDecimal oldPrice) { this.oldPrice = oldPrice; }
    public boolean isVisaFree() { return visaFree; }
    public void setVisaFree(boolean visaFree) { this.visaFree = visaFree; }
    public Integer getCityId() { return cityId; }
    public void setCityId(Integer cityId) { this.cityId = cityId; }
    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }
    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }
    /** Доля занятых мест, 0–100. */
    public int getOccupancyPercent() {
        return getTotalSeats() == 0 ? 0 : (int) Math.round(100.0 * (getTotalSeats() - getFreeSeats()) / getTotalSeats());
    }
    public void setPopular(boolean popular) { this.popular = popular; }
}
