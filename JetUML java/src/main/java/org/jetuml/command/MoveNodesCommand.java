package org.jetuml.command;

import org.jetuml.layout.EdgeRouter;
import org.jetuml.model.Diagram;
import org.jetuml.model.Node;

import java.util.List;

/**
 * Move um conjunto de nos selecionados e recalcula automaticamente todas as arestas (RF17, RF21).
 */
public class MoveNodesCommand implements Command {
    private final Diagram diagram;
    private final List<Node> nodes;
    private final int dx;
    private final int dy;

    public MoveNodesCommand(Diagram diagram, List<Node> nodes, int dx, int dy) {
        this.diagram = diagram;
        this.nodes = List.copyOf(nodes);
        this.dx = dx;
        this.dy = dy;
    }

    @Override
    public void execute() {
        for (Node node : nodes) {
            node.translate(dx, dy);
        }
        EdgeRouter.routeAll(diagram);
        diagram.setDirty(true);
        diagram.notifyListeners();
    }

    @Override
    public void undo() {
        for (Node node : nodes) {
            node.translate(-dx, -dy);
        }
        EdgeRouter.routeAll(diagram);
        diagram.setDirty(true);
        diagram.notifyListeners();
    }

    @Override
    public String getDescription() {
        return "Move Nodes";
    }
}
