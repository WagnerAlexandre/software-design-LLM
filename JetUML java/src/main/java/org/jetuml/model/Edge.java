package org.jetuml.model;

/**
 * Contrato para todas as arestas e transicoes entre nos de um diagrama UML.
 */
public interface Edge extends DiagramElement {
    Node getStartNode();

    void setStartNode(Node startNode);

    Node getEndNode();

    void setEndNode(Node endNode);

    default boolean isSelfEdge() {
        return getStartNode() != null && getStartNode().equals(getEndNode());
    }

    Edge deepCopy(Node mappedStartNode, Node mappedEndNode);
}
