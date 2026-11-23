package dev.edinho.pdi;

import dev.edinho.pdi.challenges.DigitRecognizer;
import dev.edinho.pdi.challenges.LeafRustAnalyzer;
import dev.edinho.pdi.challenges.NutInspector;
import dev.edinho.pdi.challenges.PillInspector;
import dev.edinho.pdi.challenges.ShapeCounter;
import dev.edinho.pdi.entities.AppState;
import dev.edinho.pdi.entities.CoinDetector;
import dev.edinho.pdi.entities.FeatureExtractor;
import dev.edinho.pdi.entities.ImageManipulator;
import dev.edinho.pdi.entities.RegionFeatures;
import dev.edinho.pdi.io.ImagePicker;
import dev.edinho.pdi.io.ImageRepository;
import dev.edinho.pdi.io.ImageSaver;
import dev.edinho.pdi.io.dialog.DataInputDialog;
import dev.edinho.pdi.io.dialog.DialogFactory;
import dev.edinho.pdi.io.dialog.dto.RadiusRangeInput;
import dev.edinho.pdi.io.dialog.dto.RegionGrowInput;
import dev.edinho.pdi.io.dialog.dto.RotateInput;
import dev.edinho.pdi.io.dialog.dto.ScaleInput;
import dev.edinho.pdi.io.dialog.dto.ThresholdInput;
import dev.edinho.pdi.io.dialog.dto.TranslateInput;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.scene.Cursor;
import javafx.scene.control.Alert;
import javafx.scene.input.MouseEvent;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.text.Font;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;

public class MainController {
    private final AppState state = new AppState();
    private Stage stage;

    @FXML
    private Label statusLabel;
    @FXML
    private ImageView originalImageView;
    @FXML
    private ImageView processedImageView;

    private RegionGrowInput pendingRegionGrow;

    @FXML
    private void initialize() {
        originalImageView.setOnMouseClicked(this::onOriginalImageClicked);
    }

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    private Stage getStage() {
        return stage;
    }
    @FXML
    private void onOpen() throws IOException {
        Stage stage = getStage();
        File file = new ImagePicker(stage).choose();
        if (file != null) {
            state.setActualImage(ImageRepository.load(file));
            originalImageView.setImage(new Image(file.toURI().toString()));
            statusLabel.setText("Selecionado: " + file.getName());
        }
    }

    @FXML
    private void onTranslate() {
        DataInputDialog<TranslateInput> dialog = DialogFactory.translate(stage);

        dialog.showAndWait().ifPresent(input -> {
            ImageManipulator im = new ImageManipulator(state.getActualImage());
            BufferedImage translatedImage = im.translateProcess(input.x(), input.y());
            state.setTransformedImage(translatedImage);
            processedImageView.setImage(SwingFXUtils.toFXImage(translatedImage, null));
        });
    }
    @FXML
    private void onRotate() {
        DataInputDialog<RotateInput> dialog = DialogFactory.rotate(stage);

        dialog.showAndWait().ifPresent(input -> {
            ImageManipulator im = new ImageManipulator(state.getActualImage());
            BufferedImage rotatedImage = im.rotateProcess(input.angle());
            state.setTransformedImage(rotatedImage);
            processedImageView.setImage(SwingFXUtils.toFXImage(rotatedImage, null));
        });
    }

    @FXML
    private void onScale() {
        DataInputDialog<ScaleInput> dialog = DialogFactory.scale(stage);

        dialog.showAndWait().ifPresent(input -> {
            ImageManipulator im = new ImageManipulator(state.getActualImage());
            BufferedImage scaledImage = im.scale(input.scale());
            state.setTransformedImage(scaledImage);
            processedImageView.setImage(SwingFXUtils.toFXImage(scaledImage, null));
        });
    }

    @FXML
    private void onMirrorHorizontal() {
        ImageManipulator im = new ImageManipulator(state.getActualImage());
        BufferedImage mirroredImage = im.mirrorHorizontal();
        state.setTransformedImage(mirroredImage);
        processedImageView.setImage(SwingFXUtils.toFXImage(mirroredImage, null));
    }

