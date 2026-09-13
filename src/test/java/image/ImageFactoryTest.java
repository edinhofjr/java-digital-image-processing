package image;

import dev.edinho.pdi.entities.ImageFactory;
import dev.edinho.pdi.io.ImageRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

class ImageFactoryTest {
    @Test
    void loadImageFromDisk() throws IOException {
        BufferedImage image = ImageRepository.load(new File("src/test/resources/black_pixel.png"));
        Assertions.assertNotNull(image);
    }

    @Test
    void saveImageToDisk() throws IOException {
        BufferedImage image = ImageFactory.createEmptyImage(2, 2);
        Path savedFile = Path.of("target/test-output/saved.png");
        savedFile.getParent().toFile().mkdirs();

        ImageRepository.save(image, savedFile, "png");

        BufferedImage reloaded = ImageRepository.load(savedFile.toFile());
        Assertions.assertEquals(image.getWidth(), reloaded.getWidth());
        Assertions.assertEquals(image.getHeight(), reloaded.getHeight());
    }
}
