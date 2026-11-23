package dev.edinho.pdi.challenges;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

public final class Annotator {
    private final BufferedImage canvas;

    public Annotator(BufferedImage source) {
        canvas = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = canvas.createGraphics();
        g.drawImage(source, 0, 0, null);
        g.dispose();
    }

    public Annotator outline(Region region, Color color, int thickness) {
        int rgb = color.getRGB() & 0xFFFFFF;
        int half = thickness / 2;
        region.forEachPixel((x, y) -> {
            if (!region.isBorderPixel(x, y)) return;
            for (int dy = -half; dy <= half; dy++) {
                for (int dx = -half; dx <= half; dx++) set(x + dx, y + dy, rgb);
            }
        });
        return this;
    }

    public Annotator fill(Region region, Color color, double alpha) {
        int r = color.getRed();
        int g = color.getGreen();
        int b = color.getBlue();
        region.forEachPixel((x, y) -> {
            int c = canvas.getRGB(x, y);
            int nr = (int) Math.round(((c >> 16) & 0xFF) * (1 - alpha) + r * alpha);
            int ng = (int) Math.round(((c >> 8) & 0xFF) * (1 - alpha) + g * alpha);
            int nb = (int) Math.round((c & 0xFF) * (1 - alpha) + b * alpha);
            canvas.setRGB(x, y, (nr << 16) | (ng << 8) | nb);
        });
        return this;
    }

    public Annotator label(String text, double cx, double cy, Color color) {
        Graphics2D g = canvas.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int size = Math.max(12, Math.min(canvas.getWidth(), canvas.getHeight()) / 28);
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, size));
        FontMetrics fm = g.getFontMetrics();
        int w = fm.stringWidth(text);
        int x = (int) Math.round(cx - w / 2.0);
        int y = (int) Math.round(cy + fm.getAscent() / 2.0 - fm.getDescent() / 2.0);
        g.setStroke(new BasicStroke(3));
        g.setColor(Color.BLACK);
        g.drawString(text, x - 1, y - 1);
        g.drawString(text, x + 1, y + 1);
        g.drawString(text, x - 1, y + 1);
        g.drawString(text, x + 1, y - 1);
        g.setColor(color);
        g.drawString(text, x, y);
        g.dispose();
        return this;
    }

    public BufferedImage image() {
        return canvas;
    }

    private void set(int x, int y, int rgb) {
        if (x >= 0 && y >= 0 && x < canvas.getWidth() && y < canvas.getHeight()) canvas.setRGB(x, y, rgb);
    }
}
