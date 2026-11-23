package dev.edinho.pdi;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Stage;

import java.util.Objects;


public class App extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("MainView.fxml"));
        Parent root = loader.load();

        MainController controller = loader.getController();
        controller.setStage(stage);

        Scene scene = new Scene(root, 800, 600);
        scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("styles.css")).toExternalForm());

        Thread.currentThread().setUncaughtExceptionHandler((thread, error) -> {
            Throwable cause = error;
            while (cause.getCause() != null) cause = cause.getCause();
            error.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR, cause.getClass().getSimpleName() + ": " + cause.getMessage());
            alert.initOwner(stage);
            alert.setHeaderText("Erro ao executar a operação");
            alert.showAndWait();
        });

        stage.setTitle("PDI - Edio");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
