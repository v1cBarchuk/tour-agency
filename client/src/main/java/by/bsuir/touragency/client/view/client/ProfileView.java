package by.bsuir.touragency.client.view.client;

import by.bsuir.touragency.client.component.CardGrid;
import by.bsuir.touragency.client.component.Carousel;
import by.bsuir.touragency.client.component.TourCard;
import by.bsuir.touragency.client.util.Alerts;
import by.bsuir.touragency.client.util.Async;
import by.bsuir.touragency.client.util.ClientContext;
import by.bsuir.touragency.client.util.Fmt;
import by.bsuir.touragency.client.util.Ui;
import by.bsuir.touragency.client.view.BaseView;
import by.bsuir.touragency.client.view.Shell;
import by.bsuir.touragency.common.model.ProfileStats;
import by.bsuir.touragency.common.model.Role;
import by.bsuir.touragency.common.model.Tour;
import by.bsuir.touragency.common.model.User;
import by.bsuir.touragency.common.protocol.CommandType;
import by.bsuir.touragency.common.protocol.Request;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Личный кабинет: карточка пользователя с уровнем лояльности, сводка поездок,
 * редактирование личных данных, смена пароля, быстрые переходы и недавно просмотренные туры.
 */
public class ProfileView extends BaseView {

    private final Shell shell;
    private final VBox page = new VBox(24);

    public ProfileView(Shell shell) {
        this.shell = shell;
    }

    @Override
    public String getTitle() {
        return t("menu.account");
    }

    @Override
    protected Parent build() {
        page.getStyleClass().add("page");
        page.getChildren().setAll(pageHeader(t("account.title"), t("account.subtitle")),
                by.bsuir.touragency.client.component.Skeleton.block(900, 140));
        Async.<HashMap<String, Serializable>>load(new Request(CommandType.GET_PROFILE), data ->
                show((User) data.get("user"), (ProfileStats) data.get("stats")));
        return page;
    }

    private void show(User user, ProfileStats stats) {
        boolean client = user.getRole() == Role.CLIENT;
        page.getChildren().setAll(pageHeader(t("account.title"), t("account.subtitle")), header(user, stats, client));
        if (client) {
            page.getChildren().add(statTiles(stats));
            page.getChildren().add(quickLinks());
        }
        CardGrid forms = new CardGrid(380, 18, 2);
        forms.getChildren().addAll(CardGrid.item(profileForm(user, client)), CardGrid.item(passwordForm()));
        page.getChildren().add(forms);
        if (client) {
            Label h = new Label(t("home.recent"));
            h.getStyleClass().add("section-title");
            Carousel recent = new Carousel(TourCard.WIDTH, 18);
            VBox box = new VBox(14, h, recent);
            box.setVisible(false);
            box.setManaged(false);
            page.getChildren().add(box);
            Async.<ArrayList<Tour>>quiet(new Request(CommandType.GET_RECENTLY_VIEWED), list -> {
                if (list.isEmpty()) return;
                list.forEach(x -> recent.add(new TourCard(x, y -> shell.open(new TourDetailsView(shell, y.getId())))));
                box.setVisible(true);
                box.setManaged(true);
            });
        }
    }

    /** Аватар с инициалами, имя, логин, дата регистрации и уровень постоянного клиента. */
    private Node header(User user, ProfileStats stats, boolean client) {
        Label avatar = new Label(initials(user.getFullName()));
        avatar.getStyleClass().add("profile-avatar");
        Label name = new Label(user.getFullName());
        name.getStyleClass().add("profile-name");
        Label meta = new Label(user.getLogin() + "  ·  " + t("role." + user.getRole().name())
                + (user.getCreatedAt() == null ? "" : "  ·  " + t("account.since", Fmt.date(user.getCreatedAt().toLocalDate()))));
        meta.getStyleClass().add("muted");
        VBox text = new VBox(6, name, meta);
        if (client) {
            int trips = stats.completedTrips();
            String level = trips >= 3 ? "gold" : trips >= 1 ? "regular" : "new";
            Label badge = new Label(t("account.level." + level));
            badge.getStyleClass().addAll("level-badge", "level-" + level);
            Label next = new Label(trips >= 3 ? t("account.level.max") : t("account.level.next", (trips >= 1 ? 3 : 1) - trips));
            next.getStyleClass().add("muted");
            HBox levelRow = new HBox(10, badge, next);
            levelRow.setAlignment(Pos.CENTER_LEFT);
            text.getChildren().add(levelRow);
        }
        HBox box = new HBox(20, avatar, text);
        box.setAlignment(Pos.CENTER_LEFT);
        box.getStyleClass().addAll("card", "profile-header");
        return box;
    }

    private Node statTiles(ProfileStats s) {
        CardGrid grid = new CardGrid(170, 14, 7);
        grid.getChildren().addAll(
                tile("▤", String.valueOf(s.bookings()), t("account.stat.bookings")),
                tile("✈", String.valueOf(s.upcoming()), t("account.stat.upcoming")),
                tile("✓", String.valueOf(s.completedTrips()), t("account.stat.completed")),
                tile("◎", String.valueOf(s.countries()), t("account.stat.countries")),
                tile("BYN", Fmt.money(s.totalSpent()), t("account.stat.spent")),
                tile("♥", String.valueOf(s.favorites()), t("account.stat.favorites")),
                tile("★", String.valueOf(s.reviews()), t("account.stat.reviews")));
        return grid;
    }

