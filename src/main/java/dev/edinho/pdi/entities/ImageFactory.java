package dev.edinho.pdi.entities;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class ImageFactory {
    private ImageFactory() {
        /* This utility class should not be instantiated */
    }
    public static BufferedImage createEmptyImage(int x, int y) {
       return new BufferedImage(x, y, BufferedImage.TYPE_INT_RGB);
    }

    public static BufferedImage createFromImage(BufferedImage image) {
        return new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_RGB);
    }

    public static BufferedImage loadFromPath(String path) throws IOException {
        BufferedImage image = ImageIO.read(new File(path));
        if (image == null) {
            throw new IOException("Formato de imagem não suportado: " + path);
        }
        return image;
    }
}
