package org.jetuml.command;

import org.jetuml.layout.EdgeRouter;
import org.jetuml.model.ChildNode;
import org.jetuml.model.Diagram;
import org.jetuml.model.ParentNode;

/**
 * Inclui ou desanexa um no filho de um PackageNode via Shift (RF15, RF21).
 */
public class ReparentNodeCommand implements Command {
    private final Diagram diagram;
    private final ChildNode child;
    private final ParentNode oldParent;
    private final ParentNode newParent;

    public ReparentNodeCommand(Diagram diagram, ChildNode child, ParentNode oldParent, ParentNode newParent) {
        this.diagram = diagram;
        this.child = child;
        this.oldParent = oldParent;
        this.newParent = newParent;
    }

    @Override
    public void execute() {
        applyParent(oldParent, newParent);
    }

    @Override
    public void undo() {
        applyParent(newParent, oldParent);
    }

    private void applyParent(ParentNode from, ParentNode to) {
        if (from != null) {
            from.removeChild(child);
        } else {
            diagram.removeRootNode(child);
        }

        if (to != null) {
            to.addChild(child);
        } else {
            child.setParent(null);
            diagram.addRootNode(child);
        }

        EdgeRouter.routeAll(diagram);
        diagram.setDirty(true);
        diagram.notifyListeners();
    }

    @Override
    public String getDescription() {
        return "Reparent Node";
    }
}
