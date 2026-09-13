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
}