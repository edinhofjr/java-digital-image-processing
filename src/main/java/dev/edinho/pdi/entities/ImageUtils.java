package dev.edinho.pdi.entities;

import java.awt.image.BufferedImage;

public class ImageUtils {
    private ImageUtils() {
        /* */
    }

    public static void print(BufferedImage image) {
        for (int x = 0; x < image.getWidth(); x ++) {
            for (int y = 0; y < image.getHeight(); y++) {
                System.out.println(image.getRGB(x,y) + " ");
            }
            System.out.println("\n");
        }
    }
}
