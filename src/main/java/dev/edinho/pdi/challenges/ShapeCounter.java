package dev.edinho.pdi.challenges;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Locale;

public final class ShapeCounter {
    private static final int DARK_LIMIT = 90;
    private static final int COLOR_TOLERANCE = 60;
    private static final int FLAT_TOLERANCE = 12;
    private static final double MIN_AREA_FRACTION = 0.001;
    private static final double MAX_ASPECT = 1.15;
    private static final double MIN_SQUARE_FILL = 0.88;
    private static final double MIN_CIRCLE_FILL = 0.70;
    private static final double MAX_CIRCLE_FILL = 0.86;
    private static final double MIN_CIRCULARITY = 0.80;

    public enum Kind {
        CIRCLE("Círculo"),
        SQUARE("Quadrado");

        private final String label;

        Kind(String label) {
            this.label = label;
        }

        public String label() {
            return label;
        }
    }

    public record Shape(int number, Kind kind, int area, double perimeter, Region region) {
    }

    public record Result(List<Shape> shapes, BufferedImage annotated) {
        public long count(Kind kind) {
            return shapes.stream().filter(s -> s.kind() == kind).count();
        }

        public String report() {
            StringBuilder sb = new StringBuilder();
            sb.append("Círculos: ").append(count(Kind.CIRCLE))
                    .append(" | Quadrados: ").append(count(Kind.SQUARE)).append("\n\n");
            sb.append(String.format(Locale.ROOT, "%-4s %-10s %10s %11s%n", "#", "Forma", "Área (px)", "Perím. (px)"));
            for (Shape s : shapes) {
                sb.append(String.format(Locale.ROOT, "%-4d %-10s %10d %11.1f%n",
                        s.number(), s.kind().label(), s.area(), s.perimeter()));
            }
            return sb.toString();
        }
    }

    private ShapeCounter() {
    }

    public static Result count(BufferedImage original) {
        WorkingImage work = WorkingImage.of(original);
        BufferedImage image = work.image();
        int w = image.getWidth();
        int h = image.getHeight();
        int[][] labels = new int[h][w];
        int regions = colorRegions(image, labels);

        int minArea = (int) (MIN_AREA_FRACTION * w * h);
        List<Region[]> found = new ArrayList<>();
        for (Region r : Region.fromLabelMap(labels, regions)) {
            if (r.area() < minArea || r.touchesBorder(w, h)) continue;
            Region shape = r.filled();
            Kind kind = classify(shape);
            if (kind != null) found.add(new Region[]{shape, kind == Kind.CIRCLE ? null : shape});
        }

        List<Region[]> ordered = Ordering.readingOrder(found, r -> r[0]);
        List<Shape> shapes = new ArrayList<>();
        Annotator annotator = new Annotator(image);
        int number = 1;
        for (Region[] r : ordered) {
            Kind kind = r[1] == null ? Kind.CIRCLE : Kind.SQUARE;
            Region shape = r[0];
            shapes.add(new Shape(number, kind, work.area(shape.area()), work.length(shape.perimeter()), shape));
            Color color = kind == Kind.CIRCLE ? new Color(255, 0, 255) : new Color(0, 230, 255);
            annotator.outline(shape, color, 5);
            double labelY = kind == Kind.CIRCLE ? shape.centroidY() : shape.minY() + shape.height() * 0.12;
            annotator.label((kind == Kind.CIRCLE ? "C" : "Q") + number, shape.centroidX(), labelY, color);
            number++;
        }
        return new Result(shapes, annotator.image());
    }

    static Kind classify(Region shape) {
        double[] rect = shape.minAreaRectangle();
        double aspect = rect[0] / rect[1];
        if (aspect > MAX_ASPECT) return null;
        double fill = shape.area() / (rect[0] * rect[1]);
        if (fill >= MIN_SQUARE_FILL) return Kind.SQUARE;
        if (fill >= MIN_CIRCLE_FILL && fill <= MAX_CIRCLE_FILL && shape.circularity() >= MIN_CIRCULARITY) {
            return Kind.CIRCLE;
        }
        return null;
    }

    private static int colorRegions(BufferedImage image, int[][] labels) {
        int w = image.getWidth();
        int h = image.getHeight();
        int[][] rgb = new int[h][w];
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int c = image.getRGB(x, y) & 0xFFFFFF;
                rgb[y][x] = c;
                if (isDark(c)) labels[y][x] = -1;
            }
        }
        int[] dx = {1, -1, 0, 0};
        int[] dy = {0, 0, 1, -1};
        int next = 0;
        Deque<int[]> stack = new ArrayDeque<>();
        for (int sy = 0; sy < h; sy++) {
            for (int sx = 0; sx < w; sx++) {
                if (labels[sy][sx] != 0 || !isFlat(rgb, sx, sy)) continue;
                int label = ++next;
                int seed = rgb[sy][sx];
                labels[sy][sx] = label;
                stack.push(new int[]{sx, sy});
                while (!stack.isEmpty()) {
                    int[] p = stack.pop();
                    for (int i = 0; i < 4; i++) {
                        int nx = p[0] + dx[i];
                        int ny = p[1] + dy[i];
                        if (nx < 0 || ny < 0 || nx >= w || ny >= h || labels[ny][nx] != 0) continue;
                        if (distance(seed, rgb[ny][nx]) > COLOR_TOLERANCE) continue;
                        labels[ny][nx] = label;
                        stack.push(new int[]{nx, ny});
                    }
                }
            }
        }
        return next;
    }

    private static boolean isFlat(int[][] rgb, int x, int y) {
        int h = rgb.length;
        int w = rgb[0].length;
        for (int dy = -1; dy <= 1; dy++) {
            for (int dx = -1; dx <= 1; dx++) {
                int nx = x + dx;
                int ny = y + dy;
                if (nx < 0 || ny < 0 || nx >= w || ny >= h) continue;
                if (distance(rgb[y][x], rgb[ny][nx]) > FLAT_TOLERANCE) return false;
            }
        }
        return true;
    }

    private static boolean isDark(int c) {
        int r = (c >> 16) & 0xFF;
        int g = (c >> 8) & 0xFF;
        int b = c & 0xFF;
        return Math.max(r, Math.max(g, b)) < DARK_LIMIT;
    }

    private static int distance(int a, int b) {
        int dr = Math.abs(((a >> 16) & 0xFF) - ((b >> 16) & 0xFF));
        int dg = Math.abs(((a >> 8) & 0xFF) - ((b >> 8) & 0xFF));
        int db = Math.abs((a & 0xFF) - (b & 0xFF));
        return Math.max(dr, Math.max(dg, db));
    }
}
