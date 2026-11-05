package by.bsuir.touragency.client.util;

import by.bsuir.touragency.common.model.Tour;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

/**
 * Туры, отмеченные клиентом для сравнения (не больше трёх).
 * Список общий для всего приложения: карточки и панель сравнения подписаны на его изменения.
 */
public final class CompareList {

    public static final int MAX = 3;
    private static final ObservableList<Tour> TOURS = FXCollections.observableArrayList();

    private CompareList() {
    }

    public static ObservableList<Tour> tours() {
        return TOURS;
    }

    public static boolean contains(Tour tour) {
        return TOURS.stream().anyMatch(t -> t.getId().equals(tour.getId()));
    }

    /** Добавить или убрать тур; false — список уже полный. */
    public static boolean toggle(Tour tour) {
        if (contains(tour)) {
            TOURS.removeIf(t -> t.getId().equals(tour.getId()));
            return true;
        }
        if (TOURS.size() >= MAX) return false;
        TOURS.add(tour);
        return true;
    }

    public static void clear() {
        TOURS.clear();
    }
}
