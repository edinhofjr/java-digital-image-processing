package dev.edinho.pdi.io;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class ImageRepository {
    private ImageRepository() {
        /* This is a Util class */
    }

    public static BufferedImage load(File file) throws IOException {
        BufferedImage image = ImageIO.read(file);
        if (image == null) {
            throw new IOException("Formato de imagem não suportado: " + file.getPath());
        }
        return image;
    }

    public static void save(BufferedImage image, File file, String format) throws IOException {
        boolean ok = ImageIO.write(image, format, file);
        if (!ok) throw new IOException("nenhum writer disponível para o formato: " + format);
    }
}
