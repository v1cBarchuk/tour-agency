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
                    new Section("menu.tours", "✈", () -> new ToursView(shell, null)),
                    new Section("menu.compare", "⇄", () -> new by.bsuir.touragency.client.view.client.CompareView(shell))));
            groups.put("menu.section.cabinet", List.of(
                    new Section("menu.account", "☺", () -> new by.bsuir.touragency.client.view.client.ProfileView(shell)),
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
        // кнопка сворачивания меню до узкой панели со значками
        Button collapse = new Button("☰");
        collapse.getStyleClass().add("sidebar-toggle");
        collapse.setTooltip(new javafx.scene.control.Tooltip(t("menu.collapse")));
        Region logoSpace = new Region();
        HBox.setHgrow(logoSpace, Priority.ALWAYS);
        HBox logoRow = new HBox(6, logo, logoSpace, collapse);
        logoRow.setAlignment(Pos.CENTER_LEFT);
        VBox sidebar = new VBox(2, new VBox(0, logoRow, tagline));
        sidebar.getStyleClass().add("sidebar");
        java.util.List<Node> wideOnly = new java.util.ArrayList<>(List.of(logo, tagline));

        groups.forEach((groupKey, sections) -> {
            Label group = new Label(t(groupKey).toUpperCase(I18n.getLang().getLocale()));
            group.getStyleClass().add("menu-group");
            sidebar.getChildren().add(group);
            for (Section s : sections) {
                Label icon = new Label(s.icon());
                icon.getStyleClass().add("menu-icon");
                Label text = new Label(t(s.key()));
                text.getStyleClass().add("menu-text");
                Region gap = new Region();
                HBox.setHgrow(gap, Priority.ALWAYS);
                Label badge = new Label();
                badge.getStyleClass().add("menu-badge");
                badge.setVisible(false);
                badge.setManaged(false);
                HBox row = new HBox(12, icon, text, gap, badge);
                row.setAlignment(Pos.CENTER_LEFT);
                Button item = new Button();
                item.setGraphic(row);
                item.setContentDisplay(javafx.scene.control.ContentDisplay.GRAPHIC_ONLY);
                item.getStyleClass().add("menu-item");
                item.setMaxWidth(Double.MAX_VALUE);
                row.prefWidthProperty().bind(item.widthProperty().subtract(24));
                item.setTooltip(new javafx.scene.control.Tooltip(t(s.key())));
                item.setOnAction(e -> openSection(s.key()));
                menuButtons.put(s.key(), item);
                menuBadges.put(s.key(), badge);
                menuTexts.put(s.key(), text);
                sidebar.getChildren().add(item);
            }
        });
        Region grow = new Region();
        VBox.setVgrow(grow, Priority.ALWAYS);
        Label who = new Label(user.getFullName());
        who.getStyleClass().add("sidebar-user");
        Label role = new Label(t("role." + user.getRole().name()));
        role.getStyleClass().add("sidebar-role");
        VBox userBox = new VBox(0, who, role);
        wideOnly.add(userBox);
        sidebar.getChildren().addAll(grow, userBox);
        collapse.setOnAction(e -> {
            boolean now = !sidebar.getStyleClass().contains("sidebar-collapsed");
            setSidebarCollapsed(sidebar, wideOnly, now);
            try {
                java.util.prefs.Preferences.userNodeForPackage(MainLayout.class).putBoolean("sidebar.collapsed", now);
            } catch (Exception ignored) {
                // состояние меню просто не запомнится
            }
        });
        setSidebarCollapsed(sidebar, wideOnly,
                java.util.prefs.Preferences.userNodeForPackage(MainLayout.class).getBoolean("sidebar.collapsed", false));
        // счётчики «Избранное» и «Сравнение»
        by.bsuir.touragency.client.util.FavoritesCount.property().addListener((o, a, b) -> setBadge("menu.favorites", b.intValue()));
        if (user.getRole() == Role.CLIENT) {
            Async.<java.util.ArrayList<by.bsuir.touragency.common.model.Tour>>quiet(new Request(CommandType.GET_FAVORITES),
                    list -> by.bsuir.touragency.client.util.FavoritesCount.set(list.size()));
        }
        setBadge("menu.favorites", by.bsuir.touragency.client.util.FavoritesCount.property().get());

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
        avatar.setCursor(javafx.scene.Cursor.HAND);
        javafx.scene.control.Tooltip.install(avatar, new javafx.scene.control.Tooltip(t("menu.account")));
        avatar.setOnMouseClicked(e -> {
            if (menuButtons.containsKey("menu.account")) {
                openSection("menu.account");
            } else {
                open(new by.bsuir.touragency.client.view.client.ProfileView(this));
            }
        });
        Button logout = new Button(t("common.logout"));
        logout.getStyleClass().add("btn-outline");
        logout.setOnAction(e -> logout());
        by.bsuir.touragency.client.component.CurrencySwitch currency =
                new by.bsuir.touragency.client.component.CurrencySwitch(this::rebuildPages);
        Async.<java.util.HashMap<String, java.math.BigDecimal>>quiet(new Request(CommandType.GET_RATES), rates -> {
            by.bsuir.touragency.client.util.Currency.setRates(rates);
            if (!by.bsuir.touragency.client.util.Currency.isBase()) rebuildPages();   // сохранённая валюта
        });
        HBox topbar = new HBox(14, backButton, pageTitle, spacer, new by.bsuir.touragency.client.component.ThemeSwitch(),
                currency, lang);
        if (user.getRole() == Role.CLIENT) {
            topbar.getChildren().add(bell());
        }
        topbar.getChildren().addAll(avatar, logout);
        topbar.setAlignment(Pos.CENTER_LEFT);
        topbar.getStyleClass().add("topbar");

        content = new StackPane();
        content.getStyleClass().add("content");
        compareBar = compareBar();

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
        bellTimer = new Timeline(new KeyFrame(Duration.seconds(10), e -> refreshBell()));
        bellTimer.setCycleCount(Animation.INDEFINITE);
        bellTimer.play();
        return bell;
    }

    /** Обновить счётчик непрочитанных уведомлений. */
    private long lastNotificationId = -1;   // -1 — первая загрузка, всплывающих сообщений нет

    public void refreshBell() {
        if (bellCount == null) return;
        Async.send(new Request(CommandType.GET_NOTIFICATIONS), r -> {
            if (!r.isOk()) return;
            java.util.ArrayList<Notification> list = r.getData();
            long maxId = list.stream().mapToLong(Notification::id).max().orElse(0);
            if (lastNotificationId >= 0) {
                list.stream().filter(n -> !n.read() && n.id() > lastNotificationId)
                        .limit(2).forEach(n -> by.bsuir.touragency.client.util.Alerts.toast(Texts.notification(n)));
            }
            lastNotificationId = Math.max(lastNotificationId, maxId);
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
    public void refresh() {
        if (stack.size() == 1 && currentSection != null) {
            openSection(currentSection);            // раздел создаётся заново и загружает свежие данные
        } else if (!stack.isEmpty()) {
            BaseView page = stack.pop();
            stack.push(page.fresh());
            render();
        }
    }

    /** Перестроить все открытые страницы (цены в новой валюте), оставаясь на текущей. */
    private void rebuildPages() {
        stack.forEach(BaseView::invalidate);
        if (!stack.isEmpty()) render();
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
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);      // страница подстраивается по ширине
        scroll.getStyleClass().add("content-scroll");
        content.getChildren().setAll(scroll, compareBar);
        updateCompareBar();
        // плавное появление раздела: прозрачность и небольшой сдвиг снизу вверх (250 мс)
        node.setOpacity(0);
        node.setTranslateY(10);
        javafx.animation.FadeTransition fade = new javafx.animation.FadeTransition(javafx.util.Duration.millis(250), node);
        fade.setToValue(1);
        javafx.animation.TranslateTransition slide = new javafx.animation.TranslateTransition(javafx.util.Duration.millis(250), node);
        slide.setToY(0);
        new javafx.animation.ParallelTransition(fade, slide).play();
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

    // ------------------------------------------------------------ панель сравнения

    private HBox compareBar;
    private Label compareText;

    /** Плавающая панель внизу окна: сколько туров выбрано для сравнения и переход к сравнению. */
    private HBox compareBar() {
        compareText = new Label();
        compareText.getStyleClass().add("compare-bar-text");
        Button open = new Button(t("compare.open"));
        open.getStyleClass().add("btn-accent");
        open.setOnAction(e -> openSection("menu.compare"));
        Button clear = new Button(t("compare.clear"));
        clear.getStyleClass().add("btn-ghost");
        clear.setOnAction(e -> by.bsuir.touragency.client.util.CompareList.clear());
        HBox bar = new HBox(14, new Label("⇄"), compareText, open, clear);
        bar.getStyleClass().add("compare-bar");
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        StackPane.setAlignment(bar, Pos.BOTTOM_CENTER);
        StackPane.setMargin(bar, new Insets(0, 0, 22, 0));
        by.bsuir.touragency.client.util.CompareList.tours().addListener(
                (javafx.collections.ListChangeListener<by.bsuir.touragency.common.model.Tour>) c -> updateCompareBar());
        return bar;
    }

    private final Map<String, Label> menuBadges = new LinkedHashMap<>();
    private final Map<String, Label> menuTexts = new LinkedHashMap<>();

    /** Число рядом с пунктом меню; 0 — счётчик скрыт. */
    private void setBadge(String key, int count) {
        Label badge = menuBadges.get(key);
        if (badge == null) return;
        badge.setText(String.valueOf(count));
        badge.setVisible(count > 0);
        badge.setManaged(count > 0);
    }

    /** Узкое меню (72 px): только значки и счётчики, названия — во всплывающих подсказках. */
    private void setSidebarCollapsed(VBox sidebar, List<Node> wideOnly, boolean collapsed) {
        sidebar.getStyleClass().remove("sidebar-collapsed");
        if (collapsed) sidebar.getStyleClass().add("sidebar-collapsed");
        wideOnly.forEach(n -> {
            n.setVisible(!collapsed);
            n.setManaged(!collapsed);
        });
        menuTexts.values().forEach(l -> {
            l.setVisible(!collapsed);
            l.setManaged(!collapsed);
        });
        menuButtons.values().forEach(b -> {
            if (b.getGraphic() instanceof HBox row) {
                row.setSpacing(collapsed ? 3 : 12);
                row.setAlignment(collapsed ? Pos.CENTER : Pos.CENTER_LEFT);
                Node gap = row.getChildren().get(2);                  // распорка между текстом и счётчиком
                gap.setVisible(!collapsed);
                gap.setManaged(!collapsed);
            }
        });
    }

    private void updateCompareBar() {
        if (compareBar == null) return;
        int n = by.bsuir.touragency.client.util.CompareList.tours().size();
        boolean show = n > 0 && !"menu.compare".equals(currentSection);
        compareBar.setVisible(show);
        compareBar.setManaged(show);
        compareText.setText(t("compare.bar", n, by.bsuir.touragency.client.util.CompareList.MAX));
        setBadge("menu.compare", n);
    }

    private void logout() {
        by.bsuir.touragency.client.util.CompareList.clear();
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
