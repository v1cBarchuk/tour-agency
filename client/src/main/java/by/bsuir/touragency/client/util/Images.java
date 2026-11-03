package by.bsuir.touragency.client.util;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Rectangle;

import java.io.InputStream;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Фотографии туров, отелей и стран. Ищутся в ресурсах клиента
 * (client/src/main/resources/ + путь из БД, например images/hotels/….jpg).
 * Если файла нет — рисуется аккуратная градиентная заглушка с подписью.
 */
public final class Images {

    private static final Map<String, Optional<Image>> CACHE = new ConcurrentHashMap<>();

    /** Пары цветов «море, закат, джунгли…» для заглушек. */
    private static final String[][] PALETTES = {
            {"#0f6e8c", "#38b2ac"}, {"#1e3a8a", "#3b82f6"}, {"#b45309", "#f59e0b"}, {"#047857", "#34d399"},
            {"#7c3aed", "#ec4899"}, {"#0e7490", "#67e8f9"}, {"#9a3412", "#fb923c"}, {"#1f2937", "#0ea5e9"},
    };

    private Images() {
    }

    /** Изображение из ресурсов или null, если файла нет. */
    public static Image raw(String path) {
        return load(path);
    }

    private static Image load(String path) {
        if (path == null || path.isBlank()) return null;
        return CACHE.computeIfAbsent(path, p -> {
            try (InputStream in = Images.class.getResourceAsStream("/" + p)) {
                return in == null ? Optional.<Image>empty() : Optional.of(new Image(in));
            } catch (Exception e) {
                return Optional.<Image>empty();
            }
        }).orElse(null);
    }

    /**
     * Изображение заданного размера с обрезкой по центру и скруглением сверху.
     *
     * @param caption подпись на заглушке (например, название отеля)
     */
    public static Node cover(String path, String caption, double width, double height, double radius) {
        Rectangle clip = new Rectangle(width, height);
        clip.setArcWidth(radius * 2);
        clip.setArcHeight(radius * 2);

        Image image = load(path);
        if (image != null) {
            ImageView view = new ImageView(image);
            double scale = Math.max(width / image.getWidth(), height / image.getHeight());
            double w = width / scale;
            double h = height / scale;
            view.setViewport(new javafx.geometry.Rectangle2D(
                    (image.getWidth() - w) / 2, (image.getHeight() - h) / 2, w, h));
            view.setFitWidth(width);
            view.setFitHeight(height);
            view.setSmooth(true);
            StackPane box = new StackPane(view);
            box.setClip(clip);
            box.setMinSize(width, height);
            box.setMaxSize(width, height);
            return box;
        }
        return placeholder(caption, path, width, height, clip);
    }

    private static Node placeholder(String caption, String path, double width, double height, Rectangle clip) {
        String seed = caption != null && !caption.isBlank() ? caption : String.valueOf(path);
        String[] colors = PALETTES[Math.floorMod(seed.hashCode(), PALETTES.length)];
        Rectangle bg = new Rectangle(width, height);
        bg.setFill(new LinearGradient(0, 0, 1, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web(colors[0])), new Stop(1, Color.web(colors[1]))));

        // «солнце» и «волны» — простая иллюстрация отдыха
        javafx.scene.shape.Circle sun = new javafx.scene.shape.Circle(height * 0.16, Color.web("#ffffff", 0.22));
        StackPane.setAlignment(sun, Pos.TOP_RIGHT);
        sun.setTranslateX(-width * 0.12);
        sun.setTranslateY(height * 0.14);
        Rectangle wave = new Rectangle(width, height * 0.28, Color.web("#ffffff", 0.10));
        StackPane.setAlignment(wave, Pos.BOTTOM_CENTER);

        Label text = new Label(caption == null ? "" : caption);
        text.getStyleClass().add("placeholder-caption");
        text.setWrapText(true);
        text.setMaxWidth(width - 32);

        StackPane box = new StackPane(bg, sun, wave, text);
        box.setMinSize(width, height);
        box.setMaxSize(width, height);
        box.setClip(clip);
        return box;
    }
}
