package dev.edinho.pdi.challenges;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class NutInspector {
    private static final double MIN_AREA_FRACTION = 0.002;
    private static final double MIN_HOLE_FRACTION = 0.02;
    private static final double MAX_HOLE_FRACTION = 0.5;
    private static final int DARK_MARGIN = 45;
    private static final int BRIGHT_MARGIN = 20;
    private static final int EDGE_GRADIENT = 30;
    private static final int[] HOLE_GRADIENT_LIMITS = {40, 32, 25, 18, 12};

    public record Nut(int number, int area, double outerPerimeter, double innerPerimeter, int holeArea,
                      double centroidX, double centroidY) {
        public boolean hasHole() {
            return holeArea > 0;
        }
    }

    public record Result(List<Nut> nuts, BufferedImage annotated) {
        public String report() {
            StringBuilder sb = new StringBuilder();
            sb.append("Porcas encontradas: ").append(nuts.size()).append("\n\n");
            sb.append(String.format(Locale.ROOT, "%-6s %12s %14s %14s %12s%n",
                    "Porca", "Área (px)", "Per. ext (px)", "Per. int (px)", "Furo (px)"));
            for (Nut n : nuts) {
                sb.append(String.format(Locale.ROOT, "%-6d %12d %14.1f %14.1f %12d%s%n",
                        n.number(), n.area(), n.outerPerimeter(), n.innerPerimeter(), n.holeArea(),
                        n.hasHole() ? "" : "  sem furo"));
            }
            return sb.toString();
        }
    }

    private NutInspector() {
    }

    public static Result inspect(BufferedImage original) {
        WorkingImage work = WorkingImage.of(original);
        BufferedImage image = work.image();
        int w = image.getWidth();
        int h = image.getHeight();
        int[][] gray = BinaryOps.gaussianBlur(BinaryOps.gray(image));
        int background = BinaryOps.borderMedian(gray);
        int[][] gradient = BinaryOps.sobel(gray);
        boolean[][] mask = new boolean[h][w];
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int d = gray[y][x] - background;
                mask[y][x] = d < -DARK_MARGIN || d > BRIGHT_MARGIN || gradient[y][x] > EDGE_GRADIENT;
            }
        }
        mask = BinaryOps.open(BinaryOps.fillHoles(BinaryOps.close(mask, 4)), 3);

        int minArea = (int) (MIN_AREA_FRACTION * w * h);
        List<Region[]> found = new ArrayList<>();
        for (Region region : Region.label(mask, 8)) {
            if (region.area() < minArea) continue;
            Region hole = findHole(region, gradient);
            Region body = hole == null ? region : region.without(List.of(hole));
            found.add(new Region[]{body, region, hole});
        }

        List<Region[]> ordered = Ordering.readingOrder(found, r -> r[1]);
        List<Nut> nuts = new ArrayList<>();
        Annotator annotator = new Annotator(image);
        int number = 1;
        for (Region[] r : ordered) {
            Region body = r[0];
            Region filled = r[1];
            Region hole = r[2];
            nuts.add(new Nut(number, work.area(body.area()), work.length(filled.perimeter()),
                    hole == null ? 0 : work.length(hole.perimeter()), hole == null ? 0 : work.area(hole.area()),
                    work.length(filled.centroidX()), work.length(filled.centroidY())));
            annotator.outline(filled, new Color(255, 60, 60), 3);
            if (hole != null) {
                annotator.fill(hole, new Color(0, 160, 255), 0.45);
                annotator.outline(hole, new Color(0, 200, 0), 3);
            }
            annotator.label(String.valueOf(number), filled.centroidX(), filled.minY() + filled.height() * 0.12,
                    Color.YELLOW);
            number++;
        }
        return new Result(nuts, annotator.image());
    }

    private static Region findHole(Region nut, int[][] gradient) {
        for (int limit : HOLE_GRADIENT_LIMITS) {
            Region hole = growHole(nut, gradient, limit);
            if (hole != null) return hole;
        }
        return null;
    }

    private static Region growHole(Region nut, int[][] gradient, int maxGradient) {
        int margin = Math.max(3, nut.width() / 30);
        int seedX = (int) Math.round(nut.centroidX());
        int seedY = (int) Math.round(nut.centroidY());
        int h = gradient.length;
        int w = gradient[0].length;
        boolean[][] grown = new boolean[h][w];
        ArrayDeque<int[]> queue = new ArrayDeque<>();
        grown[seedY][seedX] = true;
        queue.add(new int[]{seedX, seedY});
        int count = 1;
        int limit = (int) (nut.area() * MAX_HOLE_FRACTION);
        int[] dx = {1, -1, 0, 0};
        int[] dy = {0, 0, 1, -1};
        while (!queue.isEmpty()) {
            int[] p = queue.poll();
            for (int i = 0; i < 4; i++) {
                int nx = p[0] + dx[i];
                int ny = p[1] + dy[i];
                if (nx < 0 || ny < 0 || nx >= w || ny >= h || grown[ny][nx]) continue;
                if (gradient[ny][nx] > maxGradient) continue;
                if (!nut.containsWithMargin(nx, ny, margin)) return null;
                grown[ny][nx] = true;
                count++;
                if (count > limit) return null;
                queue.add(new int[]{nx, ny});
            }
        }
        if (count < MIN_HOLE_FRACTION * nut.area()) return null;
        boolean[][] closed = BinaryOps.fillHoles(BinaryOps.close(grown, 2));
        return Region.label(closed, 4).stream()
                .filter(r -> r.contains(seedX, seedY))
                .findFirst()
                .orElse(null);
    }
}
