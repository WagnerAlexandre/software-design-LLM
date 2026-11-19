package org.jetuml.command;

import org.jetuml.layout.EdgeRouter;
import org.jetuml.model.*;
import org.jetuml.model.edges.NoteConnectorEdge;
import org.jetuml.model.nodes.CallNode;
import org.jetuml.model.nodes.PointNode;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Exclui elementos selecionados (nos, filhos e arestas incidentes) restaurando-os
 * integralmente ao desfazer (RF21, RF35).
 */
public class DeleteElementsCommand implements Command {
    private final Diagram diagram;
    private final List<DeletedNodeRecord> deletedNodes = new ArrayList<>();
    private final List<DeletedEdgeRecord> deletedEdges = new ArrayList<>();
    private boolean initialized = false;

    public DeleteElementsCommand(Diagram diagram, Set<DiagramElement> elementsToDelete) {
        this.diagram = diagram;
        prepareDeletionSnapshot(elementsToDelete);
    }

    private void prepareDeletionSnapshot(Set<DiagramElement> selection) {
        if (selection == null || selection.isEmpty()) return;

        Set<Node> nodesToRemove = new LinkedHashSet<>();
        for (DiagramElement el : selection) {
            if (el instanceof Node n) {
                collectNodeAndDescendants(n, nodesToRemove);
            }
        }

        Set<Edge> edgesToRemove = new LinkedHashSet<>();
        for (DiagramElement el : selection) {
            if (el instanceof Edge e) {
                edgesToRemove.add(e);
            }
        }

        // Qualquer aresta conectada a um no removido tambem e removida
        for (Edge edge : diagram.getEdges()) {
            if (nodesToRemove.contains(edge.getStartNode()) || nodesToRemove.contains(edge.getEndNode())) {
                edgesToRemove.add(edge);
            }
        }

        // Remover PointNodes orfaos ligados exclusivamente a NoteConnectorEdges removidas
        for (Edge edge : edgesToRemove) {
            if (edge instanceof NoteConnectorEdge) {
                if (edge.getStartNode() instanceof PointNode p) nodesToRemove.add(p);
                if (edge.getEndNode() instanceof PointNode p) nodesToRemove.add(p);
            }
        }

        List<Edge> currentEdges = diagram.getEdges();
        for (int i = 0; i < currentEdges.size(); i++) {
            Edge e = currentEdges.get(i);
            if (edgesToRemove.contains(e)) {
                deletedEdges.add(new DeletedEdgeRecord(e, i));
            }
        }

        // Registrar nos raiz e filhos na ordem adequada
        List<Node> roots = diagram.getRootNodes();
        for (int i = 0; i < roots.size(); i++) {
            Node root = roots.get(i);
            if (nodesToRemove.contains(root)) {
                deletedNodes.add(new DeletedNodeRecord(root, null, i));
            } else if (root instanceof ParentNode parent) {
                collectChildDeletions(parent, nodesToRemove);
            }
        }
        initialized = true;
    }

    private void collectNodeAndDescendants(Node node, Set<Node> acc) {
        acc.add(node);
        if (node instanceof ParentNode parent) {
            for (ChildNode child : parent.getChildren()) {
                collectNodeAndDescendants(child, acc);
            }
        }
    }

    private void collectChildDeletions(ParentNode parent, Set<Node> nodesToRemove) {
        List<ChildNode> children = parent.getChildren();
        for (int i = 0; i < children.size(); i++) {
            ChildNode child = children.get(i);
            if (nodesToRemove.contains(child)) {
                deletedNodes.add(new DeletedNodeRecord(child, parent, i));
            } else if (child instanceof ParentNode subParent) {
                collectChildDeletions(subParent, nodesToRemove);
            }
        }
    }

    @Override
    public void execute() {
        if (!initialized) return;

        for (DeletedEdgeRecord er : deletedEdges) {
            diagram.removeEdge(er.edge());
        }

        // Remover filhos antes dos pais
        for (int i = deletedNodes.size() - 1; i >= 0; i--) {
            DeletedNodeRecord nr = deletedNodes.get(i);
            if (nr.parent() != null && nr.node() instanceof ChildNode child) {
                nr.parent().removeChild(child);
                if (nr.parent() instanceof CallNode call && call.getImplicitParameter() != null) {
                    call.getImplicitParameter().layoutChildren();
                }
            } else {
                diagram.removeRootNode(nr.node());
            }
        }

        EdgeRouter.routeAll(diagram);
        diagram.clearSelection();
        diagram.setDirty(true);
    }

    @Override
    public void undo() {
        if (!initialized) return;

        for (DeletedNodeRecord nr : deletedNodes) {
            if (nr.parent() != null && nr.node() instanceof ChildNode child) {
                nr.parent().insertChild(nr.index(), child);
                if (nr.parent() instanceof CallNode call && call.getImplicitParameter() != null) {
                    call.getImplicitParameter().layoutChildren();
                }
            } else {
                diagram.insertRootNode(nr.index(), nr.node());
            }
        }

        for (DeletedEdgeRecord er : deletedEdges) {
            diagram.insertEdge(er.index(), er.edge());
        }

        EdgeRouter.routeAll(diagram);
        diagram.setDirty(true);
        diagram.notifyListeners();
    }

    @Override
    public String getDescription() {
        return "Delete Elements";
    }

    private record DeletedNodeRecord(Node node, ParentNode parent, int index) {
    }

    private record DeletedEdgeRecord(Edge edge, int index) {
    }
}
