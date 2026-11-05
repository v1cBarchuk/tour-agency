package by.bsuir.touragency.server.service;

import by.bsuir.touragency.common.model.WeatherForecast;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Разбор ответа сервиса погоды Open-Meteo. */
class WeatherParseTest {

    private static final String JSON = "{\"latitude\":36.9,\"daily_units\":{\"time\":\"iso8601\","
            + "\"temperature_2m_max\":\"°C\"},\"daily\":{\"time\":[\"2026-11-05\",\"2026-11-06\",\"2026-11-20\"],"
            + "\"weather_code\":[0,61],\"temperature_2m_max\":[24.3,19.8,null],\"temperature_2m_min\":[15.1,null,null],"
            + "\"precipitation_sum\":[0.0,6.4],\"wind_speed_10m_max\":[12.0,30.5]}}";

    @Test
    @DisplayName("Дни без данных (null) пропускаются, отдельные пропуски — NaN, а не 0°")
    void parsesDailyArrays() {
        List<WeatherForecast.Day> days = WeatherService.parse(JSON);
        assertEquals(2, days.size());
        assertEquals(LocalDate.of(2026, 11, 5), days.get(0).date());
        assertEquals(24.3, days.get(0).tempMax());
        assertEquals(61, days.get(1).code());
        assertTrue(Double.isNaN(days.get(1).tempMin()));
        assertEquals(6.4, days.get(1).precipitation());
    }
}
