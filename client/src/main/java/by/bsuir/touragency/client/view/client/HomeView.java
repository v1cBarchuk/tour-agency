package by.bsuir.touragency.client.view.client;

import by.bsuir.touragency.client.component.CountryCard;
import by.bsuir.touragency.client.component.Carousel;
import by.bsuir.touragency.client.component.TourCard;
import by.bsuir.touragency.client.util.Async;
import by.bsuir.touragency.client.util.ClientContext;
import by.bsuir.touragency.client.util.Images;
import by.bsuir.touragency.client.util.Ui;
import by.bsuir.touragency.client.view.BaseView;
import by.bsuir.touragency.client.view.Shell;
import by.bsuir.touragency.common.model.Country;
import by.bsuir.touragency.common.model.Tour;
import by.bsuir.touragency.common.model.TourFilter;
import by.bsuir.touragency.common.protocol.CommandType;
import by.bsuir.touragency.common.protocol.Request;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;

/**
 * Главная страница клиента: большой баннер с поиском, популярные направления,
 * горящие и рекомендуемые туры, преимущества.
 */
public class HomeView extends BaseView {

    private final Shell shell;
    private by.bsuir.touragency.client.component.CardGrid destinations;
    private Carousel hot;
    private Carousel featured;
    private Carousel recent;
    private VBox recentSection;

    public HomeView(Shell shell) {
        this.shell = shell;
    }

    @Override
    public String getTitle() {
        return t("menu.home");
    }

    @Override
    protected Parent build() {
        destinations = new by.bsuir.touragency.client.component.CardGrid(240, 18, 4);
        for (int i = 0; i < 4; i++) {
            destinations.getChildren().add(by.bsuir.touragency.client.component.Skeleton.card(260, 150));
        }
        hot = carousel();
        featured = carousel();
        recent = new Carousel(TourCard.WIDTH, 18);          // секция появляется только когда есть данные
        boolean client = ClientContext.getUser() != null
                && ClientContext.getUser().getRole() == by.bsuir.touragency.common.model.Role.CLIENT;
        recentSection = section(t("home.recent"), recent, "menu.account");
        recentSection.setVisible(false);
        recentSection.setManaged(false);
        VBox page = new VBox(26,
                new VBox(14, hero(), quickTags()),
                section(t("home.popularDestinations"), destinations, "menu.destinations"),
                section(t("home.hotOffers"), hot, "menu.tours"),
                recentSection,
                new by.bsuir.touragency.client.component.AdBanner(),
                section(t("home.featured"), featured, "menu.tours"),
                whyUs());
        if (client) {
            Async.<ArrayList<Tour>>quiet(new Request(CommandType.GET_RECENTLY_VIEWED), list -> {
                if (list.isEmpty()) return;
                list.forEach(x -> recent.add(card(x)));
                recentSection.setVisible(true);
                recentSection.setManaged(true);
            });
        }
        page.getStyleClass().add("page");
        load();
        return page;
    }

    /** Высота баннера: форма поиска, быстрые теги и заголовок следующей секции видны без прокрутки. */
    private static final double HERO_HEIGHT = 360;

    private Node hero() {
        StackPane hero = new StackPane();
        hero.getStyleClass().add("hero");
        hero.setMinHeight(HERO_HEIGHT);
        hero.setPrefHeight(HERO_HEIGHT);
        // скругление 16 px по контуру баннера
        javafx.scene.shape.Rectangle clip = new javafx.scene.shape.Rectangle();
        clip.setArcWidth(32);
        clip.setArcHeight(32);
        clip.widthProperty().bind(hero.widthProperty());
        clip.heightProperty().bind(hero.heightProperty());
        hero.setClip(clip);
        var photo = Images.raw("images/hero/home.jpg");
        if (photo != null) {
            hero.getChildren().add(coverBackground(photo));
        } else {
            Region bg = new Region();
            bg.getStyleClass().add("hero-bg");
            hero.getChildren().add(bg);
        }
        Region shade = new Region();
        shade.getStyleClass().add("hero-shade");

        Label title = new Label(t("hero.title"));
        title.getStyleClass().addAll("hero-title", "hero-title-compact");
        title.setWrapText(true);
        Label subtitle = new Label(t("hero.subtitle"));
        subtitle.getStyleClass().add("hero-subtitle");
        subtitle.setWrapText(true);
        VBox text = new VBox(8, title, subtitle, searchPanel());
        text.setPadding(new Insets(28, 32, 26, 32));
        text.setAlignment(Pos.BOTTOM_LEFT);
        hero.getChildren().addAll(shade, text);
        return hero;
    }

