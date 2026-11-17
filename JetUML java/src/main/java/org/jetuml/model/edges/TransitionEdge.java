package org.jetuml.model.edges;

import org.jetuml.model.AbstractEdge;
import org.jetuml.model.Edge;
import org.jetuml.model.Node;

/**
 * Transicao entre estados (ou autotransicao) no Diagrama de Estados (RF19, RF20).
 */
public class TransitionEdge extends AbstractEdge {
    public static final String ELEMENT_TYPE = "TransitionEdge";

    private String middleLabel = "";

    public TransitionEdge(Node startNode, Node endNode) {
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
        TransitionEdge copy = new TransitionEdge(mappedStartNode, mappedEndNode);
        copy.middleLabel = this.middleLabel;
        return copy;
    }
}
