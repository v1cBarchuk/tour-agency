package by.bsuir.touragency.client.view.manager;

import by.bsuir.touragency.client.util.Async;
import by.bsuir.touragency.client.util.Ui;
import by.bsuir.touragency.client.view.BaseView;
import by.bsuir.touragency.common.model.AuditEntry;
import by.bsuir.touragency.common.model.User;
import by.bsuir.touragency.common.protocol.CommandType;
import by.bsuir.touragency.common.protocol.Request;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.layout.VBox;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/** Журнал действий (администратор): кто, что, когда, над каким объектом и что изменилось. */
public class AuditView extends BaseView {

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("dd.MM.yy HH:mm");
    private static final List<String> ACTIONS = List.of("LOGIN", "LOGIN_FAILED", "LOGOUT", "REGISTER", "CREATE",
            "UPDATE", "DELETE", "PAY", "CANCEL", "BLOCK", "UNBLOCK", "FAVORITE_ADD", "FAVORITE_REMOVE");
    private static final List<String> OBJECTS = List.of("TOUR", "BOOKING", "USER", "HOTEL", "COUNTRY", "CITY",
            "DISCOUNT", "REVIEW", "PREFERENCES");

    private final User user;
    private TablePanel<AuditEntry> panel;
    private ComboBox<String> action;
    private ComboBox<String> object;
    private final DatePicker from = Ui.date(null);
    private final DatePicker to = Ui.date(null);

    /** @param user если задан — журнал только этого пользователя */
    public AuditView(User user) {
        this.user = user;
    }

    @Override
    public String getTitle() {
        return user == null ? t("menu.audit") : t("au.forUser", user.getFullName());
    }

    @Override
    protected Parent build() {
        panel = new TablePanel<>();
        action = Ui.combo(ACTIONS, a -> t("action." + a), t("au.allActions"));
        action.setPrefWidth(190);
        object = Ui.combo(OBJECTS, o -> t("object." + o), t("au.allObjects"));
        object.setPrefWidth(170);
        from.setPromptText(t("an.from"));
        to.setPromptText(t("an.to"));
        from.setPrefWidth(140);
        to.setPrefWidth(140);
        Button apply = new Button(t("common.apply"));
        apply.getStyleClass().add("btn-primary");
        apply.setMinWidth(javafx.scene.layout.Region.USE_PREF_SIZE);
        apply.setOnAction(e -> reload());
        action.setOnAction(e -> reload());
        object.setOnAction(e -> reload());
        panel.left(action, object, from, to, apply);

        var table = panel.table();
        table.getColumns().add(Ui.column(t("au.col.time"), a -> a.createdAt() == null ? "" : a.createdAt().format(TIME), 128));
        table.getColumns().add(Ui.column(t("au.col.user"),
                a -> a.userName() == null ? "—" : a.userName() + " (" + a.login() + ")", 175));
        table.getColumns().add(Ui.column(t("au.col.action"), a -> t("action." + a.action()), 130));
        table.getColumns().add(Ui.column(t("au.col.object"), a -> a.objectType() == null ? "—"
                : t("object." + a.objectType()) + (a.objectId() == null ? "" : " #" + a.objectId()), 130));
        table.getColumns().add(Ui.column(t("au.col.details"), a -> a.details() == null ? "" : a.details(), 300));

        VBox page = new VBox(20, pageHeader(getTitle(), t("au.subtitle")), panel);
        page.getStyleClass().add("page");
        reload();
        return page;
    }

    private void reload() {
        Async.<ArrayList<AuditEntry>>load(new Request(CommandType.GET_AUDIT)
                .with("userId", user == null ? null : user.getId())
                .with("action", action.getValue())
                .with("objectType", object.getValue())
                .with("from", from.getValue())
                .with("to", to.getValue()), panel::setItems);
    }
}
