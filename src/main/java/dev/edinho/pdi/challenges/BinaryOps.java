package dev.edinho.pdi.challenges;

import java.awt.image.BufferedImage;
import java.util.ArrayDeque;
import java.util.Deque;

public final class BinaryOps {
    private BinaryOps() {
    }

    public static int[][] gray(BufferedImage image) {
        int w = image.getWidth();
        int h = image.getHeight();
        int[][] out = new int[h][w];
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int rgb = image.getRGB(x, y);
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;
                out[y][x] = (int) Math.round(0.299 * r + 0.587 * g + 0.114 * b);
            }
        }
        return out;
    }

    public static int[][] gaussianBlur(int[][] src) {
        int[] k = {1, 4, 6, 4, 1};
        int h = src.length;
        int w = src[0].length;
        int[][] tmp = new int[h][w];
        int[][] out = new int[h][w];
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int sum = 0;
                for (int i = -2; i <= 2; i++) sum += k[i + 2] * src[y][Math.clamp(x + i, 0, w - 1)];
                tmp[y][x] = sum;
            }
        }
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int sum = 0;
                for (int i = -2; i <= 2; i++) sum += k[i + 2] * tmp[Math.clamp(y + i, 0, h - 1)][x];
                out[y][x] = (sum + 128) / 256;
            }
        }
        return out;
    }

    public static int otsu(int[][] values) {
        long[] hist = new long[256];
        long total = 0;
        for (int[] row : values) {
            for (int v : row) {
                hist[Math.clamp(v, 0, 255)]++;
                total++;
            }
        }
        double sumAll = 0;
        for (int i = 0; i < 256; i++) sumAll += i * (double) hist[i];
        double sumBack = 0;
        long weightBack = 0;
        double bestVariance = -1;
        int best = 0;
        for (int t = 0; t < 256; t++) {
            weightBack += hist[t];
            if (weightBack == 0) continue;
            long weightFore = total - weightBack;
            if (weightFore == 0) break;
            sumBack += t * (double) hist[t];
            double meanBack = sumBack / weightBack;
            double meanFore = (sumAll - sumBack) / weightFore;
            double variance = (double) weightBack * weightFore * (meanBack - meanFore) * (meanBack - meanFore);
            if (variance > bestVariance) {
                bestVariance = variance;
                best = t;
            }
        }
        return best;
    }

    public static boolean[][] below(int[][] values, int threshold) {
        boolean[][] out = new boolean[values.length][values[0].length];
        for (int y = 0; y < values.length; y++) {
            for (int x = 0; x < values[0].length; x++) out[y][x] = values[y][x] <= threshold;
        }
        return out;
    }

    public static boolean[][] above(int[][] values, int threshold) {
        boolean[][] out = new boolean[values.length][values[0].length];
        for (int y = 0; y < values.length; y++) {
            for (int x = 0; x < values[0].length; x++) out[y][x] = values[y][x] > threshold;
        }
        return out;
    }

    public static boolean[][] fillHoles(boolean[][] mask) {
        int h = mask.length;
        int w = mask[0].length;
        boolean[][] outside = new boolean[h][w];
        Deque<int[]> stack = new ArrayDeque<>();
        for (int x = 0; x < w; x++) {
            seed(mask, outside, stack, x, 0);
            seed(mask, outside, stack, x, h - 1);
        }
        for (int y = 0; y < h; y++) {
            seed(mask, outside, stack, 0, y);
            seed(mask, outside, stack, w - 1, y);
        }
        int[] dx = {1, -1, 0, 0};
        int[] dy = {0, 0, 1, -1};
        while (!stack.isEmpty()) {
            int[] p = stack.pop();
            for (int i = 0; i < 4; i++) seed(mask, outside, stack, p[0] + dx[i], p[1] + dy[i]);
        }
        boolean[][] out = new boolean[h][w];
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) out[y][x] = !outside[y][x];
        }
        return out;
    }

    private static void seed(boolean[][] mask, boolean[][] outside, Deque<int[]> stack, int x, int y) {
        if (x < 0 || y < 0 || y >= mask.length || x >= mask[0].length) return;
        if (mask[y][x] || outside[y][x]) return;
        outside[y][x] = true;
        stack.push(new int[]{x, y});
    }

    public static boolean[][] dilate(boolean[][] mask, int radius) {
        return morph(mask, radius, true);
    }

    public static boolean[][] erode(boolean[][] mask, int radius) {
        return morph(mask, radius, false);
    }

    public static boolean[][] open(boolean[][] mask, int radius) {
        return dilate(erode(mask, radius), radius);
    }

    public static boolean[][] close(boolean[][] mask, int radius) {
        return erode(dilate(mask, radius), radius);
    }

    private static boolean[][] morph(boolean[][] mask, int radius, boolean dilate) {
        int h = mask.length;
        int w = mask[0].length;
        boolean[][] tmp = new boolean[h][w];
        boolean[][] out = new boolean[h][w];
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) tmp[y][x] = window(mask, x, y, radius, dilate, true);
        }
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) out[y][x] = window(tmp, x, y, radius, dilate, false);
        }
        return out;
    }

    private static boolean window(boolean[][] m, int x, int y, int radius, boolean dilate, boolean horizontal) {
        for (int i = -radius; i <= radius; i++) {
            int nx = horizontal ? x + i : x;
            int ny = horizontal ? y : y + i;
            boolean inside = nx >= 0 && ny >= 0 && ny < m.length && nx < m[0].length;
            boolean v = inside ? m[ny][nx] : !dilate;
            if (dilate && v) return true;
            if (!dilate && !v) return false;
        }
        return !dilate;
    }

    public static boolean[][] not(boolean[][] mask) {
        boolean[][] out = new boolean[mask.length][mask[0].length];
        for (int y = 0; y < mask.length; y++) {
            for (int x = 0; x < mask[0].length; x++) out[y][x] = !mask[y][x];
        }
        return out;
    }

    public static double fraction(boolean[][] mask) {
        long on = 0;
        for (boolean[] row : mask) for (boolean v : row) if (v) on++;
        return (double) on / ((long) mask.length * mask[0].length);
    }

    public static boolean majorityOnBorder(boolean[][] mask) {
        int h = mask.length;
        int w = mask[0].length;
        int on = 0;
        int total = 0;
        for (int x = 0; x < w; x++) {
            on += (mask[0][x] ? 1 : 0) + (mask[h - 1][x] ? 1 : 0);
            total += 2;
        }
        for (int y = 0; y < h; y++) {
            on += (mask[y][0] ? 1 : 0) + (mask[y][w - 1] ? 1 : 0);
            total += 2;
        }
        return on * 2 > total;
    }

    public static int borderMedian(int[][] values) {
        int h = values.length;
        int w = values[0].length;
        int[] hist = new int[256];
        int total = 0;
        for (int x = 0; x < w; x++) {
            hist[Math.clamp(values[0][x], 0, 255)]++;
            hist[Math.clamp(values[h - 1][x], 0, 255)]++;
            total += 2;
        }
        for (int y = 0; y < h; y++) {
            hist[Math.clamp(values[y][0], 0, 255)]++;
            hist[Math.clamp(values[y][w - 1], 0, 255)]++;
            total += 2;
        }
        int acc = 0;
        for (int v = 0; v < 256; v++) {
            acc += hist[v];
            if (acc * 2 >= total) return v;
        }
        return 255;
    }

    public static int[][] sobel(int[][] src) {
        int h = src.length;
        int w = src[0].length;
        int[][] out = new int[h][w];
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int a = at(src, x - 1, y - 1), b = at(src, x, y - 1), c = at(src, x + 1, y - 1);
                int d = at(src, x - 1, y), f = at(src, x + 1, y);
                int g = at(src, x - 1, y + 1), k = at(src, x, y + 1), m = at(src, x + 1, y + 1);
                int gx = (c + 2 * f + m) - (a + 2 * d + g);
                int gy = (g + 2 * k + m) - (a + 2 * b + c);
                out[y][x] = (int) Math.round(Math.hypot(gx, gy) / 4);
            }
        }
        return out;
    }

    private static int at(int[][] src, int x, int y) {
        return src[Math.clamp(y, 0, src.length - 1)][Math.clamp(x, 0, src[0].length - 1)];
    }

    public static int[][] blackHat(int[][] src, int radius) {
        int[][] closed = grayMin(grayMax(src, radius), radius);
        int[][] out = new int[src.length][src[0].length];
        for (int y = 0; y < src.length; y++) {
            for (int x = 0; x < src[0].length; x++) out[y][x] = closed[y][x] - src[y][x];
        }
        return out;
    }

    private static int[][] grayMax(int[][] src, int radius) {
        return grayFilter(src, radius, true);
    }

    private static int[][] grayMin(int[][] src, int radius) {
        return grayFilter(src, radius, false);
    }

    private static int[][] grayFilter(int[][] src, int radius, boolean max) {
        int h = src.length;
        int w = src[0].length;
        int[][] out = new int[h][w];
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int best = max ? Integer.MIN_VALUE : Integer.MAX_VALUE;
                for (int dy = -radius; dy <= radius; dy++) {
                    for (int dx = -radius; dx <= radius; dx++) {
                        int v = at(src, x + dx, y + dy);
                        best = max ? Math.max(best, v) : Math.min(best, v);
                    }
                }
                out[y][x] = best;
            }
        }
        return out;
    }
}
