package dev.edinho.pdi.io.dialog;

import dev.edinho.pdi.io.dialog.dto.RotateInput;
import dev.edinho.pdi.io.dialog.dto.TranslateInput;
import javafx.stage.Window;

public final class DialogFactory {

    private DialogFactory() {
    }

    public static DataInputDialog<TranslateInput> translate(Window owner) {
        return new DataInputDialogBuilder<TranslateInput>()
                .title("Translatar")
                .owner(owner)
                .field(DataInputField.integer("x", "Valor para X:"))
                .field(DataInputField.integer("y", "Valor para Y:"))
                .resultMapper(values -> new TranslateInput(
                        Integer.parseInt(values.get("x")),
                        Integer.parseInt(values.get("y"))))
                .build();
    }

    public static DataInputDialog<RotateInput> rotate(Window owner) {
        return new DataInputDialogBuilder<RotateInput>()
                .title("Rotacionar")
                .owner(owner)
                .field(DataInputField.integer("angle", "Ângulo (graus):"))
                .resultMapper(values -> new RotateInput(
                        Integer.parseInt(values.get("angle"))))
                .build();
    }
}
