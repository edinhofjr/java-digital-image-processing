package dev.edinho.pdi.challenges;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;

public record WorkingImage(BufferedImage image, double scale) {
    public static final int MAX_DIMENSION = 800;

    public static WorkingImage of(BufferedImage original) {
        int w = original.getWidth();
        int h = original.getHeight();
        int largest = Math.max(w, h);
        if (largest <= MAX_DIMENSION) return new WorkingImage(toRgb(original), 1.0);
        double scale = (double) largest / MAX_DIMENSION;
        int nw = (int) Math.round(w / scale);
        int nh = (int) Math.round(h / scale);
        Image scaled = original.getScaledInstance(nw, nh, Image.SCALE_AREA_AVERAGING);
        BufferedImage out = new BufferedImage(nw, nh, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = out.createGraphics();
        g.drawImage(scaled, 0, 0, null);
        g.dispose();
        return new WorkingImage(out, (double) w / nw);
    }

    private static BufferedImage toRgb(BufferedImage source) {
        if (source.getType() == BufferedImage.TYPE_INT_RGB) return source;
        BufferedImage out = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = out.createGraphics();
        g.drawImage(source, 0, 0, null);
        g.dispose();
        return out;
    }

    public int area(int pixels) {
        return (int) Math.round(pixels * scale * scale);
    }

    public double length(double pixels) {
        return pixels * scale;
    }
}
