package org.jetuml.ui;

import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import org.jetuml.command.Command;
import org.jetuml.command.EditElementCommand;
import org.jetuml.model.Diagram;
import org.jetuml.model.DiagramElement;
import org.jetuml.model.edges.*;
import org.jetuml.model.nodes.*;

import java.util.Optional;

/**
 * Caixa de dialogo modal para edicao das propriedades de qualquer no ou aresta UML (RF20, RF22, RF31).
 */
public final class PropertyEditorDialog {

    private PropertyEditorDialog() {
    }

    public static Optional<Command> showAndBuildCommand(Diagram diagram, DiagramElement element) {
        if (diagram == null || element == null) return Optional.empty();

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Propriedades - " + element.getElementType());
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(10);
        grid.setPadding(new Insets(16));

        if (element instanceof ClassNode cls) {
            TextField nameField = new TextField(cls.getName());
            TextArea attrArea = new TextArea(cls.getAttributes());
            attrArea.setPrefRowCount(4);
            TextArea methArea = new TextArea(cls.getMethods());
            methArea.setPrefRowCount(4);

            grid.addRow(0, new Label("Nome:"), nameField);
            grid.addRow(1, new Label("Atributos:"), attrArea);
            grid.addRow(2, new Label("Metodos:"), methArea);
            dialog.getDialogPane().setContent(grid);

            if (dialog.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
                String oldName = cls.getName();
                String oldAttr = cls.getAttributes();
                String oldMeth = cls.getMethods();
                String newName = nameField.getText();
                String newAttr = attrArea.getText();
                String newMeth = methArea.getText();
                return Optional.of(new EditElementCommand(
                        diagram,
                        cls,
                        () -> {
                            cls.setName(oldName);
                            cls.setAttributes(oldAttr);
                            cls.setMethods(oldMeth);
                        },
                        () -> {
                            cls.setName(newName);
                            cls.setAttributes(newAttr);
                            cls.setMethods(newMeth);
                        }
                ));
            }
        } else if (element instanceof InterfaceNode iface) {
            TextField nameField = new TextField(iface.getName());
            TextArea methArea = new TextArea(iface.getMethods());
            methArea.setPrefRowCount(5);

            grid.addRow(0, new Label("Nome:"), nameField);
            grid.addRow(1, new Label("Metodos:"), methArea);
            dialog.getDialogPane().setContent(grid);

            if (dialog.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
                String oldName = iface.getName();
                String oldMeth = iface.getMethods();
                String newName = nameField.getText();
                String newMeth = methArea.getText();
                return Optional.of(new EditElementCommand(
                        diagram,
                        iface,
                        () -> {
                            iface.setName(oldName);
                            iface.setMethods(oldMeth);
                        },
                        () -> {
                            iface.setName(newName);
                            iface.setMethods(newMeth);
                        }
                ));
            }
        } else if (element instanceof PackageNode pkg) {
            TextField nameField = new TextField(pkg.getName());
            grid.addRow(0, new Label("Nome do Pacote:"), nameField);
            dialog.getDialogPane().setContent(grid);

            if (dialog.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
                String oldName = pkg.getName();
                String newName = nameField.getText();
                return Optional.of(new EditElementCommand(
                        diagram, pkg, () -> pkg.setName(oldName), () -> pkg.setName(newName)
                ));
            }
        } else if (element instanceof PackageDescriptionNode pd) {
            TextField nameField = new TextField(pd.getName());
            TextArea contentArea = new TextArea(pd.getContents());
            contentArea.setPrefRowCount(4);
            grid.addRow(0, new Label("Nome:"), nameField);
            grid.addRow(1, new Label("Conteudo:"), contentArea);
            dialog.getDialogPane().setContent(grid);

            if (dialog.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
                String oldName = pd.getName();
                String oldCont = pd.getContents();
                String newName = nameField.getText();
                String newCont = contentArea.getText();
                return Optional.of(new EditElementCommand(
                        diagram,
                        pd,
                        () -> {
                            pd.setName(oldName);
                            pd.setContents(oldCont);
                        },
                        () -> {
                            pd.setName(newName);
                            pd.setContents(newCont);
                        }
                ));
            }
        } else if (element instanceof ImplicitParameterNode ip) {
            TextField nameField = new TextField(ip.getName());
            grid.addRow(0, new Label("Objeto / Participante:"), nameField);
            dialog.getDialogPane().setContent(grid);

            if (dialog.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
                String oldName = ip.getName();
                String newName = nameField.getText();
                return Optional.of(new EditElementCommand(
                        diagram, ip, () -> ip.setName(oldName), () -> ip.setName(newName)
                ));
            }
        } else if (element instanceof CallNode call) {
            CheckBox openBottomBox = new CheckBox("Sequencia incompleta (borda inferior aberta)");
            openBottomBox.setSelected(call.isOpenBottom());
            grid.addRow(0, openBottomBox);
            dialog.getDialogPane().setContent(grid);

            if (dialog.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
                boolean oldVal = call.isOpenBottom();
                boolean newVal = openBottomBox.isSelected();
                return Optional.of(new EditElementCommand(
                        diagram, call, () -> call.setOpenBottom(oldVal), () -> call.setOpenBottom(newVal)
                ));
            }
        } else if (element instanceof StateNode state) {
            TextField nameField = new TextField(state.getName());
            grid.addRow(0, new Label("Nome do Estado:"), nameField);
            dialog.getDialogPane().setContent(grid);

            if (dialog.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
                String oldName = state.getName();
                String newName = nameField.getText();
                return Optional.of(new EditElementCommand(
                        diagram, state, () -> state.setName(oldName), () -> state.setName(newName)
                ));
            }
        } else if (element instanceof ObjectNode obj) {
            TextField nameField = new TextField(obj.getName());
            grid.addRow(0, new Label("Nome do Objeto:"), nameField);
            dialog.getDialogPane().setContent(grid);

            if (dialog.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
                String oldName = obj.getName();
                String newName = nameField.getText();
                return Optional.of(new EditElementCommand(
                        diagram, obj, () -> obj.setName(oldName), () -> obj.setName(newName)
                ));
            }
        } else if (element instanceof FieldNode field) {
            TextField nameField = new TextField(field.getName());
            TextField valueField = new TextField(field.getValue());
            grid.addRow(0, new Label("Nome do Campo:"), nameField);
            grid.addRow(1, new Label("Valor:"), valueField);
            dialog.getDialogPane().setContent(grid);

            if (dialog.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
                String oldName = field.getName();
                String oldVal = field.getValue();
                String newName = nameField.getText();
                String newVal = valueField.getText();
                return Optional.of(new EditElementCommand(
                        diagram,
                        field,
                        () -> {
                            field.setName(oldName);
                            field.setValue(oldVal);
                        },
                        () -> {
                            field.setName(newName);
                            field.setValue(newVal);
                        }
                ));
            }
        } else if (element instanceof ActorNode actor) {
            TextField nameField = new TextField(actor.getName());
            grid.addRow(0, new Label("Nome do Ator:"), nameField);
            dialog.getDialogPane().setContent(grid);

            if (dialog.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
                String oldName = actor.getName();
                String newName = nameField.getText();
                return Optional.of(new EditElementCommand(
                        diagram, actor, () -> actor.setName(oldName), () -> actor.setName(newName)
                ));
            }
        } else if (element instanceof UseCaseNode uc) {
            TextField nameField = new TextField(uc.getName());
            grid.addRow(0, new Label("Nome do Caso de Uso:"), nameField);
            dialog.getDialogPane().setContent(grid);

            if (dialog.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
                String oldName = uc.getName();
                String newName = nameField.getText();
                return Optional.of(new EditElementCommand(
                        diagram, uc, () -> uc.setName(oldName), () -> uc.setName(newName)
                ));
            }
        } else if (element instanceof NoteNode note) {
            TextArea textArea = new TextArea(note.getText());
            textArea.setPrefRowCount(4);
            grid.addRow(0, new Label("Texto da Nota:"), textArea);
            dialog.getDialogPane().setContent(grid);

            if (dialog.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
                String oldText = note.getText();
                String newText = textArea.getText();
                return Optional.of(new EditElementCommand(
                        diagram, note, () -> note.setText(oldText), () -> note.setText(newText)
                ));
            }
        } else if (element instanceof ClassRelationshipEdge rel) {
            ComboBox<Directionality> dirBox = new ComboBox<>();
            dirBox.getItems().addAll(Directionality.values());
            dirBox.setValue(rel.getDirectionality());

            TextField startLabel = new TextField(rel.getStartLabel());
            TextField midLabel = new TextField(rel.getMiddleLabel());
            TextField endLabel = new TextField(rel.getEndLabel());

            grid.addRow(0, new Label("Direcionalidade:"), dirBox);
            grid.addRow(1, new Label("Rotulo Inicial:"), startLabel);
            grid.addRow(2, new Label("Rotulo Central:"), midLabel);
            grid.addRow(3, new Label("Rotulo Final:"), endLabel);
            dialog.getDialogPane().setContent(grid);

            if (dialog.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
                Directionality oldDir = rel.getDirectionality();
                String oldStart = rel.getStartLabel();
                String oldMid = rel.getMiddleLabel();
                String oldEnd = rel.getEndLabel();

                Directionality newDir = dirBox.getValue();
                String newStart = startLabel.getText();
                String newMid = midLabel.getText();
                String newEnd = endLabel.getText();

                return Optional.of(new EditElementCommand(
                        diagram,
                        rel,
                        () -> {
                            rel.setDirectionality(oldDir);
                            rel.setStartLabel(oldStart);
                            rel.setMiddleLabel(oldMid);
                            rel.setEndLabel(oldEnd);
                        },
                        () -> {
                            rel.setDirectionality(newDir);
                            rel.setStartLabel(newStart);
                            rel.setMiddleLabel(newMid);
                            rel.setEndLabel(newEnd);
                        }
                ));
            }
        } else if (element instanceof CallEdge callEdge) {
            TextField midLabel = new TextField(callEdge.getMiddleLabel());
            grid.addRow(0, new Label("Mensagem / Metodo:"), midLabel);
            dialog.getDialogPane().setContent(grid);

            if (dialog.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
                String oldMid = callEdge.getMiddleLabel();
                String newMid = midLabel.getText();
                return Optional.of(new EditElementCommand(
                        diagram, callEdge, () -> callEdge.setMiddleLabel(oldMid), () -> callEdge.setMiddleLabel(newMid)
                ));
            }
        } else if (element instanceof ReturnEdge retEdge) {
            TextField midLabel = new TextField(retEdge.getMiddleLabel());
            grid.addRow(0, new Label("Valor de Retorno:"), midLabel);
            dialog.getDialogPane().setContent(grid);

            if (dialog.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
                String oldMid = retEdge.getMiddleLabel();
                String newMid = midLabel.getText();
                return Optional.of(new EditElementCommand(
                        diagram, retEdge, () -> retEdge.setMiddleLabel(oldMid), () -> retEdge.setMiddleLabel(newMid)
                ));
            }
        } else if (element instanceof TransitionEdge trEdge) {
            TextField midLabel = new TextField(trEdge.getMiddleLabel());
            grid.addRow(0, new Label("Evento / Guarda / Acao:"), midLabel);
            dialog.getDialogPane().setContent(grid);

            if (dialog.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
                String oldMid = trEdge.getMiddleLabel();
                String newMid = midLabel.getText();
                return Optional.of(new EditElementCommand(
                        diagram, trEdge, () -> trEdge.setMiddleLabel(oldMid), () -> trEdge.setMiddleLabel(newMid)
                ));
            }
        } else if (element instanceof CollaborationEdge colEdge) {
            TextField midLabel = new TextField(colEdge.getMiddleLabel());
            grid.addRow(0, new Label("Rotulo:"), midLabel);
            dialog.getDialogPane().setContent(grid);

            if (dialog.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
                String oldMid = colEdge.getMiddleLabel();
                String newMid = midLabel.getText();
                return Optional.of(new EditElementCommand(
                        diagram, colEdge, () -> colEdge.setMiddleLabel(oldMid), () -> colEdge.setMiddleLabel(newMid)
                ));
            }
        } else if (element instanceof UseCaseEdge ucEdge && ucEdge.isDependency()) {
            ComboBox<UseCaseEdge.DependencyType> depBox = new ComboBox<>();
            depBox.getItems().addAll(UseCaseEdge.DependencyType.INCLUDE, UseCaseEdge.DependencyType.EXTEND);
            depBox.setValue(ucEdge.getDependencyType());
            grid.addRow(0, new Label("Tipo de Dependencia:"), depBox);
            dialog.getDialogPane().setContent(grid);

            if (dialog.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
                UseCaseEdge.DependencyType oldDep = ucEdge.getDependencyType();
                UseCaseEdge.DependencyType newDep = depBox.getValue();
                return Optional.of(new EditElementCommand(
                        diagram, ucEdge, () -> ucEdge.setDependencyType(oldDep), () -> ucEdge.setDependencyType(newDep)
                ));
            }
        }

        return Optional.empty();
    }
}