    @FXML
    private void onMirrorVertical() {
        ImageManipulator im = new ImageManipulator(state.getActualImage());
        BufferedImage mirroredImage = im.mirrorVertical();
        state.setTransformedImage(mirroredImage);
        processedImageView.setImage(SwingFXUtils.toFXImage(mirroredImage, null));
    }

    @FXML
    private void onGrayscale() {
        ImageManipulator im = new ImageManipulator(state.getActualImage());
        BufferedImage grayscaleImage = im.grayscale();
        state.setTransformedImage(grayscaleImage);
        processedImageView.setImage(SwingFXUtils.toFXImage(grayscaleImage, null));
    }

    @FXML
    private void onLowPassFilter() {
        ImageManipulator im = new ImageManipulator(state.getActualImage());
        BufferedImage filteredImage = im.lowPassFilter();
        state.setTransformedImage(filteredImage);
        processedImageView.setImage(SwingFXUtils.toFXImage(filteredImage, null));
    }

    @FXML
    private void onHighPassFilter() {
        ImageManipulator im = new ImageManipulator(state.getActualImage());
        BufferedImage filteredImage = im.highPassFilter();
        state.setTransformedImage(filteredImage);
        processedImageView.setImage(SwingFXUtils.toFXImage(filteredImage, null));
    }

    @FXML
    private void onThreshold() {
        DataInputDialog<ThresholdInput> dialog = DialogFactory.threshold(stage);

        dialog.showAndWait().ifPresent(input -> {
            ImageManipulator im = new ImageManipulator(state.getActualImage());
            BufferedImage thresholdedImage = im.threshold(input.value());
            state.setTransformedImage(thresholdedImage);
            processedImageView.setImage(SwingFXUtils.toFXImage(thresholdedImage, null));
        });
    }

    @FXML
    private void onErosion() {
        ImageManipulator im = new ImageManipulator(state.getActualImage());
        BufferedImage erodedImage = im.erosion();
        state.setTransformedImage(erodedImage);
        processedImageView.setImage(SwingFXUtils.toFXImage(erodedImage, null));
    }

    @FXML
    private void onDilation() {
        ImageManipulator im = new ImageManipulator(state.getActualImage());
        BufferedImage dilatedImage = im.dilation();
        state.setTransformedImage(dilatedImage);
        processedImageView.setImage(SwingFXUtils.toFXImage(dilatedImage, null));
    }

    @FXML
    private void onOpening() {
        ImageManipulator im = new ImageManipulator(state.getActualImage());
        BufferedImage openedImage = im.opening();
        state.setTransformedImage(openedImage);
        processedImageView.setImage(SwingFXUtils.toFXImage(openedImage, null));
    }

    @FXML
    private void onClosing() {
        ImageManipulator im = new ImageManipulator(state.getActualImage());
        BufferedImage closedImage = im.closing();
        state.setTransformedImage(closedImage);
        processedImageView.setImage(SwingFXUtils.toFXImage(closedImage, null));
    }

    @FXML
    private void onThinning() {
        showProcessed(new ImageManipulator(state.getActualImage()).thinning());
    }

    @FXML
    private void onSobel() {
        showProcessed(new ImageManipulator(state.getActualImage()).sobel());
    }

    @FXML
    private void onPrewitt() {
        showProcessed(new ImageManipulator(state.getActualImage()).prewitt());
    }

    @FXML
    private void onLaplacian() {
        showProcessed(new ImageManipulator(state.getActualImage()).laplacian());
    }

    @FXML
    private void onExtractFeatures() {
        DialogFactory.features(stage, "Extrair Características").showAndWait().ifPresent(input -> {
            FeatureExtractor extractor = new FeatureExtractor(state.getActualImage(), input.connectivity());
            List<RegionFeatures> regions = extractor.extract(input.threshold());
            showProcessed(extractor.annotate(regions));
            statusLabel.setText("Objetos encontrados: " + regions.size());

            showReport("Características dos Objetos", "Objetos encontrados: " + regions.size(),
                    FeatureExtractor.report(regions));
        });
    }

