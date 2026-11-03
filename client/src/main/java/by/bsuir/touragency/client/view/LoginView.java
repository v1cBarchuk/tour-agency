package by.bsuir.touragency.client.view;

import by.bsuir.touragency.client.util.Async;
import by.bsuir.touragency.client.util.ClientContext;
import by.bsuir.touragency.client.util.I18n;
import by.bsuir.touragency.common.model.ReferenceData;
import by.bsuir.touragency.common.model.User;
import by.bsuir.touragency.common.protocol.CommandType;
import by.bsuir.touragency.common.protocol.Request;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class LoginView extends AuthView {

    private TextField loginField;
    private PasswordField passwordField;
    private Label errorLabel;
    private Button loginButton;
    private String keptLogin = "";

    @Override
    public String getTitle() {
        return t("login.title");
    }

    @Override
    public BaseView fresh() {
        LoginView view = new LoginView();
        view.keptLogin = loginField == null ? "" : loginField.getText();
        return view;
    }

    @Override
    protected Parent buildForm() {
        Label title = new Label(t("login.title"));
        title.getStyleClass().add("auth-title");
        Label subtitle = new Label(t("login.subtitle"));
        subtitle.getStyleClass().add("page-subtitle");

        loginField = new TextField(keptLogin);
        loginField.setPromptText(t("login.login"));
        passwordField = new PasswordField();
        passwordField.setPromptText(t("login.password"));
        passwordField.setOnAction(e -> login());

        errorLabel = new Label();
        errorLabel.getStyleClass().add("error");
        errorLabel.setWrapText(true);

        loginButton = new Button(t("login.submit"));
        loginButton.getStyleClass().add("btn-primary");
        loginButton.setDefaultButton(true);
        loginButton.setMaxWidth(Double.MAX_VALUE);
        loginButton.setOnAction(e -> login());

        Hyperlink register = new Hyperlink(t("login.register"));
        register.setOnAction(e -> Navigator.show(new RegisterView()));
        Label noAccount = new Label(t("login.noAccount"));
        noAccount.getStyleClass().add("muted");
        HBox registerRow = new HBox(4, noAccount, register);
        registerRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        Label demo = new Label(t("login.demo"));
        demo.getStyleClass().add("demo-hint");
        demo.setWrapText(true);

        VBox form = new VBox(14, title, subtitle, loginField, passwordField, errorLabel, loginButton,
                registerRow, demo);
        form.getStyleClass().add("auth-form");
        form.setMaxWidth(380);
        return form;
    }

    private void login() {
        errorLabel.setText("");
        loginButton.setDisable(true);
        Request request = new Request(CommandType.LOGIN)
                .with("login", loginField.getText())
                .with("password", passwordField.getText());

        Async.send(request, response -> {
            if (!response.isOk()) {
                loginButton.setDisable(false);
                errorLabel.setText(response.getMessage());
                passwordField.clear();
                return;
            }
            User user = response.getData();
            ClientContext.setUser(user);
            // выбранный на экране входа язык сохраняется в профиле
            if (user.getLanguage() != I18n.getLang()) {
                Async.send(new Request(CommandType.SET_LANGUAGE), r -> { });
            }
            Async.send(new Request(CommandType.GET_REFERENCE_DATA), ref -> {
                loginButton.setDisable(false);
                if (ref.isOk()) {
                    ClientContext.setReference(ref.<ReferenceData>getData());
                    Navigator.show(new MainLayout(user, null));
                } else {
                    errorLabel.setText(ref.getMessage());
                }
            });
        }, error -> {
            loginButton.setDisable(false);
            errorLabel.setText(error.getMessage());
        });
    }
}
