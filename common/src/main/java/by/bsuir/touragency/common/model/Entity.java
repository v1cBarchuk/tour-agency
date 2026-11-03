package by.bsuir.touragency.common.model;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

/**
 * Базовый класс всех сущностей предметной области.
 * Сериализуемый: объекты передаются между клиентом и сервером через сокет.
 */
public abstract class Entity implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Integer id;

    protected Entity() {
    }

    protected Entity(Integer id) {
        this.id = id;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    /** Отображаемое имя сущности в интерфейсе. */
    public abstract String getDisplayName();

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Entity other = (Entity) o;
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(getClass(), id);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "#" + id;
    }
}
