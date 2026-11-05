package by.bsuir.touragency.client;

import by.bsuir.touragency.client.network.ClientConfig;
import by.bsuir.touragency.client.network.ServerConnection;
import by.bsuir.touragency.client.view.LoginView;
import by.bsuir.touragency.client.view.Navigator;
import javafx.application.Application;
import javafx.stage.Stage;

public class ClientApp extends Application {

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) throws Exception {
        ClientConfig config = ClientConfig.load(getParameters().getRaw());
        ServerConnection.init(config.getHost(), config.getPort());
        // после обрыва связи соединение и сессия восстанавливаются автоматически
        ServerConnection.getInstance().setOnRestored(() -> javafx.application.Platform.runLater(
                () -> by.bsuir.touragency.client.util.Alerts.toast(
                        by.bsuir.touragency.client.util.I18n.t("conn.restored"))));

        stage.setMinWidth(480);
        stage.setMinHeight(420);
        Navigator.init(stage);
        Navigator.show(new LoginView());
        stage.show();
    }

    @Override
    public void stop() {
        ServerConnection.getInstance().close();
    }
}
