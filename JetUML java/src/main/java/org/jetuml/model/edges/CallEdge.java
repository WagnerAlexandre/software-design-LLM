package org.jetuml.model.edges;

import org.jetuml.model.AbstractEdge;
import org.jetuml.model.Edge;
import org.jetuml.model.Node;

/**
 * Aresta de chamada sincrona ou construtor <<create>> no Diagrama de Sequencia (RF20, RF27, RF28, RF29).
 */
public class CallEdge extends AbstractEdge {
    public static final String ELEMENT_TYPE = "CallEdge";

    private String middleLabel = "";
    private boolean createCall;

    public CallEdge(Node startNode, Node endNode) {
        this(startNode, endNode, false);
    }

    public CallEdge(Node startNode, Node endNode, boolean createCall) {
        super(startNode, endNode);
        this.createCall = createCall;
        if (createCall) {
            this.middleLabel = "«create»";
        }
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

    public boolean isCreateCall() {
        return createCall;
    }

    public void setCreateCall(boolean createCall) {
        this.createCall = createCall;
    }

    @Override
    public Edge deepCopy(Node mappedStartNode, Node mappedEndNode) {
        CallEdge copy = new CallEdge(mappedStartNode, mappedEndNode, this.createCall);
        copy.middleLabel = this.middleLabel;
        return copy;
    }
}
