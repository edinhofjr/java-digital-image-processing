package dev.edinho.pdi.io.dialog;

import dev.edinho.pdi.io.dialog.dto.FeatureInput;
import dev.edinho.pdi.io.dialog.dto.RadiusRangeInput;
import dev.edinho.pdi.io.dialog.dto.RegionGrowInput;
import dev.edinho.pdi.io.dialog.dto.RotateInput;
import dev.edinho.pdi.io.dialog.dto.ScaleInput;
import dev.edinho.pdi.io.dialog.dto.ThresholdInput;
import dev.edinho.pdi.io.dialog.dto.TranslateInput;
import javafx.scene.control.TextFormatter;
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

    public static DataInputDialog<ScaleInput> scale(Window owner) {
        return new DataInputDialogBuilder<ScaleInput>()
                .title("Escalar")
                .owner(owner)
                .field(DataInputField.decimal("scale", "Escala:"))
                .resultMapper(values -> new ScaleInput(
                        Double.parseDouble(values.get("scale"))))
                .build();
    }

    private static DataInputField connectivityField() {
        return new DataInputField("connectivity", "Vizinhança (4 ou 8):", "8", () -> new TextFormatter<>(change ->
                change.getControlNewText().matches("[48]?") ? change : null));
    }

    private static DataInputField integerWithDefault(String key, String label, String defaultValue) {
        DataInputField base = DataInputField.integer(key, label);
        return new DataInputField(key, label, defaultValue, base.formatterSupplier());
    }

    private static int parseConnectivity(String value) {
        return "4".equals(value) ? 4 : 8;
    }

    public static DataInputDialog<FeatureInput> features(Window owner, String title) {
        return new DataInputDialogBuilder<FeatureInput>()
                .title(title)
                .owner(owner)
                .field(integerWithDefault("threshold", "Limiar (0-255):", "128"))
                .field(connectivityField())
                .resultMapper(values -> new FeatureInput(
                        Integer.parseInt(values.get("threshold")),
                        parseConnectivity(values.get("connectivity"))))
                .build();
    }

    public static DataInputDialog<RegionGrowInput> regionGrow(Window owner) {
        return new DataInputDialogBuilder<RegionGrowInput>()
                .title("Crescimento de Região")
                .owner(owner)
                .field(integerWithDefault("tolerance", "Tolerância de cinza (0-255):", "20"))
                .field(connectivityField())
                .resultMapper(values -> new RegionGrowInput(
                        Integer.parseInt(values.get("tolerance")),
                        parseConnectivity(values.get("connectivity"))))
                .build();
    }

    public static DataInputDialog<RadiusRangeInput> radiusRange(Window owner) {
        return new DataInputDialogBuilder<RadiusRangeInput>()
                .title("Moedas por Tamanho")
                .owner(owner)
                .field(DataInputField.decimal("min", "Raio mínimo (px):"))
                .field(DataInputField.decimal("max", "Raio máximo (px):"))
                .resultMapper(values -> new RadiusRangeInput(
                        Double.parseDouble(values.get("min")),
                        Double.parseDouble(values.get("max"))))
                .build();
    }

    public static DataInputDialog<ThresholdInput> threshold(Window owner) {
        return threshold(owner, "Threshold");
    }

    public static DataInputDialog<ThresholdInput> threshold(Window owner, String title) {
        return new DataInputDialogBuilder<ThresholdInput>()
                .title(title)
                .owner(owner)
                .field(DataInputField.integer("value", "Limiar (0-255):"))
                .resultMapper(values -> new ThresholdInput(
                        Integer.parseInt(values.get("value"))))
                .build();
    }
}
