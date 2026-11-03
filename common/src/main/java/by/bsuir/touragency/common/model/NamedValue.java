package by.bsuir.touragency.common.model;

import java.io.Serial;
import java.io.Serializable;

/** Точка ряда для графиков и отчётов: подпись, значение и дополнительное значение. */
public record NamedValue(String name, double value, double extra) implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    public NamedValue(String name, double value) {
        this(name, value, 0);
    }
}
