package org.jetuml.model.edges;

import org.jetuml.model.AbstractEdge;
import org.jetuml.model.Edge;
import org.jetuml.model.Node;

/**
 * Aresta de Referencia que liga a parte Value de um FieldNode a um ObjectNode (RF26).
 */
public class ReferenceEdge extends AbstractEdge {
    public static final String ELEMENT_TYPE = "ReferenceEdge";

    public ReferenceEdge(Node startNode, Node endNode) {
        super(startNode, endNode);
    }

    @Override
    public String getElementType() {
        return ELEMENT_TYPE;
    }

    @Override
    public boolean hasEditableProperties() {
        return false;
    }

    @Override
    public Edge deepCopy(Node mappedStartNode, Node mappedEndNode) {
        return new ReferenceEdge(mappedStartNode, mappedEndNode);
    }
}
