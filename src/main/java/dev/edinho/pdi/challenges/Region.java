package dev.edinho.pdi.challenges;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;

public final class Region {
    private static final int[] DX4 = {1, -1, 0, 0};
    private static final int[] DY4 = {0, 0, 1, -1};
    private static final int[] DX8 = {1, -1, 0, 0, 1, 1, -1, -1};
    private static final int[] DY8 = {0, 0, 1, -1, 1, -1, 1, -1};
    private static final int[] TRACE_DX = {0, 1, 1, 1, 0, -1, -1, -1};
    private static final int[] TRACE_DY = {-1, -1, 0, 1, 1, 1, 0, -1};

    private final int originX;
    private final int originY;
    private final boolean[][] mask;
    private final int area;
    private final double centroidX;
    private final double centroidY;
    private final int minX;
    private final int minY;
    private final int maxX;
    private final int maxY;

    private Region(int originX, int originY, boolean[][] mask) {
        this.originX = originX;
        this.originY = originY;
        this.mask = mask;
        int count = 0;
        long sumX = 0;
        long sumY = 0;
        int x0 = Integer.MAX_VALUE, y0 = Integer.MAX_VALUE, x1 = Integer.MIN_VALUE, y1 = Integer.MIN_VALUE;
        for (int y = 0; y < mask.length; y++) {
            for (int x = 0; x < mask[0].length; x++) {
                if (!mask[y][x]) continue;
                count++;
                sumX += x;
                sumY += y;
                x0 = Math.min(x0, x);
                y0 = Math.min(y0, y);
                x1 = Math.max(x1, x);
                y1 = Math.max(y1, y);
            }
        }
        this.area = count;
        this.centroidX = count == 0 ? 0 : originX + (double) sumX / count;
        this.centroidY = count == 0 ? 0 : originY + (double) sumY / count;
        this.minX = originX + x0;
        this.minY = originY + y0;
        this.maxX = originX + x1;
        this.maxY = originY + y1;
    }

    public static List<Region> label(boolean[][] image, int connectivity) {
        int h = image.length;
        int w = image[0].length;
        int[] dx = connectivity == 4 ? DX4 : DX8;
        int[] dy = connectivity == 4 ? DY4 : DY8;
        boolean[][] seen = new boolean[h][w];
        List<Region> regions = new ArrayList<>();
        Deque<int[]> stack = new ArrayDeque<>();
        for (int sy = 0; sy < h; sy++) {
            for (int sx = 0; sx < w; sx++) {
                if (!image[sy][sx] || seen[sy][sx]) continue;
                List<int[]> pixels = new ArrayList<>();
                int x0 = sx, y0 = sy, x1 = sx, y1 = sy;
                seen[sy][sx] = true;
                stack.push(new int[]{sx, sy});
                while (!stack.isEmpty()) {
                    int[] p = stack.pop();
                    pixels.add(p);
                    x0 = Math.min(x0, p[0]);
                    x1 = Math.max(x1, p[0]);
                    y0 = Math.min(y0, p[1]);
                    y1 = Math.max(y1, p[1]);
                    for (int i = 0; i < dx.length; i++) {
                        int nx = p[0] + dx[i];
                        int ny = p[1] + dy[i];
                        if (nx >= 0 && ny >= 0 && nx < w && ny < h && image[ny][nx] && !seen[ny][nx]) {
                            seen[ny][nx] = true;
                            stack.push(new int[]{nx, ny});
                        }
                    }
                }
                boolean[][] local = new boolean[y1 - y0 + 3][x1 - x0 + 3];
                for (int[] p : pixels) local[p[1] - y0 + 1][p[0] - x0 + 1] = true;
                regions.add(new Region(x0 - 1, y0 - 1, local));
            }
        }
        return regions;
    }

