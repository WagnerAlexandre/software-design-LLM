package org.jetuml.model.edges;

import org.jetuml.model.AbstractEdge;
import org.jetuml.model.Edge;
import org.jetuml.model.Node;

/**
 * Conector pontilhado que liga um elemento UML a uma NoteNode
 * ou ancora uma NoteNode a um PointNode fixo (RF14).
 */
public class NoteConnectorEdge extends AbstractEdge {
    public static final String ELEMENT_TYPE = "NoteConnectorEdge";

    public NoteConnectorEdge(Node startNode, Node endNode) {
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
        return new NoteConnectorEdge(mappedStartNode, mappedEndNode);
    }
}
