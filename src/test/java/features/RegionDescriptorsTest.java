package features;

import dev.edinho.pdi.entities.CoinDetector;
import dev.edinho.pdi.entities.FeatureExtractor;
import dev.edinho.pdi.entities.RegionFeatures;
import dev.edinho.pdi.io.ImageRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;

class RegionDescriptorsTest {

    private static BufferedImage blank(int w, int h) {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, w, h);
        g.dispose();
        return img;
    }

    @Test
    void diagonalPixelsAreOneComponentWith8AndTwoWith4() {
        boolean[][] fg = new boolean[4][4];
        fg[1][1] = true;
        fg[2][2] = true;
        BufferedImage img = blank(4, 4);

        Assertions.assertEquals(1, new FeatureExtractor(img, 8).extract(fg).size());
        Assertions.assertEquals(2, new FeatureExtractor(img, 4).extract(fg).size());
    }

    @Test
    void ringHasOneHoleAndEulerZero() {
        BufferedImage img = blank(60, 60);
        Graphics2D g = img.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(10, 10, 40, 40);
        g.setColor(Color.BLACK);
        g.fillRect(20, 20, 20, 20);
        g.dispose();

        List<RegionFeatures> regions = new FeatureExtractor(img).extract(128);

        Assertions.assertEquals(1, regions.size());
        Assertions.assertEquals(1, regions.getFirst().holes());
        Assertions.assertEquals(0, regions.getFirst().eulerNumber());
    }

    @Test
    void letterBLikeShapeHasTwoHoles() {
        BufferedImage img = blank(60, 80);
        Graphics2D g = img.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(10, 10, 40, 60);
        g.setColor(Color.BLACK);
        g.fillRect(20, 20, 20, 15);
        g.fillRect(20, 45, 20, 15);
        g.dispose();

        RegionFeatures r = new FeatureExtractor(img).extract(128).getFirst();
        Assertions.assertEquals(2, r.holes());
        Assertions.assertEquals(-1, r.eulerNumber());
    }

    @Test
    void diskIsRoundAndCompact() {
        BufferedImage img = blank(200, 200);
        Graphics2D g = img.createGraphics();
        g.setColor(Color.WHITE);
        g.fill(new Ellipse2D.Double(50, 50, 100, 100));
        g.dispose();

        RegionFeatures r = new FeatureExtractor(img).extract(128).getFirst();

        Assertions.assertEquals(100, r.centroidX(), 1.0);
        Assertions.assertEquals(100, r.centroidY(), 1.0);
        Assertions.assertTrue(r.eccentricity() < 0.15, "excentricidade " + r.eccentricity());
        Assertions.assertEquals(50, r.maxRadius(), 2.0);
        Assertions.assertEquals(50, r.minRadius(), 2.5);
        Assertions.assertEquals(100, r.majorAxis(), 3.0);
        Assertions.assertEquals(0, r.holes());
    }

    @Test
    void rotatedEllipseHasExpectedOrientationAndAxes() {
        BufferedImage img = blank(300, 300);
        Graphics2D g = img.createGraphics();
        g.setColor(Color.WHITE);
        // elipse 160 x 60 girada 30° no sentido anti-horário da tela
        g.setTransform(AffineTransform.getRotateInstance(Math.toRadians(-30), 150, 150));
        g.fill(new Ellipse2D.Double(70, 120, 160, 60));
        g.dispose();

        RegionFeatures r = new FeatureExtractor(img).extract(128).getFirst();

        Assertions.assertEquals(30, r.orientationDegrees(), 1.5);
        Assertions.assertEquals(160, r.majorAxis(), 4.0);
        Assertions.assertEquals(60, r.minorAxis(), 4.0);
        Assertions.assertTrue(r.eccentricity() > 0.9, "excentricidade " + r.eccentricity());
        Assertions.assertTrue(r.compactness() > 4 * Math.PI, "compacidade " + r.compactness());
    }

    @Test
    void regionGrowFillsOnlyConnectedSimilarPixels() {
        BufferedImage img = blank(50, 50);
        Graphics2D g = img.createGraphics();
        g.setColor(new Color(200, 200, 200));
        g.fillRect(5, 5, 10, 10);   // região da semente
        g.fillRect(30, 30, 10, 10); // mesma cor, mas desconectada
        g.dispose();

        boolean[][] region = new FeatureExtractor(img).regionGrow(8, 8, 10);
        int count = 0;
        for (boolean[] row : region) for (boolean v : row) if (v) count++;

        Assertions.assertEquals(100, count);
        Assertions.assertFalse(region[35][35]);
    }

    @Test
    void isolatesOnlySmallCoins() throws IOException {
        // Fundo escuro com 2 moedas pequenas e 3 grandes, como no exemplo da aula.
        BufferedImage img = blank(400, 300);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(new Color(190, 190, 195));
        int[][] big = {{80, 80, 45}, {200, 60, 45}, {150, 200, 45}};
        for (int[] c : big) g.fillOval(c[0] - c[2], c[1] - c[2], 2 * c[2], 2 * c[2]);
        g.setColor(new Color(180, 100, 60));
        int[][] small = {{320, 120, 28}, {300, 240, 28}};
        for (int[] c : small) g.fillOval(c[0] - c[2], c[1] - c[2], 2 * c[2], 2 * c[2]);
        g.dispose();

        CoinDetector detector = new CoinDetector(img);
        CoinDetector.Result result = detector.detect();
        Assertions.assertEquals(5, result.coins().size(), CoinDetector.report(result));

        List<RegionFeatures> selected = CoinDetector.filterByRadius(result, 20, 35);
        Assertions.assertEquals(2, selected.size());

        BufferedImage isolated = detector.isolateByRadius(result, 20, 35);
        File out = new File("target/test-output/coins_isolated.png");
        out.getParentFile().mkdirs();
        ImageRepository.save(isolated, out, "png");
        ImageRepository.save(FeatureExtractor.labelImage(result.labeling()),
                new File("target/test-output/coins_labels.png"), "png");

        Assertions.assertEquals(0, isolated.getRGB(80, 80) & 0xFFFFFF, "moeda grande deve sumir");
        Assertions.assertNotEquals(0, isolated.getRGB(320, 120) & 0xFFFFFF, "moeda pequena deve ficar");
    }
}
