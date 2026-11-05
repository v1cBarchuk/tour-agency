package by.bsuir.touragency.client.component;

import by.bsuir.touragency.client.util.Currency;
import by.bsuir.touragency.client.util.I18n;
import javafx.geometry.Pos;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;

/** Переключатель валюты цен в шапке: BYN / USD / EUR. */
public class CurrencySwitch extends HBox {

    public CurrencySwitch(Runnable onChange) {
        setAlignment(Pos.CENTER);
        getStyleClass().addAll("lang-switch", "currency-switch");
        ToggleGroup group = new ToggleGroup();
        for (String code : Currency.CODES) {
            ToggleButton b = new ToggleButton(code);
            b.setToggleGroup(group);
            b.getStyleClass().add("lang-button");
            b.setSelected(code.equals(Currency.selected()));
            b.setTooltip(new Tooltip(I18n.t("currency.tip")));
            b.setOnAction(e -> {
                b.setSelected(true);
                if (!code.equals(Currency.selected())) {
                    Currency.set(code);
                    onChange.run();
                }
            });
            getChildren().add(b);
        }
    }
}
