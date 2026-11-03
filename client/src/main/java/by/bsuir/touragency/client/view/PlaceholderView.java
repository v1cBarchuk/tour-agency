package by.bsuir.touragency.client.view;

import by.bsuir.touragency.client.component.EmptyState;
import javafx.scene.Parent;
import javafx.scene.layout.VBox;

/** Раздел, который будет реализован на следующих этапах. */
public class PlaceholderView extends BaseView {

    private final String titleKey;

    public PlaceholderView(String titleKey) {
        this.titleKey = titleKey;
    }

    @Override
    public String getTitle() {
        return t(titleKey);
    }

    @Override
    protected Parent build() {
        VBox box = new VBox(24, pageHeader(t(titleKey), null),
                new EmptyState("✈", t(titleKey), t("common.soon")));
        box.getStyleClass().add("page");
        return box;
    }
}
