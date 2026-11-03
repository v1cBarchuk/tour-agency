package by.bsuir.touragency.client.component;

import by.bsuir.touragency.client.util.Texts;
import by.bsuir.touragency.common.model.BookingStatus;
import by.bsuir.touragency.common.model.StatusChange;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Временная шкала статусов заявки: пройденные шаги — закрашены,
 * текущий — выделен, отменённая заявка показывает шаг «Отменена».
 */
public class StatusTimeline extends HBox {

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("dd.MM HH:mm");

    public StatusTimeline(BookingStatus current, List<StatusChange> history) {
        setAlignment(Pos.TOP_LEFT);
        getStyleClass().add("timeline");
        BookingStatus[] flow = BookingStatus.flow();
        int currentIndex = -1;
        for (int i = 0; i < flow.length; i++) {
            if (flow[i] == current) currentIndex = i;
        }
        // для отменённой заявки последним пройденным считаем последний статус до отмены
        if (current == BookingStatus.CANCELLED) {
            for (StatusChange h : history) {
                for (int i = 0; i < flow.length; i++) {
                    if (flow[i] == h.status()) currentIndex = Math.max(currentIndex, i);
                }
            }
        }
        for (int i = 0; i < flow.length; i++) {
            boolean done = i <= currentIndex;
            boolean isCurrent = i == currentIndex && current != BookingStatus.CANCELLED;
            getChildren().add(step(flow[i], done, isCurrent, history));
            if (i < flow.length - 1) {
                Region line = new Region();
                line.getStyleClass().addAll("timeline-line", i < currentIndex ? "done" : "todo");
                HBox.setHgrow(line, Priority.ALWAYS);
                line.setMinWidth(12);
                VBox wrap = new VBox(line);
                wrap.setAlignment(Pos.TOP_CENTER);
                wrap.setPadding(new javafx.geometry.Insets(11, 0, 0, 0));
                HBox.setHgrow(wrap, Priority.ALWAYS);
                getChildren().add(wrap);
            }
        }
        if (current == BookingStatus.CANCELLED) {
            getChildren().add(step(BookingStatus.CANCELLED, true, true, history));
        }
    }

    private static VBox step(BookingStatus status, boolean done, boolean current, List<StatusChange> history) {
        Label dot = new Label(done ? (status == BookingStatus.CANCELLED ? "✕" : "✓") : "");
        dot.getStyleClass().addAll("timeline-dot", done ? "done" : "todo");
        if (current) dot.getStyleClass().add("current");
        if (status == BookingStatus.CANCELLED) dot.getStyleClass().add("cancelled");
        Label name = new Label(Texts.status(status));
        name.getStyleClass().add(current ? "timeline-label-current" : "timeline-label");
        name.setWrapText(true);
        name.setMaxWidth(96);
        name.setAlignment(Pos.CENTER);
        name.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
        VBox box = new VBox(4, dot, name);
        history.stream().filter(h -> h.status() == status).reduce((a, b) -> b).ifPresent(h -> {
            Label when = new Label(h.changedAt() == null ? "" : h.changedAt().format(TIME));
            when.getStyleClass().add("timeline-time");
            box.getChildren().add(when);
        });
        box.setAlignment(Pos.TOP_CENTER);
        box.setMinWidth(80);
        box.setMaxWidth(100);
        return box;
    }
}
