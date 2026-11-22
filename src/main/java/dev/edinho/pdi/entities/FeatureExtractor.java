package dev.edinho.pdi.entities;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Extração de características de objetos: binarização, rotulação de componentes conexos
 * (flood fill com pilha, vizinhança 4 ou 8), crescimento de regiões a partir de semente
 * e cálculo de descritores dimensionais, inerciais e topológicos para cada região.
 */
public class FeatureExtractor {
    private static final int[] DX4 = {1, -1, 0, 0};
    private static final int[] DY4 = {0, 0, 1, -1};
    private static final int[] DX8 = {1, -1, 0, 0, 1, 1, -1, -1};
    private static final int[] DY8 = {0, 0, 1, -1, 1, -1, 1, -1};

    private final BufferedImage image;
    private final int width;
    private final int height;
    private final int connectivity;

    public FeatureExtractor(BufferedImage image) {
        this(image, 8);
    }

    /** @param connectivity 4 ou 8 — vizinhança usada para considerar pixels conectados. */
    public FeatureExtractor(BufferedImage image, int connectivity) {
        if (image == null) throw new IllegalArgumentException("imagem nula");
        if (connectivity != 4 && connectivity != 8) {
            throw new IllegalArgumentException("vizinhança deve ser 4 ou 8");
        }
        this.image = image;
        this.width = image.getWidth();
        this.height = image.getHeight();
        this.connectivity = connectivity;
    }

    // ============================================================ segmentação