    /**
     * Фон баннера как object-fit: cover — фото заполняет блок без искажений, лишнее обрезается
     * по центру. Сам фон не влияет на размер баннера (его высоту задаёт содержимое).
     */
    private static Region coverBackground(javafx.scene.image.Image image) {
        ImageView iv = new ImageView(image);
        iv.setSmooth(true);
        Region bg = new Region() {
            {
                getChildren().add(iv);
            }

            @Override
            protected void layoutChildren() {
                double w = getWidth();
                double h = getHeight();
                if (w <= 0 || h <= 0) return;
                double scale = Math.max(w / image.getWidth(), h / image.getHeight());
                double vw = w / scale;
                double vh = h / scale;
                iv.setViewport(new javafx.geometry.Rectangle2D((image.getWidth() - vw) / 2,
                        (image.getHeight() - vh) / 2, vw, vh));
                iv.setFitWidth(w);
                iv.setFitHeight(h);
                iv.relocate(0, 0);
            }

            @Override
            protected double computePrefWidth(double height) {
                return 0;
            }

            @Override
            protected double computePrefHeight(double width) {
                return 0;
            }
        };
        bg.setMinSize(0, 0);
        bg.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        return bg;
    }

    /**
     * Поисковая панель: страна, дата, ночи, туристы, бюджет → каталог с фильтром.
     * На широком экране — одна строка, на узком поля переносятся в две-три строки, а не обрезаются.
     */
    private Node searchPanel() {
        ComboBox<Country> country = Ui.combo(ClientContext.countries(), Country::getName, t("common.anyF"));
        DatePicker date = Ui.date(null);
        Spinner<Integer> nights = new Spinner<>(1, 21, 7);
        nights.setEditable(true);
        Spinner<Integer> persons = new Spinner<>(1, 9, 2);
        persons.setEditable(true);
        TextField budget = Ui.numberField("3000");
        Button find = new Button(t("home.find"));
        find.getStyleClass().addAll("btn-accent");
        find.setMinWidth(Region.USE_PREF_SIZE);
        find.setMaxWidth(Double.MAX_VALUE);
        find.setOnAction(e -> {
            TourFilter f = new TourFilter()
                    .setCountryId(country.getValue() == null ? null : country.getValue().getId())
                    .setDateFrom(date.getValue())
                    .setDateTo(date.getValue() == null ? null : date.getValue().plusDays(14))
                    .setMinNights(Math.max(1, nights.getValue() - 2))
                    .setMaxNights(nights.getValue() + 3)
                    .setPersons(persons.getValue())
                    .setMaxPrice(Ui.decimal(budget));
            ClientContext.setDesiredPersons(persons.getValue());
            shell.openSection("menu.tours", new ToursView(shell, f));
        });
        Node[] fields = {Ui.field(t("home.country"), country), Ui.field(t("home.date"), date),
                Ui.field(t("home.nights"), nights), Ui.field(t("home.persons"), persons),
                Ui.field(t("home.budget"), budget), new VBox(new Label(" "), find)};
        double[] weights = {1.6, 1.3, 0.8, 0.8, 1, 1};
        ((Label) ((VBox) fields[5]).getChildren().get(0)).getStyleClass().add("field-label");

        javafx.scene.layout.GridPane panel = new javafx.scene.layout.GridPane();
        panel.setHgap(10);
        panel.setVgap(10);
        panel.getStyleClass().addAll("search-panel", "search-compact");
        // раскладка по ширине: 6 колонок → 3 → 2
        Runnable relayout = () -> {
            double w = panel.getWidth();
            int cols = w <= 0 || w >= 900 ? 6 : w >= 560 ? 3 : 2;
            if (panel.getColumnConstraints().size() == cols && !panel.getChildren().isEmpty()) return;
            panel.getChildren().clear();
            panel.getColumnConstraints().clear();
            double total = 0;
            for (int i = 0; i < cols; i++) total += cols == 6 ? weights[i] : 1;
            for (int i = 0; i < cols; i++) {
                javafx.scene.layout.ColumnConstraints cc = new javafx.scene.layout.ColumnConstraints();
                cc.setPercentWidth(100 * (cols == 6 ? weights[i] : 1) / total);
                cc.setFillWidth(true);
                panel.getColumnConstraints().add(cc);
            }
            for (int i = 0; i < fields.length; i++) {
                ((Region) fields[i]).setMinWidth(0);
                ((Region) fields[i]).setMaxWidth(Double.MAX_VALUE);
                panel.add(fields[i], i % cols, i / cols);
            }
        };
        panel.widthProperty().addListener((o, a, b) -> javafx.application.Platform.runLater(relayout));
        relayout.run();
        for (Node f : fields) {
            if (f instanceof VBox v && v.getChildren().size() > 1 && v.getChildren().get(1) instanceof Region r) {
                r.setMinWidth(0);
                r.setPrefWidth(80);
            }
        }
        return panel;
    }

