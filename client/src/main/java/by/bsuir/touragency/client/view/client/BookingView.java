package by.bsuir.touragency.client.view.client;

import by.bsuir.touragency.client.util.Alerts;
import by.bsuir.touragency.client.util.Async;
import by.bsuir.touragency.client.util.ClientContext;
import by.bsuir.touragency.client.util.Fmt;
import by.bsuir.touragency.client.util.Images;
import by.bsuir.touragency.client.util.Ui;
import by.bsuir.touragency.client.view.BaseView;
import by.bsuir.touragency.client.view.Shell;
import by.bsuir.touragency.common.model.BookingQuote;
import by.bsuir.touragency.common.model.Tour;
import by.bsuir.touragency.common.model.Tourist;
import by.bsuir.touragency.common.model.User;
import by.bsuir.touragency.common.protocol.CommandType;
import by.bsuir.touragency.common.protocol.Request;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Оформление бронирования: туристы, контакты и расчёт стоимости со скидкой.
 * Цена и скидка пересчитываются сервером при каждом изменении числа туристов.
 */
public class BookingView extends BaseView {

    private record TouristRow(TextField name, DatePicker birth) {
    }

    private final Shell shell;
    private final Tour tour;
    private final List<TouristRow> rows = new ArrayList<>();
    private Spinner<Integer> persons;
    private VBox touristsBox;
    private TextField phone;
    private TextField email;
    private Label base;
    private Label discount;
    private Label discountWhy;
    private Label total;
    private Label error;
    private Button submit;
    private TextField promo;
    private Label promoStatus;
    private String appliedPromo;

    private int initialPersons = 1;

    /** Бронирование сразу на выбранное на странице тура число туристов. */
    public BookingView(Shell shell, Tour tour, int persons) {
        this(shell, tour);
        this.initialPersons = Math.max(1, persons);
    }

    public BookingView(Shell shell, Tour tour) {
        this.shell = shell;
        this.tour = tour;
    }

    @Override
    public String getTitle() {
        return t("book.title");
    }

    @Override
    protected Parent build() {
        User user = ClientContext.getUser();
        // сводка тура
        Label hotel = new Label(tour.getHotelName() + "  " + Fmt.stars(tour.getHotelStars()));
        hotel.getStyleClass().add("card-title");
        Label place = new Label(tour.getLocation());
        place.getStyleClass().add("card-overline");
        Label dates = new Label(Fmt.dateRange(tour.getStartDate(), tour.getEndDate()) + " · "
                + t("common.nights", tour.getNights()) + " · " + tour.getMealTypeName());
        dates.getStyleClass().add("card-info");
        HBox summary = new HBox(18, Images.cover(tour.getImagePath(), tour.getHotelName(), 220, 130, 12),
                new VBox(6, place, hotel, dates));
        summary.getStyleClass().addAll("card", "book-summary");

        // туристы
        int maxPersons = Math.max(1, Math.min(9, tour.getFreeSeats()));
        persons = new Spinner<>(1, maxPersons, Math.min(initialPersons, maxPersons));
        persons.setEditable(true);
        persons.valueProperty().addListener((o, a, b) -> {
            rebuildTourists(b);
            requestQuote();
        });
        touristsBox = new VBox(10);
        Label touristsTitle = new Label(t("book.tourists"));
        touristsTitle.getStyleClass().add("section-title");
        Label seats = new Label(t("common.seatsFree", tour.getFreeSeats()));
        seats.getStyleClass().add(tour.getFreeSeats() <= 3 ? "seats-low" : "muted");

        // контакты
        phone = new TextField(user.getPhone() == null ? "" : user.getPhone());
        phone.setPromptText("+375 29 123-45-67");
        email = new TextField(user.getEmail());
        Label contactsTitle = new Label(t("book.contacts"));
        contactsTitle.getStyleClass().add("section-title");
        HBox contacts = Ui.row(12, Ui.field(t("book.phone"), phone), Ui.field(t("book.email"), email));
        contacts.getChildren().forEach(n -> HBox.setHgrow(n, Priority.ALWAYS));

        VBox form = new VBox(16, summary,
                Ui.row(16, touristsTitle, Ui.hSpacer(), seats),
                Ui.row(12, new Label(t("book.persons")), persons), touristsBox,
                contactsTitle, contacts);
        HBox.setHgrow(form, Priority.ALWAYS);
        form.setMaxWidth(960);

        // стоимость
        base = new Label("—");
        discount = new Label("—");
        discountWhy = new Label();
        discountWhy.setWrapText(true);
        discountWhy.getStyleClass().add("discount-why");
        total = new Label("—");
        total.getStyleClass().add("price-large");
        error = new Label();
        error.getStyleClass().add("error");
        error.setWrapText(true);
        submit = new Button(t("book.submit"));
        submit.getStyleClass().addAll("btn-accent", "btn-large");
        submit.setMaxWidth(Double.MAX_VALUE);
        submit.setOnAction(e -> submit());
        base.setWrapText(true);
        base.getStyleClass().add("fact-value");
        discount.setWrapText(true);
        VBox prices = new VBox(10, line(t("book.base"), base), line(t("book.discount"), discount));
        Label totalLabel = new Label(t("book.total"));
        totalLabel.getStyleClass().add("muted");
        Label caption = new Label(t("details.price"));
        caption.getStyleClass().add("section-title");
        // промокод: проверяется сервером при расчёте цены (стратегия PROMO_CODE)
        promo = new TextField();
        promo.setPromptText(t("promo.prompt"));
        HBox.setHgrow(promo, Priority.ALWAYS);
        Button applyPromo = new Button(t("promo.apply"));
        applyPromo.getStyleClass().add("btn-outline");
        applyPromo.setMinWidth(javafx.scene.layout.Region.USE_PREF_SIZE);
        applyPromo.setOnAction(e -> {
            String code = promo.getText().trim();
            promoStatus.setText("");
            promoStatus.getStyleClass().setAll("label");
            appliedPromo = code.isEmpty() ? null : code;
            requestQuote();
        });
        promo.setOnAction(e -> applyPromo.fire());
        promoStatus = new Label();
        promoStatus.setWrapText(true);
        Label promoLabel = new Label(t("promo.title"));
        promoLabel.getStyleClass().add("field-label");
        VBox promoBox = new VBox(6, promoLabel, Ui.row(8, promo, applyPromo), promoStatus);

        VBox priceCard = new VBox(12, caption, prices, discountWhy, promoBox, totalLabel, total, submit, error);
        priceCard.getStyleClass().addAll("card", "price-card");
        priceCard.setPrefWidth(340);
        priceCard.setMinWidth(300);
        priceCard.setMaxWidth(360);

        rebuildTourists(persons.getValue());
        requestQuote();
        HBox layout = new HBox(24, form, new VBox(priceCard));
        layout.setAlignment(Pos.TOP_LEFT);
        VBox page = new VBox(20, pageHeader(t("book.title"), tour.getTitle()), layout);
        page.getStyleClass().add("page");
        return page;
    }

