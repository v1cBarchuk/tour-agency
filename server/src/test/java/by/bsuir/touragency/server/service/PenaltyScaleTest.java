package by.bsuir.touragency.server.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Проверка шкалы штрафов за отмену бронирования. */
class PenaltyScaleTest {

    private static final String SCALE = "31:5;15:15;7:30;3:50;0:80";

    @Test
    @DisplayName("Ступени шкалы по умолчанию, включая граничные значения")
    void defaultScale() {
        assertEquals(5, BookingService.penaltyPercent(SCALE, 60));
        assertEquals(5, BookingService.penaltyPercent(SCALE, 31));
        assertEquals(15, BookingService.penaltyPercent(SCALE, 30));
        assertEquals(15, BookingService.penaltyPercent(SCALE, 15));
        assertEquals(30, BookingService.penaltyPercent(SCALE, 14));
        assertEquals(30, BookingService.penaltyPercent(SCALE, 7));
        assertEquals(50, BookingService.penaltyPercent(SCALE, 3));
        assertEquals(80, BookingService.penaltyPercent(SCALE, 0));
    }

    @Test
    @DisplayName("Порядок ступеней в настройке не важен")
    void orderDoesNotMatter() {
        assertEquals(30, BookingService.penaltyPercent("0:80; 7:30 ;31:5", 10));
    }
}
