package dev.edinho.pdi;

import dev.edinho.pdi.entities.AppState;
import dev.edinho.pdi.entities.ImageManipulator;
import dev.edinho.pdi.io.ImagePicker;
import dev.edinho.pdi.io.ImageRepository;
import dev.edinho.pdi.io.ImageSaver;
import dev.edinho.pdi.io.dialog.DataInputDialog;
import dev.edinho.pdi.io.dialog.DialogFactory;
import dev.edinho.pdi.io.dialog.dto.RotateInput;
import dev.edinho.pdi.io.dialog.dto.ScaleInput;
import dev.edinho.pdi.io.dialog.dto.ThresholdInput;
import dev.edinho.pdi.io.dialog.dto.TranslateInput;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class MainController {
    private final AppState state = new AppState();
    private Stage stage;

    @FXML
    private Label statusLabel;
    @FXML
    private ImageView originalImageView;
    @FXML
    private ImageView processedImageView;

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
