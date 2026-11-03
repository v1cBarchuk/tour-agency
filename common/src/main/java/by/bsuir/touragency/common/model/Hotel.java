package by.bsuir.touragency.common.model;

import java.io.Serial;
import java.util.ArrayList;
import java.util.List;

public class Hotel extends Entity {

    @Serial
    private static final long serialVersionUID = 1L;

    private Integer cityId;
    private String cityName;
    private Integer countryId;
    private String countryName;
    private String name;
    private int stars;
    private String address;
    private String description;       // на языке запроса (для показа)
    private String descriptionRu;     // для редактирования
    private String descriptionEn;
    private Double rating;            // средняя оценка по отзывам, null — нет отзывов
    private int reviewsCount;
    private Integer minPrice;         // минимальная цена активного тура
    private String mainPhoto;
    private ArrayList<Integer> amenityIds = new ArrayList<>();
    private ArrayList<String> amenityNames = new ArrayList<>();

    public Hotel() {
    }

    @Override
    public String getDisplayName() {
        return name + " " + stars + "*";
    }

    @Override
    public String toString() {
        return name + " (" + cityName + ")";
    }

    public String getLocation() {
        return cityName + ", " + countryName;
    }

    public Integer getCityId() { return cityId; }
    public void setCityId(Integer cityId) { this.cityId = cityId; }
    public String getCityName() { return cityName; }
    public void setCityName(String cityName) { this.cityName = cityName; }
    public Integer getCountryId() { return countryId; }
    public void setCountryId(Integer countryId) { this.countryId = countryId; }
    public String getCountryName() { return countryName; }
    public void setCountryName(String countryName) { this.countryName = countryName; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public int getStars() { return stars; }
    public void setStars(int stars) { this.stars = stars; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getDescriptionRu() { return descriptionRu; }
    public void setDescriptionRu(String v) { this.descriptionRu = v; }
    public String getDescriptionEn() { return descriptionEn; }
    public void setDescriptionEn(String v) { this.descriptionEn = v; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Double getRating() { return rating; }
    public void setRating(Double rating) { this.rating = rating; }
    public int getReviewsCount() { return reviewsCount; }
    public void setReviewsCount(int reviewsCount) { this.reviewsCount = reviewsCount; }
    public Integer getMinPrice() { return minPrice; }
    public void setMinPrice(Integer minPrice) { this.minPrice = minPrice; }
    public String getMainPhoto() { return mainPhoto; }
    public void setMainPhoto(String mainPhoto) { this.mainPhoto = mainPhoto; }
    public List<Integer> getAmenityIds() { return amenityIds; }
    public void setAmenityIds(List<Integer> ids) { this.amenityIds = new ArrayList<>(ids); }
    public List<String> getAmenityNames() { return amenityNames; }
    public void setAmenityNames(List<String> names) { this.amenityNames = new ArrayList<>(names); }
}
