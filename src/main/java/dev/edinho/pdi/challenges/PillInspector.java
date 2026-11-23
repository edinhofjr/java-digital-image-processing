package dev.edinho.pdi.challenges;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public final class PillInspector {
    private static final double MIN_AREA_FRACTION = 0.002;
    private static final double SIZE_TOLERANCE = 0.15;
    private static final double SOLIDITY_DROP = 0.007;
    private static final double MAX_ASPECT = 1.10;
    private static final double MIN_CIRCULARITY = 0.80;
    private static final double CRACK_MIN_LENGTH = 0.15;
    private static final int DEFECT_CONTRAST = 30;
    private static final int SPOT_MIN_AREA = 12;
    private static final int MIN_SPOTS = 5;

    public record Pill(int number, int area, double perimeter, double circularity, double solidity,
                       double aspectRatio, double crackLength, int spots, List<String> problems, Region region) {
        public boolean ok() {
            return problems.isEmpty();
        }

        public String status() {
            return ok() ? "OK" : String.join(", ", problems);
        }
    }

    public record Result(List<Pill> pills, BufferedImage annotated) {
        public long defective() {
            return pills.stream().filter(p -> !p.ok()).count();
        }

        public String report() {
            StringBuilder sb = new StringBuilder();
            sb.append("Comprimidos encontrados: ").append(pills.size())
                    .append(" | aprovados: ").append(pills.size() - defective())
                    .append(" | com problema: ").append(defective()).append("\n\n");
            sb.append(String.format(Locale.ROOT, "%-4s %9s %10s %8s %8s %7s %7s %6s  %s%n",
                    "#", "Área", "Perímetro", "Circul.", "Solidez", "Razão", "Fissura", "Manchas", "Situação"));
            for (Pill p : pills) {
                sb.append(String.format(Locale.ROOT, "%-4d %9d %10.1f %8.3f %8.3f %7.3f %7.2f %6d  %s%n",
                        p.number(), p.area(), p.perimeter(), p.circularity(), p.solidity(), p.aspectRatio(),
                        p.crackLength(), p.spots(), p.status()));
            }
            return sb.toString();
        }
    }

    private PillInspector() {
    }

    public static Result inspect(BufferedImage original) {
        WorkingImage work = WorkingImage.of(original);
        BufferedImage image = work.image();
        int w = image.getWidth();
        int h = image.getHeight();
        int[][] gray = BinaryOps.gaussianBlur(BinaryOps.gray(image));
        int threshold = BinaryOps.otsu(gray);
        boolean[][] raw = BinaryOps.above(gray, threshold);
        if (BinaryOps.majorityOnBorder(raw)) raw = BinaryOps.not(raw);
        boolean[][] mask = BinaryOps.fillHoles(BinaryOps.open(BinaryOps.close(raw, 3), 2));

        int[][] sharp = BinaryOps.gray(image);
        int[][] blackHat = BinaryOps.blackHat(sharp, 1);
        int minArea = (int) (MIN_AREA_FRACTION * w * h);
        List<Region> regions = new ArrayList<>();
        for (Region r : Region.label(mask, 8)) {
            if (r.area() >= minArea && !r.touchesBorder(w, h)) regions.add(r);
        }
        regions = Ordering.readingOrder(regions, r -> r);

        double medianArea = median(regions.stream().mapToDouble(Region::area).toArray());
        double medianSolidity = median(regions.stream().mapToDouble(Region::solidity).toArray());
        List<Pill> pills = new ArrayList<>();
        Annotator annotator = new Annotator(image);
        int number = 1;
        for (Region r : regions) {
            double circularity = r.circularity();
            double solidity = r.solidity();
            double[] rect = r.minAreaRectangle();
            double aspect = rect[0] / rect[1];
            double[] defects = surfaceDefects(r, blackHat);
            double crack = defects[0];
            int spots = (int) defects[1];

            List<String> problems = new ArrayList<>();
            if (r.area() < medianArea * (1 - SIZE_TOLERANCE)) problems.add("pequeno");
            if (r.area() > medianArea * (1 + SIZE_TOLERANCE)) problems.add("grande");
            if (solidity < medianSolidity - SOLIDITY_DROP) problems.add("quebrado");
            if (aspect > MAX_ASPECT) problems.add("deformado");
            else if (circularity < MIN_CIRCULARITY) problems.add("irregular");
            if (crack >= CRACK_MIN_LENGTH) problems.add("rachado");
            else if (spots >= MIN_SPOTS) problems.add("superfície danificada");

            Pill pill = new Pill(number, work.area(r.area()), work.length(r.perimeter()), circularity, solidity, aspect, crack, spots,
                    List.copyOf(problems), r);
            pills.add(pill);
            annotator.outline(r, pill.ok() ? new Color(0, 200, 0) : new Color(255, 40, 40), 3);
            annotator.label(String.valueOf(number), r.centroidX(), r.centroidY(),
                    pill.ok() ? Color.GREEN : Color.RED);
            number++;
        }
        return new Result(pills, annotator.image());
    }

    private static double[] surfaceDefects(Region pill, int[][] blackHat) {
        int h = blackHat.length;
        int w = blackHat[0].length;
        boolean[][] dark = new boolean[h][w];
        int margin = Math.max(4, (int) Math.round(pill.equivalentDiameter() * 0.06));
        pill.forEachPixel((x, y) -> {
            if (blackHat[y][x] >= DEFECT_CONTRAST && pill.containsWithMargin(x, y, margin)) dark[y][x] = true;
        });
        double longest = 0;
        int spots = 0;
        for (Region part : Region.label(BinaryOps.dilate(dark, 1), 8)) {
            double length = part.minAreaRectangle()[0] - 2;
            longest = Math.max(longest, length);
            if (part.area() >= SPOT_MIN_AREA) spots++;
        }
        return new double[]{longest / pill.equivalentDiameter(), spots};
    }

    private static double median(double[] values) {
        if (values.length == 0) return 0;
        double[] copy = values.clone();
        Arrays.sort(copy);
        int mid = copy.length / 2;
        return copy.length % 2 == 1 ? copy[mid] : (copy[mid - 1] + copy[mid]) / 2;
    }
}
