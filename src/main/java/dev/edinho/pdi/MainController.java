package dev.edinho.pdi;

import dev.edinho.pdi.io.ImagePicker;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;

public class MainController {

    @FXML
    private Label statusLabel;

    @FXML
    private ImageView originalImageView;

    @FXML
    private void onOpen() throws IOException {
        Stage stage = (Stage) statusLabel.getScene().getWindow();
        File file = new ImagePicker(stage).choose();
        if (file != null) {
            originalImageView.setImage(new Image(file.toURI().toString()));
            statusLabel.setText("Selecionado: " + file.getName());
        }
    }

    @FXML
    private void onSave() {
        statusLabel.setText("Arquivo > Salvar clicado");
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
