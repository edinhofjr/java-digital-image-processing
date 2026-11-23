package challenges;

import dev.edinho.pdi.challenges.DigitRecognizer;
import dev.edinho.pdi.challenges.LeafRustAnalyzer;
import dev.edinho.pdi.challenges.NutInspector;
import dev.edinho.pdi.challenges.PillInspector;
import dev.edinho.pdi.challenges.ShapeCounter;
import dev.edinho.pdi.io.ImageRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

class FullResolutionChallengesTest {
    private static final File DIR = new File("imagens/desafios");

    private static BufferedImage load(String name) throws IOException {
        File file = new File(DIR, name);
        Assertions.assertTrue(file.exists(), file.getPath());
        return ImageRepository.load(file);
    }

    @Test
    void nuts() throws IOException {
        NutInspector.Result mixed = NutInspector.inspect(load("Oito Porcas Metálicas Variadas.png"));
        Assertions.assertEquals(8, mixed.nuts().size());
        mixed.nuts().forEach(n -> Assertions.assertTrue(n.hasHole() && n.holeArea() < n.area(), "porca " + n.number()));

        NutInspector.Result grid = NutInspector.inspect(load("Porcas Hexagonais Metálicas em Grade.png"));
        Assertions.assertEquals(6, grid.nuts().size());
        double min = grid.nuts().stream().mapToDouble(NutInspector.Nut::holeArea).min().orElseThrow();
        double max = grid.nuts().stream().mapToDouble(NutInspector.Nut::holeArea).max().orElseThrow();
        Assertions.assertTrue(max / min < 1.3);
    }

    @Test
    void digits() throws IOException {
        DigitRecognizer.Result result = DigitRecognizer.recognize(load("Grade de dígitos numerados.png"));
        Assertions.assertEquals(List.of(0, 1, 2, 3, 4, 5, 6, 7, 8, 9),
                result.digits().stream().map(DigitRecognizer.Digit::value).toList());
    }

    @Test
    void pills() throws IOException {
        PillInspector.Result first = PillInspector.inspect(load("Grade de 12 comprimidos brancos marcados.png"));
        Assertions.assertEquals(12, first.pills().size());
        Assertions.assertEquals(Set.of(4, 5, 7, 10), defective(first));
        Assertions.assertTrue(first.pills().get(4).problems().contains("rachado"));

        PillInspector.Result second = PillInspector.inspect(load("Grade de 15 Comprimidos Brancos Variados.png"));
        Assertions.assertEquals(15, second.pills().size());
        Assertions.assertTrue(defective(second).containsAll(Set.of(3, 5, 7, 8, 10, 12)));
        Assertions.assertTrue(second.pills().get(7).problems().contains("rachado"));
    }

    private static Set<Integer> defective(PillInspector.Result result) {
        return result.pills().stream().filter(p -> !p.ok()).map(PillInspector.Pill::number).collect(Collectors.toSet());
    }

    @Test
    void shapes() throws IOException {
        ShapeCounter.Result grid = ShapeCounter.count(load("Grade de Formas Geométricas Coloridas.png"));
        Assertions.assertEquals(10, grid.count(ShapeCounter.Kind.CIRCLE));
        Assertions.assertEquals(10, grid.count(ShapeCounter.Kind.SQUARE));

        ShapeCounter.Result board = ShapeCounter.count(load("Mosaico Geométrico Colorido e Simétrico.png"));
        Assertions.assertEquals(5, board.count(ShapeCounter.Kind.CIRCLE));
        Assertions.assertEquals(5, board.count(ShapeCounter.Kind.SQUARE));
    }

    @Test
    void leaves() throws IOException {
        Assertions.assertEquals(LeafRustAnalyzer.Status.HEALTHY,
                LeafRustAnalyzer.analyze(load("Folha Verde Serrilhada em Detalhe.png")).status());
        Assertions.assertEquals(LeafRustAnalyzer.Status.OBSERVATION,
                LeafRustAnalyzer.analyze(load("Folha Verde com Manchas de Ferrugem.png")).status());
        Assertions.assertEquals(LeafRustAnalyzer.Status.SICK,
                LeafRustAnalyzer.analyze(load("Folha Verde com Manchas de Ferrugem (1).png")).status());
    }
}
