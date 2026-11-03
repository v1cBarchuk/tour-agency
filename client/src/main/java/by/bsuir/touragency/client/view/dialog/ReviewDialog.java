package by.bsuir.touragency.client.view.dialog;

import by.bsuir.touragency.client.util.I18n;
import by.bsuir.touragency.client.util.Ui;
import by.bsuir.touragency.common.exception.ValidationException;
import by.bsuir.touragency.common.model.Booking;
import by.bsuir.touragency.common.protocol.CommandType;
import by.bsuir.touragency.common.protocol.Request;
import javafx.scene.control.TextArea;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;

/** Отзыв о завершённой поездке: оценка 1–5 звёзд и текст. */
public class ReviewDialog extends EditDialog<Integer> {

    private final Booking booking;
    private final ToggleGroup stars = new ToggleGroup();
    private final TextArea text = new TextArea();

    public ReviewDialog(Booking booking) {
        super(I18n.t("review.title", booking.getHotelName()));
        this.booking = booking;
        HBox row = new HBox(6);
        for (int i = 1; i <= 5; i++) {
            ToggleButton star = new ToggleButton("★ " + i);
            star.setUserData(i);
            star.setToggleGroup(stars);
            star.getStyleClass().add("choice-chip");
            row.getChildren().add(star);
            if (i == 5) star.setSelected(true);
        }
        text.setPromptText(I18n.t("review.prompt"));
        text.setWrapText(true);
        text.setPrefRowCount(5);
        add(Ui.field(I18n.t("review.rating"), row), 0, 0, 2);
        add(Ui.field(I18n.t("review.text"), text), 0, 1, 2);
    }

    @Override
    protected Integer collect() throws ValidationException {
        if (stars.getSelectedToggle() == null) {
            throw new ValidationException("review.chooseRating");
        }
        return (Integer) stars.getSelectedToggle().getUserData();
    }

    @Override
    protected Request saveRequest(Integer rating) {
        return new Request(CommandType.ADD_REVIEW).with("bookingId", booking.getId()).with("rating", rating)
                .with("comment", text.getText());
    }
}
