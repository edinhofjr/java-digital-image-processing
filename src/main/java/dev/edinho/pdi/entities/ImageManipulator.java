package dev.edinho.pdi.entities;

import java.awt.image.BufferedImage;

public class ImageManipulator {
    private final BufferedImage image;

    public ImageManipulator(BufferedImage image) {
        if (image == null) throw new IllegalArgumentException("imagem nula");
        this.image = image;
    }

    public BufferedImage translateProcess(int x, int y) {
        int lx = image.getWidth();
        int ly = image.getHeight();

        BufferedImage newImage = ImageFactory.createEmptyImage(image);

        int inicioX = Math.max(0, -x), fimX = Math.min(lx, lx - x);
        int inicioY = Math.max(0, -y), fimY = Math.min(ly, ly - y);

        for (int iy = inicioY; iy < fimY; iy++) {
            for (int ix = inicioX; ix < fimX; ix++) {
                newImage.setRGB(ix + x, iy + y, image.getRGB(ix, iy));
            }
        }
        return newImage;
    }
}