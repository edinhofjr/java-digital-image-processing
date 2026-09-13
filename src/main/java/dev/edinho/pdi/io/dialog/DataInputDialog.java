package dev.edinho.pdi.io.dialog;

import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.stage.Modality;
import javafx.stage.Window;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class DataInputDialog<R> extends Dialog<R> {

    private final Map<String, TextField> fields = new LinkedHashMap<>();

    DataInputDialog(String title, Window owner, List<DataInputField> dataFields,
                     Function<Map<String, String>, R> resultMapper) {
        setTitle(title);

        if (owner != null) {
            initOwner(owner);
            initModality(Modality.APPLICATION_MODAL);
        }

        GridPane grid = new GridPane();
        grid.setHgap(8);
        grid.setVgap(8);

        int row = 0;
        for (DataInputField field : dataFields) {
            TextField textField = new TextField(field.defaultValue());
            if (field.formatterSupplier() != null) {
                textField.setTextFormatter(field.formatterSupplier().get());
            }

            Label label = new Label(field.label());
            label.setLabelFor(textField);

            grid.addRow(row++, label, textField);
            fields.put(field.key(), textField);
        }
        getDialogPane().setContent(grid);

        ButtonType okButtonType = new ButtonType("Confirmar", ButtonBar.ButtonData.OK_DONE);
        getDialogPane().getButtonTypes().addAll(okButtonType, ButtonType.CANCEL);

        setResultConverter(button -> {
            if (button == null || button.getButtonData() != ButtonBar.ButtonData.OK_DONE) {
                return null;
            }
            Map<String, String> values = new LinkedHashMap<>();
            fields.forEach((key, textField) -> values.put(key, textField.getText()));
            return resultMapper.apply(values);
        });

        setOnShown(ev -> Platform.runLater(() ->
                fields.values().stream().findFirst().ifPresent(Node::requestFocus)
        ));
    }
}
