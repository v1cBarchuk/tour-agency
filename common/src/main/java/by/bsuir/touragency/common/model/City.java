package by.bsuir.touragency.common.model;

import java.io.Serial;

public class City extends Entity {

    @Serial
    private static final long serialVersionUID = 1L;

    private Integer countryId;
    private String countryName;
    private String nameRu;
    private String nameEn;
    private String name;              // на языке запроса
    private int hotelsCount;

    public City() {
    }

    public City(Integer id, Integer countryId, String nameRu, String nameEn) {
        super(id);
        this.countryId = countryId;
        this.nameRu = nameRu;
        this.nameEn = nameEn;
    }

    @Override
    public String getDisplayName() {
        return countryName == null ? getName() : getName() + ", " + countryName;
    }

    @Override
    public String toString() {
        return getDisplayName();
    }

    public Integer getCountryId() { return countryId; }
    public void setCountryId(Integer countryId) { this.countryId = countryId; }
    public String getCountryName() { return countryName; }
    public void setCountryName(String countryName) { this.countryName = countryName; }
    public String getName() { return name != null ? name : nameRu; }
    public void setName(String v) { this.name = v; }
    public String getNameRu() { return nameRu; }
    public void setNameRu(String nameRu) { this.nameRu = nameRu; }
    public String getNameEn() { return nameEn; }
    public void setNameEn(String nameEn) { this.nameEn = nameEn; }
    public int getHotelsCount() { return hotelsCount; }
    public void setHotelsCount(int hotelsCount) { this.hotelsCount = hotelsCount; }
}
