package dev.edinho.pdi.entities;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * Destaca moedas sobre um fundo arbitrário.
 *
 * <p>Pipeline:
 * <ol>
 *   <li>tons de cinza + suavização gaussiana 5x5 (reduz ruído/textura do fundo);</li>
 *   <li>duas segmentações candidatas:
 *     <ul>
 *       <li><b>intensidade</b>: limiar de Otsu; o fundo é a classe predominante na borda da imagem;</li>
 *       <li><b>bordas</b>: magnitude de Sobel + Otsu, útil quando moeda e fundo têm brilho parecido;</li>
 *     </ul></li>
 *   <li>pós-processamento morfológico: fechamento, preenchimento de buracos e abertura;</li>
 *   <li>componentes conexos filtrados por forma (área, circularidade, extent, razão de aspecto);</li>
 *   <li>escolhe a segmentação que encontrou mais regiões com forma de moeda.</li>
 * </ol>
 * O resultado escurece/dessatura o fundo e contorna cada moeda.
 */
public class CoinDetector {

    public record Result(BufferedImage highlighted, List<RegionFeatures> coins, String method,
                         FeatureExtractor.Labeling labeling) {
    }

    private static final double MIN_AREA_FRACTION = 0.0005;
    private static final double MAX_AREA_FRACTION = 0.5;
    private static final double MIN_CIRCULARITY = 0.7;
    private static final double MIN_EXTENT = 0.6;
    private static final double MAX_EXTENT = 0.92;
    private static final double MIN_ASPECT = 0.6;
    private static final double MAX_ASPECT = 1.0 / MIN_ASPECT;

    private final BufferedImage image;
    private final int width;
    private final int height;

    public CoinDetector(BufferedImage image) {
        if (image == null) throw new IllegalArgumentException("imagem nula");
        this.image = image;
        this.width = image.getWidth();
        this.height = image.getHeight();
    }

    public Result detect() {
        double[][] gray = gaussianBlur(grayMatrix());
        int radius = Math.max(1, Math.min(width, height) / 200);

        Candidate byIntensity = evaluate(cleanUp(intensityMask(gray), radius), "intensidade (Otsu)");
        Candidate byEdges = evaluate(cleanUp(edgeMask(gray), radius), "bordas (Sobel + Otsu)");
        Candidate best = byEdges.isBetterThan(byIntensity) ? byEdges : byIntensity;

        return new Result(highlight(best), best.coins, best.method, best.labeling);
    }

    // ---------- segmentação ----------

