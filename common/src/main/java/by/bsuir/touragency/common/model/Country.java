package by.bsuir.touragency.common.model;

import java.io.Serial;

public class Country extends Entity {

    @Serial
    private static final long serialVersionUID = 1L;

    private String nameRu;
    private String nameEn;
    private String name;              // на языке запроса
    private String description;       // на языке запроса (для показа)
    private String descriptionRu;     // для редактирования
    private String descriptionEn;
    private String imagePath;
    private int toursCount;      // активные будущие туры
    private int citiesCount;
    private Integer minPrice;    // минимальная цена тура, BYN

    public Country() {
    }

    public Country(Integer id, String nameRu, String nameEn) {
        super(id);
        this.nameRu = nameRu;
        this.nameEn = nameEn;
    }

    @Override
    public String getDisplayName() {
        return getName();
    }

    @Override
    public String toString() {
        return getName();
    }

    public String getName() { return name != null ? name : nameRu; }
    public void setName(String v) { this.name = v; }
    public String getNameRu() { return nameRu; }
    public void setNameRu(String nameRu) { this.nameRu = nameRu; }
    public String getNameEn() { return nameEn; }
    public void setNameEn(String nameEn) { this.nameEn = nameEn; }
    public String getDescriptionRu() { return descriptionRu; }
    public void setDescriptionRu(String v) { this.descriptionRu = v; }
    public String getDescriptionEn() { return descriptionEn; }
    public void setDescriptionEn(String v) { this.descriptionEn = v; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getImagePath() { return imagePath; }
    public void setImagePath(String imagePath) { this.imagePath = imagePath; }
    public int getToursCount() { return toursCount; }
    public void setToursCount(int toursCount) { this.toursCount = toursCount; }
    public int getCitiesCount() { return citiesCount; }
    public void setCitiesCount(int citiesCount) { this.citiesCount = citiesCount; }
    public Integer getMinPrice() { return minPrice; }
    public void setMinPrice(Integer minPrice) { this.minPrice = minPrice; }
}
