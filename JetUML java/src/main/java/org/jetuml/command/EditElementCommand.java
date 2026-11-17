package org.jetuml.command;

import org.jetuml.layout.EdgeRouter;
import org.jetuml.model.Diagram;
import org.jetuml.model.DiagramElement;

/**
 * Encapsula a edicao de propriedades de qualquer no ou aresta com suporte a Undo/Redo (RF20, RF21, RF22).
 */
public class EditElementCommand implements Command {
    private final Diagram diagram;
    private final DiagramElement element;
    private final Runnable applyNewValues;
    private final Runnable restoreOldValues;

    public EditElementCommand(
            Diagram diagram,
            DiagramElement element,
            Runnable restoreOldValues,
            Runnable applyNewValues
    ) {
        this.diagram = diagram;
        this.element = element;
        this.restoreOldValues = restoreOldValues;
        this.applyNewValues = applyNewValues;
    }

    @Override
    public void execute() {
        applyNewValues.run();
        EdgeRouter.routeAll(diagram);
        diagram.setDirty(true);
        diagram.notifyListeners();
    }

    @Override
    public void undo() {
        restoreOldValues.run();
        EdgeRouter.routeAll(diagram);
        diagram.setDirty(true);
        diagram.notifyListeners();
    }

    @Override
    public String getDescription() {
        return "Edit " + element.getElementType();
    }
}
