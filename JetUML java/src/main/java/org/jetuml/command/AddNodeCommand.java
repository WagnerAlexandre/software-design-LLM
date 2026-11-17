package org.jetuml.command;

import org.jetuml.layout.EdgeRouter;
import org.jetuml.model.ChildNode;
import org.jetuml.model.Diagram;
import org.jetuml.model.Node;
import org.jetuml.model.ParentNode;

/**
 * Adiciona um no raiz ou filho ao diagrama com suporte a Undo/Redo (RF11, RF15, RF21, RF25).
 */
public class AddNodeCommand implements Command {
    private final Diagram diagram;
    private final Node node;
    private final ParentNode parent;

    public AddNodeCommand(Diagram diagram, Node node, ParentNode parent) {
        this.diagram = diagram;
        this.node = node;
        this.parent = parent;
    }

    @Override
    public void execute() {
        if (parent != null && node instanceof ChildNode child) {
            parent.addChild(child);
        } else {
            diagram.addRootNode(node);
        }
        EdgeRouter.routeAll(diagram);
        diagram.selectSingle(node);
        diagram.setDirty(true);
    }

    @Override
    public void undo() {
        if (parent != null && node instanceof ChildNode child) {
            parent.removeChild(child);
        } else {
            diagram.removeRootNode(node);
        }
        EdgeRouter.routeAll(diagram);
        diagram.clearSelection();
        diagram.setDirty(true);
    }

    @Override
    public String getDescription() {
        return "Add " + node.getElementType();
    }
}
