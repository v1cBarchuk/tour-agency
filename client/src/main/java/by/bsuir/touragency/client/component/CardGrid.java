package by.bsuir.touragency.client.component;

import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.layout.Pane;

import java.util.ArrayList;
import java.util.List;

/**
 * Адаптивная сетка карточек: число колонок зависит от ширины окна,
 * карточки растягиваются так, чтобы ряд занимал всю ширину без пустого места справа.
 * Узлы без класса «grid-item» (пустое состояние, подпись) занимают целую строку.
 */
public class CardGrid extends Pane {

    private final double minColumn;
    private final double gap;
    private final int maxColumns;

    public CardGrid(double minColumn, double gap) {
        this(minColumn, gap, 6);
    }

    public CardGrid(double minColumn, double gap, int maxColumns) {
        this.minColumn = minColumn;
        this.gap = gap;
        this.maxColumns = maxColumns;
        getStyleClass().add("card-grid");
    }

    /** Пометить узел как обычную ячейку сетки. */
    public static <T extends Node> T item(T node) {
        if (!node.getStyleClass().contains("grid-item")) node.getStyleClass().add("grid-item");
        return node;
    }

    private int columns(double width) {
        int cols = (int) Math.floor((width + gap) / (minColumn + gap));
        return Math.max(1, Math.min(maxColumns, cols));
    }

    private double columnWidth(double width, int cols) {
        return (width - gap * (cols - 1)) / cols;
    }

    /** Раскладывает узлы по строкам; при apply = true ещё и расставляет их. */
    private double place(double width, boolean apply) {
        Insets in = getInsets();
        double inner = Math.max(minColumn, width - in.getLeft() - in.getRight());
        int cols = columns(inner);
        double colW = columnWidth(inner, cols);
        double y = in.getTop();
        List<Node> row = new ArrayList<>();
        List<Node> managed = getManagedChildren();
        for (int i = 0; i <= managed.size(); i++) {
            Node n = i < managed.size() ? managed.get(i) : null;
            boolean full = n != null && !n.getStyleClass().contains("grid-item");
            if (n == null || full || row.size() == cols) {
                if (!row.isEmpty()) {
                    double h = 0;
                    for (Node r : row) h = Math.max(h, r.prefHeight(colW));
                    for (int k = 0; k < row.size(); k++) {
                        if (apply) row.get(k).resizeRelocate(in.getLeft() + k * (colW + gap), y, colW, h);
                    }
                    y += h + gap;
                    row.clear();
                }
            }
            if (n == null) break;
            if (full) {
                double h = n.prefHeight(inner);
                if (apply) n.resizeRelocate(in.getLeft(), y, inner, h);
                y += h + gap;
            } else {
                row.add(n);
            }
        }
        return (managed.isEmpty() ? y : y - gap) + in.getBottom();
    }

    @Override
    protected void layoutChildren() {
        place(getWidth(), true);
    }

    @Override
    protected double computePrefHeight(double width) {
        return place(width > 0 ? width : (getWidth() > 0 ? getWidth() : minColumn * 3), false);
    }

    @Override
    protected double computeMinWidth(double height) {
        return minColumn + getInsets().getLeft() + getInsets().getRight();
    }

    @Override
    protected double computePrefWidth(double height) {
        return minColumn * 3 + gap * 2;
    }
}
