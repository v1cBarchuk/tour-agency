package by.bsuir.touragency.common.model;

import java.io.Serial;
import java.io.Serializable;

/**
 * Один критерий оценки тура: сколько баллов набрано из максимума и почему.
 * Текст строится на клиенте по ключу «crit.CODE.RESULT» с аргументами args.
 */
public class Criterion implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    public enum Code { COUNTRY, BUDGET, VACATION_TYPE, DATES, NIGHTS, STARS, MEAL, HISTORY, FAVORITES }

    public enum Result { MATCH, PARTIAL, MISS, SKIPPED, BONUS }

    private final Code code;
    private final double points;
    private final double max;
    private final Result result;
    private final String[] args;

    public Criterion(Code code, double points, double max, Result result, String... args) {
        this.code = code;
        this.points = points;
        this.max = max;
        this.result = result;
        this.args = args;
    }

    public Code getCode() { return code; }
    public double getPoints() { return points; }
    public double getMax() { return max; }
    public Result getResult() { return result; }
    public String[] getArgs() { return args; }

    /** Критерий учитывался в оценке (пользователь его заполнил). */
    public boolean isCounted() {
        return result != Result.SKIPPED && result != Result.BONUS && max > 0;
    }
}
