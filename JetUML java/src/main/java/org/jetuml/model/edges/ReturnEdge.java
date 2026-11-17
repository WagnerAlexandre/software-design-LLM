package org.jetuml.model.edges;

import org.jetuml.model.AbstractEdge;
import org.jetuml.model.Edge;
import org.jetuml.model.Node;

/**
 * Aresta de retorno tracejada no Diagrama de Sequencia (RF20, RF27, RF30).
 */
public class ReturnEdge extends AbstractEdge {
    public static final String ELEMENT_TYPE = "ReturnEdge";

    private String middleLabel = "";

    public ReturnEdge(Node startNode, Node endNode) {
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
        ReturnEdge copy = new ReturnEdge(mappedStartNode, mappedEndNode);
        copy.middleLabel = this.middleLabel;
        return copy;
    }
}
