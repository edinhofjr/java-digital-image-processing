package dev.edinho.pdi.challenges;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.Comparator;
import java.util.Locale;

public final class LeafRustAnalyzer {
    private static final double BACKGROUND_MAX_SATURATION = 0.20;
    private static final float RUST_MAX_HUE = 55f;
    private static final float RUST_WRAP_HUE = 340f;
    private static final double RUST_MIN_SATURATION = 0.25;
    private static final double HEALTHY_LIMIT = 4.0;
    private static final double OBSERVATION_LIMIT = 30.0;

    public enum Status {
        HEALTHY("Saudável"),
        OBSERVATION("Merece observação"),
        SICK("Doente");

        private final String label;

        Status(String label) {
            this.label = label;
        }

        public String label() {
            return label;
        }

        public static Status of(double percent) {
            if (percent <= HEALTHY_LIMIT) return HEALTHY;
            if (percent <= OBSERVATION_LIMIT) return OBSERVATION;
            return SICK;
        }
    }

    public record Result(int leafArea, int rustArea, Status status, BufferedImage annotated) {
        public double rustPercent() {
            return leafArea == 0 ? 0 : 100.0 * rustArea / leafArea;
        }

        public String report() {
            return String.format(Locale.ROOT,
                    "Área da folha:      %d px%nÁrea com ferrugem:  %d px%nPercentual:         %.2f %%%nClassificação:      %s%n",
                    leafArea, rustArea, rustPercent(), status.label());
        }
    }

    private LeafRustAnalyzer() {
    }

    public static Result analyze(BufferedImage original) {
        WorkingImage work = WorkingImage.of(original);
        BufferedImage image = work.image();
        int w = image.getWidth();
        int h = image.getHeight();
        float[][][] hsv = new float[h][w][];
        boolean[][] foreground = new boolean[h][w];
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int c = image.getRGB(x, y);
                float[] v = Color.RGBtoHSB((c >> 16) & 0xFF, (c >> 8) & 0xFF, c & 0xFF, null);
                hsv[y][x] = v;
                foreground[y][x] = v[1] > BACKGROUND_MAX_SATURATION;
            }
        }
        foreground = BinaryOps.open(BinaryOps.close(foreground, 2), 2);
        Region leaf = Region.label(foreground, 8).stream()
                .max(Comparator.comparingInt(Region::area))
                .map(Region::filled)
                .orElse(null);
        if (leaf == null) return new Result(0, 0, Status.HEALTHY, image);
        int leafArea = work.area(leaf.area());

        boolean[][] rust = new boolean[h][w];
        int[] rustArea = {0};
        leaf.forEachPixel((x, y) -> {
            if (isRust(hsv[y][x])) {
                rust[y][x] = true;
                rustArea[0]++;
            }
        });

        Annotator annotator = new Annotator(image);
        for (Region spot : Region.label(rust, 8)) annotator.fill(spot, new Color(255, 0, 200), 0.55);
        annotator.outline(leaf, new Color(0, 90, 255), 3);
        Result result = new Result(leafArea, work.area(rustArea[0]), null, null);
        Status status = Status.of(result.rustPercent());
        annotator.label(String.format(Locale.ROOT, "%s (%.1f%%)", status.label(), result.rustPercent()),
                w / 2.0, h * 0.06, Color.WHITE);
        return new Result(leafArea, result.rustArea(), status, annotator.image());
    }

    static boolean isRust(float[] hsv) {
        float hue = hsv[0] * 360f;
        boolean warm = hue <= RUST_MAX_HUE || hue >= RUST_WRAP_HUE;
        return warm && hsv[1] >= RUST_MIN_SATURATION;
    }
}
