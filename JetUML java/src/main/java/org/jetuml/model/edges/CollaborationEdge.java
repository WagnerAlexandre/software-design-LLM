package org.jetuml.model.edges;

import org.jetuml.model.AbstractEdge;
import org.jetuml.model.Edge;
import org.jetuml.model.Node;

/**
 * Aresta de Colaboracao entre dois ObjectNodes no Diagrama de Objetos (RF18, RF19, RF20).
 */
public class CollaborationEdge extends AbstractEdge {
    public static final String ELEMENT_TYPE = "CollaborationEdge";

    private String middleLabel = "";

    public CollaborationEdge(Node startNode, Node endNode) {
        super(startNode, endNode);
    }

    @Override
    public String getElementType() {
        return ELEMENT_TYPE;
    }

    public String getMiddleLabel() {
        return middleLabel;
    }

    public void setMiddleLabel(String middleLabel) {
        this.middleLabel = middleLabel != null ? middleLabel : "";
    }

    @Override
    public Edge deepCopy(Node mappedStartNode, Node mappedEndNode) {
        CollaborationEdge copy = new CollaborationEdge(mappedStartNode, mappedEndNode);
        copy.middleLabel = this.middleLabel;
        return copy;
    }
}
