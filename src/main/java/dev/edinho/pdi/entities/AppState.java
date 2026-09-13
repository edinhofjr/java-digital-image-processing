package dev.edinho.pdi.entities;

import java.awt.image.BufferedImage;

public class AppState {
    private BufferedImage actualImage;
    private BufferedImage transformedImage;

    public BufferedImage getActualImage() {
        return actualImage;
    }

    public void setActualImage(BufferedImage actualImage) {
        this.actualImage = actualImage;
    }

    public BufferedImage getTransformedImage() {
        return transformedImage;
    }

    public void setTransformedImage(BufferedImage transformedImage) {
        this.transformedImage = transformedImage;
    }
}