    /**
     * Binariza com o limiar dado. Pixels com cinza >= limiar são "claros"; a classe
     * minoritária é considerada objeto, de forma que tanto objetos claros em fundo
     * escuro quanto objetos escuros em fundo claro funcionam.
     */
    public boolean[][] binarize(int threshold) {
        boolean[][] bright = new boolean[height][width];
        int brightCount = 0;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                bright[y][x] = toGray(image.getRGB(x, y)) >= threshold;
                if (bright[y][x]) brightCount++;
            }
        }
        boolean objectIsBright = brightCount <= (width * height) / 2;
        if (objectIsBright) return bright;

        boolean[][] dark = new boolean[height][width];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                dark[y][x] = !bright[y][x];
            }
        }
        return dark;
    }

    /**
     * Crescimento de região (flood fill) a partir de uma semente: agrega os vizinhos cujo
     * tom de cinza difere do da semente em no máximo {@code tolerance}.
     */
    public boolean[][] regionGrow(int seedX, int seedY, int tolerance) {
        if (!inBounds(seedX, seedY)) throw new IllegalArgumentException("semente fora da imagem");
        int[][] gray = new int[height][width];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) gray[y][x] = toGray(image.getRGB(x, y));
        }
        int seedGray = gray[seedY][seedX];

        boolean[][] region = new boolean[height][width];
        Deque<int[]> stack = new ArrayDeque<>();
        region[seedY][seedX] = true;
        stack.push(new int[]{seedX, seedY});
        int[] dx = connectivity == 8 ? DX8 : DX4;
        int[] dy = connectivity == 8 ? DY8 : DY4;

        while (!stack.isEmpty()) {
            int[] p = stack.pop();
            for (int i = 0; i < dx.length; i++) {
                int nx = p[0] + dx[i];
                int ny = p[1] + dy[i];
                if (inBounds(nx, ny) && !region[ny][nx]
                        && Math.abs(gray[ny][nx] - seedGray) <= tolerance) {
                    region[ny][nx] = true;
                    stack.push(new int[]{nx, ny});
                }
            }
        }
        return region;
    }

    // ============================================================ rotulação

    /** Mapa de rótulos (0 = fundo, k = região k) junto com as características de cada região. */
    public record Labeling(int[][] labels, List<RegionFeatures> regions) {
    }

    public List<RegionFeatures> extract(int threshold) {
        return extract(binarize(threshold));
    }

    public List<RegionFeatures> extract(boolean[][] foreground) {
        return label(foreground).regions();
    }

    public Labeling label(int threshold) {
        return label(binarize(threshold));
    }

    public Labeling label(boolean[][] foreground) {
        int[][] labels = new int[height][width];
        List<Accumulator> accs = new ArrayList<>();
        int nextLabel = 1;

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (foreground[y][x] && labels[y][x] == 0) {
                    accs.add(floodFill(foreground, labels, x, y, nextLabel++));
                }
            }
        }

        List<RegionFeatures> regions = new ArrayList<>(accs.size());
        for (Accumulator acc : accs) {
            regions.add(acc.toFeatures(countHoles(labels, acc)));
        }
        return new Labeling(labels, regions);
    }

    /** Somatórios acumulados durante o flood fill de uma região. */
    private static final class Accumulator {
        final int label;
        int area;
        long sumX, sumY;
        double sumXX, sumYY, sumXY;
        int minX, minY, maxX, maxY;
        final List<int[]> border = new ArrayList<>();

        Accumulator(int label, int x, int y) {
            this.label = label;
            minX = maxX = x;
            minY = maxY = y;
        }

        void add(int x, int y) {
            area++;
            sumX += x;
            sumY += y;
            sumXX += (double) x * x;
            sumYY += (double) y * y;
            sumXY += (double) x * y;
            minX = Math.min(minX, x);
            maxX = Math.max(maxX, x);
            minY = Math.min(minY, y);
            maxY = Math.max(maxY, y);
        }

        RegionFeatures toFeatures(int holes) {
            double cx = (double) sumX / area;
            double cy = (double) sumY / area;
            // momentos centrais normalizados pela área: mu_pq / m00
            double mu20 = sumXX / area - cx * cx;
            double mu02 = sumYY / area - cy * cy;
            double mu11 = sumXY / area - cx * cy;

            double maxR = 0;
            double minR = Double.MAX_VALUE;
            for (int[] b : border) {
                double d = Math.hypot(b[0] - cx, b[1] - cy);
                maxR = Math.max(maxR, d);
                minR = Math.min(minR, d);
            }
            if (border.isEmpty()) minR = 0;

            return new RegionFeatures(label, area, border.size(), cx, cy, minX, minY, maxX, maxY,
                    mu20, mu02, mu11, maxR, minR, holes);
        }
    }

    private Accumulator floodFill(boolean[][] fg, int[][] labels, int startX, int startY, int label) {
        Accumulator acc = new Accumulator(label, startX, startY);
        Deque<int[]> stack = new ArrayDeque<>();
        stack.push(new int[]{startX, startY});
        labels[startY][startX] = label;
        int[] dx = connectivity == 8 ? DX8 : DX4;
        int[] dy = connectivity == 8 ? DY8 : DY4;

        while (!stack.isEmpty()) {
            int[] p = stack.pop();
            int x = p[0];
            int y = p[1];
            acc.add(x, y);
            if (isBorderPixel(fg, x, y)) acc.border.add(p);

            for (int i = 0; i < dx.length; i++) {
                int nx = x + dx[i];
                int ny = y + dy[i];
                if (inBounds(nx, ny) && fg[ny][nx] && labels[ny][nx] == 0) {
                    labels[ny][nx] = label;
                    stack.push(new int[]{nx, ny});
                }
            }
        }
        return acc;
    }

    /** Pixel de contorno: objeto com algum vizinho-4 de fundo (ou na moldura da imagem). */
    private boolean isBorderPixel(boolean[][] fg, int x, int y) {
        for (int i = 0; i < 4; i++) {
            int nx = x + DX4[i];
            int ny = y + DY4[i];
            if (!inBounds(nx, ny) || !fg[ny][nx]) return true;
        }
        return false;
    }

    /**
     * Conta os furos de uma região: componentes de "não-objeto" dentro do retângulo envolvente
     * (com 1 px de margem) que não alcançam a margem. O fundo usa a vizinhança complementar
     * (8 → 4, 4 → 8) para que objeto e fundo não "atravessem" um ao outro na diagonal.
     */
    private int countHoles(int[][] labels, Accumulator acc) {
        int ox = acc.minX - 1;
        int oy = acc.minY - 1;
        int w = acc.maxX - acc.minX + 3;
        int h = acc.maxY - acc.minY + 3;
        boolean[][] visited = new boolean[h][w];
        int[] dx = connectivity == 8 ? DX4 : DX8;
        int[] dy = connectivity == 8 ? DY4 : DY8;

        // Marca o fundo alcançável a partir da margem (canto (0,0) está sempre fora do objeto).
        fillBackground(labels, acc.label, ox, oy, visited, 0, 0, dx, dy);

        int holes = 0;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                if (!visited[y][x] && !isLabel(labels, ox + x, oy + y, acc.label)) {
                    holes++;
                    fillBackground(labels, acc.label, ox, oy, visited, x, y, dx, dy);
                }
            }
        }
        return holes;
    }

    private void fillBackground(int[][] labels, int label, int ox, int oy, boolean[][] visited,
                                int sx, int sy, int[] dx, int[] dy) {
        int h = visited.length;
        int w = visited[0].length;
        Deque<int[]> stack = new ArrayDeque<>();
        visited[sy][sx] = true;
        stack.push(new int[]{sx, sy});
        while (!stack.isEmpty()) {
            int[] p = stack.pop();
            for (int i = 0; i < dx.length; i++) {
                int nx = p[0] + dx[i];
                int ny = p[1] + dy[i];
                if (nx >= 0 && ny >= 0 && nx < w && ny < h && !visited[ny][nx]
                        && !isLabel(labels, ox + nx, oy + ny, label)) {
                    visited[ny][nx] = true;
                    stack.push(new int[]{nx, ny});
                }
            }
        }
    }

    private boolean isLabel(int[][] labels, int x, int y, int label) {
        return inBounds(x, y) && labels[y][x] == label;
    }

    private boolean inBounds(int x, int y) {
        return x >= 0 && x < width && y >= 0 && y < height;
    }

    // ============================================================ visualizações

    /** Máscara binária: objeto branco, fundo preto. */
    public static BufferedImage maskImage(boolean[][] mask) {
        int h = mask.length;
        int w = mask[0].length;
        BufferedImage out = ImageFactory.createEmptyImage(w, h);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) out.setRGB(x, y, mask[y][x] ? 0xFFFFFF : 0);
        }
        return out;
    }

    /** Cada componente conexo pintado com uma cor diferente sobre fundo azul-escuro. */
    public static BufferedImage labelImage(Labeling labeling) {
        int[][] labels = labeling.labels();
        int n = labeling.regions().size();
        int[] palette = new int[n + 1];
        palette[0] = 0x00008B;
        for (int k = 1; k <= n; k++) {
            float hue = (float) ((k * 0.618033988749895) % 1.0); // razão áurea: cores bem espalhadas
            palette[k] = Color.HSBtoRGB(hue, 0.75f, 0.95f) & 0xFFFFFF;
        }
        int h = labels.length;
        int w = labels[0].length;
        BufferedImage out = ImageFactory.createEmptyImage(w, h);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) out.setRGB(x, y, palette[labels[y][x]]);
        }
        return out;
    }

    /** Mantém os pixels originais apenas das regiões escolhidas; o resto fica preto. */
    public BufferedImage keepRegions(Labeling labeling, Collection<RegionFeatures> keep) {
        Set<Integer> wanted = new HashSet<>();
        for (RegionFeatures r : keep) wanted.add(r.label());
        int[][] labels = labeling.labels();
        BufferedImage out = ImageFactory.createEmptyImage(image);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (wanted.contains(labels[y][x])) out.setRGB(x, y, image.getRGB(x, y) & 0xFFFFFF);
            }
        }
        return out;
    }

    /** Pinta uma máscara em vermelho semitransparente sobre a imagem. */
    public BufferedImage overlay(boolean[][] mask) {
        BufferedImage out = ImageFactory.createEmptyImage(image);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgb = image.getRGB(x, y);
                if (mask[y][x]) {
                    int r = (((rgb >> 16) & 0xFF) + 255) / 2;
                    int g = ((rgb >> 8) & 0xFF) / 2;
                    int b = (rgb & 0xFF) / 2;
                    rgb = (r << 16) | (g << 8) | b;
                }
                out.setRGB(x, y, rgb & 0xFFFFFF);
            }
        }
        return out;
    }

    /**
     * Desenha, para cada região: retângulo envolvente (vermelho), elipse ajustada com eixo
     * maior (ciano), centro geométrico (verde) e rótulo (amarelo).
     */
    public BufferedImage annotate(List<RegionFeatures> regions) {
        BufferedImage out = ImageFactory.createEmptyImage(image);
        Graphics2D g = out.createGraphics();
        g.drawImage(image, 0, 0, null);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int stroke = Math.max(1, Math.min(width, height) / 300);
        int fontSize = Math.max(10, Math.min(width, height) / 40);
        g.setStroke(new BasicStroke(stroke));
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, fontSize));

        for (RegionFeatures r : regions) {
            g.setColor(Color.RED);
            g.drawRect(r.minX(), r.minY(), r.width() - 1, r.height() - 1);

            double cx = r.centroidX();
            double cy = r.centroidY();
            double a = r.majorAxis() / 2;
            double b = r.minorAxis() / 2;
            double theta = -Math.toRadians(r.orientationDegrees()); // de volta ao sistema da imagem

            Graphics2D ge = (Graphics2D) g.create();
            ge.translate(cx, cy);
            ge.rotate(theta);
            ge.setColor(Color.CYAN);
            ge.draw(new java.awt.geom.Ellipse2D.Double(-a, -b, 2 * a, 2 * b));
            ge.draw(new java.awt.geom.Line2D.Double(-a, 0, a, 0));
            ge.dispose();

            int c = stroke * 3;
            int icx = (int) Math.round(cx);
            int icy = (int) Math.round(cy);
            g.setColor(Color.GREEN);
            g.drawLine(icx - c, icy, icx + c, icy);
            g.drawLine(icx, icy - c, icx, icy + c);

            g.setColor(Color.YELLOW);
            g.drawString(String.valueOf(r.label()), r.minX() + stroke, Math.max(fontSize, r.minY() - stroke));
        }
        g.dispose();
        return out;
    }

    // ============================================================ relatório

    public static String report(List<RegionFeatures> regions) {
        int holes = regions.stream().mapToInt(RegionFeatures::holes).sum();
        StringBuilder sb = new StringBuilder();
        sb.append("Componentes conexos: ").append(regions.size()).append('\n');
        sb.append("Número de Euler da imagem: ").append(regions.size() - holes)
                .append("  (componentes − furos)\n\n");

        for (RegionFeatures r : regions) {
            sb.append(String.format("── Objeto %d ──────────────────────────────%n", r.label()));
            sb.append(String.format("  Dimensionais: área=%d  perímetro=%d  compacidade=%.2f  circularidade=%.3f%n",
                    r.area(), r.perimeter(), r.compactness(), r.circularity()));
            sb.append(String.format("                excentricidade=%.3f  raio máx=%.1f  raio mín=%.1f%n",
                    r.eccentricity(), r.maxRadius(), r.minRadius()));
            sb.append(String.format("  Inerciais:    centro=(%.1f, %.1f)  orientação=%.1f°%n",
                    r.centroidX(), r.centroidY(), r.orientationDegrees()));
            sb.append(String.format("                momentos μ20=%.1f μ02=%.1f μ11=%.1f%n",
                    r.mu20(), r.mu02(), r.mu11()));
            sb.append(String.format("                retângulo=(%d,%d)–(%d,%d) %dx%d  elipse=%.1f x %.1f%n",
                    r.minX(), r.minY(), r.maxX(), r.maxY(), r.width(), r.height(),
                    r.majorAxis(), r.minorAxis()));
            sb.append(String.format("  Topológicas:  furos=%d  Euler=%d%n%n", r.holes(), r.eulerNumber()));
        }
        return sb.toString();
    }

    private static int toGray(int rgb) {
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;
        return (int) Math.round(0.299 * r + 0.587 * g + 0.114 * b);
    }
}
