package by.bsuir.touragency.common.model;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.ArrayList;

/**
 * Прогноз погоды для города тура (источник — открытый сервис Open-Meteo).
 *
 * @param forTourDates true — прогноз на даты тура; false — тур позже горизонта прогноза
 *                     (16 дней), поэтому показывается погода на ближайшую неделю
 */
public record WeatherForecast(String city, boolean forTourDates, ArrayList<Day> days) implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /** Один день: температура днём и ночью, код погоды WMO (-1 — нет данных), осадки (мм), ветер (км/ч); NaN — нет данных. */
    public record Day(LocalDate date, double tempMax, double tempMin, int code, double precipitation,
                      double windMax) implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
    }
}