    public static List<Region> fromLabelMap(int[][] labels, int count) {
        int h = labels.length;
        int w = labels[0].length;
        int[] x0 = new int[count + 1];
        int[] y0 = new int[count + 1];
        int[] x1 = new int[count + 1];
        int[] y1 = new int[count + 1];
        Arrays.fill(x0, Integer.MAX_VALUE);
        Arrays.fill(y0, Integer.MAX_VALUE);
        Arrays.fill(x1, -1);
        Arrays.fill(y1, -1);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int k = labels[y][x];
                if (k <= 0) continue;
                x0[k] = Math.min(x0[k], x);
                y0[k] = Math.min(y0[k], y);
                x1[k] = Math.max(x1[k], x);
                y1[k] = Math.max(y1[k], y);
            }
        }
        boolean[][][] masks = new boolean[count + 1][][];
        for (int k = 1; k <= count; k++) {
            if (x1[k] >= 0) masks[k] = new boolean[y1[k] - y0[k] + 3][x1[k] - x0[k] + 3];
        }
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int k = labels[y][x];
                if (k > 0) masks[k][y - y0[k] + 1][x - x0[k] + 1] = true;
            }
        }
        List<Region> out = new ArrayList<>();
        for (int k = 1; k <= count; k++) {
            if (masks[k] != null) out.add(new Region(x0[k] - 1, y0[k] - 1, masks[k]));
        }
        return out;
    }

    public int area() {
        return area;
    }

    public double centroidX() {
        return centroidX;
    }

    public double centroidY() {
        return centroidY;
    }

    public int minX() {
        return minX;
    }

    public int minY() {
        return minY;
    }

    public int maxX() {
        return maxX;
    }

    public int maxY() {
        return maxY;
    }

    public int width() {
        return maxX - minX + 1;
    }

    public int height() {
        return maxY - minY + 1;
    }

    public boolean contains(int x, int y) {
        int lx = x - originX;
        int ly = y - originY;
        return ly >= 0 && lx >= 0 && ly < mask.length && lx < mask[0].length && mask[ly][lx];
    }

    public boolean containsWithMargin(int x, int y, int margin) {
        return contains(x - margin, y) && contains(x + margin, y) && contains(x, y - margin) && contains(x, y + margin);
    }

    public boolean touchesBorder(int imageWidth, int imageHeight) {
        return minX == 0 || minY == 0 || maxX == imageWidth - 1 || maxY == imageHeight - 1;
    }

    public Region filled() {
        return new Region(originX, originY, BinaryOps.fillHoles(mask));
    }

    public List<Region> holes() {
        boolean[][] filled = BinaryOps.fillHoles(mask);
        boolean[][] holeMask = new boolean[mask.length][mask[0].length];
        for (int y = 0; y < mask.length; y++) {
            for (int x = 0; x < mask[0].length; x++) holeMask[y][x] = filled[y][x] && !mask[y][x];
        }
        List<Region> holes = new ArrayList<>();
        for (Region r : label(holeMask, 4)) {
            holes.add(r.translate(originX, originY));
        }
        return holes;
    }

    public Region without(List<Region> parts) {
        boolean[][] copy = copyMask();
        for (Region part : parts) {
            for (int y = 0; y < mask.length; y++) {
                for (int x = 0; x < mask[0].length; x++) {
                    if (part.contains(originX + x, originY + y)) copy[y][x] = false;
                }
            }
        }
        return new Region(originX, originY, copy);
    }

    public Region union(List<Region> parts) {
        boolean[][] copy = copyMask();
        for (Region part : parts) {
            for (int y = 0; y < mask.length; y++) {
                for (int x = 0; x < mask[0].length; x++) {
                    if (part.contains(originX + x, originY + y)) copy[y][x] = true;
                }
            }
        }
        return new Region(originX, originY, copy);
    }

    private Region translate(int dx, int dy) {
        return new Region(originX + dx, originY + dy, mask);
    }

    private boolean[][] copyMask() {
        boolean[][] copy = new boolean[mask.length][];
        for (int y = 0; y < mask.length; y++) copy[y] = Arrays.copyOf(mask[y], mask[y].length);
        return copy;
    }

    public double perimeter() {
        if (area == 0) return 0;
        if (area == 1) return 0;
        int sx = -1, sy = -1;
        outer:
        for (int y = 0; y < mask.length; y++) {
            for (int x = 0; x < mask[0].length; x++) {
                if (mask[y][x]) {
                    sx = x;
                    sy = y;
                    break outer;
                }
            }
        }
        int cx = sx, cy = sy;
        int backDir = 6;
        int straight = 0;
        int diagonal = 0;
        int firstMove = -1;
        int limit = 8 * mask.length * mask[0].length;
        for (int steps = 0; steps < limit; steps++) {
            int move = -1;
            for (int i = 1; i <= 8; i++) {
                int k = (backDir + i) % 8;
                int nx = cx + TRACE_DX[k];
                int ny = cy + TRACE_DY[k];
                if (mask[ny][nx]) {
                    move = k;
                    break;
                }
            }
            if (move == -1) return 0;
            if (cx == sx && cy == sy && firstMove != -1 && move == firstMove) break;
            if (firstMove == -1) firstMove = move;
            if (move % 2 == 0) straight++;
            else diagonal++;
            cx += TRACE_DX[move];
            cy += TRACE_DY[move];
            backDir = (move + 4) % 8;
            backDir = (backDir + (move % 2 == 0 ? 1 : 0)) % 8;
        }
        return straight + Math.sqrt(2) * diagonal;
    }

    public double circularity() {
        double p = perimeter();
        return p == 0 ? 0 : Math.min(1.0, 4 * Math.PI * area / (p * p));
    }

    public double equivalentDiameter() {
        return 2 * Math.sqrt(area / Math.PI);
    }

    public List<double[]> convexHull() {
        List<double[]> points = new ArrayList<>();
        for (int y = 0; y < mask.length; y++) {
            for (int x = 0; x < mask[0].length; x++) {
                if (!mask[y][x]) continue;
                boolean border = !mask[y - 1][x] || !mask[y + 1][x] || !mask[y][x - 1] || !mask[y][x + 1];
                if (!border) continue;
                double gx = originX + x;
                double gy = originY + y;
                points.add(new double[]{gx, gy});
                points.add(new double[]{gx + 1, gy});
                points.add(new double[]{gx, gy + 1});
                points.add(new double[]{gx + 1, gy + 1});
            }
        }
        points.sort((a, b) -> a[0] != b[0] ? Double.compare(a[0], b[0]) : Double.compare(a[1], b[1]));
        int n = points.size();
        if (n < 3) return points;
        double[][] hull = new double[2 * n][];
        int k = 0;
        for (double[] p : points) {
            while (k >= 2 && cross(hull[k - 2], hull[k - 1], p) <= 0) k--;
            hull[k++] = p;
        }
        for (int i = n - 2, t = k + 1; i >= 0; i--) {
            double[] p = points.get(i);
            while (k >= t && cross(hull[k - 2], hull[k - 1], p) <= 0) k--;
            hull[k++] = p;
        }
        return new ArrayList<>(Arrays.asList(hull).subList(0, k - 1));
    }

    public double convexArea() {
        List<double[]> hull = convexHull();
        double sum = 0;
        for (int i = 0; i < hull.size(); i++) {
            double[] a = hull.get(i);
            double[] b = hull.get((i + 1) % hull.size());
            sum += a[0] * b[1] - b[0] * a[1];
        }
        return Math.abs(sum) / 2;
    }

    public double solidity() {
        double hull = convexArea();
        return hull == 0 ? 0 : area / hull;
    }

    public double[] minAreaRectangle() {
        List<double[]> hull = convexHull();
        double bestArea = Double.MAX_VALUE;
        double[] best = {width(), height()};
        for (int i = 0; i < hull.size(); i++) {
            double[] a = hull.get(i);
            double[] b = hull.get((i + 1) % hull.size());
            double angle = Math.atan2(b[1] - a[1], b[0] - a[0]);
            double cos = Math.cos(angle);
            double sin = Math.sin(angle);
            double minU = Double.MAX_VALUE, maxU = -Double.MAX_VALUE, minV = Double.MAX_VALUE, maxV = -Double.MAX_VALUE;
            for (double[] p : hull) {
                double u = p[0] * cos + p[1] * sin;
                double v = -p[0] * sin + p[1] * cos;
                minU = Math.min(minU, u);
                maxU = Math.max(maxU, u);
                minV = Math.min(minV, v);
                maxV = Math.max(maxV, v);
            }
            double w = maxU - minU;
            double h = maxV - minV;
            if (w * h < bestArea) {
                bestArea = w * h;
                best = new double[]{Math.max(w, h), Math.min(w, h)};
            }
        }
        return best;
    }

    public double rectangularity() {
        double[] r = minAreaRectangle();
        return area / (r[0] * r[1]);
    }

    private static double cross(double[] o, double[] a, double[] b) {
        return (a[0] - o[0]) * (b[1] - o[1]) - (a[1] - o[1]) * (b[0] - o[0]);
    }

    public void forEachPixel(PixelConsumer consumer) {
        for (int y = 0; y < mask.length; y++) {
            for (int x = 0; x < mask[0].length; x++) {
                if (mask[y][x]) consumer.accept(originX + x, originY + y);
            }
        }
    }

    public boolean isBorderPixel(int x, int y) {
        if (!contains(x, y)) return false;
        return !contains(x + 1, y) || !contains(x - 1, y) || !contains(x, y + 1) || !contains(x, y - 1);
    }

    @FunctionalInterface
    public interface PixelConsumer {
        void accept(int x, int y);
    }
}
