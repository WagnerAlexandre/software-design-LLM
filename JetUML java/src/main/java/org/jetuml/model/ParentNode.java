package org.jetuml.model;

import java.util.List;

/**
 * Representa um no que pode conter nos filhos (ex.: PackageNode, ObjectNode, ImplicitParameterNode, CallNode).
 */
public interface ParentNode extends Node {
    List<ChildNode> getChildren();

    void addChild(ChildNode child);

    void insertChild(int index, ChildNode child);

    void removeChild(ChildNode child);

    boolean canAcceptChild(Node candidate);
}
