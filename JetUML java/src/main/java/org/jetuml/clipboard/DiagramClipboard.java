package org.jetuml.clipboard;

import org.jetuml.command.Command;
import org.jetuml.command.DeleteElementsCommand;
import org.jetuml.geom.Grid;
import org.jetuml.layout.EdgeRouter;
import org.jetuml.model.*;
import org.jetuml.model.nodes.FieldNode;
import org.jetuml.validation.DiagramValidator;
import org.jetuml.validation.ValidationResult;

import java.util.*;

/**
 * Implementa Recortar (Ctrl+X), Copiar (Ctrl+C) e Colar (Ctrl+V) entre elementos
 * do mesmo diagrama ou entre diagramas diferentes (RF34).
 */
public final class DiagramClipboard {
    private static final DiagramClipboard INSTANCE = new DiagramClipboard();

    private final List<Node> copiedRootNodes = new ArrayList<>();
    private final List<Edge> copiedEdges = new ArrayList<>();
    private int pasteOffsetMultiplier = 1;

    private DiagramClipboard() {
    }

    public static DiagramClipboard getInstance() {
        return INSTANCE;
    }

    public boolean isEmpty() {
        return copiedRootNodes.isEmpty();
    }

    public void copy(Diagram diagram) {
        if (diagram == null) return;
        Set<DiagramElement> selection = diagram.getSelectedElements();
        if (selection.isEmpty()) return;

        copiedRootNodes.clear();
        copiedEdges.clear();
        pasteOffsetMultiplier = 1;

        Set<Node> selectedNodes = new LinkedHashSet<>();
        for (DiagramElement el : selection) {
            if (el instanceof Node n) {
                selectedNodes.add(n);
            }
        }

        // Selecionar apenas nos de mais alto nivel na selecao (evita duplicar filhos cujo pai ja esta selecionado)
        List<Node> topLevelSelection = new ArrayList<>();
        for (Node n : selectedNodes) {
            if (n instanceof FieldNode field && (field.getParent() == null || !selectedNodes.contains(field.getParent()))) {
                // FieldNode solto sem seu ObjectNode pai nao pode existir como raiz (RF25)
                continue;
            }
            if (n instanceof ChildNode child && child.getParent() != null && selectedNodes.contains(child.getParent())) {
                continue;
            }
            topLevelSelection.add(n);
        }

        Map<Node, Node> cloneMap = new LinkedHashMap<>();
        for (Node top : topLevelSelection) {
            Node cloned = cloneSubtree(top, cloneMap);
            copiedRootNodes.add(cloned);
        }

        for (Edge edge : diagram.getEdges()) {
            Node mappedStart = cloneMap.get(edge.getStartNode());
            Node mappedEnd = cloneMap.get(edge.getEndNode());
            if (mappedStart != null && mappedEnd != null) {
                copiedEdges.add(edge.deepCopy(mappedStart, mappedEnd));
            }
        }
    }

    public Command createCutCommand(Diagram diagram) {
        copy(diagram);
        return new DeleteElementsCommand(diagram, diagram.getSelectedElements());
    }

    public ValidationResult validatePaste(Diagram targetDiagram) {
        List<Node> allCopiedNodes = new ArrayList<>();
        for (Node root : copiedRootNodes) {
            collectAll(root, allCopiedNodes);
        }
        return DiagramValidator.validatePasteCompatibility(targetDiagram, allCopiedNodes);
    }

    public Command createPasteCommand(Diagram targetDiagram) {
        int offset = Grid.DEFAULT_GRID_SIZE * 2 * pasteOffsetMultiplier++;
        Map<Node, Node> pasteMap = new LinkedHashMap<>();
        List<Node> pastedRoots = new ArrayList<>();

        for (Node templateRoot : copiedRootNodes) {
            Node clonedRoot = cloneSubtree(templateRoot, pasteMap);
            clonedRoot.translate(offset, offset);
            pastedRoots.add(clonedRoot);
        }

        List<Edge> pastedEdges = new ArrayList<>();
        for (Edge templateEdge : copiedEdges) {
            Node s = pasteMap.get(templateEdge.getStartNode());
            Node e = pasteMap.get(templateEdge.getEndNode());
            if (s != null && e != null) {
                pastedEdges.add(templateEdge.deepCopy(s, e));
            }
        }

        return new Command() {
            @Override
            public void execute() {
                Set<DiagramElement> newSelection = new LinkedHashSet<>();
                for (Node root : pastedRoots) {
                    if (root instanceof ChildNode child) {
                        child.setParent(null);
                    }
                    targetDiagram.addRootNode(root);
                    newSelection.add(root);
                }
                for (Edge edge : pastedEdges) {
                    targetDiagram.addEdge(edge);
                    newSelection.add(edge);
                }
                EdgeRouter.routeAll(targetDiagram);
                targetDiagram.setSelection(newSelection);
                targetDiagram.setDirty(true);
            }

            @Override
            public void undo() {
                for (Edge edge : pastedEdges) {
                    targetDiagram.removeEdge(edge);
                }
                for (Node root : pastedRoots) {
                    targetDiagram.removeRootNode(root);
                }
                EdgeRouter.routeAll(targetDiagram);
                targetDiagram.clearSelection();
                targetDiagram.setDirty(true);
            }

            @Override
            public String getDescription() {
                return "Paste Elements";
            }
        };
    }

    private Node cloneSubtree(Node original, Map<Node, Node> map) {
        Node cloned = original.deepCopy();
        map.put(original, cloned);
        if (original instanceof ParentNode origParent && cloned instanceof ParentNode clonedParent) {
            for (ChildNode origChild : origParent.getChildren()) {
                Node clonedChildNode = cloneSubtree(origChild, map);
                if (clonedChildNode instanceof ChildNode clonedChild) {
                    clonedParent.addChild(clonedChild);
                }
            }
        }
        return cloned;
    }

    private void collectAll(Node node, List<Node> acc) {
        acc.add(node);
        if (node instanceof ParentNode parent) {
            for (ChildNode child : parent.getChildren()) {
                collectAll(child, acc);
            }
        }
    }
}
