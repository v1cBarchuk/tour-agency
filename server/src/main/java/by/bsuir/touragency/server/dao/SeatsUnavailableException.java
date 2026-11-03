package by.bsuir.touragency.server.dao;

import java.io.Serial;

/**
 * Свободных мест меньше, чем запрошено. Бросается внутри транзакции бронирования
 * после блокировки строки тура — значит, место успел занять другой клиент.
 */
public class SeatsUnavailableException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    private final int freeSeats;

    public SeatsUnavailableException(int freeSeats) {
        super("Not enough free seats: " + freeSeats);
        this.freeSeats = freeSeats;
    }

    public int getFreeSeats() {
        return freeSeats;
    }
}
