package by.bsuir.touragency.client.component;

import by.bsuir.touragency.client.util.I18n;
import by.bsuir.touragency.common.model.Lang;
import javafx.geometry.Pos;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;

import java.util.function.Consumer;

/** Переключатель языка RU / EN. */
public class LangSwitch extends HBox {

    public LangSwitch(Consumer<Lang> onChange) {
        setAlignment(Pos.CENTER);
        getStyleClass().add("lang-switch");
        ToggleGroup group = new ToggleGroup();
        for (Lang lang : Lang.values()) {
            ToggleButton button = new ToggleButton(lang.name());
            button.setToggleGroup(group);
            button.getStyleClass().add("lang-button");
            button.setSelected(lang == I18n.getLang());
            button.setOnAction(e -> {
                button.setSelected(true);   // нельзя «отжать» текущий язык
                if (lang != I18n.getLang()) {
                    onChange.accept(lang);
                }
            });
            getChildren().add(button);
        }
    }
}
