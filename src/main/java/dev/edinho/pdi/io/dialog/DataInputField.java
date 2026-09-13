package dev.edinho.pdi.io.dialog;

import javafx.scene.control.TextFormatter;

import java.util.function.Supplier;

public record DataInputField(String key, String label, String defaultValue, Supplier<TextFormatter<String>> formatterSupplier) {

    public static DataInputField text(String key, String label) {
        return new DataInputField(key, label, "", null);
    }

    public static DataInputField integer(String key, String label) {
        return new DataInputField(key, label, "0", () -> new TextFormatter<>(change ->
                change.getControlNewText().matches("-?\\d*") ? change : null));
    }

    public static DataInputField decimal(String key, String label) {
        return new DataInputField(key, label, "0", () -> new TextFormatter<>(change ->
                change.getControlNewText().matches("-?\\d*(\\.\\d*)?") ? change : null));
    }
}
