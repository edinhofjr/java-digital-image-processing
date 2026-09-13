package transformations;

import dev.edinho.pdi.entities.ImageManipulator;
import dev.edinho.pdi.io.ImageRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

class TranslateTransformationTest {
    @Test
    void translateImage() throws IOException {
        BufferedImage image = ImageRepository.load(new File("src/test/resources/2x2.png"));
        ImageManipulator im = new ImageManipulator(image);
        BufferedImage translated = im.translateProcess(1, 0);

        Path savedFile = Path.of("target/test-output/translated.png");
        savedFile.getParent().toFile().mkdirs();
        ImageRepository.save(translated, savedFile.toFile(), "png");

        Assertions.assertTrue(savedFile.toFile().exists());
    }
}
