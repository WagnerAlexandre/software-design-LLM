package org.jetuml.model;

import org.jetuml.geom.Point2DInt;
import org.jetuml.model.edges.*;
import org.jetuml.model.nodes.*;

import java.util.Optional;

/**
 * Fabrica centralizada para instanciacao coesa de nos e arestas UML a partir
 * de ferramentas da barra lateral (ToolType) ou identificadores de serializacao JSON.
 */
public final class ElementFactory {

    private ElementFactory() {
    }

    public static Optional<Node> createNode(ToolType tool, Point2DInt position) {
        if (tool == null || !tool.isNodeTool()) {
            return Optional.empty();
        }
        Node created = switch (tool) {
            case CLASS -> new ClassNode(position);
            case INTERFACE -> new InterfaceNode(position);
            case PACKAGE -> new PackageNode(position);
            case PACKAGE_DESCRIPTION -> new PackageDescriptionNode(position);
            case IMPLICIT_PARAMETER -> new ImplicitParameterNode(position);
            case STATE -> new StateNode(position);
            case INITIAL_STATE -> new InitialStateNode(position);
            case FINAL_STATE -> new FinalStateNode(position);
            case OBJECT -> new ObjectNode(position);
            case FIELD -> new FieldNode(position);
            case ACTOR -> new ActorNode(position);
            case USE_CASE -> new UseCaseNode(position);
            case NOTE -> new NoteNode(position);
            default -> null;
        };
        return Optional.ofNullable(created);
    }

    public static Optional<Node> createNodeByTypeName(String elementType) {
        if (elementType == null) return Optional.empty();
        Node created = switch (elementType) {
            case ClassNode.ELEMENT_TYPE -> new ClassNode();
            case InterfaceNode.ELEMENT_TYPE -> new InterfaceNode();
            case PackageNode.ELEMENT_TYPE -> new PackageNode();
            case PackageDescriptionNode.ELEMENT_TYPE -> new PackageDescriptionNode();
            case ImplicitParameterNode.ELEMENT_TYPE -> new ImplicitParameterNode();
            case CallNode.ELEMENT_TYPE -> new CallNode();
            case StateNode.ELEMENT_TYPE -> new StateNode();
            case InitialStateNode.ELEMENT_TYPE -> new InitialStateNode();
            case FinalStateNode.ELEMENT_TYPE -> new FinalStateNode();
            case ObjectNode.ELEMENT_TYPE -> new ObjectNode();
            case FieldNode.ELEMENT_TYPE -> new FieldNode();
            case ActorNode.ELEMENT_TYPE -> new ActorNode();
            case UseCaseNode.ELEMENT_TYPE -> new UseCaseNode();
            case NoteNode.ELEMENT_TYPE -> new NoteNode();
            case PointNode.ELEMENT_TYPE -> new PointNode();
            default -> null;
        };
        return Optional.ofNullable(created);
    }

    public static Optional<Edge> createEdge(ToolType tool, Node startNode, Node endNode) {
        if (tool == null || !tool.isEdgeTool() || startNode == null || endNode == null) {
            return Optional.empty();
        }
        Edge created = switch (tool) {
            case DEPENDENCY ->
                    new ClassRelationshipEdge(ClassRelationshipEdge.RelationshipKind.DEPENDENCY, startNode, endNode);
            case INHERITANCE ->
                    new ClassRelationshipEdge(ClassRelationshipEdge.RelationshipKind.INHERITANCE, startNode, endNode);
            case REALIZATION ->
                    new ClassRelationshipEdge(ClassRelationshipEdge.RelationshipKind.REALIZATION, startNode, endNode);
            case ASSOCIATION ->
                    new ClassRelationshipEdge(ClassRelationshipEdge.RelationshipKind.ASSOCIATION, startNode, endNode);
            case AGGREGATION ->
                    new ClassRelationshipEdge(ClassRelationshipEdge.RelationshipKind.AGGREGATION, startNode, endNode);
            case COMPOSITION ->
                    new ClassRelationshipEdge(ClassRelationshipEdge.RelationshipKind.COMPOSITION, startNode, endNode);
            case CALL -> new CallEdge(startNode, endNode, false);
            case RETURN -> new ReturnEdge(startNode, endNode);
            case TRANSITION -> new TransitionEdge(startNode, endNode);
            case REFERENCE -> new ReferenceEdge(startNode, endNode);
            case COLLABORATION -> new CollaborationEdge(startNode, endNode);
            case USE_CASE_ASSOCIATION -> new UseCaseEdge(UseCaseEdge.UseCaseKind.ASSOCIATION, startNode, endNode);
            case INCLUDE -> new UseCaseEdge(UseCaseEdge.UseCaseKind.INCLUDE, startNode, endNode);
            case EXTEND -> new UseCaseEdge(UseCaseEdge.UseCaseKind.EXTEND, startNode, endNode);
            case GENERALIZATION -> new UseCaseEdge(UseCaseEdge.UseCaseKind.GENERALIZATION, startNode, endNode);
            case NOTE_CONNECTOR -> new NoteConnectorEdge(startNode, endNode);
            default -> null;
        };
        return Optional.ofNullable(created);
    }
}