    private boolean[][] intensityMask(double[][] gray) {
        int t = otsu(gray);
        boolean[][] mask = new boolean[height][width];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                mask[y][x] = gray[y][x] > t;
            }
        }
        // O fundo é a classe que mais aparece na moldura da imagem.
        if (borderTrueFraction(mask) > 0.5) invert(mask);
        return mask;
    }

    private boolean[][] edgeMask(double[][] gray) {
        double[][] magnitude = new double[height][width];
        double max = 0;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                double gx = at(gray, x + 1, y - 1) + 2 * at(gray, x + 1, y) + at(gray, x + 1, y + 1)
                        - at(gray, x - 1, y - 1) - 2 * at(gray, x - 1, y) - at(gray, x - 1, y + 1);
                double gy = at(gray, x - 1, y + 1) + 2 * at(gray, x, y + 1) + at(gray, x + 1, y + 1)
                        - at(gray, x - 1, y - 1) - 2 * at(gray, x, y - 1) - at(gray, x + 1, y - 1);
                magnitude[y][x] = Math.hypot(gx, gy);
                max = Math.max(max, magnitude[y][x]);
            }
        }
        if (max > 0) {
            for (double[] row : magnitude) {
                for (int x = 0; x < width; x++) row[x] = row[x] * 255 / max;
            }
        }
        int t = otsu(magnitude);
        boolean[][] mask = new boolean[height][width];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                mask[y][x] = magnitude[y][x] > t;
            }
        }
        return mask;
    }

    /** Fecha contornos, preenche o interior das moedas e remove ruído pequeno. */
    private boolean[][] cleanUp(boolean[][] mask, int radius) {
        boolean[][] closed = erode(dilate(mask, radius * 2), radius * 2);
        boolean[][] filled = fillHoles(closed);
        return dilate(erode(filled, radius), radius);
    }

    // ---------- avaliação ----------

    private record Candidate(FeatureExtractor.Labeling labeling, List<RegionFeatures> coins, String method) {
        int totalArea() {
            return coins.stream().mapToInt(RegionFeatures::area).sum();
        }

        boolean isBetterThan(Candidate other) {
            if (coins.size() != other.coins.size()) return coins.size() > other.coins.size();
            return totalArea() > other.totalArea();
        }
    }

    private Candidate evaluate(boolean[][] mask, String method) {
        FeatureExtractor.Labeling labeling = new FeatureExtractor(image).label(mask);
        List<RegionFeatures> coins = new ArrayList<>();
        for (RegionFeatures r : labeling.regions()) {
            if (isCoinLike(r)) coins.add(r);
        }
        return new Candidate(labeling, coins, method);
    }

    private boolean isCoinLike(RegionFeatures r) {
        double total = (double) width * height;
        return r.area() >= total * MIN_AREA_FRACTION
                && r.area() <= total * MAX_AREA_FRACTION
                && r.circularity() >= MIN_CIRCULARITY
                && r.extent() >= MIN_EXTENT && r.extent() <= MAX_EXTENT
                && r.aspectRatio() >= MIN_ASPECT && r.aspectRatio() <= MAX_ASPECT;
    }

    // ---------- destaque ----------

    private BufferedImage highlight(Candidate candidate) {
        boolean[] isCoinLabel = new boolean[candidate.labeling.regions().size() + 1];
        for (RegionFeatures c : candidate.coins) isCoinLabel[c.label()] = true;
        int[][] labels = candidate.labeling.labels();

        BufferedImage out = ImageFactory.createEmptyImage(image);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgb = image.getRGB(x, y);
                if (isCoinLabel[labels[y][x]]) {
                    out.setRGB(x, y, rgb & 0xFFFFFF);
                } else {
                    // fundo: cinza escurecido
                    int g = (int) Math.round(toGray(rgb) * 0.3);
                    out.setRGB(x, y, (g << 16) | (g << 8) | g);
                }
            }
        }

        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int stroke = Math.max(2, Math.min(width, height) / 250);
        int fontSize = Math.max(12, Math.min(width, height) / 30);
        g.setStroke(new BasicStroke(stroke));
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, fontSize));

        int n = 1;
        for (RegionFeatures c : candidate.coins) {
            double r = c.equivalentRadius();
            int cx = (int) Math.round(c.centroidX());
            int cy = (int) Math.round(c.centroidY());
            int d = (int) Math.round(2 * r);

            g.setColor(new Color(255, 200, 0));
            g.drawOval(cx - d / 2, cy - d / 2, d, d);

            String text = String.valueOf(n++);
            int tw = g.getFontMetrics().stringWidth(text);
            g.setColor(Color.BLACK);
            g.drawString(text, cx - tw / 2 + 1, cy + fontSize / 3 + 1);
            g.setColor(Color.WHITE);
            g.drawString(text, cx - tw / 2, cy + fontSize / 3);
        }
        g.dispose();
        return out;
    }

    /**
     * Isola as moedas cujo raio equivalente está em [minRadius, maxRadius]: mantém os pixels
     * originais dessas moedas e deixa todo o resto preto (ex.: "encontrar as moedas de 5 centavos").
     */
    public BufferedImage isolateByRadius(Result result, double minRadius, double maxRadius) {
        return new FeatureExtractor(image).keepRegions(result.labeling(), filterByRadius(result, minRadius, maxRadius));
    }

    public static List<RegionFeatures> filterByRadius(Result result, double minRadius, double maxRadius) {
        List<RegionFeatures> selected = new ArrayList<>();
        for (RegionFeatures c : result.coins()) {
            double r = c.equivalentRadius();
            if (r >= minRadius && r <= maxRadius) selected.add(c);
        }
        return selected;
    }

    public static String report(Result result) {
        StringBuilder sb = new StringBuilder();
        sb.append("Moedas encontradas: ").append(result.coins().size()).append('\n');
        sb.append("Segmentação usada: ").append(result.method()).append("\n\n");
        sb.append(String.format("%-4s %8s %8s %16s %8s%n", "#", "Área", "Raio", "Centro", "Circul."));
        int n = 1;
        for (RegionFeatures c : result.coins()) {
            sb.append(String.format("%-4d %8d %8.1f %16s %8.3f%n",
                    n++, c.area(), c.equivalentRadius(),
                    String.format("(%.0f, %.0f)", c.centroidX(), c.centroidY()),
                    c.circularity()));
        }
        return sb.toString();
    }

    // ---------- utilitários ----------

    private double[][] grayMatrix() {
        double[][] gray = new double[height][width];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                gray[y][x] = toGray(image.getRGB(x, y));
            }
        }
        return gray;
    }

    private double[][] gaussianBlur(double[][] src) {
        double[] k = {1, 4, 6, 4, 1}; // separável, soma 16
        double[][] tmp = new double[height][width];
        double[][] out = new double[height][width];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                double s = 0;
                for (int i = -2; i <= 2; i++) s += k[i + 2] * at(src, x + i, y);
                tmp[y][x] = s / 16;
            }
        }
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                double s = 0;
                for (int i = -2; i <= 2; i++) s += k[i + 2] * at(tmp, x, y + i);
                out[y][x] = s / 16;
            }
        }
        return out;
    }

    private double at(double[][] m, int x, int y) {
        return m[clamp(y, height - 1)][clamp(x, width - 1)];
    }

    private static int clamp(int v, int max) {
        return Math.max(0, Math.min(max, v));
    }

    /** Limiar de Otsu sobre valores em [0, 255]. */
    static int otsu(double[][] values) {
        int[] hist = new int[256];
        int total = 0;
        for (double[] row : values) {
            for (double v : row) {
                hist[Math.max(0, Math.min(255, (int) Math.round(v)))]++;
                total++;
            }
        }
        double sumAll = 0;
        for (int i = 0; i < 256; i++) sumAll += (double) i * hist[i];

        double sumB = 0;
        int wB = 0;
        double bestVar = -1;
        int best = 0;
        for (int t = 0; t < 256; t++) {
            wB += hist[t];
            if (wB == 0) continue;
            int wF = total - wB;
            if (wF == 0) break;
            sumB += (double) t * hist[t];
            double mB = sumB / wB;
            double mF = (sumAll - sumB) / wF;
            double between = (double) wB * wF * (mB - mF) * (mB - mF);
            if (between > bestVar) {
                bestVar = between;
                best = t;
            }
        }
        return best;
    }

    private double borderTrueFraction(boolean[][] mask) {
        int count = 0;
        int total = 0;
        for (int x = 0; x < width; x++) {
            count += (mask[0][x] ? 1 : 0) + (mask[height - 1][x] ? 1 : 0);
            total += 2;
        }
        for (int y = 0; y < height; y++) {
            count += (mask[y][0] ? 1 : 0) + (mask[y][width - 1] ? 1 : 0);
            total += 2;
        }
        return (double) count / total;
    }

    private static void invert(boolean[][] mask) {
        for (boolean[] row : mask) {
            for (int x = 0; x < row.length; x++) row[x] = !row[x];
        }
    }

    /** Erosão binária com elemento estruturante quadrado (separável: linha + coluna). */
    private boolean[][] erode(boolean[][] src, int r) {
        return morph(src, r, true);
    }

    private boolean[][] dilate(boolean[][] src, int r) {
        return morph(src, r, false);
    }

    private boolean[][] morph(boolean[][] src, int r, boolean erode) {
        boolean[][] tmp = new boolean[height][width];
        boolean[][] out = new boolean[height][width];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                tmp[y][x] = window(src, x, y, r, true, erode);
            }
        }
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                out[y][x] = window(tmp, x, y, r, false, erode);
            }
        }
        return out;
    }

    private boolean window(boolean[][] m, int x, int y, int r, boolean horizontal, boolean erode) {
        for (int i = -r; i <= r; i++) {
            int sx = horizontal ? clamp(x + i, width - 1) : x;
            int sy = horizontal ? y : clamp(y + i, height - 1);
            if (erode && !m[sy][sx]) return false;
            if (!erode && m[sy][sx]) return true;
        }
        return erode;
    }

    /** Preenche buracos: todo fundo não alcançável a partir da moldura da imagem vira objeto. */
    private boolean[][] fillHoles(boolean[][] mask) {
        boolean[][] reached = new boolean[height][width];
        Deque<int[]> stack = new ArrayDeque<>();
        for (int x = 0; x < width; x++) {
            seed(mask, reached, stack, x, 0);
            seed(mask, reached, stack, x, height - 1);
        }
        for (int y = 0; y < height; y++) {
            seed(mask, reached, stack, 0, y);
            seed(mask, reached, stack, width - 1, y);
        }
        while (!stack.isEmpty()) {
            int[] p = stack.pop();
            seed(mask, reached, stack, p[0] + 1, p[1]);
            seed(mask, reached, stack, p[0] - 1, p[1]);
            seed(mask, reached, stack, p[0], p[1] + 1);
            seed(mask, reached, stack, p[0], p[1] - 1);
        }
        boolean[][] out = new boolean[height][width];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                out[y][x] = mask[y][x] || !reached[y][x];
            }
        }
        return out;
    }

    private void seed(boolean[][] mask, boolean[][] reached, Deque<int[]> stack, int x, int y) {
        if (x < 0 || y < 0 || x >= width || y >= height) return;
        if (mask[y][x] || reached[y][x]) return;
        reached[y][x] = true;
        stack.push(new int[]{x, y});
    }

    private static int toGray(int rgb) {
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;
        return (int) Math.round(0.299 * r + 0.587 * g + 0.114 * b);
    }
}
