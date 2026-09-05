package dev.edinho.pdi.io;

import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;

public class ImagePicker {

    private final FileChooser fileChooser = new FileChooser();
    private final Stage parent;

    public ImagePicker(Stage parent) {
        this.parent = parent;
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Imagem", "*.png", "*.jpg", "*.jpeg"));
        fileChooser.setTitle("Escolha uma imagem");
        fileChooser.setInitialDirectory(new java.io.File(System.getProperty("user.home")));
    }

    public File choose() {
        return fileChooser.showOpenDialog(
            parent
        );
    }
}
