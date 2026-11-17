package org.jetuml.ui;

import javafx.scene.canvas.Canvas;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.input.*;
import org.jetuml.command.*;
import org.jetuml.geom.Point2DInt;
import org.jetuml.geom.Rectangle2DInt;
import org.jetuml.layout.EdgeRouter;
import org.jetuml.layout.SequenceFlowManager;
import org.jetuml.layout.SequenceFlowManager.SequenceCallCreationPlan;
import org.jetuml.model.*;
import org.jetuml.model.edges.NoteConnectorEdge;
import org.jetuml.model.nodes.NoteNode;
import org.jetuml.model.nodes.PackageNode;
import org.jetuml.model.nodes.PointNode;
import org.jetuml.rendering.DiagramRenderer;
import org.jetuml.rendering.DiagramRenderer.OverlayState;
import org.jetuml.validation.DiagramValidator;
import org.jetuml.validation.ValidationResult;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Area de desenho interativa em JavaFX Canvas com suporte completo a:
 * - Criacao de nos com snap-to-grid (RF11, RF15, RF25);
 * - Criacao de arestas por clique-e-arraste com linha elastica (RF14, RF16, RF26-RF30);
 * - Selecao individual, Ctrl+Clique, laco de selecao e teclado (RF12, RF13, RF32, RF33);
 * - Arraste de nos com roteamento automatico e inclusao/remocao em Pacotes via Shift (RF15, RF17);
 * - Duplo clique para edicao de propriedades (RF20).
 */
public class DiagramCanvasView extends Canvas {

    private final Diagram diagram;
    private final CommandManager commandManager;
    private final NotificationOverlay notifications;
    private final Supplier<ToolType> activeToolSupplier;
    private final Runnable resetToSelectToolCallback;
    private final OverlayState overlayState = new OverlayState();
    private final Consumer<ToolType> selectToolCallback;
    private final ContextMenu toolsContextMenu = new ContextMenu();
    private InteractionMode mode = InteractionMode.NONE;
    private Point2DInt pressPoint;
    private Point2DInt lastDragPoint;
    private int accumulatedDragDx = 0;
    private int accumulatedDragDy = 0;
    private Node edgeStartNode;
    public DiagramCanvasView(
            Diagram diagram,
            CommandManager commandManager,
            NotificationOverlay notifications,
            Supplier<ToolType> activeToolSupplier,
            Consumer<ToolType> selectToolCallback,
            Runnable resetToSelectToolCallback
    ) {
        super(diagram.getWidth(), diagram.getHeight());
        this.diagram = diagram;
        this.commandManager = commandManager;
        this.notifications = notifications;
        this.activeToolSupplier = activeToolSupplier;
        this.selectToolCallback = selectToolCallback;
        this.resetToSelectToolCallback = resetToSelectToolCallback;

        setFocusTraversable(true);

        diagram.addListener(d -> redraw());
        setOnMousePressed(this::handleMousePressed);
        setOnMouseDragged(this::handleMouseDragged);
        setOnMouseReleased(this::handleMouseReleased);
        setOnKeyPressed(this::handleKeyPressed);
        setOnContextMenuRequested(this::handleContextMenuRequested);

        redraw();
    }

    public Diagram getDiagram() {
        return diagram;
    }

    public CommandManager getCommandManager() {
        return commandManager;
    }

    private void handleContextMenuRequested(ContextMenuEvent event) {
        toolsContextMenu.hide();
        toolsContextMenu.getItems().clear();

        ToolType activeTool = activeToolSupplier.get();
        List<ToolType> tools = diagram.getType().getAvailableTools();

        for (int i = 0; i < tools.size(); i++) {
            ToolType tool = tools.get(i);
            int shortcutNum = i + 1;
            String prefix = (shortcutNum <= 9) ? "[" + shortcutNum + "] " : "";
            String activeMarker = (tool == activeTool) ? " ✓" : "";

            MenuItem item = new MenuItem(prefix + tool.getDisplayName() + activeMarker);
            item.setOnAction(e -> {
                if (selectToolCallback != null) {
                    selectToolCallback.accept(tool);
                }
            });

            toolsContextMenu.getItems().add(item);

            // Separa visualmente a ferramenta Selecionar das ferramentas de criacao
            if (i == 0 && tools.size() > 1) {
                toolsContextMenu.getItems().add(new SeparatorMenuItem());
            }
        }

        toolsContextMenu.show(this, event.getScreenX(), event.getScreenY());
        event.consume();
    }

