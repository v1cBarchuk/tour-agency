package by.bsuir.touragency.client.view;

import by.bsuir.touragency.client.component.LangSwitch;
import by.bsuir.touragency.client.util.Alerts;
import by.bsuir.touragency.client.util.Async;
import by.bsuir.touragency.client.util.ClientContext;
import by.bsuir.touragency.client.util.I18n;
import by.bsuir.touragency.client.util.Texts;
import by.bsuir.touragency.client.view.client.DestinationsView;
import by.bsuir.touragency.client.view.client.FavoritesView;
import by.bsuir.touragency.client.view.client.MyBookingsView;
import by.bsuir.touragency.client.view.client.NotificationsView;
import by.bsuir.touragency.client.view.client.PreferencesView;
import by.bsuir.touragency.client.view.client.RecommendationsView;
import by.bsuir.touragency.client.view.manager.AnalyticsView;
import by.bsuir.touragency.client.view.manager.AuditView;
import by.bsuir.touragency.client.view.manager.BookingsManageView;
import by.bsuir.touragency.client.view.manager.DashboardView;
import by.bsuir.touragency.client.view.manager.UsersView;
import by.bsuir.touragency.common.model.Notification;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.util.Duration;
import by.bsuir.touragency.client.view.client.HomeView;
import by.bsuir.touragency.client.view.client.ToursView;
import by.bsuir.touragency.client.view.manager.DiscountsView;
import by.bsuir.touragency.client.view.manager.GeoView;
import by.bsuir.touragency.client.view.manager.ManageToursView;
import by.bsuir.touragency.common.model.Lang;
import by.bsuir.touragency.common.model.ReferenceData;
import by.bsuir.touragency.common.model.Role;
import by.bsuir.touragency.common.model.User;
import by.bsuir.touragency.common.protocol.CommandType;
import by.bsuir.touragency.common.protocol.Request;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Главное окно после входа: боковое меню (набор разделов зависит от роли),
 * верхняя панель и область содержимого со стеком страниц.
 */
public class MainLayout extends BaseView implements Shell {

    /** Пункт меню: ключ текста, значок и фабрика раздела. */
    private record Section(String key, String icon, Supplier<BaseView> factory) {
    }

    private final User user;
    private final String initialSection;
    private final Map<String, List<Section>> groups = new LinkedHashMap<>();
    private final Map<String, Button> menuButtons = new LinkedHashMap<>();
    private final Deque<BaseView> stack = new ArrayDeque<>();
    private String currentSection;
    private StackPane content;
    private Button backButton;
    private Label pageTitle;

    public MainLayout(User user, String initialSection) {
        this.user = user;
        this.initialSection = initialSection;
        defineSections();
    }

    @Override
    public String getTitle() {
        return t("role." + user.getRole().name());
    }

    @Override
    public BaseView fresh() {
        if (bellTimer != null) bellTimer.stop();
        return new MainLayout(user, currentSection);
    }

    private void defineSections() {
        Shell shell = this;
        if (user.getRole() == Role.CLIENT) {
            groups.put("menu.section.client", List.of(
                    new Section("menu.home", "⌂", () -> new HomeView(shell)),
                    new Section("menu.destinations", "◎", () -> new DestinationsView(shell)),
                    new Section("menu.tours", "✈", () -> new ToursView(shell, null))));
            groups.put("menu.section.cabinet", List.of(
                    new Section("menu.preferences", "✎", () -> new PreferencesView(shell)),
                    new Section("menu.recommendations", "★", () -> new RecommendationsView(shell)),
                    new Section("menu.bookings", "▤", () -> new MyBookingsView(shell)),
                    new Section("menu.favorites", "♥", () -> new FavoritesView(shell)),
                    new Section("menu.notifications", "✉", () -> new NotificationsView(shell, this::refreshBell))));
        } else {
            List<Section> manage = new ArrayList<>(List.of(
                    new Section("menu.dashboard", "▦", () -> new DashboardView(shell)),
                    new Section("menu.manageTours", "✈", () -> new ManageToursView(shell)),
                    new Section("menu.geo", "◎", () -> new GeoView()),
                    new Section("menu.discounts", "%", () -> new DiscountsView()),
                    new Section("menu.requests", "▤", () -> new BookingsManageView()),
                    new Section("menu.analytics", "▲", () -> new AnalyticsView())));
            groups.put("menu.section.manage", manage);
            if (user.getRole() == Role.ADMIN) {
                groups.put("menu.section.admin", List.of(
                        new Section("menu.users", "☺", () -> new UsersView(shell)),
                        new Section("menu.audit", "☰", () -> new AuditView(null))));
            }
        }
    }

