package features;

import dev.edinho.pdi.entities.CoinDetector;
import dev.edinho.pdi.entities.FeatureExtractor;
import dev.edinho.pdi.entities.ImageManipulator;
import dev.edinho.pdi.entities.RegionFeatures;
import dev.edinho.pdi.io.ImageRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Random;

class FeatureExtractionTest {

    @Test
    void extractsAreaCentroidAndBoundingBoxOfSquare() {
        BufferedImage img = new BufferedImage(20, 20, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(5, 5, 4, 4);
        g.dispose();

        List<RegionFeatures> regions = new FeatureExtractor(img).extract(128);

        Assertions.assertEquals(1, regions.size());
        RegionFeatures r = regions.getFirst();
        Assertions.assertEquals(16, r.area());
        Assertions.assertEquals(12, r.perimeter());
        Assertions.assertEquals(6.5, r.centroidX(), 1e-9);
        Assertions.assertEquals(6.5, r.centroidY(), 1e-9);
        Assertions.assertEquals(4, r.width());
        Assertions.assertEquals(4, r.height());
    }

    @Test
    void countsSeparateObjectsAndHandlesDarkObjectsOnLightBackground() {
        BufferedImage img = new BufferedImage(30, 30, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, 30, 30);
        g.setColor(Color.BLACK);
        g.fillRect(2, 2, 3, 3);
        g.fillRect(20, 20, 5, 5);
        g.dispose();

        Assertions.assertEquals(2, new FeatureExtractor(img).extract(128).size());
    }

    @Test
    void sobelHighlightsVerticalEdge() {
        BufferedImage img = new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(5, 0, 5, 10);
        g.dispose();

        BufferedImage edges = new ImageManipulator(img).sobel();
        Assertions.assertEquals(0, edges.getRGB(1, 5) & 0xFF);
        Assertions.assertEquals(255, edges.getRGB(5, 5) & 0xFF);
    }

    @Test
    void detectsCoinsOnLightBackground() throws IOException {
        BufferedImage img = coins(new Color(230, 230, 225), null, new Color(150, 110, 60));
        assertCoins(img, "coins_light.png");
    }

    @Test
    void detectsCoinsOnDarkBackground() throws IOException {
        BufferedImage img = coins(new Color(25, 30, 40), null, new Color(200, 190, 170));
        assertCoins(img, "coins_dark.png");
    }

    @Test
    void detectsCoinsOnTexturedBackground() throws IOException {
        BufferedImage img = coins(new Color(120, 90, 60), new Random(42), new Color(220, 210, 180));
        assertCoins(img, "coins_textured.png");
    }

    private static void assertCoins(BufferedImage img, String name) throws IOException {
        CoinDetector.Result result = new CoinDetector(img).detect();

        File out = new File("target/test-output/" + name);
        out.getParentFile().mkdirs();
        ImageRepository.save(result.highlighted(), out, "png");

        Assertions.assertEquals(4, result.coins().size(), CoinDetector.report(result));
    }

    /** 4 moedas (com relevo interno) + um retângulo que não deve ser reconhecido como moeda. */
    private static BufferedImage coins(Color background, Random noise, Color coin) {
        int w = 400, h = 300;
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(background);
        g.fillRect(0, 0, w, h);
        if (noise != null) {
            for (int i = 0; i < 4000; i++) {
                int v = noise.nextInt(60) - 30;
                g.setColor(new Color(clamp(background.getRed() + v), clamp(background.getGreen() + v),
                        clamp(background.getBlue() + v)));
                g.fillRect(noise.nextInt(w), noise.nextInt(h), 3, 3);
            }
        }

        int[][] circles = {{70, 70, 40}, {200, 80, 50}, {320, 200, 45}, {110, 210, 35}};
        for (int[] c : circles) {
            g.setColor(coin);
            g.fillOval(c[0] - c[2], c[1] - c[2], 2 * c[2], 2 * c[2]);
            g.setColor(coin.darker());
            g.drawOval(c[0] - c[2] / 2, c[1] - c[2] / 2, c[2], c[2]); // relevo
        }
        g.setColor(coin);
        g.fillRect(230, 170, 30, 100); // objeto alongado: não é moeda
        g.dispose();
        return img;
    }

    private static int clamp(int v) {
        return Math.max(0, Math.min(255, v));
    }
}
