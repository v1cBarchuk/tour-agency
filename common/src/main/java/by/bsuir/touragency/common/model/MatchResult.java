package by.bsuir.touragency.common.model;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Результат оценки тура по анкете: итоговые баллы, процент совпадения
 * и разбор по каждому критерию.
 */
public class MatchResult implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final Tour tour;
    private final ArrayList<Criterion> criteria;
    private final double score;        // набранные баллы по заполненным критериям
    private final double maxScore;     // максимум по заполненным критериям
    private final double bonus;        // бонус за историю поездок и избранное
    private final int percent;         // итог 0–100

    public MatchResult(Tour tour, List<Criterion> criteria, double score, double maxScore, double bonus, int percent) {
        this.tour = tour;
        this.criteria = new ArrayList<>(criteria);
        this.score = score;
        this.maxScore = maxScore;
        this.bonus = bonus;
        this.percent = percent;
    }

    public Tour getTour() { return tour; }
    public List<Criterion> getCriteria() { return criteria; }
    public double getScore() { return score; }
    public double getMaxScore() { return maxScore; }
    public double getBonus() { return bonus; }
    public int getPercent() { return percent; }

    public List<Criterion> matched() {
        return criteria.stream().filter(c -> c.getResult() == Criterion.Result.MATCH
                || c.getResult() == Criterion.Result.BONUS).toList();
    }

    public List<Criterion> differs() {
        return criteria.stream().filter(c -> c.getResult() == Criterion.Result.MISS
                || c.getResult() == Criterion.Result.PARTIAL).toList();
    }
}
