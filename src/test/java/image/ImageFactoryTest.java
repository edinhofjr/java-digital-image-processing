package image;

import dev.edinho.pdi.entities.ImageFactory;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;
import java.io.IOException;

class ImageFactoryTest {
    @Test
    void loadImageFromDisk() throws IOException {
        BufferedImage image = ImageFactory.loadFromPath("src/test/resources/black_pixel.png");
        Assertions.assertNotNull(image);
    }
}
