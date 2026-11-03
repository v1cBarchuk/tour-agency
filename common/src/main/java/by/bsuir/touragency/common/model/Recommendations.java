package by.bsuir.touragency.common.model;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/** Страница «Мои рекомендации»: четыре подборки. */
public class Recommendations implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private boolean hasPreferences;
    private ArrayList<MatchResult> forYou = new ArrayList<>();
    private ArrayList<MatchResult> basedOnTrips = new ArrayList<>();
    private String visitedCountry;           // «Вам понравился отдых в …»
    private ArrayList<MatchResult> popular = new ArrayList<>();
    private ArrayList<MatchResult> newMatching = new ArrayList<>();

    public boolean isHasPreferences() { return hasPreferences; }
    public void setHasPreferences(boolean v) { hasPreferences = v; }
    public List<MatchResult> getForYou() { return forYou; }
    public void setForYou(List<MatchResult> v) { forYou = new ArrayList<>(v); }
    public List<MatchResult> getBasedOnTrips() { return basedOnTrips; }
    public void setBasedOnTrips(List<MatchResult> v) { basedOnTrips = new ArrayList<>(v); }
    public String getVisitedCountry() { return visitedCountry; }
    public void setVisitedCountry(String v) { visitedCountry = v; }
    public List<MatchResult> getPopular() { return popular; }
    public void setPopular(List<MatchResult> v) { popular = new ArrayList<>(v); }
    public List<MatchResult> getNewMatching() { return newMatching; }
    public void setNewMatching(List<MatchResult> v) { newMatching = new ArrayList<>(v); }
}
