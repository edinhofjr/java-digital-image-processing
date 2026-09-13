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
                .field(new DataInputField("x", "Valor para X:", DataInputFieldType.INTEGER))
                .field(new DataInputField("y", "Valor para Y:", DataInputFieldType.INTEGER))
                .resultMapper(values -> new TranslateInput(
                        Integer.parseInt(values.get("x")),
                        Integer.parseInt(values.get("y"))))
                .build();
    }

    public static DataInputDialog<RotateInput> rotate(Window owner) {
        return new DataInputDialogBuilder<RotateInput>()
                .title("Rotacionar")
                .owner(owner)
                .field(new DataInputField("angle", "Ângulo (graus):", DataInputFieldType.INTEGER))
                .resultMapper(values -> new RotateInput(
                        Integer.parseInt(values.get("angle"))))
                .build();
    }
}
