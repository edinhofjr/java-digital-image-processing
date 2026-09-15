package dev.edinho.pdi.entities;

import java.awt.image.BufferedImage;

public class ImageManipulator {
    private final BufferedImage image;

    public ImageManipulator(BufferedImage image) {
        if (image == null) throw new IllegalArgumentException("imagem nula");
        this.image = image;
    }

    public BufferedImage translateProcess(int x, int y) {
        Matrix inverseTranslation = new Matrix(new double[][]{
                {1, 0, -x},
                {0, 1, -y},
                {0, 0, 1}
        });
        return applyInverseTransform(inverseTranslation);
    }

    public BufferedImage rotateProcess(int angleDegrees) {
        double angleRadians = Math.toRadians(-angleDegrees);
        double cos = Math.cos(angleRadians);
        double sin = Math.sin(angleRadians);
        double cx = image.getWidth() / 2.0;
        double cy = image.getHeight() / 2.0;

        Matrix toOrigin = new Matrix(new double[][]{
                {1, 0, -cx},
                {0, 1, -cy},
                {0, 0, 1}
        });
        Matrix rotation = new Matrix(new double[][]{
                {cos, -sin, 0},
                {sin, cos, 0},
                {0, 0, 1}
        });
        Matrix backToCenter = new Matrix(new double[][]{
                {1, 0, cx},
                {0, 1, cy},
                {0, 0, 1}
        });

        Matrix inverseRotationMatrix = MatrixOperator.product(backToCenter, MatrixOperator.product(rotation, toOrigin));
        return applyInverseTransform(inverseRotationMatrix);
    }

    public BufferedImage scale(double value) {
        Matrix inverseScaleMatrix = new Matrix(new double[][]{
                {1 / value, 0, 0},
                {0, 1 / value, 0},
                {0, 0, 1}
        });
        return applyInverseTransform(inverseScaleMatrix);
    }

    public BufferedImage mirrorHorizontal() {
        double maxX = image.getWidth() - 1;
        Matrix inverseMirror = new Matrix(new double[][]{
                {-1, 0, maxX},
                {0, 1, 0},
                {0, 0, 1}
        });
        return applyInverseTransform(inverseMirror);
    }

    public BufferedImage mirrorVertical() {
        double maxY = image.getHeight() - 1;
        Matrix inverseMirror = new Matrix(new double[][]{
                {1, 0, 0},
                {0, -1, maxY},
                {0, 0, 1}
        });
        return applyInverseTransform(inverseMirror);
    }

    private BufferedImage applyInverseTransform(Matrix inverseTransform) {
        int lx = image.getWidth();
        int ly = image.getHeight();

        BufferedImage newImage = ImageFactory.createEmptyImage(image);

        for (int ny = 0; ny < ly; ny++) {
            for (int nx = 0; nx < lx; nx++) {
                Matrix position = Matrices.columnsFromArray(new double[]{nx, ny, 1});
                Matrix sourcePosition = MatrixOperator.product(inverseTransform, position);

                int sx = (int) Math.round(sourcePosition.get(0, 0));
                int sy = (int) Math.round(sourcePosition.get(1, 0));

                if (sx >= 0 && sx < lx && sy >= 0 && sy < ly) {
                    newImage.setRGB(nx, ny, image.getRGB(sx, sy));
                }
            }
        }
        return newImage;
    }

    public BufferedImage grayscale() {
        int lx = image.getWidth();
        int ly = image.getHeight();

        BufferedImage newImage = ImageFactory.createEmptyImage(image);

        for (int y = 0; y < ly; y++) {
            for (int x = 0; x < lx; x++) {
                int gray = toGray(image.getRGB(x, y));
                newImage.setRGB(x, y, (gray << 16) | (gray << 8) | gray);
            }
        }
        return newImage;
    }

    public BufferedImage threshold(int value) {
        int lx = image.getWidth();
        int ly = image.getHeight();

        BufferedImage newImage = ImageFactory.createEmptyImage(image);

        for (int y = 0; y < ly; y++) {
            for (int x = 0; x < lx; x++) {
                int gray = toGray(image.getRGB(x, y));
                int binary = gray >= value ? 255 : 0;
                newImage.setRGB(x, y, (binary << 16) | (binary << 8) | binary);
            }
        }
        return newImage;
    }

    public BufferedImage lowPassFilter() {
        double[][] kernel = {
                {1 / 9.0, 1 / 9.0, 1 / 9.0},
                {1 / 9.0, 1 / 9.0, 1 / 9.0},
                {1 / 9.0, 1 / 9.0, 1 / 9.0}
        };
        return applyConvolution(kernel);
    }

    public BufferedImage highPassFilter() {
        double[][] kernel = {
                {0, -1, 0},
                {-1, 5, -1},
                {0, -1, 0}
        };
        return applyConvolution(kernel);
    }

    private static int toGray(int rgb) {
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;
        return (int) Math.round(0.299 * r + 0.587 * g + 0.114 * b);
    }

    private BufferedImage applyConvolution(double[][] kernel) {
        int lx = image.getWidth();
        int ly = image.getHeight();
        int kSize = kernel.length;
        int kOffset = kSize / 2;

        BufferedImage newImage = ImageFactory.createEmptyImage(image);

        for (int y = 0; y < ly; y++) {
            for (int x = 0; x < lx; x++) {
                double r = 0, g = 0, b = 0;

                for (int ky = 0; ky < kSize; ky++) {
                    for (int kx = 0; kx < kSize; kx++) {
                        int sx = clamp(x + kx - kOffset, 0, lx - 1);
                        int sy = clamp(y + ky - kOffset, 0, ly - 1);
                        int rgb = image.getRGB(sx, sy);
                        double weight = kernel[ky][kx];

                        r += weight * ((rgb >> 16) & 0xFF);
                        g += weight * ((rgb >> 8) & 0xFF);
                        b += weight * (rgb & 0xFF);
                    }
                }

                int nr = clampColor((int) Math.round(r));
                int ng = clampColor((int) Math.round(g));
                int nb = clampColor((int) Math.round(b));
                newImage.setRGB(x, y, (nr << 16) | (ng << 8) | nb);
            }
        }
        return newImage;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static int clampColor(int value) {
        return clamp(value, 0, 255);
    }
}