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

        Matrix translationMatrix = new Matrix(new double[][]{
                {1, 0, x},
                {0, 1, y},
                {0, 0, 1}
        });

        int inicioX = Math.max(0, -x), fimX = Math.min(lx, lx - x);
        int inicioY = Math.max(0, -y), fimY = Math.min(ly, ly - y);

        for (int iy = inicioY; iy < fimY; iy++) {
            for (int ix = inicioX; ix < fimX; ix++) {
                Matrix position = Matrices.columnsFromArray(new double[]{ix, iy, 1});
                Matrix newPosition = MatrixOperator.product(translationMatrix, position);

                int nx = (int) newPosition.get(0, 0);
                int ny = (int) newPosition.get(1, 0);

                newImage.setRGB(nx, ny, image.getRGB(ix, iy));
            }
        }
        return newImage;
    }

    public BufferedImage rotateProcess(int angleDegrees) {
        int lx = image.getWidth();
        int ly = image.getHeight();

        BufferedImage newImage = ImageFactory.createEmptyImage(image);

        double angleRadians = Math.toRadians(-angleDegrees);
        double cos = Math.cos(angleRadians);
        double sin = Math.sin(angleRadians);
        double cx = lx / 2.0;
        double cy = ly / 2.0;

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

        for (int ny = 0; ny < ly; ny++) {
            for (int nx = 0; nx < lx; nx++) {
                Matrix position = Matrices.columnsFromArray(new double[]{nx, ny, 1});
                Matrix sourcePosition = MatrixOperator.product(inverseRotationMatrix, position);

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