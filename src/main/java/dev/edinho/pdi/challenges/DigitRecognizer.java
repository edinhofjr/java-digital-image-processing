package dev.edinho.pdi.challenges;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public final class DigitRecognizer {
    private static final int GRID_W = 6;
    private static final int GRID_H = 9;
    private static final double ASPECT_WEIGHT = 2.0;
    private static final double HOLE_POSITION_WEIGHT = 2.0;
    private static final String[] FONTS = {Font.SANS_SERIF, Font.DIALOG, Font.SERIF, Font.MONOSPACED};
    private static final int[] STYLES = {Font.BOLD, Font.PLAIN};
    private static volatile List<Template> templates;

    public record Digit(int value, Region region, double distance) {
        public boolean even() {
            return value % 2 == 0;
        }

        public String parity() {
            return even() ? "par" : "ímpar";
        }
    }

    public record Result(List<Digit> digits, BufferedImage annotated) {
        public String report() {
            if (digits.isEmpty()) return "Nenhum algarismo encontrado.";
            StringBuilder sb = new StringBuilder();
            if (digits.size() == 1) {
                Digit d = digits.getFirst();
                sb.append("Número reconhecido: ").append(d.value()).append('\n');
                sb.append("Classificação: ").append(d.parity()).append('\n');
                return sb.toString();
            }
            sb.append("Algarismos encontrados: ").append(digits.size()).append("\n\n");
            sb.append(String.format(Locale.ROOT, "%-4s %-8s %-6s%n", "#", "Número", "Paridade"));
            int i = 1;
            for (Digit d : digits) {
                sb.append(String.format(Locale.ROOT, "%-4d %-8d %-6s%n", i++, d.value(), d.parity()));
            }
            return sb.toString();
        }
    }

    private record Template(int digit, Features features) {
    }

    private record Features(double[] zones, double aspect, int holes, double holeY) {
        double distance(Features other) {
            double sum = 0;
            for (int i = 0; i < zones.length; i++) {
                double d = zones[i] - other.zones[i];
                sum += d * d;
            }
            double da = Math.log(aspect / other.aspect) * ASPECT_WEIGHT;
            sum += da * da;
            if (holes > 0 && other.holes > 0) {
                double dh = (holeY - other.holeY) * HOLE_POSITION_WEIGHT;
                sum += dh * dh;
            }
            return Math.sqrt(sum);
        }
    }

    private DigitRecognizer() {
    }

    public static Result recognize(BufferedImage image) {
        int[][] gray = BinaryOps.gray(image);
        boolean[][] ink = BinaryOps.below(gray, BinaryOps.otsu(gray));
        if (BinaryOps.fraction(ink) > 0.5) ink = BinaryOps.not(ink);

        List<Region> candidates = digitCandidates(ink, image.getWidth(), image.getHeight());
        List<Digit> digits = new ArrayList<>();
        for (Region region : Ordering.readingOrder(candidates, r -> r)) {
            digits.add(classify(region));
        }

        Annotator annotator = new Annotator(image);
        for (Digit d : digits) {
            Color color = d.even() ? new Color(0, 150, 255) : new Color(255, 120, 0);
            annotator.fill(d.region(), color, 0.6);
            annotator.label(d.value() + " " + d.parity(), d.region().centroidX(),
                    d.region().maxY() + d.region().height() * 0.12, color);
        }
        return new Result(digits, annotator.image());
    }

    private static List<Region> digitCandidates(boolean[][] ink, int width, int height) {
        List<Region> regions = new ArrayList<>();
        int minArea = Math.max(20, width * height / 5000);
        for (Region r : Region.label(ink, 8)) {
            if (r.area() >= minArea && !r.touchesBorder(width, height)) regions.add(r);
        }
        List<Region> withoutFrames = new ArrayList<>();
        for (Region r : regions) {
            if (!isContainer(r, regions)) withoutFrames.add(r);
        }
        int tallest = withoutFrames.stream().mapToInt(Region::height).max().orElse(0);
        List<Region> digits = new ArrayList<>();
        for (Region r : withoutFrames) {
            if (r.height() >= tallest * 0.5) digits.add(r);
        }
        return digits;
    }

    private static boolean isContainer(Region region, List<Region> all) {
        Region filled = null;
        for (Region other : all) {
            if (other == region) continue;
            boolean inside = other.minX() > region.minX() && other.maxX() < region.maxX()
                    && other.minY() > region.minY() && other.maxY() < region.maxY();
            if (!inside) continue;
            int cx = (int) Math.round(other.centroidX());
            int cy = (int) Math.round(other.centroidY());
            if (filled == null) filled = region.filled();
            if (filled.contains(cx, cy) && !region.contains(cx, cy)) return true;
        }
        return false;
    }

    private static Digit classify(Region region) {
        Features features = features(region);
        List<Template> all = templates();
        List<Template> sameHoles = all.stream().filter(t -> t.features().holes() == features.holes()).toList();
        List<Template> pool = sameHoles.isEmpty() ? all : sameHoles;
        Template best = pool.stream()
                .min(Comparator.comparingDouble(t -> t.features().distance(features)))
                .orElseThrow();
        return new Digit(best.digit(), region, best.features().distance(features));
    }

    private static Features features(Region region) {
        double[] zones = new double[GRID_W * GRID_H];
        int[] counts = new int[zones.length];
        int x0 = region.minX();
        int y0 = region.minY();
        int w = region.width();
        int h = region.height();
        for (int y = y0; y <= region.maxY(); y++) {
            for (int x = x0; x <= region.maxX(); x++) {
                int gx = Math.min(GRID_W - 1, (x - x0) * GRID_W / w);
                int gy = Math.min(GRID_H - 1, (y - y0) * GRID_H / h);
                int idx = gy * GRID_W + gx;
                counts[idx]++;
                if (region.contains(x, y)) zones[idx]++;
            }
        }
        for (int i = 0; i < zones.length; i++) zones[i] = counts[i] == 0 ? 0 : zones[i] / counts[i];

        int minHole = Math.max(4, region.area() / 200);
        List<Region> holes = region.holes().stream().filter(r -> r.area() >= minHole).toList();
        double holeY = 0;
        for (Region hole : holes) holeY += (hole.centroidY() - y0) / h;
        if (!holes.isEmpty()) holeY /= holes.size();
        return new Features(zones, (double) w / h, holes.size(), holeY);
    }

    private static List<Template> templates() {
        List<Template> local = templates;
        if (local != null) return local;
        synchronized (DigitRecognizer.class) {
            if (templates == null) templates = buildTemplates();
            return templates;
        }
    }

    private static List<Template> buildTemplates() {
        List<Template> out = new ArrayList<>();
        for (String family : FONTS) {
            for (int style : STYLES) {
                for (int digit = 0; digit <= 9; digit++) {
                    Region glyph = renderGlyph(String.valueOf(digit), new Font(family, style, 160));
                    if (glyph != null) out.add(new Template(digit, features(glyph)));
                }
            }
        }
        return out;
    }

    private static Region renderGlyph(String text, Font font) {
        BufferedImage img = new BufferedImage(240, 240, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, 240, 240);
        g.setColor(Color.BLACK);
        g.setFont(font);
        FontMetrics fm = g.getFontMetrics();
        g.drawString(text, (240 - fm.stringWidth(text)) / 2, 120 + fm.getAscent() / 3);
        g.dispose();
        boolean[][] ink = BinaryOps.below(BinaryOps.gray(img), 127);
        return Region.label(ink, 8).stream().max(Comparator.comparingInt(Region::area)).orElse(null);
    }
}