    private static Node tile(String icon, String value, String label) {
        Label i = new Label(icon);
        i.getStyleClass().add("kpi-icon");
        Label v = new Label(value);
        v.getStyleClass().add("stat-value");
        Label l = new Label(label);
        l.getStyleClass().add("muted");
        l.setWrapText(true);
        VBox box = new VBox(6, i, v, l);
        box.getStyleClass().addAll("card", "stat-tile", "grid-item");
        return box;
    }

    private Node quickLinks() {
        HBox row = new HBox(10,
                link("▤  " + t("menu.bookings"), "menu.bookings"),
                link("♥  " + t("menu.favorites"), "menu.favorites"),
                link("⇄  " + t("menu.compare"), "menu.compare"),
                link("✎  " + t("menu.preferences"), "menu.preferences"),
                link("✉  " + t("menu.notifications"), "menu.notifications"));
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private Button link(String text, String section) {
        Button b = new Button(text);
        b.getStyleClass().add("chip-button");
        b.setOnAction(e -> shell.openSection(section));
        return b;
    }

    private Node profileForm(User user, boolean client) {
        Label title = new Label(t("account.personal"));
        title.getStyleClass().add("section-title");
        TextField fullName = new TextField(user.getFullName());
        TextField email = new TextField(user.getEmail());
        TextField phone = new TextField(user.getPhone() == null ? "" : user.getPhone());
        phone.setPromptText("+375 29 123-45-67");
        DatePicker birth = Ui.date(user.getBirthDate());
        TextField city = new TextField(user.getHomeCity() == null ? "" : user.getHomeCity());
        city.setPromptText(t("account.homeCity.prompt"));
        CheckBox priceDrop = new CheckBox(t("account.notifyPriceDrop"));
        priceDrop.setSelected(user.isNotifyPriceDrop());
        priceDrop.setWrapText(true);
        Label status = new Label();
        status.setWrapText(true);
        Button save = new Button(t("common.save"));
        save.getStyleClass().add("btn-primary");
        save.setOnAction(e -> {
            User changes = new User();
            changes.setFullName(fullName.getText());
            changes.setEmail(email.getText());
            changes.setPhone(phone.getText());
            changes.setBirthDate(birth.getValue());
            changes.setHomeCity(city.getText());
            changes.setNotifyPriceDrop(priceDrop.isSelected());
            save.setDisable(true);
            Async.send(new Request(CommandType.SAVE_PROFILE).with("user", changes), r -> {
                save.setDisable(false);
                if (r.isOk()) {
                    User updated = r.getData();
                    ClientContext.getUser().setFullName(updated.getFullName());
                    ClientContext.getUser().setEmail(updated.getEmail());
                    ClientContext.getUser().setPhone(updated.getPhone());
                    status.getStyleClass().setAll("label", "reason-ok");
                    status.setText("✓ " + r.getMessage());
                    Alerts.toast(r.getMessage());
                } else {
                    status.getStyleClass().setAll("label", "error");
                    status.setText(r.getMessage());
                }
            });
        });
        HBox two = Ui.row(12, Ui.field(t("account.birthDate"), birth), Ui.field(t("account.homeCity"), city));
        two.getChildren().forEach(n -> HBox.setHgrow(n, Priority.ALWAYS));
        HBox contacts = Ui.row(12, Ui.field(t("book.email"), email), Ui.field(t("book.phone"), phone));
        contacts.getChildren().forEach(n -> HBox.setHgrow(n, Priority.ALWAYS));
        VBox box = new VBox(12, title, Ui.field(t("account.fullName"), fullName), contacts, two);
        if (client) box.getChildren().add(priceDrop);
        box.getChildren().addAll(status, save);
        box.getStyleClass().addAll("card", "form-card");
        return box;
    }

    private Node passwordForm() {
        Label title = new Label(t("account.security"));
        title.getStyleClass().add("section-title");
        Label hint = new Label(t("account.passwordHint"));
        hint.getStyleClass().add("muted");
        hint.setWrapText(true);
        PasswordField oldPass = new PasswordField();
        PasswordField newPass = new PasswordField();
        PasswordField repeat = new PasswordField();
        Label status = new Label();
        status.setWrapText(true);
        Button change = new Button(t("account.changePassword"));
        change.getStyleClass().add("btn-outline");
        change.setOnAction(e -> {
            if (!newPass.getText().equals(repeat.getText())) {
                status.getStyleClass().setAll("label", "error");
                status.setText(t("account.passwordsDiffer"));
                return;
            }
            Async.send(new Request(CommandType.CHANGE_PASSWORD).with("oldPassword", oldPass.getText())
                    .with("newPassword", newPass.getText()), r -> {
                status.getStyleClass().setAll("label", r.isOk() ? "reason-ok" : "error");
                status.setText(r.isOk() ? "✓ " + r.getMessage() : r.getMessage());
                if (r.isOk()) {
                    oldPass.clear();
                    newPass.clear();
                    repeat.clear();
                }
            });
        });
        VBox box = new VBox(12, title, hint, Ui.field(t("account.oldPassword"), oldPass),
                Ui.field(t("account.newPassword"), newPass), Ui.field(t("account.repeatPassword"), repeat), status, change);
        box.getStyleClass().addAll("card", "form-card");
        return box;
    }

    private static String initials(String name) {
        StringBuilder sb = new StringBuilder();
        for (String part : name.trim().split("\\s+")) {
            if (!part.isEmpty() && sb.length() < 2) sb.append(Character.toUpperCase(part.charAt(0)));
        }
        return sb.toString();
    }
}
