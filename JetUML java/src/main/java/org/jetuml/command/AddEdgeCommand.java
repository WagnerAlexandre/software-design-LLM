package org.jetuml.command;

import org.jetuml.layout.EdgeRouter;
import org.jetuml.model.Diagram;
import org.jetuml.model.Edge;
import org.jetuml.model.nodes.PointNode;

/**
 * Adiciona uma aresta (e opcionalmente um PointNode ancora para NoteConnector) ao diagrama (RF14, RF16, RF21).
 */
public class AddEdgeCommand implements Command {
    private final Diagram diagram;
    private final Edge edge;
    private final PointNode optionalAnchorNode;

    public AddEdgeCommand(Diagram diagram, Edge edge) {
        this(diagram, edge, null);
    }

    public AddEdgeCommand(Diagram diagram, Edge edge, PointNode optionalAnchorNode) {
        this.diagram = diagram;
        this.edge = edge;
        this.optionalAnchorNode = optionalAnchorNode;
    }

    @Override
    public void execute() {
        if (optionalAnchorNode != null) {
            diagram.addRootNode(optionalAnchorNode);
        }
        diagram.addEdge(edge);
        EdgeRouter.routeAll(diagram);
        diagram.selectSingle(edge);
        diagram.setDirty(true);
    }

    @Override
    public void undo() {
        diagram.removeEdge(edge);
        if (optionalAnchorNode != null) {
            diagram.removeRootNode(optionalAnchorNode);
        }
        EdgeRouter.routeAll(diagram);
        diagram.clearSelection();
        diagram.setDirty(true);
    }

    @Override
    public String getDescription() {
        return "Add " + edge.getElementType();
    }
}