    public void redraw() {
        if (getWidth() != diagram.getWidth()) {
            setWidth(diagram.getWidth());
        }
        if (getHeight() != diagram.getHeight()) {
            setHeight(diagram.getHeight());
        }
        DiagramRenderer.render(getGraphicsContext2D(), diagram, true, true, overlayState);
    }

    private void handleMousePressed(MouseEvent event) {
        toolsContextMenu.hide();
        if (event.getButton() != MouseButton.PRIMARY) return;
        requestFocus();

        Point2DInt pt = new Point2DInt((int) Math.round(event.getX()), (int) Math.round(event.getY()));
        this.pressPoint = pt;
        this.lastDragPoint = pt;

        ToolType currentTool = activeToolSupplier.get();

        // Duplo clique abre editor de propriedades (RF20)
        if (event.getClickCount() == 2) {
            Optional<DiagramElement> hit = diagram.findElementAt(pt);
            if (hit.isPresent()) {
                diagram.selectSingle(hit.get());
                PropertyEditorDialog.showAndBuildCommand(diagram, hit.get())
                        .ifPresent(commandManager::execute);
            }
            return;
        }

        // Modo 1: Ferramenta de criacao de NO (RF11, RF15, RF25)
        if (currentTool != null && currentTool.isNodeTool()) {
            handleNodeCreationClick(currentTool, pt);
            return;
        }

        // Modo 2: Ferramenta de criacao de ARESTA (RF14, RF16, RF26-RF30)
        if (currentTool != null && currentTool.isEdgeTool()) {
            Optional<Node> startOpt = diagram.findNodeAt(pt);
            if (startOpt.isPresent()) {
                this.mode = InteractionMode.DRAWING_EDGE;
                this.edgeStartNode = startOpt.get();
                this.overlayState.rubberBandStart = pt;
                this.overlayState.rubberBandEnd = pt;
                redraw();
            } else {
                notifications.showError("Clique e arraste a partir de um elemento valido para criar a conexao.");
            }
            return;
        }

        // Modo 3: Ferramenta SELECT (RF12, RF32, RF33)
        Optional<DiagramElement> hitOpt = diagram.findElementAt(pt);
        if (hitOpt.isPresent()) {
            DiagramElement hit = hitOpt.get();
            if (event.isControlDown()) {
                diagram.toggleSelection(hit);
            } else if (!diagram.isSelected(hit)) {
                diagram.selectSingle(hit);
            }

            if (hit instanceof Node) {
                this.mode = InteractionMode.DRAGGING_NODES;
                this.accumulatedDragDx = 0;
                this.accumulatedDragDy = 0;
            }
        } else {
            if (!event.isControlDown()) {
                diagram.clearSelection();
            }
            this.mode = InteractionMode.LASSO_SELECTING;
            this.overlayState.selectionLasso = new Rectangle2DInt(pt.x(), pt.y(), 0, 0);
            redraw();
        }
    }

    private void handleNodeCreationClick(ToolType tool, Point2DInt rawPt) {
        Point2DInt snapped = diagram.getGrid().snap(rawPt);
        Optional<Node> containerOpt = diagram.findContainerCandidateAt(rawPt);
        Node parentCandidate = containerOpt.orElse(null);

        ValidationResult validation = DiagramValidator.validateNodeCreation(diagram, tool, parentCandidate);
        if (!validation.isValid()) {
            if (!validation.isSilent()) {
                notifications.showError(validation.getErrorMessage());
            }
            return;
        }

        Optional<Node> createdOpt = ElementFactory.createNode(tool, snapped);
        if (createdOpt.isEmpty()) return;

        Node created = createdOpt.get();
        ParentNode parent = (parentCandidate instanceof ParentNode p) ? p : null;
        commandManager.execute(new AddNodeCommand(diagram, created, parent));
        if (resetToSelectToolCallback != null) {
            resetToSelectToolCallback.run();
        }
    }