    private static VBox line(String label, Label value) {
        Label l = new Label(label);
        l.getStyleClass().add("field-label");
        return new VBox(2, l, value);
    }

    private void rebuildTourists(int count) {
        while (rows.size() < count) {
            int n = rows.size() + 1;
            TextField name = new TextField();
            name.setPromptText(t("book.fullName"));
            if (n == 1) name.setText(ClientContext.getUser().getFullName());
            DatePicker birth = Ui.date(null);
            birth.setPromptText(t("book.birthDate"));
            rows.add(new TouristRow(name, birth));
        }
        while (rows.size() > count) {
            rows.remove(rows.size() - 1);
        }
        touristsBox.getChildren().clear();
        for (int i = 0; i < rows.size(); i++) {
            TouristRow r = rows.get(i);
            Label no = new Label(String.valueOf(i + 1));
            no.getStyleClass().add("tourist-no");
            HBox.setHgrow(r.name(), Priority.ALWAYS);
            r.birth().setPrefWidth(170);
            HBox row = Ui.row(10, no, r.name(), r.birth());
            touristsBox.getChildren().add(row);
        }
    }

    private void requestQuote() {
        Request rq = new Request(CommandType.QUOTE_BOOKING).with("tourId", tour.getId())
                .with("persons", persons.getValue());
        if (appliedPromo != null) rq.with("promoCode", appliedPromo);
        Async.send(rq, response -> {
            if (!response.isOk() && appliedPromo != null) {
                // неверный промокод: сообщаем и пересчитываем цену без него
                promoStatus.getStyleClass().setAll("label", "error");
                promoStatus.setText(response.getMessage());
                appliedPromo = null;
                requestQuote();
                return;
            }
            if (!response.isOk()) {
                error.setText(response.getMessage());
                submit.setDisable(true);
                return;
            }
            BookingQuote q = response.getData();
            error.setText("");
            submit.setDisable(false);
            base.setText(Fmt.money(q.basePrice()) + "  =  " + Fmt.money(q.pricePerPerson()) + " × " + q.persons());
            if (q.discountId() == null) {
                discount.setText("—");
            } else {
                discount.setText("−" + Fmt.money(q.discountAmount()) + "  ·  " + q.discountName() + ", "
                        + q.discountPercent().stripTrailingZeros().toPlainString() + "%");
                discount.getStyleClass().add("discount-amount");
            }
            discountWhy.setText(explain(q));
            if (appliedPromo != null) {
                boolean used = "discount.why.PROMO_CODE".equals(q.explanationKey());
                promoStatus.getStyleClass().setAll("label", used ? "reason-ok" : "muted");
                promoStatus.setText(used ? t("promo.applied", appliedPromo.toUpperCase()) : t("promo.notBest"));
            } else if (!promoStatus.getStyleClass().contains("error")) {
                promoStatus.setText("");
            }
            total.setText(Fmt.money(q.total()));
        });
    }

    private static String explain(BookingQuote q) {
        String[] a = q.explanationArgs();
        Object[] args = new Object[a.length];
        for (int i = 0; i < a.length; i++) {
            args[i] = a[i].matches("\\d{4}-\\d{2}-\\d{2}") ? Fmt.date(LocalDate.parse(a[i])) : a[i];
        }
        return t(q.explanationKey(), args);
    }

    private void submit() {
        error.setText("");
        ArrayList<Tourist> tourists = new ArrayList<>();
        for (int i = 0; i < rows.size(); i++) {
            TouristRow r = rows.get(i);
            tourists.add(new Tourist(i + 1, r.name().getText().trim(), r.birth().getValue()));
        }
        submit.setDisable(true);
        Async.send(new Request(CommandType.CREATE_BOOKING).with("tourId", tour.getId()).with("tourists", tourists)
                .with("phone", phone.getText()).with("email", email.getText())
                .with("promoCode", appliedPromo), response -> {
            submit.setDisable(false);
            if (response.isOk()) {
                Alerts.info(t("book.done.title"), t("book.done.text"));
                shell.openSection("menu.bookings");
            } else {
                error.setText(response.getMessage());
            }
        }, ex -> {
            submit.setDisable(false);
            error.setText(ex.getMessage());
        });
    }
}