    /** Быстрые теги под баннером: один щелчок — каталог с готовым фильтром. */
    private Node quickTags() {
        FlowPane tags = new FlowPane(10, 10);
        tags.getStyleClass().add("quick-tags");
        tags.getChildren().add(tag("⚡  " + t("tag.hot"), new TourFilter().setOnlyHot(true)));
        Integer beach = typeId("BEACH");
        if (beach != null) tags.getChildren().add(tag("☀  " + t("tag.beach"), new TourFilter().setVacationTypeId(beach)));
        tags.getChildren().add(tag("✈  " + t("tag.visaFree"), new TourFilter().setOnlyVisaFree(true)));
        Integer ai = ClientContext.mealTypes().stream().filter(m -> "AI".equals(m.getCode())).map(m -> m.getId())
                .findFirst().orElse(null);
        if (ai != null) tags.getChildren().add(tag("✦  " + t("tag.allInclusive"), new TourFilter().setMealTypeId(ai)));
        Integer family = typeId("FAMILY");
        if (family != null) tags.getChildren().add(tag("☺  " + t("tag.family"), new TourFilter().setVacationTypeId(family)));
        Integer ski = typeId("SKI");
        if (ski != null) tags.getChildren().add(tag("❄  " + t("tag.ski"), new TourFilter().setVacationTypeId(ski)));
        Integer sights = typeId("SIGHTSEEING");
        if (sights != null) tags.getChildren().add(tag("⌂  " + t("tag.sightseeing"), new TourFilter().setVacationTypeId(sights)));
        tags.getChildren().add(tag("≤  " + t("tag.budget", by.bsuir.touragency.client.util.Fmt.money(new java.math.BigDecimal(2000))),
                new TourFilter().setMaxPrice(new java.math.BigDecimal(2000)).setSort(TourFilter.Sort.PRICE_ASC)));
        return tags;
    }

    private Node tag(String text, TourFilter filter) {
        Button b = new Button(text);
        b.getStyleClass().addAll("chip-button", "quick-tag");
        b.setOnAction(e -> shell.openSection("menu.tours", new ToursView(shell, filter)));
        return b;
    }

    private static Integer typeId(String code) {
        return ClientContext.vacationTypes().stream().filter(v -> code.equals(v.getCode())).map(v -> v.getId())
                .findFirst().orElse(null);
    }

    private VBox section(String title, Node body, String sectionKey) {
        Label h = new Label(title);
        h.getStyleClass().add("section-title");
        Hyperlink all = new Hyperlink(t("home.viewAll") + "  →");
        all.setOnAction(e -> shell.openSection(sectionKey));
        HBox header = Ui.row(8, h, Ui.hSpacer(), all);
        return new VBox(14, header, body);
    }

    private static Carousel carousel() {
        Carousel c = new Carousel(TourCard.WIDTH, 18);
        for (int i = 0; i < 4; i++) c.add(by.bsuir.touragency.client.component.Skeleton.card(TourCard.WIDTH, 170));
        return c;
    }

    private Node card(Tour tour) {
        return new TourCard(tour, x -> shell.open(new TourDetailsView(shell, x.getId())));
    }

    private Node whyUs() {
        Label h = new Label(t("home.whyUs"));
        h.getStyleClass().add("section-title");
        HBox cards = new HBox(18,
                why("◎", "home.why1"), why("BYN", "home.why2"), why("★", "home.why3"));
        return new VBox(14, h, cards);
    }

    private static Node why(String icon, String key) {
        Label i = new Label(icon);
        i.getStyleClass().add("why-icon");
        Label title = new Label(t(key + ".title"));
        title.getStyleClass().add("card-title");
        Label text = new Label(t(key + ".text"));
        text.setWrapText(true);
        text.getStyleClass().add("card-info");
        VBox card = new VBox(10, i, title, text);
        card.getStyleClass().addAll("card", "why-card");
        HBox.setHgrow(card, Priority.ALWAYS);
        card.setMaxWidth(Double.MAX_VALUE);
        return card;
    }

    private void load() {
        Async.<ArrayList<Country>>load(new Request(CommandType.GET_COUNTRIES), countries -> {
            destinations.getChildren().clear();
            countries.stream().filter(c -> c.getToursCount() > 0).limit(4).forEach(c -> destinations.getChildren()
                    .add(new CountryCard(c, 292, 200, x -> shell.open(new CountryView(shell, x)))));
        });
        Async.<ArrayList<Tour>>load(new Request(CommandType.GET_TOURS).with("filter", new TourFilter()), tours -> {
            // в «горящих» — туры с ближайшим вылетом, снижением цены или почти распроданные
            fill(hot, tours.stream().filter(x -> x.isHot() || x.getOldPrice() != null || x.isAlmostSoldOut())
                    .limit(12).toList());
            fill(featured, tours.stream().filter(Tour::isFeatured).limit(12).toList());
        });
    }

    private void fill(Carousel carousel, List<Tour> tours) {
        carousel.clear();
        if (tours.isEmpty()) {
            Label none = new Label(t("common.empty"));
            none.getStyleClass().add("muted");
            carousel.add(none);
        }
        tours.forEach(x -> carousel.add(card(x)));
    }
}
