package dev.edinho.pdi.challenges;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;

public final class Ordering {
    private Ordering() {
    }

    public static <T> List<T> readingOrder(List<T> items, Function<T, Region> region) {
        List<T> sorted = new ArrayList<>(items);
        sorted.sort(Comparator.comparingDouble(t -> region.apply(t).centroidY()));
        List<List<T>> rows = new ArrayList<>();
        for (T item : sorted) {
            Region r = region.apply(item);
            List<T> row = rows.isEmpty() ? null : rows.getLast();
            if (row != null) {
                Region first = region.apply(row.getFirst());
                double tolerance = Math.min(first.height(), r.height()) / 2.0;
                if (Math.abs(r.centroidY() - first.centroidY()) > tolerance) row = null;
            }
            if (row == null) {
                row = new ArrayList<>();
                rows.add(row);
            }
            row.add(item);
        }
        List<T> out = new ArrayList<>();
        for (List<T> row : rows) {
            row.sort(Comparator.comparingDouble(t -> region.apply(t).centroidX()));
            out.addAll(row);
        }
        return out;
    }
}
