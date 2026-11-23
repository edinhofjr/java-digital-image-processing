package challenges;

import dev.edinho.pdi.challenges.DigitRecognizer;
import dev.edinho.pdi.challenges.LeafRustAnalyzer;
import dev.edinho.pdi.challenges.NutInspector;
import dev.edinho.pdi.challenges.PillInspector;
import dev.edinho.pdi.challenges.Region;
import dev.edinho.pdi.challenges.ShapeCounter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

class ChallengesTest {

    private static BufferedImage sample(String name) throws IOException {
        try (InputStream in = ChallengesTest.class.getResourceAsStream("/desafios/" + name)) {
            Assertions.assertNotNull(in, name);
            return ImageIO.read(in);
        }
    }

    @Test
    void perimeterOfSquareAndCircle() {
        boolean[][] square = new boolean[30][30];
        boolean[][] circle = new boolean[60][60];
        for (int y = 0; y < 30; y++) for (int x = 0; x < 30; x++) square[y][x] = x >= 10 && x < 20 && y >= 10 && y < 20;
        for (int y = 0; y < 60; y++) for (int x = 0; x < 60; x++) circle[y][x] = Math.hypot(x - 30, y - 30) <= 20;

        Region sq = Region.label(square, 8).getFirst();
        Assertions.assertEquals(36, sq.perimeter(), 1e-9);
        Assertions.assertEquals(1.0, sq.rectangularity(), 1e-9);

        Region c = Region.label(circle, 8).getFirst();
        Assertions.assertEquals(2 * Math.PI * 20, c.perimeter(), 2 * Math.PI * 20 * 0.08);
        Assertions.assertTrue(c.circularity() > 0.85);
    }

    @Test
    void holesAreFoundInsideRegion() {
        boolean[][] ring = new boolean[50][50];
        for (int y = 0; y < 50; y++) {
            for (int x = 0; x < 50; x++) {
                double d = Math.hypot(x - 25, y - 25);
                ring[y][x] = d <= 20 && d > 8;
            }
        }
        Region r = Region.label(ring, 8).getFirst();
        Assertions.assertEquals(1, r.holes().size());
        Assertions.assertEquals(r.area() + r.holes().getFirst().area(), r.filled().area());
    }

    @Test
    void nutsAreCountedWithTheirHoles() throws IOException {
        NutInspector.Result mixed = NutInspector.inspect(sample("porcas_1.jpg"));
        Assertions.assertEquals(8, mixed.nuts().size());
        for (NutInspector.Nut nut : mixed.nuts()) {
            Assertions.assertTrue(nut.hasHole(), "porca " + nut.number());
            Assertions.assertTrue(nut.innerPerimeter() < nut.outerPerimeter());
            Assertions.assertTrue(nut.holeArea() < nut.area());
        }

        NutInspector.Result equal = NutInspector.inspect(sample("porcas_2.jpg"));
        Assertions.assertEquals(6, equal.nuts().size());
        double min = equal.nuts().stream().mapToDouble(NutInspector.Nut::holeArea).min().orElseThrow();
        double max = equal.nuts().stream().mapToDouble(NutInspector.Nut::holeArea).max().orElseThrow();
        Assertions.assertTrue(max / min < 1.3, "furos de porcas iguais devem ter áreas parecidas");
    }

    @Test
    void recognizesAllDigitsOfTheSheet() throws IOException {
        DigitRecognizer.Result result = DigitRecognizer.recognize(sample("digitos.png"));
        List<Integer> values = result.digits().stream().map(DigitRecognizer.Digit::value).toList();
        Assertions.assertEquals(List.of(0, 1, 2, 3, 4, 5, 6, 7, 8, 9), values);
    }

