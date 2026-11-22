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

    public BufferedImage erosion() {
        return applyMorphology(3, true);
    }

    public BufferedImage dilation() {
        return applyMorphology(3, false);
    }

    public BufferedImage opening() {
        return new ImageManipulator(erosion()).dilation();
    }

    public BufferedImage closing() {
        return new ImageManipulator(dilation()).erosion();
    }

    /**
     * Afinamento (esqueletização) de Zhang-Suen. A imagem é binarizada com limiar de Otsu
     * (a classe minoritária é o objeto) e o resultado é o esqueleto branco sobre fundo preto.
     */
    public BufferedImage thinning() {
        // Otsu separa em [0, t] e (t, 255]; binarize considera claro o que é >= limiar.
        int threshold = CoinDetector.otsu(grayMatrix()) + 1;
        boolean[][] skeleton = thin(new FeatureExtractor(image).binarize(threshold));

        BufferedImage newImage = ImageFactory.createEmptyImage(image);
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                newImage.setRGB(x, y, skeleton[y][x] ? 0xFFFFFF : 0);
            }
        }
        return newImage;
    }

    /** Zhang-Suen (com a correção de Lü-Wang): remove iterativamente pixels de borda até restar o esqueleto. */
    public static boolean[][] thin(boolean[][] input) {
        int ly = input.length;
        int lx = input[0].length;
        boolean[][] img = new boolean[ly][];
        for (int y = 0; y < ly; y++) img[y] = input[y].clone();

        java.util.List<int[]> toRemove = new java.util.ArrayList<>();
        boolean changed = true;
        while (changed) {
            changed = false;
            for (int step = 0; step < 2; step++) {
                toRemove.clear();
                for (int y = 0; y < ly; y++) {
                    for (int x = 0; x < lx; x++) {
                        if (img[y][x] && shouldRemove(img, x, y, step)) toRemove.add(new int[]{x, y});
                    }
                }
                for (int[] p : toRemove) img[p[1]][p[0]] = false;
                changed |= !toRemove.isEmpty();
            }
        }
        return img;
    }

    private static boolean shouldRemove(boolean[][] img, int x, int y, int step) {
        // Vizinhos P2..P9 em sentido horário a partir do norte.
        int p2 = px(img, x, y - 1), p3 = px(img, x + 1, y - 1), p4 = px(img, x + 1, y);
        int p5 = px(img, x + 1, y + 1), p6 = px(img, x, y + 1), p7 = px(img, x - 1, y + 1);
        int p8 = px(img, x - 1, y), p9 = px(img, x - 1, y - 1);
        int[] n = {p2, p3, p4, p5, p6, p7, p8, p9};

        // Nº de vizinhos de objeto. O Zhang-Suen original usa 2 <= B <= 6, o que apaga por
        // completo linhas diagonais de 2 px; a variante de Lü & Wang (B >= 3) as preserva.
        int b = p2 + p3 + p4 + p5 + p6 + p7 + p8 + p9;
        if (b < 3 || b > 6) return false;

        int a = 0;                                       // nº de transições 0 -> 1
        for (int i = 0; i < 8; i++) {
            if (n[i] == 0 && n[(i + 1) % 8] == 1) a++;
        }
        if (a != 1) return false;

        if (step == 0) return p2 * p4 * p6 == 0 && p4 * p6 * p8 == 0;
        return p2 * p4 * p8 == 0 && p2 * p6 * p8 == 0;
    }

    private static int px(boolean[][] img, int x, int y) {
        if (y < 0 || y >= img.length || x < 0 || x >= img[0].length) return 0;
        return img[y][x] ? 1 : 0;
    }

    public BufferedImage sobel() {
        return applyGradient(
                new double[][]{{-1, 0, 1}, {-2, 0, 2}, {-1, 0, 1}},
                new double[][]{{-1, -2, -1}, {0, 0, 0}, {1, 2, 1}}
        );
    }

    public BufferedImage prewitt() {
        return applyGradient(
                new double[][]{{-1, 0, 1}, {-1, 0, 1}, {-1, 0, 1}},
                new double[][]{{-1, -1, -1}, {0, 0, 0}, {1, 1, 1}}
        );
    }

    public BufferedImage laplacian() {
        double[][] kernel = {
                {0, 1, 0},
                {1, -4, 1},
                {0, 1, 0}
        };
        double[][] gray = grayMatrix();
        BufferedImage newImage = ImageFactory.createEmptyImage(image);
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int v = clampColor((int) Math.round(Math.abs(convolveAt(gray, kernel, x, y))));
                newImage.setRGB(x, y, (v << 16) | (v << 8) | v);
            }
        }
        return newImage;
    }

    /** Magnitude do gradiente sqrt(Gx² + Gy²) sobre a imagem em tons de cinza. */
    private BufferedImage applyGradient(double[][] kernelX, double[][] kernelY) {
        double[][] gray = grayMatrix();
        BufferedImage newImage = ImageFactory.createEmptyImage(image);
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                double gx = convolveAt(gray, kernelX, x, y);
                double gy = convolveAt(gray, kernelY, x, y);
                int v = clampColor((int) Math.round(Math.hypot(gx, gy)));
                newImage.setRGB(x, y, (v << 16) | (v << 8) | v);
            }
        }
        return newImage;
    }

    private double[][] grayMatrix() {
        int lx = image.getWidth();
        int ly = image.getHeight();
        double[][] gray = new double[ly][lx];
        for (int y = 0; y < ly; y++) {
            for (int x = 0; x < lx; x++) {
                gray[y][x] = toGray(image.getRGB(x, y));
            }
        }
        return gray;
    }

    private static double convolveAt(double[][] gray, double[][] kernel, int x, int y) {
        int ly = gray.length;
        int lx = gray[0].length;
        int kOffset = kernel.length / 2;
        double sum = 0;
        for (int ky = 0; ky < kernel.length; ky++) {
            for (int kx = 0; kx < kernel.length; kx++) {
                int sx = clamp(x + kx - kOffset, 0, lx - 1);
                int sy = clamp(y + ky - kOffset, 0, ly - 1);
                sum += kernel[ky][kx] * gray[sy][sx];
            }
        }
        return sum;
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

    private BufferedImage applyMorphology(int size, boolean erode) {
        int lx = image.getWidth();
        int ly = image.getHeight();
        int offset = size / 2;

        BufferedImage newImage = ImageFactory.createEmptyImage(image);

        for (int y = 0; y < ly; y++) {
            for (int x = 0; x < lx; x++) {
                int r = erode ? 255 : 0;
                int g = erode ? 255 : 0;
                int b = erode ? 255 : 0;

                for (int ky = 0; ky < size; ky++) {
                    for (int kx = 0; kx < size; kx++) {
                        int sx = clamp(x + kx - offset, 0, lx - 1);
                        int sy = clamp(y + ky - offset, 0, ly - 1);
                        int rgb = image.getRGB(sx, sy);
                        int sr = (rgb >> 16) & 0xFF;
                        int sg = (rgb >> 8) & 0xFF;
                        int sb = rgb & 0xFF;

                        if (erode) {
                            r = Math.min(r, sr);
                            g = Math.min(g, sg);
                            b = Math.min(b, sb);
                        } else {
                            r = Math.max(r, sr);
                            g = Math.max(g, sg);
                            b = Math.max(b, sb);
                        }
                    }
                }

                newImage.setRGB(x, y, (r << 16) | (g << 8) | b);
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