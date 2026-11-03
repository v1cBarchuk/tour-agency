package by.bsuir.touragency.client.view;

import by.bsuir.touragency.client.component.LangSwitch;
import by.bsuir.touragency.client.util.I18n;
import by.bsuir.touragency.client.util.Images;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/**
 * Общий макет экранов входа и регистрации: иллюстрация с девизом слева,
 * форма справа, переключатель языка в углу.
 */
public abstract class AuthView extends BaseView {

    /** Содержимое правой части (форма). */
    protected abstract Parent buildForm();

    @Override
    protected final Parent build() {
        StackPane hero = new StackPane();
        hero.getStyleClass().add("auth-hero");
        HBox.setHgrow(hero, Priority.ALWAYS);
        hero.setMinWidth(420);
        Region image = new Region();
        image.getStyleClass().add("auth-hero-bg");
        Region shade = new Region();
        shade.getStyleClass().add("auth-hero-shade");

        Label logo = new Label(t("app.name"));
        logo.getStyleClass().add("logo-light");
        Label title = new Label(t("hero.title"));
        title.getStyleClass().add("hero-title");
        title.setWrapText(true);
        Label subtitle = new Label(t("hero.subtitle"));
        subtitle.getStyleClass().add("hero-subtitle");
        subtitle.setWrapText(true);
        Region grow = new Region();
        VBox.setVgrow(grow, Priority.ALWAYS);
        VBox text = new VBox(14, logo, grow, title, subtitle);
        text.setPadding(new Insets(40));
        text.setMaxWidth(620);
        StackPane.setAlignment(text, Pos.TOP_LEFT);

        // если в ресурсах есть фото images/hero/login.jpg — показываем его под затемнением
        javafx.scene.image.Image photo = Images.raw("images/hero/login.jpg");
        if (photo != null) {
            javafx.scene.image.ImageView iv = new javafx.scene.image.ImageView(photo);
            iv.setPreserveRatio(false);
            iv.fitWidthProperty().bind(hero.widthProperty());
            iv.fitHeightProperty().bind(hero.heightProperty());
            hero.getChildren().add(iv);
        } else {
            hero.getChildren().add(image);   // градиентная подложка, пока фото не добавлено
        }
        hero.getChildren().addAll(shade, text);

        LangSwitch lang = new LangSwitch(I18n::setLang);
        HBox top = new HBox(lang);
        top.setAlignment(Pos.TOP_RIGHT);

        VBox formBox = new VBox(buildForm());
        formBox.setAlignment(Pos.CENTER);
        VBox.setVgrow(formBox, Priority.ALWAYS);

        VBox right = new VBox(top, formBox);
        right.getStyleClass().add("auth-side");
        right.setPadding(new Insets(24, 32, 32, 32));
        right.setMinWidth(460);
        right.setPrefWidth(500);

        return new HBox(hero, right);
    }
}
