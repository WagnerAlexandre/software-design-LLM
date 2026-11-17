package org.jetuml.model;

import org.jetuml.config.UserPreferences;
import org.jetuml.geom.Grid;
import org.jetuml.geom.Point2DInt;
import org.jetuml.geom.Rectangle2DInt;
import org.jetuml.model.nodes.ObjectNode;
import org.jetuml.model.nodes.PackageNode;

import java.nio.file.Path;
import java.util.*;

/**
 * Modelo central de um diagrama UML. Mantem a hierarquia de nos raiz, arestas,
 * selecao ativa, dimensoes fixas (RF38) e notificacao aos observadores.
 */
public class Diagram {
    private final DiagramType type;
    private final Grid grid;
    private final List<Node> rootNodes = new ArrayList<>();
    private final List<Edge> edges = new ArrayList<>();
    private final Set<DiagramElement> selectedElements = new LinkedHashSet<>();
    private final List<DiagramListener> listeners = new ArrayList<>();

    private String title;
    private Path filePath;
    private boolean dirty;
    private int width;
    private int height;

    public Diagram(DiagramType type) {
        this.type = type;
        this.grid = new Grid(Grid.DEFAULT_GRID_SIZE);
        this.title = "Untitled " + type.getDisplayName();
        UserPreferences prefs = UserPreferences.getInstance();
        this.width = prefs.getDiagramWidth();
        this.height = prefs.getDiagramHeight();
    }

    public DiagramType getType() {
        return type;
    }

    public Grid getGrid() {
        return grid;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
        notifyListeners();
    }

    public Optional<Path> getFilePath() {
        return Optional.ofNullable(filePath);
    }

    public void setFilePath(Path filePath) {
        this.filePath = filePath;
        if (filePath != null && filePath.getFileName() != null) {
            this.title = filePath.getFileName().toString();
        }
        notifyListeners();
    }

    public boolean isDirty() {
        return dirty;
    }