    private void handleMouseDragged(MouseEvent event) {
        if (event.getButton() != MouseButton.PRIMARY) return;
        Point2DInt pt = new Point2DInt((int) Math.round(event.getX()), (int) Math.round(event.getY()));

        if (mode == InteractionMode.DRAGGING_NODES) {
            Point2DInt snappedPrev = diagram.getGrid().snap(lastDragPoint);
            Point2DInt snappedCurr = diagram.getGrid().snap(pt);
            int dx = snappedCurr.x() - snappedPrev.x();
            int dy = snappedCurr.y() - snappedPrev.y();

            if (dx != 0 || dy != 0) {
                for (Node n : getTopLevelSelectedNodes()) {
                    n.translate(dx, dy);
                }
                accumulatedDragDx += dx;
                accumulatedDragDy += dy;
                lastDragPoint = snappedCurr;
                EdgeRouter.routeAll(diagram);
            }

            // Destaque visual quando Shift esta pressionado sobre um PackageNode (RF15)
            overlayState.dropTargetHighlight = null;
            if (event.isShiftDown()) {
                diagram.findPackageAt(pt).ifPresent(pkg -> overlayState.dropTargetHighlight = pkg.getBounds());
            }
            redraw();
        } else if (mode == InteractionMode.DRAWING_EDGE) {
            overlayState.rubberBandEnd = pt;
            redraw();
        } else if (mode == InteractionMode.LASSO_SELECTING && pressPoint != null) {
            int minX = Math.min(pressPoint.x(), pt.x());
            int minY = Math.min(pressPoint.y(), pt.y());
            int w = Math.abs(pt.x() - pressPoint.x());
            int h = Math.abs(pt.y() - pressPoint.y());
            overlayState.selectionLasso = new Rectangle2DInt(minX, minY, w, h);
            redraw();
        }
    }

    private void handleMouseReleased(MouseEvent event) {
        if (event.getButton() != MouseButton.PRIMARY) return;
        Point2DInt releasePt = new Point2DInt((int) Math.round(event.getX()), (int) Math.round(event.getY()));

        if (mode == InteractionMode.DRAGGING_NODES) {
            List<Node> moved = getTopLevelSelectedNodes();
            if (accumulatedDragDx != 0 || accumulatedDragDy != 0) {
                // Reverte o deslocamento provisorio e executa via Command para suportar Undo/Redo (RF21)
                for (Node n : moved) {
                    n.translate(-accumulatedDragDx, -accumulatedDragDy);
                }
                commandManager.execute(new MoveNodesCommand(diagram, moved, accumulatedDragDx, accumulatedDragDy));
            }

            // Inclusao ou desanexacao de Pacote via Shift (RF15)
            if (event.isShiftDown() && moved.size() == 1 && moved.get(0) instanceof ChildNode child) {
                handlePackageReparenting(child, releasePt);
            }
        } else if (mode == InteractionMode.DRAWING_EDGE && edgeStartNode != null) {
            handleEdgeCreationRelease(edgeStartNode, pressPoint, releasePt);
        } else if (mode == InteractionMode.LASSO_SELECTING && overlayState.selectionLasso != null) {
            diagram.selectInArea(overlayState.selectionLasso, event.isControlDown());
        }

        // Limpar estado transitorio
        this.mode = InteractionMode.NONE;
        this.edgeStartNode = null;
        this.overlayState.rubberBandStart = null;
        this.overlayState.rubberBandEnd = null;
        this.overlayState.selectionLasso = null;
        this.overlayState.dropTargetHighlight = null;
        redraw();
    }