    @Override
    protected Parent build() {
        // ---------- боковое меню
        Label logo = new Label(t("app.name"));
        logo.getStyleClass().add("logo");
        Label tagline = new Label(t("app.tagline"));
        tagline.getStyleClass().add("logo-tagline");
        tagline.setWrapText(true);
        VBox sidebar = new VBox(2, new VBox(0, logo, tagline));
        sidebar.getStyleClass().add("sidebar");

        groups.forEach((groupKey, sections) -> {
            Label group = new Label(t(groupKey).toUpperCase(I18n.getLang().getLocale()));
            group.getStyleClass().add("menu-group");
            sidebar.getChildren().add(group);
            for (Section s : sections) {
                Button item = new Button(s.icon() + "   " + t(s.key()));
                item.getStyleClass().add("menu-item");
                item.setMaxWidth(Double.MAX_VALUE);
                item.setOnAction(e -> openSection(s.key()));
                menuButtons.put(s.key(), item);
                sidebar.getChildren().add(item);
            }
        });
        Region grow = new Region();
        VBox.setVgrow(grow, Priority.ALWAYS);
        Label who = new Label(user.getFullName());
        who.getStyleClass().add("sidebar-user");
        Label role = new Label(t("role." + user.getRole().name()));
        role.getStyleClass().add("sidebar-role");
        sidebar.getChildren().addAll(grow, new VBox(0, who, role));

        // ---------- верхняя панель
        backButton = new Button("←  " + t("common.back"));
        backButton.getStyleClass().add("btn-ghost");
        backButton.setOnAction(e -> back());
        pageTitle = new Label();
        pageTitle.getStyleClass().add("topbar-title");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        LangSwitch lang = new LangSwitch(this::changeLanguage);
        Label avatar = new Label(initials(user.getFullName()));
        avatar.getStyleClass().add("avatar");
        Button logout = new Button(t("common.logout"));
        logout.getStyleClass().add("btn-outline");
        logout.setOnAction(e -> logout());
        HBox topbar = new HBox(14, backButton, pageTitle, spacer, lang);
        if (user.getRole() == Role.CLIENT) {
            topbar.getChildren().add(bell());
        }
        topbar.getChildren().addAll(avatar, logout);
        topbar.setAlignment(Pos.CENTER_LEFT);
        topbar.getStyleClass().add("topbar");

        content = new StackPane();
        content.getStyleClass().add("content");

        BorderPane main = new BorderPane();
        main.setTop(topbar);
        main.setCenter(content);

        BorderPane root = new BorderPane();
        root.setLeft(sidebar);
        root.setCenter(main);

        String first = initialSection != null && menuButtons.containsKey(initialSection)
                ? initialSection : menuButtons.keySet().iterator().next();
        openSection(first);
        return root;
    }

    // ------------------------------------------------------------ уведомления

    private Label bellCount;
    private ContextMenu bellMenu;
    private Timeline bellTimer;