    @FXML
    private void onSegmentation() {
        DialogFactory.features(stage, "Segmentação Binária").showAndWait().ifPresent(input -> {
            FeatureExtractor extractor = new FeatureExtractor(state.getActualImage(), input.connectivity());
            showProcessed(FeatureExtractor.maskImage(extractor.binarize(input.threshold())));
        });
    }

    @FXML
    private void onLabeling() {
        DialogFactory.features(stage, "Rotulação").showAndWait().ifPresent(input -> {
            FeatureExtractor extractor = new FeatureExtractor(state.getActualImage(), input.connectivity());
            FeatureExtractor.Labeling labeling = extractor.label(input.threshold());
            showProcessed(FeatureExtractor.labelImage(labeling));
            statusLabel.setText("Componentes conexos (vizinhança-" + input.connectivity() + "): "
                    + labeling.regions().size());
        });
    }

    @FXML
    private void onRegionGrow() {
        if (state.getActualImage() == null) return;
        DialogFactory.regionGrow(stage).showAndWait().ifPresent(input -> {
            pendingRegionGrow = input;
            originalImageView.setCursor(Cursor.CROSSHAIR);
            statusLabel.setText("Clique na imagem original para escolher a semente");
        });
    }

    private void onOriginalImageClicked(MouseEvent event) {
        RegionGrowInput input = pendingRegionGrow;
        BufferedImage img = state.getActualImage();
        if (input == null || img == null) return;
        pendingRegionGrow = null;
        originalImageView.setCursor(Cursor.DEFAULT);

        // O ImageView está escalado (preserveRatio): converte a coordenada exibida em pixel.
        double scale = originalImageView.getBoundsInLocal().getWidth() / img.getWidth();
        int x = (int) Math.min(img.getWidth() - 1, Math.max(0, event.getX() / scale));
        int y = (int) Math.min(img.getHeight() - 1, Math.max(0, event.getY() / scale));

        FeatureExtractor extractor = new FeatureExtractor(img, input.connectivity());
        boolean[][] region = extractor.regionGrow(x, y, input.tolerance());
        showProcessed(extractor.overlay(region));

        List<RegionFeatures> features = extractor.extract(region);
        statusLabel.setText("Semente (" + x + ", " + y + "): região com "
                + features.stream().mapToInt(RegionFeatures::area).sum() + " pixels");
        showReport("Crescimento de Região", "Semente em (" + x + ", " + y + ")", FeatureExtractor.report(features));
    }

    @FXML
    private void onHighlightCoins() {
        CoinDetector.Result result = new CoinDetector(state.getActualImage()).detect();
        showProcessed(result.highlighted());
        statusLabel.setText("Moedas encontradas: " + result.coins().size());
        showReport("Moedas", "Moedas encontradas: " + result.coins().size(), CoinDetector.report(result));
    }

    @FXML
    private void onCoinsBySize() {
        BufferedImage img = state.getActualImage();
        if (img == null) return;
        CoinDetector detector = new CoinDetector(img);
        CoinDetector.Result result = detector.detect();

        DataInputDialog<RadiusRangeInput> dialog = DialogFactory.radiusRange(stage);
        dialog.setHeaderText("Raios detectados (px): " + result.coins().stream()
                .mapToDouble(RegionFeatures::equivalentRadius)
                .sorted()
                .mapToObj(r -> String.format("%.0f", r))
                .toList());

        dialog.showAndWait().ifPresent(input -> {
            List<RegionFeatures> selected = CoinDetector.filterByRadius(result, input.min(), input.max());
            showProcessed(detector.isolateByRadius(result, input.min(), input.max()));
            statusLabel.setText("Moedas com raio entre " + input.min() + " e " + input.max() + ": " + selected.size());
        });
    }

