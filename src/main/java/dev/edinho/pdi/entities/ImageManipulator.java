package dev.edinho.pdi.entities;

import java.awt.image.BufferedImage;

public class ImageManipulator {
    BufferedImage image;

    public ImageManipulator(BufferedImage image) {
        this.image = image;
    }

    public BufferedImage translateProcess(double x, double y) {
        throw new UnsupportedOperationException("Not implemented yet");
    }
}
