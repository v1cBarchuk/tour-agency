package by.bsuir.touragency.common.model;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Показатели для панели управления и аналитики: набор чисел (KPI)
 * и именованные ряды для графиков и таблиц.
 */
public class Stats implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private LocalDate from;
    private LocalDate to;
    private final LinkedHashMap<String, Double> kpi = new LinkedHashMap<>();
    private final LinkedHashMap<String, ArrayList<NamedValue>> series = new LinkedHashMap<>();

    public LocalDate getFrom() { return from; }
    public void setFrom(LocalDate v) { from = v; }
    public LocalDate getTo() { return to; }
    public void setTo(LocalDate v) { to = v; }

    public void putKpi(String key, double value) {
        kpi.put(key, value);
    }

    public double kpi(String key) {
        return kpi.getOrDefault(key, 0.0);
    }

    public Map<String, Double> getKpi() {
        return kpi;
    }

    public void putSeries(String key, List<NamedValue> values) {
        series.put(key, new ArrayList<>(values));
    }

    public List<NamedValue> series(String key) {
        return series.getOrDefault(key, new ArrayList<>());
    }
}
