package by.bsuir.touragency.server.service;

import by.bsuir.touragency.common.exception.AppException;
import by.bsuir.touragency.common.exception.BusinessRuleException;
import by.bsuir.touragency.common.model.Tour;
import by.bsuir.touragency.common.model.WeatherForecast;
import by.bsuir.touragency.server.dao.SettingsDao;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Прогноз погоды для города тура через открытый сервис Open-Meteo (ключ не нужен).
 * Сервис даёт прогноз на 16 дней вперёд: если тур начинается в этом окне — показываются
 * даты тура, иначе — ближайшая неделя. Ответы кэшируются на час, чтобы не обращаться
 * к внешнему сервису при каждом открытии страницы.
 */
public class WeatherService {

    private static final Logger LOG = Logger.getLogger(WeatherService.class.getName());
    static final int HORIZON_DAYS = 16;
    private static final Duration CACHE_TTL = Duration.ofHours(1);
    private static final String DAILY = "weather_code,temperature_2m_max,temperature_2m_min,precipitation_sum,"
            + "wind_speed_10m_max";

    private final SettingsDao settings;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    private final Map<String, Cached> cache = new ConcurrentHashMap<>();

    private record Cached(long at, WeatherForecast forecast) {
    }

    public WeatherService(SettingsDao settings) {
        this.settings = settings;
    }

    public WeatherForecast forecast(Tour tour) throws AppException {
        if (tour.getLatitude() == null || tour.getLongitude() == null) {
            throw new BusinessRuleException("weather.noCoordinates");
        }
        LocalDate today = LocalDate.now();
        LocalDate last = today.plusDays(HORIZON_DAYS - 1);
        boolean forTour = !tour.getStartDate().isAfter(last) && !tour.getEndDate().isBefore(today);
        LocalDate from = forTour ? max(today, tour.getStartDate()) : today;
        LocalDate to = forTour ? min(last, tour.getEndDate()) : today.plusDays(6);

        String key = tour.getLatitude() + "," + tour.getLongitude() + "," + from + "," + to;
        Cached c = cache.get(key);
        if (c != null && System.currentTimeMillis() - c.at() < CACHE_TTL.toMillis()) {
            return c.forecast();
        }
        String url = String.format(Locale.ROOT, "%s?latitude=%.4f&longitude=%.4f&daily=%s&timezone=auto"
                        + "&start_date=%s&end_date=%s",
                settings.get("weather.api.url", "https://api.open-meteo.com/v1/forecast"),
                tour.getLatitude(), tour.getLongitude(), DAILY, from, to);
        try {
            HttpResponse<String> response = http.send(HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(8)).GET().build(), HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new IllegalStateException("HTTP " + response.statusCode());
            }
            WeatherForecast f = new WeatherForecast(tour.getCityName(), forTour, parse(response.body()));
            cache.put(key, new Cached(System.currentTimeMillis(), f));
            return f;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessRuleException("weather.unavailable");
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Прогноз погоды недоступен: " + e.getMessage());
            throw new BusinessRuleException("weather.unavailable");
        }
    }

    /** Разбор ответа Open-Meteo: массивы daily.time, temperature_2m_max и т.д. одинаковой длины. */
    static ArrayList<WeatherForecast.Day> parse(String json) {
        String[] time = array(json, "time");
        String[] max = array(json, "temperature_2m_max");
        String[] min = array(json, "temperature_2m_min");
        String[] code = array(json, "weather_code");
        String[] rain = array(json, "precipitation_sum");
        String[] wind = array(json, "wind_speed_10m_max");
        ArrayList<WeatherForecast.Day> days = new ArrayList<>();
        for (int i = 0; i < time.length; i++) {
            double tMax = num(max, i);
            double tMin = num(min, i);
            // на краю горизонта прогноза сервис возвращает null — такой день не показываем
            if (Double.isNaN(tMax) && Double.isNaN(tMin)) continue;
            double c = num(code, i);
            days.add(new WeatherForecast.Day(LocalDate.parse(time[i].replace("\"", "")),
                    tMax, tMin, Double.isNaN(c) ? -1 : (int) c, num(rain, i), num(wind, i)));
        }
        return days;
    }

    private static String[] array(String json, String name) {
        Matcher m = Pattern.compile("\"" + name + "\"\\s*:\\s*\\[([^\\]]*)]").matcher(json);
        if (!m.find()) return new String[0];
        String body = m.group(1).trim();
        return body.isEmpty() ? new String[0] : body.split("\\s*,\\s*");
    }

    /** Число из массива; нет значения или null — NaN (клиент показывает «—»). */
    private static double num(String[] values, int i) {
        if (i >= values.length || values[i].equals("null")) return Double.NaN;
        return Double.parseDouble(values[i]);
    }

    private static LocalDate max(LocalDate a, LocalDate b) {
        return a.isAfter(b) ? a : b;
    }

    private static LocalDate min(LocalDate a, LocalDate b) {
        return a.isBefore(b) ? a : b;
    }
}
