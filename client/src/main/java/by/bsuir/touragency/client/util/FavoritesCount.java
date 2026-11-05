package by.bsuir.touragency.client.util;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;

/** Число туров в избранном — для счётчика в боковом меню. */
public final class FavoritesCount {

    private static final IntegerProperty COUNT = new SimpleIntegerProperty();

    private FavoritesCount() {
    }

    public static IntegerProperty property() {
        return COUNT;
    }

    public static void set(int value) {
        COUNT.set(Math.max(0, value));
    }

    public static void changed(boolean added) {
        set(COUNT.get() + (added ? 1 : -1));
    }
}