    private void showReport(String title, String header, String text) {
        TextArea report = new TextArea(text);
        report.setEditable(false);
        report.setFont(Font.font("Monospaced", 12));
        report.setPrefSize(640, 360);

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.initOwner(stage);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert   .getDialogPane().setContent(report);
        alert.setResizable(true);
        alert.showAndWait();
    }

    private void showProcessed(BufferedImage processed) {
        state.setTransformedImage(processed);
        processedImageView.setImage(SwingFXUtils.toFXImage(processed, null));
    }

    private boolean requireImage() {
        if (state.getActualImage() != null) return true;
        Alert alert = new Alert(Alert.AlertType.WARNING, "Abra uma imagem em Arquivo > Abrir antes de executar o desafio.");
        alert.initOwner(stage);
        alert.setHeaderText(null);
        alert.showAndWait();
        return false;
    }

    @FXML
    private void onNutInspection() {
        if (!requireImage()) return;
        NutInspector.Result result = NutInspector.inspect(state.getActualImage());
        showProcessed(result.annotated());
        statusLabel.setText("Porcas encontradas: " + result.nuts().size());
        showReport("Controle de Qualidade - Porcas", "Porcas encontradas: " + result.nuts().size(), result.report());
    }

    @FXML
    private void onDigitRecognition() {
        if (!requireImage()) return;
        DigitRecognizer.Result result = DigitRecognizer.recognize(state.getActualImage());
        showProcessed(result.annotated());
        String header = result.digits().size() == 1
                ? "Número " + result.digits().getFirst().value() + ": " + result.digits().getFirst().parity()
                : "Algarismos encontrados: " + result.digits().size();
        statusLabel.setText(header);
        showReport("Reconhecimento de Números", header, result.report());
    }

    @FXML
    private void onPillInspection() {
        if (!requireImage()) return;
        PillInspector.Result result = PillInspector.inspect(state.getActualImage());
        showProcessed(result.annotated());
        String header = "Comprimidos: " + result.pills().size() + " | com problema: " + result.defective();
        statusLabel.setText(header);
        showReport("Controle de Qualidade - Comprimidos", header, result.report());
    }

    @FXML
    private void onShapeCount() {
        if (!requireImage()) return;
        ShapeCounter.Result result = ShapeCounter.count(state.getActualImage());
        showProcessed(result.annotated());
        String header = "Círculos: " + result.count(ShapeCounter.Kind.CIRCLE)
                + " | Quadrados: " + result.count(ShapeCounter.Kind.SQUARE);
        statusLabel.setText(header);
        showReport("Contagem de Círculos e Quadrados", header, result.report());
    }

    @FXML
    private void onLeafAnalysis() {
        if (!requireImage()) return;
        LeafRustAnalyzer.Result result = LeafRustAnalyzer.analyze(state.getActualImage());
        showProcessed(result.annotated());
        String header = String.format("%s (%.2f%% com ferrugem)", result.status().label(), result.rustPercent());
        statusLabel.setText(header);
        showReport("Análise de Plantas", header, result.report());
    }

    @FXML
    private void onSave() throws IOException {
        ImageSaver saver = new ImageSaver(stage);

        File saveFile = saver.choose();

        if (saveFile != null) {
            BufferedImage img = state.getTransformedImage();
            if (img == null) {
                throw new IllegalArgumentException("Doesnt has any transformed image.");
            }

            ImageRepository.save(
                img, saveFile, extractFormat(saveFile)
            );
        }

    }

    private static String extractFormat(File file) {
        String name = file.getName();
        int dotIndex = name.lastIndexOf('.');
        if (dotIndex == -1 || dotIndex == name.length() - 1) {
            return "png";
        }
        return name.substring(dotIndex + 1);
    }

    @FXML
    private void onExit() {
        Platform.exit();
    }

    @FXML
    private void onUndo() {
        statusLabel.setText("Editar > Desfazer clicado");
    }

    @FXML
    private void onAbout() {
        statusLabel.setText("PDI - versão de teste");
    }
}