    @Test
    void recognizesEachFramedDigitAlone() throws IOException {
        BufferedImage sheet = sample("digitos.png");
        int[] columns = {22, 277, 532, 787, 1041};
        int[] rows = {84, 445};
        for (int digit = 0; digit <= 9; digit++) {
            BufferedImage single = sheet.getSubimage(columns[digit % 5], rows[digit / 5], 236, 282);
            DigitRecognizer.Result result = DigitRecognizer.recognize(single);
            Assertions.assertEquals(1, result.digits().size(), "imagem do " + digit);
            DigitRecognizer.Digit found = result.digits().getFirst();
            Assertions.assertEquals(digit, found.value());
            Assertions.assertEquals(digit % 2 == 0 ? "par" : "ímpar", found.parity());
            Assertions.assertTrue(result.report().contains("Número reconhecido: " + digit));
        }
    }

    @Test
    void recognizesRenderedDigit() {
        BufferedImage img = new BufferedImage(200, 260, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, 200, 260);
        g.setColor(Color.BLACK);
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 200));
        g.drawString("4", 45, 200);
        g.dispose();
        DigitRecognizer.Digit digit = DigitRecognizer.recognize(img).digits().getFirst();
        Assertions.assertEquals(4, digit.value());
        Assertions.assertTrue(digit.even());
    }

    @Test
    void pillsWithDefectsAreFlagged() throws IOException {
        PillInspector.Result first = PillInspector.inspect(sample("comprimidos_1.jpg"));
        Assertions.assertEquals(12, first.pills().size());
        Assertions.assertEquals(Set.of(4, 5, 7, 10), defective(first));
        Assertions.assertTrue(first.pills().get(4).problems().contains("rachado"));
        Assertions.assertTrue(first.pills().get(6).problems().contains("deformado"));

        PillInspector.Result second = PillInspector.inspect(sample("comprimidos_2.jpg"));
        Assertions.assertEquals(15, second.pills().size());
        Assertions.assertTrue(defective(second).containsAll(Set.of(3, 5, 7, 8, 10, 12)));
        Assertions.assertTrue(second.pills().get(7).problems().contains("rachado"));
        Assertions.assertTrue(second.pills().get(6).problems().contains("pequeno"));
    }

    private static Set<Integer> defective(PillInspector.Result result) {
        return result.pills().stream().filter(p -> !p.ok()).map(PillInspector.Pill::number).collect(Collectors.toSet());
    }

    @Test
    void countsCirclesAndSquares() throws IOException {
        ShapeCounter.Result grid = ShapeCounter.count(sample("formas.png"));
        Assertions.assertEquals(10, grid.count(ShapeCounter.Kind.CIRCLE));
        Assertions.assertEquals(10, grid.count(ShapeCounter.Kind.SQUARE));

        ShapeCounter.Result board = ShapeCounter.count(sample("tabuleiro.png"));
        Assertions.assertEquals(5, board.count(ShapeCounter.Kind.CIRCLE));
        Assertions.assertEquals(5, board.count(ShapeCounter.Kind.SQUARE));
    }

    @Test
    void classifiesLeavesByRustPercentage() throws IOException {
        Assertions.assertEquals(LeafRustAnalyzer.Status.HEALTHY, LeafRustAnalyzer.analyze(sample("folha_1.jpg")).status());
        Assertions.assertEquals(LeafRustAnalyzer.Status.OBSERVATION, LeafRustAnalyzer.analyze(sample("folha_2.jpg")).status());
        Assertions.assertEquals(LeafRustAnalyzer.Status.SICK, LeafRustAnalyzer.analyze(sample("folha_3.jpg")).status());
    }

    @Test
    void leafStatusThresholds() {
        Assertions.assertEquals(LeafRustAnalyzer.Status.HEALTHY, LeafRustAnalyzer.Status.of(4.0));
        Assertions.assertEquals(LeafRustAnalyzer.Status.OBSERVATION, LeafRustAnalyzer.Status.of(4.01));
        Assertions.assertEquals(LeafRustAnalyzer.Status.OBSERVATION, LeafRustAnalyzer.Status.of(30.0));
        Assertions.assertEquals(LeafRustAnalyzer.Status.SICK, LeafRustAnalyzer.Status.of(30.01));
    }
}