    public void setDirty(boolean dirty) {
        this.dirty = dirty;
        notifyListeners();
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public void setCanvasSize(int width, int height) {
        this.width = UserPreferences.clampDiagramSize(width);
        this.height = UserPreferences.clampDiagramSize(height);
        notifyListeners();
    }

    /**
     * Expande automaticamente o tamanho do diagrama caso algum elemento ultrapasse
     * a area configurada ao abrir um arquivo (RF38).
     */
    public void ensureFitsContent() {
        Rectangle2DInt content = getContentBounds();
        if (content.width() > 0 && content.height() > 0) {
            int requiredW = Math.max(this.width, content.getMaxX() + 40);
            int requiredH = Math.max(this.height, content.getMaxY() + 40);
            this.width = UserPreferences.clampDiagramSize(requiredW);
            this.height = UserPreferences.clampDiagramSize(requiredH);
        }
    }

    public List<Node> getRootNodes() {
        return Collections.unmodifiableList(rootNodes);
    }

    /**
     * Retorna todos os nos do diagrama (raiz e filhos aninhados em profundidade).
     */
    public List<Node> getAllNodes() {
        List<Node> result = new ArrayList<>();
        for (Node root : rootNodes) {
            collectNodesRecursive(root, result);
        }
        return Collections.unmodifiableList(result);
    }

    private void collectNodesRecursive(Node node, List<Node> accumulator) {
        accumulator.add(node);
        if (node instanceof ParentNode parent) {
            for (ChildNode child : parent.getChildren()) {
                collectNodesRecursive(child, accumulator);
            }
        }
    }

    public List<Edge> getEdges() {
        return Collections.unmodifiableList(edges);
    }

    public void addRootNode(Node node) {
        if (node == null || rootNodes.contains(node)) return;
        if (node instanceof ChildNode child && child.getParent() != null) {
            child.getParent().removeChild(child);
            child.setParent(null);
        }
        rootNodes.add(node);
        dirty = true;
        notifyListeners();
    }

    public void insertRootNode(int index, Node node) {
        if (node == null || rootNodes.contains(node)) return;
        int safeIndex = Math.max(0, Math.min(index, rootNodes.size()));
        rootNodes.add(safeIndex, node);
        dirty = true;
        notifyListeners();
    }

    public void removeRootNode(Node node) {
        if (rootNodes.remove(node)) {
            selectedElements.remove(node);
            dirty = true;
            notifyListeners();
        }
    }

    public void addEdge(Edge edge) {
        if (edge == null || edges.contains(edge)) return;
        edges.add(edge);
        dirty = true;
        notifyListeners();
    }

    public void insertEdge(int index, Edge edge) {
        if (edge == null || edges.contains(edge)) return;
        int safeIndex = Math.max(0, Math.min(index, edges.size()));
        edges.add(safeIndex, edge);
        dirty = true;
        notifyListeners();
    }

    public void removeEdge(Edge edge) {
        if (edges.remove(edge)) {
            selectedElements.remove(edge);
            dirty = true;
            notifyListeners();
        }
    }

    /**
     * Localiza o elemento mais especifico (no filho antes de no pai, nos antes de arestas)
     * sob o ponto informado.
     */
    public Optional<DiagramElement> findElementAt(Point2DInt point) {
        List<Node> allNodes = getAllNodes();
        for (int i = allNodes.size() - 1; i >= 0; i--) {
            Node n = allNodes.get(i);
            if (n.contains(point)) {
                return Optional.of(n);
            }
        }
        for (int i = edges.size() - 1; i >= 0; i--) {
            Edge e = edges.get(i);
            if (e.contains(point)) {
                return Optional.of(e);
            }
        }
        return Optional.empty();
    }

    public Optional<Node> findNodeAt(Point2DInt point) {
        if (point == null) {
            return Optional.empty();
        }
        // Percorre de tras para frente para priorizar nos desenhados por ultimo (topo da pilha visual)
        for (int i = rootNodes.size() - 1; i >= 0; i--) {
            Node root = rootNodes.get(i);
            Optional<Node> deepMatch = findDeepestNodeAt(root, point);
            if (deepMatch.isPresent()) {
                return deepMatch;
            }
        }
        return Optional.empty();
    }

    public boolean isSelected(DiagramElement element) {
        return selectedElements.contains(element);
    }

    public Optional<Node> findContainerCandidateAt(Point2DInt point) {
        if (point == null) {
            return Optional.empty();
        }
        for (int i = rootNodes.size() - 1; i >= 0; i--) {
            Node root = rootNodes.get(i);
            if ((root instanceof PackageNode || root instanceof ObjectNode) && root.contains(point)) {
                return Optional.of(root);
            }
        }
        return Optional.empty();
    }

    public Optional<PackageNode> findPackageAt(Point2DInt point) {
        if (point == null) {
            return Optional.empty();
        }
        for (int i = rootNodes.size() - 1; i >= 0; i--) {
            Node root = rootNodes.get(i);
            if (root instanceof PackageNode pkg && pkg.contains(point)) {
                return Optional.of(pkg);
            }
        }
        return Optional.empty();
    }

    public void selectInArea(Rectangle2DInt area, boolean additive) {
        if (!additive) {
            selectedElements.clear();
        }
        if (area == null) {
            notifyListeners();
            return;
        }
        for (Node root : rootNodes) {
            selectNodeInAreaRecursive(root, area);
        }
        for (Edge edge : edges) {
            if (area.intersects(edge.getBounds())) {
                selectedElements.add(edge);
            }
        }
        notifyListeners();
    }

    private void selectNodeInAreaRecursive(Node node, Rectangle2DInt area) {
        if (area.intersects(node.getBounds())) {
            selectedElements.add(node);
        }
        if (node instanceof ParentNode parent) {
            for (ChildNode child : parent.getChildren()) {
                selectNodeInAreaRecursive(child, area);
            }
        }
    }

    private Optional<Node> findDeepestNodeAt(Node current, Point2DInt point) {
        if (current instanceof ParentNode parent) {
            List<ChildNode> children = parent.getChildren();
            for (int i = children.size() - 1; i >= 0; i--) {
                Optional<Node> childMatch = findDeepestNodeAt(children.get(i), point);
                if (childMatch.isPresent()) {
                    return childMatch;
                }
            }
        }
        if (current.contains(point)) {
            return Optional.of(current);
        }
        return Optional.empty();
    }

    /**
     * Localiza o no pai mais interno que contem o ponto informado e aceita o filho candidato.
     */
    public Optional<ParentNode> findContainerAt(Point2DInt point, Node candidate) {
        List<Node> allNodes = getAllNodes();
        for (int i = allNodes.size() - 1; i >= 0; i--) {
            Node n = allNodes.get(i);
            if (n != candidate && n instanceof ParentNode parent
                    && parent.contains(point) && parent.canAcceptChild(candidate)) {
                return Optional.of(parent);
            }
        }
        return Optional.empty();
    }

    public Set<DiagramElement> getSelectedElements() {
        return Collections.unmodifiableSet(selectedElements);
    }

    public void setSelection(Set<DiagramElement> elements) {
        selectedElements.clear();
        if (elements != null) {
            selectedElements.addAll(elements);
        }
        notifyListeners();
    }

    public void selectSingle(DiagramElement element) {
        selectedElements.clear();
        if (element != null) {
            selectedElements.add(element);
        }
        notifyListeners();
    }

    public void toggleSelection(DiagramElement element) {
        if (element == null) return;
        if (selectedElements.contains(element)) {
            selectedElements.remove(element);
        } else {
            selectedElements.add(element);
        }
        notifyListeners();
    }

    public void selectAll() {
        selectedElements.clear();
        selectedElements.addAll(getAllNodes());
        selectedElements.addAll(edges);
        notifyListeners();
    }

    public void clearSelection() {
        if (!selectedElements.isEmpty()) {
            selectedElements.clear();
            notifyListeners();
        }
    }

    /**
     * Calcula o retangulo minimo que engloba todos os nos e arestas do diagrama (RF04, RF05).
     */
    public Rectangle2DInt getContentBounds() {
        Rectangle2DInt result = null;
        for (Node node : getAllNodes()) {
            Rectangle2DInt b = node.getBounds();
            result = (result == null) ? b : result.union(b);
        }
        for (Edge edge : edges) {
            Rectangle2DInt b = edge.getBounds();
            result = (result == null) ? b : result.union(b);
        }
        return result != null ? result : new Rectangle2DInt(0, 0, 200, 150);
    }

    /**
     * Cria uma copia profunda completa do diagrama atual (RF03).
     */
    public Diagram deepCopy() {
        Diagram copy = new Diagram(this.type);
        copy.title = "Copy of " + this.title;
        copy.width = this.width;
        copy.height = this.height;

        Map<Node, Node> nodeMap = new LinkedHashMap<>();
        for (Node root : this.rootNodes) {
            Node clonedRoot = cloneHierarchy(root, nodeMap);
            copy.rootNodes.add(clonedRoot);
        }
        for (Edge edge : this.edges) {
            Node mappedStart = nodeMap.get(edge.getStartNode());
            Node mappedEnd = nodeMap.get(edge.getEndNode());
            if (mappedStart != null && mappedEnd != null) {
                copy.edges.add(edge.deepCopy(mappedStart, mappedEnd));
            }
        }
        copy.dirty = true;
        return copy;
    }

    private Node cloneHierarchy(Node original, Map<Node, Node> nodeMap) {
        Node cloned = original.deepCopy();
        nodeMap.put(original, cloned);
        if (original instanceof ParentNode origParent && cloned instanceof ParentNode clonedParent) {
            for (ChildNode origChild : origParent.getChildren()) {
                Node clonedChildNode = cloneHierarchy(origChild, nodeMap);
                if (clonedChildNode instanceof ChildNode clonedChild) {
                    clonedParent.addChild(clonedChild);
                }
            }
        }
        return cloned;
    }

    public void addListener(DiagramListener listener) {
        listeners.add(listener);
    }

    public void removeListener(DiagramListener listener) {
        listeners.remove(listener);
    }

    public void notifyListeners() {
        for (DiagramListener listener : List.copyOf(listeners)) {
            listener.onDiagramChanged(this);
        }
    }
}