    /** Колокольчик: счётчик непрочитанных и выпадающий список последних уведомлений. */
    private Node bell() {
        Label icon = new Label("✉");
        icon.getStyleClass().add("bell-icon");
        bellCount = new Label();
        bellCount.getStyleClass().add("bell-count");
        bellCount.setVisible(false);
        StackPane.setAlignment(bellCount, Pos.TOP_RIGHT);
        StackPane bell = new StackPane(icon, bellCount);
        bell.getStyleClass().add("bell");
        bell.setCursor(javafx.scene.Cursor.HAND);
        bellMenu = new ContextMenu();
        bell.setOnMouseClicked(e -> Async.<java.util.ArrayList<Notification>>load(
                new Request(CommandType.GET_NOTIFICATIONS), list -> {
                    bellMenu.getItems().clear();
                    list.stream().limit(5).forEach(n -> {
                        MenuItem item = new MenuItem((n.read() ? "   " : "●  ") + Texts.notification(n));
                        item.setOnAction(a -> openSection("menu.notifications"));
                        bellMenu.getItems().add(item);
                    });
                    if (list.isEmpty()) {
                        MenuItem none = new MenuItem(t("notif.empty"));
                        none.setDisable(true);
                        bellMenu.getItems().add(none);
                    }
                    bellMenu.getItems().add(new SeparatorMenuItem());
                    MenuItem all = new MenuItem(t("notif.all"));
                    all.setOnAction(a -> openSection("menu.notifications"));
                    bellMenu.getItems().add(all);
                    bellMenu.show(bell, javafx.geometry.Side.BOTTOM, -260, 6);
                }));
        refreshBell();
        bellTimer = new Timeline(new KeyFrame(Duration.seconds(30), e -> refreshBell()));
        bellTimer.setCycleCount(Animation.INDEFINITE);
        bellTimer.play();
        return bell;
    }

    /** Обновить счётчик непрочитанных уведомлений. */
    public void refreshBell() {
        if (bellCount == null) return;
        Async.send(new Request(CommandType.GET_NOTIFICATIONS), r -> {
            if (!r.isOk()) return;
            java.util.ArrayList<Notification> list = r.getData();
            long unread = list.stream().filter(n -> !n.read()).count();
            bellCount.setText(unread > 9 ? "9+" : String.valueOf(unread));
            bellCount.setVisible(unread > 0);
        }, ex -> { });
    }

    private static String initials(String fullName) {
        StringBuilder sb = new StringBuilder();
        for (String part : fullName.trim().split("\\s+")) {
            if (!part.isEmpty() && sb.length() < 2) sb.append(Character.toUpperCase(part.charAt(0)));
        }
        return sb.toString();
    }

    // ------------------------------------------------------------ Shell

    @Override
    public void openSection(String key) {
        Section section = groups.values().stream().flatMap(List::stream)
                .filter(s -> s.key().equals(key)).findFirst().orElse(null);
        if (section != null) {
            openSection(key, section.factory().get());
        }
    }

    @Override
    public void openSection(String key, BaseView page) {
        currentSection = key;
        menuButtons.forEach((k, b) -> b.getStyleClass().remove("menu-item-active"));
        Button active = menuButtons.get(key);
        if (active != null) active.getStyleClass().add("menu-item-active");
        stack.clear();
        stack.push(page);
        render();
    }

    @Override
    public void open(BaseView page) {
        stack.push(page);
        render();
    }

    @Override
    public void back() {
        if (stack.size() > 1) {
            stack.pop();
            render();
        }
    }

    private void render() {
        BaseView page = stack.peek();
        Node node = page.getRoot();
        ScrollPane scroll = new ScrollPane(node);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("content-scroll");
        content.getChildren().setAll(scroll);
        pageTitle.setText(page.getTitle());
        backButton.setVisible(stack.size() > 1);
        backButton.setManaged(stack.size() > 1);
        Navigator.getStage().setTitle(t("app.name") + " — " + page.getTitle());
    }

    // ------------------------------------------------------------ язык и выход

    /**
     * Смена языка: сохраняем выбор в профиле, перезагружаем справочники на новом языке
     * и только затем перестраиваем интерфейс.
     */
    private void changeLanguage(Lang lang) {
        Async.send(new Request(CommandType.SET_LANGUAGE).setLang(lang), r -> { });
        Async.send(new Request(CommandType.GET_REFERENCE_DATA).setLang(lang), response -> {
            if (response.isOk()) {
                ClientContext.setReference(response.<ReferenceData>getData());
                user.setLanguage(lang);
                I18n.setLang(lang);          // Navigator пересоздаст окно
            } else {
                Alerts.error(response.getMessage());
            }
        });
    }

    private void logout() {
        if (bellTimer != null) bellTimer.stop();
        Async.send(new Request(CommandType.LOGOUT), response -> {
            ClientContext.clear();
            Navigator.show(new LoginView());
        });
    }

    /** Отступы страницы по умолчанию для разделов. */
    public static Insets pagePadding() {
        return new Insets(28, 32, 32, 32);
    }
}