    private void handlePackageReparenting(ChildNode child, Point2DInt releasePt) {
        Optional<PackageNode> targetPkgOpt = diagram.findPackageAt(releasePt)
                .filter(pkg -> pkg != child);
        ParentNode currentParent = child.getParent();

        if (targetPkgOpt.isPresent()) {
            PackageNode targetPkg = targetPkgOpt.get();
            if (currentParent != targetPkg) {
                ValidationResult res = DiagramValidator.validateNodeCreation(diagram, child.getToolType(), targetPkg);
                if (res.isValid()) {
                    commandManager.execute(new ReparentNodeCommand(diagram, child, currentParent, targetPkg));
                } else if (!res.isSilent()) {
                    notifications.showError(res.getErrorMessage());
                }
            }
        } else if (currentParent instanceof PackageNode) {
            // Desanexar do pacote ao soltar fora mantendo Shift pressionado (RF15)
            commandManager.execute(new ReparentNodeCommand(diagram, child, currentParent, null));
        }
    }

    private void handleEdgeCreationRelease(Node startNode, Point2DInt startPt, Point2DInt endPt) {
        ToolType tool = activeToolSupplier.get();
        if (tool == null || !tool.isEdgeTool()) return;

        Optional<Node> endNodeOpt = diagram.findNodeAt(endPt);

        // RF14: NoteConnector pode terminar em um ponto fixo do canvas (cria PointNode)
        if (tool == ToolType.NOTE_CONNECTOR && endNodeOpt.isEmpty() && startNode instanceof NoteNode) {
            Point2DInt snappedAnchor = diagram.getGrid().snap(endPt);
            PointNode anchor = new PointNode(snappedAnchor);
            NoteConnectorEdge connector = new NoteConnectorEdge(startNode, anchor);
            commandManager.execute(new AddEdgeCommand(diagram, connector, anchor));
            if (resetToSelectToolCallback != null) {
                resetToSelectToolCallback.run();
            }
            return;
        }

        Node endNode = endNodeOpt.orElse(null);
        ValidationResult validation = DiagramValidator.validateEdgeCreation(
                diagram, tool, startNode, endNode, startPt, endPt
        );
        if (!validation.isValid()) {
            if (!validation.isSilent()) {
                notifications.showError(validation.getErrorMessage());
            }
            return;
        }

        // Caso especial: Chamada no Diagrama de Sequencia (RF27, RF28, RF29)
        if (tool == ToolType.CALL) {
            SequenceCallCreationPlan plan = SequenceFlowManager.buildCallPlan(startNode, endNode, startPt, endPt);
            commandManager.execute(new SequenceCallCommand(diagram, plan));
            if (resetToSelectToolCallback != null) {
                resetToSelectToolCallback.run();
            }
            return;
        }

        Optional<Edge> edgeOpt = ElementFactory.createEdge(tool, startNode, endNode);
        if (edgeOpt.isPresent()) {
            commandManager.execute(new AddEdgeCommand(diagram, edgeOpt.get()));
            if (resetToSelectToolCallback != null) {
                resetToSelectToolCallback.run();
            }
        }
    }

    private void handleKeyPressed(KeyEvent event) {
        // Movimentacao por setas direcionais (RF33)
        int step = diagram.getGrid().gridSize();
        int dx = 0;
        int dy = 0;
        if (event.getCode() == KeyCode.LEFT) dx = -step;
        else if (event.getCode() == KeyCode.RIGHT) dx = step;
        else if (event.getCode() == KeyCode.UP) dy = -step;
        else if (event.getCode() == KeyCode.DOWN) dy = step;

        if (dx != 0 || dy != 0) {
            List<Node> selectedNodes = getTopLevelSelectedNodes();
            if (!selectedNodes.isEmpty()) {
                commandManager.execute(new MoveNodesCommand(diagram, selectedNodes, dx, dy));
                event.consume();
            }
        }
    }

    private List<Node> getTopLevelSelectedNodes() {
        Set<DiagramElement> selected = diagram.getSelectedElements();
        Set<Node> nodeSet = new LinkedHashSet<>();
        for (DiagramElement el : selected) {
            if (el instanceof Node n) {
                nodeSet.add(n);
            }
        }
        List<Node> topLevel = new ArrayList<>();
        for (Node n : nodeSet) {
            if (n instanceof ChildNode child && child.getParent() != null && nodeSet.contains(child.getParent())) {
                continue;
            }
            topLevel.add(n);
        }
        return topLevel;
    }

    private enum InteractionMode {
        NONE,
        DRAGGING_NODES,
        DRAWING_EDGE,
        LASSO_SELECTING
    }
}
