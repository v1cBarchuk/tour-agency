package by.bsuir.touragency.client.component;

import by.bsuir.touragency.client.util.Alerts;
import by.bsuir.touragency.client.util.I18n;
import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

/**
 * Рекламное место: «Здесь может быть ваша реклама» и партнёрские предложения,
 * которые сменяют друг друга каждые 7 секунд. Помечается меткой «Реклама».
 */
public class AdBanner extends HBox {

    private static final String[] SLIDES = {"ad.slot", "ad.partner1", "ad.partner2"};
    private final Label title = new Label();
    private final Label text = new Label();
    private final VBox body;
    private int index;

    public AdBanner() {
        super(18);
        getStyleClass().addAll("card", "ad-banner");
        setAlignment(Pos.CENTER_LEFT);
        Label tag = new Label(I18n.t("ad.tag"));
        tag.getStyleClass().add("ad-tag");
        Label icon = new Label("✦");
        icon.getStyleClass().add("ad-icon");
        title.getStyleClass().add("ad-title");
        text.getStyleClass().add("ad-text");
        text.setWrapText(true);
        body = new VBox(4, tag, title, text);
        HBox.setHgrow(body, Priority.ALWAYS);
        Button contact = new Button(I18n.t("ad.contact"));
        contact.getStyleClass().add("btn-accent");
        contact.setMinWidth(Region.USE_PREF_SIZE);
        contact.setOnAction(e -> Alerts.info(I18n.t("ad.tag"), I18n.t("ad.contactText")));
        getChildren().addAll(icon, body, contact);
        show();
        Timeline timer = new Timeline(new KeyFrame(Duration.seconds(7), e -> {
            index = (index + 1) % SLIDES.length;
            show();
        }));
        timer.setCycleCount(Timeline.INDEFINITE);
        sceneProperty().addListener((o, old, scene) -> { if (scene == null) timer.stop(); else timer.play(); });
    }

    private void show() {
        title.setText(I18n.t(SLIDES[index] + ".title"));
        text.setText(I18n.t(SLIDES[index] + ".text"));
        FadeTransition f = new FadeTransition(Duration.millis(300), body);
        f.setFromValue(0.2);
        f.setToValue(1);
        f.play();
    }
}
