package by.bsuir.touragency.common.model;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;

/** Турист в бронировании. */
public record Tourist(int number, String fullName, LocalDate birthDate) implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
}
