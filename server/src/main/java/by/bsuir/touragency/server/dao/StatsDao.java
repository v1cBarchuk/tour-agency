package by.bsuir.touragency.server.dao;

import by.bsuir.touragency.common.model.Lang;
import by.bsuir.touragency.common.model.Stats;

import java.time.LocalDate;

public interface StatsDao {

    /** Показатели панели управления (за всё время). */
    Stats dashboard(Lang lang);

    /** Отчёты за период по дате оформления бронирования. */
    Stats analytics(LocalDate from, LocalDate to, Lang lang);
}
