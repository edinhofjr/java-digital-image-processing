package morphology;

import dev.edinho.pdi.entities.ImageManipulator;
import dev.edinho.pdi.io.ImageRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

class ThinningTest {

    @Test
    void thickHorizontalBarBecomesOnePixelLine() {
        boolean[][] img = new boolean[11][30];
        for (int y = 3; y <= 7; y++) {
            for (int x = 3; x <= 26; x++) img[y][x] = true;
        }

        boolean[][] skeleton = ImageManipulator.thin(img);

        int total = 0;
        for (int x = 0; x < 30; x++) {
            int column = 0;
            for (int y = 0; y < 11; y++) column += skeleton[y][x] ? 1 : 0;
            Assertions.assertTrue(column <= 1, "coluna " + x + " tem mais de 1 pixel");
            total += column;
        }
        Assertions.assertTrue(total > 10, "esqueleto deveria preservar o comprimento da barra");
        Assertions.assertTrue(skeleton[5][15], "esqueleto deveria passar pelo centro da barra");
    }

    @Test
    void preservesConnectivityOfThickShape() throws IOException {
        BufferedImage img = new BufferedImage(200, 200, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, 200, 200);
        g.setColor(Color.BLACK);
        g.setStroke(new BasicStroke(15));
        g.drawLine(30, 30, 170, 170);
        g.drawLine(30, 170, 170, 30);
        g.dispose();

        BufferedImage thinned = new ImageManipulator(img).thinning();

        File out = new File("target/test-output/thinning.png");
        out.getParentFile().mkdirs();
        ImageRepository.save(thinned, out, "png");

        int white = 0;
        for (int y = 0; y < 200; y++) {
            for (int x = 0; x < 200; x++) white += (thinned.getRGB(x, y) & 0xFF) == 255 ? 1 : 0;
        }
        // Um "X" de traço 15 tem milhares de pixels; o esqueleto deve ter ~2 diagonais (~2·140 px).
        Assertions.assertTrue(white > 150 && white < 450, "pixels no esqueleto: " + white);
        Assertions.assertEquals(255, thinned.getRGB(100, 100) & 0xFF, "centro do X deve estar no esqueleto");
    }
}
