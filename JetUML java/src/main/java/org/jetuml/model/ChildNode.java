package org.jetuml.model;

/**
 * Representa um no que pode estar contido dentro de um ParentNode (RF15, RF25, RF27).
 */
public interface ChildNode extends Node {
    ParentNode getParent();


    void setParent(ParentNode parent);

    default boolean hasParent() {
        return getParent() != null;
    }
}
