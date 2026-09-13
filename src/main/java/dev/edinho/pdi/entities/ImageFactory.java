package dev.edinho.pdi.entities;

import java.awt.*;
import java.awt.image.BufferedImage;


public class ImageFactory {
    private ImageFactory() {
        /* This utility class should not be instantiated */
    }
    public static BufferedImage createEmptyImage(int x, int y) {
       return new BufferedImage(x, y, BufferedImage.TYPE_INT_RGB);
    }

    public static BufferedImage createEmptyImage(BufferedImage image) {
        return new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_RGB);
    }

    public static BufferedImage fromImage(Image img) {
        if (img instanceof BufferedImage bufferedImage) {
            return bufferedImage;
        }

        BufferedImage bimage = new BufferedImage(
                img.getWidth(null),
                img.getHeight(null),
                BufferedImage.TYPE_INT_ARGB
        );

        Graphics2D bGr = bimage.createGraphics();
        bGr.drawImage(img, 0, 0, null);
        bGr.dispose();

        return bimage;
    }
}

