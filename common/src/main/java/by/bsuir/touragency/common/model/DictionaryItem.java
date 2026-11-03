package by.bsuir.touragency.common.model;

import java.io.Serial;

/**
 * Элемент справочника: тип отдыха, тип питания, удобство.
 */
public class DictionaryItem extends Entity {

    @Serial
    private static final long serialVersionUID = 1L;

    private final String code;
    private final String name;

    public DictionaryItem(Integer id, String code, String name) {
        super(id);
        this.code = code;
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    @Override
    public String getDisplayName() {
        return name;
    }

    @Override
    public String toString() {
        return name;
    }
}
