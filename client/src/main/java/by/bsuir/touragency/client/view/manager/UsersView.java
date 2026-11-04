package by.bsuir.touragency.client.view.manager;

import by.bsuir.touragency.client.component.Badge;
import by.bsuir.touragency.client.util.Alerts;
import by.bsuir.touragency.client.util.Async;
import by.bsuir.touragency.client.util.ClientContext;
import by.bsuir.touragency.client.util.I18n;
import by.bsuir.touragency.client.util.Ui;
import by.bsuir.touragency.client.view.BaseView;
import by.bsuir.touragency.client.view.Shell;
import by.bsuir.touragency.common.model.Role;
import by.bsuir.touragency.common.model.User;
import by.bsuir.touragency.common.protocol.CommandType;
import by.bsuir.touragency.common.protocol.Request;
import javafx.scene.Parent;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Пользователи (администратор): роли, блокировка, история действий. */
public class UsersView extends BaseView {

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("dd.MM.yy HH:mm");
    private final Shell shell;
    private TablePanel<User> panel;
    private ComboBox<Role> roleFilter;
    private TextField search;
    private List<User> all = new ArrayList<>();

    public UsersView(Shell shell) {
        this.shell = shell;
    }

    @Override
    public String getTitle() {
        return t("menu.users");
    }

    @Override
    protected Parent build() {
        panel = new TablePanel<>();
        roleFilter = Ui.combo(Arrays.asList(Role.values()), r -> t("role." + r.name()), t("common.all"));
        roleFilter.setPrefWidth(180);
        roleFilter.setOnAction(e -> filter());
        search = new TextField();
        search.setPromptText(t("us.search"));
        search.setPrefWidth(220);
        search.textProperty().addListener((o, a, b) -> filter());
        panel.left(roleFilter, search);
        panel.action(t("us.changeRole"), "btn-outline", this::changeRole);
        panel.action(t("us.toggleBlock"), "btn-outline", this::toggleBlock);
        panel.action(t("us.history"), "btn-primary", u -> shell.open(new AuditView(u)));

        var table = panel.table();
        table.getColumns().add(Ui.column(t("us.col.login"), User::getLogin, 100));
        table.getColumns().add(Ui.column(t("us.col.name"), User::getFullName, 170));
        table.getColumns().add(Ui.column(t("us.col.email"), User::getEmail, 175));
        table.getColumns().add(Ui.column(t("us.col.role"), u -> t("role." + u.getRole().name()), 125));
        TableColumn<User, Object> status = Ui.column(t("us.col.status"), u -> u, 140);
        status.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(Object item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty || item == null ? null : ((User) item).isBlocked()
                        ? new Badge(t("us.blocked"), Badge.Kind.DANGER)
                        : new Badge(t("us.active"), Badge.Kind.SUCCESS));
            }
        });
        table.getColumns().add(status);
        table.getColumns().add(Ui.column(t("us.col.bookings"), User::getBookingsCount, 70));
        table.getColumns().add(Ui.column(t("us.col.lastLogin"),
                u -> u.getLastLogin() == null ? "—" : u.getLastLogin().format(TIME), 130));

        VBox page = new VBox(20, pageHeader(t("us.title"), t("us.subtitle")), panel);
        page.getStyleClass().add("page");
        panel.setChangeKey(u -> u.getId() + ":" + u.getRole() + ":" + u.isBlocked() + ":" + u.getBookingsCount()
                + ":" + u.getLastLogin());
        reload();
        Ui.autoRefresh(page, 15, this::refreshQuietly);
        return page;
    }

    private void reload() {
        Async.<ArrayList<User>>load(new Request(CommandType.GET_USERS), users -> {
            all = users;
            filter();
        });
    }

    private void refreshQuietly() {
        Async.<ArrayList<User>>quiet(new Request(CommandType.GET_USERS), users -> {
            all = users;
            filter();
        });
    }

    private void filter() {
        String q = search.getText() == null ? "" : search.getText().trim().toLowerCase();
        Role r = roleFilter.getValue();
        panel.setItems(all.stream()
                .filter(u -> r == null || u.getRole() == r)
                .filter(u -> q.isEmpty() || u.getLogin().toLowerCase().contains(q)
                        || u.getFullName().toLowerCase().contains(q) || u.getEmail().toLowerCase().contains(q))
                .toList());
    }

    private boolean isSelf(User u) {
        if (u.getId().equals(ClientContext.getUser().getId())) {
            Alerts.info(t("us.self"));
            return true;
        }
        return false;
    }

    private void changeRole(User u) {
        if (isSelf(u)) return;
        List<String> labels = Arrays.stream(Role.values()).map(r -> t("role." + r.name())).toList();
        ChoiceDialog<String> dialog = new ChoiceDialog<>(t("role." + u.getRole().name()), labels);
        dialog.setTitle(t("us.changeRole"));
        dialog.setHeaderText(I18n.t("us.roleFor", u.getFullName()));
        dialog.setContentText(t("us.col.role"));
        Alerts.localize(dialog);
        dialog.showAndWait().ifPresent(label -> {
            Role role = Role.values()[labels.indexOf(label)];
            if (role == u.getRole()) return;
            send(new Request(CommandType.SET_USER_ROLE).with("userId", u.getId()).with("role", role.name()));
        });
    }

    private void toggleBlock(User u) {
        if (isSelf(u)) return;
        boolean block = !u.isBlocked();
        if (!Alerts.confirm(I18n.t(block ? "us.confirmBlock" : "us.confirmUnblock", u.getFullName()))) return;
        send(new Request(CommandType.SET_USER_BLOCKED).with("userId", u.getId()).with("blocked", block));
    }

    private void send(Request request) {
        Async.send(request, r -> {
            if (r.isOk()) {
                reload();
            } else {
                Alerts.error(r.getMessage());
            }
        });
    }
}
