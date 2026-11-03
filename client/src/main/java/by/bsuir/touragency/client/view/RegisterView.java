package by.bsuir.touragency.client.view;

import by.bsuir.touragency.client.util.Alerts;
import by.bsuir.touragency.client.util.Async;
import by.bsuir.touragency.common.protocol.CommandType;
import by.bsuir.touragency.common.protocol.Request;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class RegisterView extends AuthView {

    private TextField fullNameField;
    private TextField loginField;
    private TextField emailField;
    private TextField phoneField;
    private PasswordField passwordField;
    private PasswordField repeatField;
    private Label errorLabel;
    private Button registerButton;

    @Override
    public String getTitle() {
        return t("register.title");
    }

    @Override
    public BaseView fresh() {
        RegisterView view = new RegisterView();
        view.getRoot();
        if (fullNameField != null) {
            view.fullNameField.setText(fullNameField.getText());
            view.loginField.setText(loginField.getText());
            view.emailField.setText(emailField.getText());
            view.phoneField.setText(phoneField.getText());
        }
        return view;
    }

    @Override
    protected Parent buildForm() {
        Label title = new Label(t("register.title"));
        title.getStyleClass().add("auth-title");
        Label subtitle = new Label(t("register.subtitle"));
        subtitle.getStyleClass().add("page-subtitle");

        fullNameField = field("register.fullName");
        loginField = field("register.login");
        emailField = field("register.email");
        phoneField = field("register.phone");
        passwordField = new PasswordField();
        passwordField.setPromptText(t("register.password"));
        repeatField = new PasswordField();
        repeatField.setPromptText(t("register.repeat"));

        errorLabel = new Label();
        errorLabel.getStyleClass().add("error");
        errorLabel.setWrapText(true);

        registerButton = new Button(t("register.submit"));
        registerButton.getStyleClass().add("btn-primary");
        registerButton.setDefaultButton(true);
        registerButton.setMaxWidth(Double.MAX_VALUE);
        registerButton.setOnAction(e -> register());

        Hyperlink back = new Hyperlink(t("register.signIn"));
        back.setOnAction(e -> Navigator.show(new LoginView()));
        Label have = new Label(t("register.haveAccount"));
        have.getStyleClass().add("muted");
        HBox backRow = new HBox(4, have, back);
        backRow.setAlignment(Pos.CENTER_LEFT);

        VBox form = new VBox(12, title, subtitle, fullNameField, loginField, emailField, phoneField,
                passwordField, repeatField, errorLabel, registerButton, backRow);
        form.getStyleClass().add("auth-form");
        form.setMaxWidth(380);
        return form;
    }

    private static TextField field(String promptKey) {
        TextField field = new TextField();
        field.setPromptText(t(promptKey));
        return field;
    }

    private void register() {
        errorLabel.setText("");
        if (!passwordField.getText().equals(repeatField.getText())) {
            errorLabel.setText(t("register.mismatch"));
            return;
        }
        registerButton.setDisable(true);
        Request request = new Request(CommandType.REGISTER)
                .with("fullName", fullNameField.getText())
                .with("login", loginField.getText())
                .with("email", emailField.getText())
                .with("phone", phoneField.getText())
                .with("password", passwordField.getText());

        Async.send(request, response -> {
            registerButton.setDisable(false);
            if (response.isOk()) {
                Alerts.info(t("register.done"), response.getMessage());
                Navigator.show(new LoginView());
            } else {
                errorLabel.setText(response.getMessage());
            }
        }, error -> {
            registerButton.setDisable(false);
            errorLabel.setText(error.getMessage());
        });
    }
}
