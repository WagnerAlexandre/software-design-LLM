package org.jetuml.ui;

import javafx.scene.Group;
import javafx.scene.control.*;
import javafx.scene.transform.Scale;
import org.jetuml.command.CommandManager;
import org.jetuml.model.Diagram;
import org.jetuml.model.ToolType;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Gerenciador de abas simultaneas de diagramas UML (RF07, RF08).
 * Cada aba encapsula seu proprio Diagram, CommandManager, DiagramCanvasView e fator de zoom independente.
 */
public class DiagramTabPane extends TabPane {

    private final NotificationOverlay notifications;
    private final Supplier<ToolType> activeToolSupplier;
    private final Consumer<ToolType> selectToolCallback;
    private final Runnable resetToSelectToolCallback;
    private final Consumer<DiagramTabContext> onActiveTabChanged;
    private final Consumer<Diagram> onSaveRequested;
    private final Map<Tab, DiagramTabContext> contexts = new HashMap<>();

    public DiagramTabPane(
            NotificationOverlay notifications,
            Supplier<ToolType> activeToolSupplier,
            Runnable resetToSelectToolCallback,
            Consumer<DiagramTabContext> onActiveTabChanged,
            Consumer<Diagram> onSaveRequested
    ) {
        this(
                notifications,
                activeToolSupplier,
                tool -> {
                },
                resetToSelectToolCallback,
                onActiveTabChanged,
                onSaveRequested
        );
    }

    public DiagramTabPane(
            NotificationOverlay notifications,
            Supplier<ToolType> activeToolSupplier,
            Consumer<ToolType> selectToolCallback,
            Runnable resetToSelectToolCallback,
            Consumer<DiagramTabContext> onActiveTabChanged,
            Consumer<Diagram> onSaveRequested
    ) {
        this.notifications = notifications;
        this.activeToolSupplier = activeToolSupplier;
        this.selectToolCallback = selectToolCallback;
        this.resetToSelectToolCallback = resetToSelectToolCallback;
        this.onActiveTabChanged = onActiveTabChanged;
        this.onSaveRequested = onSaveRequested;

        setTabClosingPolicy(TabClosingPolicy.ALL_TABS);

        getSelectionModel().selectedItemProperty().addListener((obs, oldTab, newTab) -> {
            if (this.onActiveTabChanged != null) {
                this.onActiveTabChanged.accept(newTab != null ? contexts.get(newTab) : null);
            }
        });
    }

    public DiagramTabContext openDiagram(Diagram diagram) {
        CommandManager cmdManager = new CommandManager();
        DiagramCanvasView canvasView = new DiagramCanvasView(
                diagram,
                cmdManager,
                notifications,
                activeToolSupplier,
                selectToolCallback,
                resetToSelectToolCallback
        );

        Scale zoomTransform = new Scale(1.0, 1.0, 0, 0);
        canvasView.getTransforms().add(zoomTransform);

        Group zoomGroup = new Group(canvasView);
        ScrollPane scrollPane = new ScrollPane(zoomGroup);
        scrollPane.setPannable(false);
        scrollPane.setFitToWidth(false);
        scrollPane.setFitToHeight(false);

        Tab tab = new Tab(diagram.getTitle(), scrollPane);
        DiagramTabContext context = new DiagramTabContext(tab, diagram, cmdManager, canvasView, zoomTransform);
        contexts.put(tab, context);

        diagram.addListener(d -> tab.setText(diagram.getTitle()));

        tab.setOnCloseRequest(event -> {
            if (!confirmCloseTab(context)) {
                event.consume();
            } else {
                contexts.remove(tab);
            }
        });

        getTabs().add(tab);
        getSelectionModel().select(tab);
        return context;
    }

    public Optional<DiagramTabContext> getActiveContext() {
        Tab selected = getSelectionModel().getSelectedItem();
        return Optional.ofNullable(selected != null ? contexts.get(selected) : null);
    }

    public void redrawAllTabs() {
        for (DiagramTabContext ctx : contexts.values()) {
            ctx.canvasView.redraw();
        }
    }

    public void closeActiveTab() {
        Tab selected = getSelectionModel().getSelectedItem();
        if (selected == null) return;
        DiagramTabContext ctx = contexts.get(selected);
        if (ctx == null || confirmCloseTab(ctx)) {
            contexts.remove(selected);
            getTabs().remove(selected);
        }
    }

    public boolean confirmCloseAllTabs() {
        for (Tab tab : java.util.List.copyOf(getTabs())) {
            DiagramTabContext ctx = contexts.get(tab);
            if (ctx != null && !confirmCloseTab(ctx)) {
                return false;
            }
        }
        return true;
    }

    private boolean confirmCloseTab(DiagramTabContext ctx) {
        if (!ctx.diagram.isDirty()) {
            return true;
        }
        getSelectionModel().select(ctx.tab);

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Alteracoes Nao Salvas");
        alert.setHeaderText("O diagrama " + ctx.diagram.getTitle().replace("*", "") + "possui alteracoes nao salvas.");
        alert.setContentText("Deseja salvar antes de fechar a aba?");

        ButtonType saveBtn = new ButtonType("Salvar");
        ButtonType discardBtn = new ButtonType("Descartar");
        ButtonType cancelBtn = new ButtonType("Cancelar", ButtonBar.ButtonData.CANCEL_CLOSE);
        alert.getButtonTypes().setAll(saveBtn, discardBtn, cancelBtn);

        Optional<ButtonType> choice = alert.showAndWait();
        if (choice.isEmpty() || choice.get() == cancelBtn) {
            return false;
        }
        if (choice.get() == saveBtn && onSaveRequested != null) {
            onSaveRequested.accept(ctx.diagram);
            return !ctx.diagram.isDirty();
        }
        return true;
    }

    public static class DiagramTabContext {
        public final Tab tab;
        public final Diagram diagram;
        public final CommandManager commandManager;
        public final DiagramCanvasView canvasView;
        public final Scale zoomTransform;
        private double zoomFactor = 1.0;

        public DiagramTabContext(
                Tab tab,
                Diagram diagram,
                CommandManager commandManager,
                DiagramCanvasView canvasView,
                Scale zoomTransform
        ) {
            this.tab = tab;
            this.diagram = diagram;
            this.commandManager = commandManager;
            this.canvasView = canvasView;
            this.zoomTransform = zoomTransform;
        }

        public double getZoomFactor() {
            return zoomFactor;
        }

        public void setZoomFactor(double factor) {
            double clamped = Math.max(0.50, Math.min(2.00, Math.round(factor * 100.0) / 100.0));
            this.zoomFactor = clamped;
            this.zoomTransform.setX(clamped);
            this.zoomTransform.setY(clamped);
        }
    }
}
