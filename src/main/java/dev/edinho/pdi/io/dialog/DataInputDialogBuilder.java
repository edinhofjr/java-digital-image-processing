package dev.edinho.pdi.io.dialog;

import javafx.stage.Window;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class DataInputDialogBuilder<R> {

    private final List<DataInputField> dataFields = new ArrayList<>();
    private String title = "";
    private Window owner;
    private Function<Map<String, String>, R> resultMapper;

    public DataInputDialogBuilder<R> title(String title) {
        this.title = title;
        return this;
    }

    public DataInputDialogBuilder<R> owner(Window owner) {
        this.owner = owner;
        return this;
    }

    public DataInputDialogBuilder<R> field(DataInputField field) {
        this.dataFields.add(field);
        return this;
    }

    public DataInputDialogBuilder<R> resultMapper(Function<Map<String, String>, R> resultMapper) {
        this.resultMapper = resultMapper;
        return this;
    }

    public DataInputDialog<R> build() {
        if (resultMapper == null) {
            throw new IllegalStateException("resultMapper é obrigatório");
        }
        return new DataInputDialog<>(title, owner, dataFields, resultMapper);
    }
}
