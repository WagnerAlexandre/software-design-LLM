package org.jetuml.ui;

import javafx.collections.ListChangeListener;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.jetuml.clipboard.DiagramClipboard;
import org.jetuml.command.DeleteElementsCommand;
import org.jetuml.config.UserPreferences;
import org.jetuml.export.DiagramImageExporter;
import org.jetuml.model.Diagram;
import org.jetuml.model.DiagramElement;
import org.jetuml.model.DiagramType;
import org.jetuml.model.ToolType;
import org.jetuml.persistence.DiagramJsonSerializer;
import org.jetuml.ui.DiagramTabPane.DiagramTabContext;
import org.jetuml.validation.ValidationResult;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Janela principal do JetUML reunindo:
 * - Tela de Boas-Vindas inicial e para estado vazio (RF01);
 * - Barra de menus completa (Arquivo, Editar, Exibir, Ajuda) com atalhos de teclado (RF01-RF10, RF36-RF44);
 * - Barra de ferramentas lateral dinamica por tipo de diagrama com atalhos numericos 1..9 e menu de contexto (RF09, RF10);
 * - Abas de diagramas simultaneos (RF07, RF08);
 * - Notificacoes temporarias e status de zoom (RF37, RF41, RF42).
 */
public class JetUMLMainWindow {

    private final Stage stage;
    private final BorderPane rootPane;
    private final NotificationOverlay notificationOverlay;
    private final DiagramTabPane tabPane;
    private final WelcomePane welcomePane;
    private final VBox sideToolBar;
    private final ToggleGroup toolToggleGroup = new ToggleGroup();
    private final List<ToggleButton> currentToolButtons = new ArrayList<>();
    private final Menu recentFilesMenu = new Menu("Arquivos Recentes");
    private final Label statusDiagramLabel = new Label("Nenhum diagrama aberto");
    private final Label statusZoomLabel = new Label("Zoom: 100%");

    private ToolType activeTool = ToolType.SELECT;

    public JetUMLMainWindow(Stage stage) {
        this.stage = stage;
        this.rootPane = new BorderPane();
        this.notificationOverlay = new NotificationOverlay();

        this.tabPane = new DiagramTabPane(
                notificationOverlay,
                () -> activeTool,
                this::selectTool,
                this::selectDefaultTool,
                this::onTabChanged,
                this::saveDiagram
        );

        this.welcomePane = new WelcomePane(
                this::createNewDiagram,
                this::openDiagramFromDialog,
                this::openDiagramFile
        );

        this.sideToolBar = new VBox(6);
        this.sideToolBar.setPadding(new Insets(10, 8, 10, 8));
        this.sideToolBar.setPrefWidth(190);
        this.sideToolBar.getStyleClass().add("side-toolbar");

        MenuBar menuBar = buildMenuBar();
        ToolBar quickActionBar = buildTopQuickBar();
        VBox topContainer = new VBox(menuBar, quickActionBar);

        StackPane centerStack = new StackPane(tabPane, welcomePane, notificationOverlay);

        // Alterna automaticamente entre a Tela de Boas-Vindas e as abas abertas (RF01)
        tabPane.getTabs().addListener((ListChangeListener<Tab>) change -> {
            boolean hasOpenTabs = !tabPane.getTabs().isEmpty();
            tabPane.setVisible(hasOpenTabs);
            welcomePane.setVisible(!hasOpenTabs);
            if (!hasOpenTabs) {
                welcomePane.refreshRecentFiles();
            }
        });
        tabPane.setVisible(false);
        welcomePane.setVisible(true);

        HBox statusBar = buildStatusBar();

        rootPane.setTop(topContainer);
        rootPane.setLeft(sideToolBar);
        rootPane.setCenter(centerStack);
        rootPane.setBottom(statusBar);

        Scene scene = new Scene(rootPane, 1280, 800);
        applyThemeStyles(scene);
        registerGlobalKeyShortcuts(scene);

        stage.setTitle("JetUML - Editor de Diagramas UML");
        stage.setScene(scene);
        stage.setOnCloseRequest(event -> {
            if (!tabPane.confirmCloseAllTabs()) {
                event.consume();
            }
        });

        rebuildSideToolbarFor(null);
        refreshRecentFilesMenu();
    }

    private MenuBar buildMenuBar() {
        MenuBar menuBar = new MenuBar();

        // ---------------- MENU ARQUIVO ----------------
        Menu fileMenu = new Menu("_Arquivo");

        Menu newMenu = new Menu("Novo Diagrama");
        for (DiagramType type : DiagramType.values()) {
            MenuItem item = new MenuItem(type.getDisplayName());
            if (type == DiagramType.CLASS) {
                item.setAccelerator(new KeyCodeCombination(KeyCode.N, KeyCombination.CONTROL_DOWN));
            }
            item.setOnAction(e -> createNewDiagram(type));
            newMenu.getItems().add(item);
        }

        MenuItem openItem = new MenuItem("Abrir...");
        openItem.setAccelerator(new KeyCodeCombination(KeyCode.O, KeyCombination.CONTROL_DOWN));
        openItem.setOnAction(e -> openDiagramFromDialog());

        MenuItem saveItem = new MenuItem("Salvar");
        saveItem.setAccelerator(new KeyCodeCombination(KeyCode.S, KeyCombination.CONTROL_DOWN));
        saveItem.setOnAction(e -> tabPane.getActiveContext().ifPresent(ctx -> saveDiagram(ctx.diagram)));

        MenuItem saveAsItem = new MenuItem("Salvar Como...");
        saveAsItem.setAccelerator(new KeyCodeCombination(KeyCode.S, KeyCombination.CONTROL_DOWN, KeyCombination.SHIFT_DOWN));
        saveAsItem.setOnAction(e -> tabPane.getActiveContext().ifPresent(ctx -> saveDiagramAs(ctx.diagram)));

        MenuItem exportImgItem = new MenuItem("Exportar Imagem...");
        exportImgItem.setAccelerator(new KeyCodeCombination(KeyCode.E, KeyCombination.CONTROL_DOWN));
        exportImgItem.setOnAction(e -> exportDiagramImage());

        MenuItem copyImgItem = new MenuItem("Copiar Imagem para Area de Transferencia");
        copyImgItem.setAccelerator(new KeyCodeCombination(KeyCode.B, KeyCombination.CONTROL_DOWN));
        copyImgItem.setOnAction(e -> copyDiagramImageToClipboard());

        MenuItem closeTabItem = new MenuItem("Fechar Aba");
        closeTabItem.setAccelerator(new KeyCodeCombination(KeyCode.W, KeyCombination.CONTROL_DOWN));
        closeTabItem.setOnAction(e -> tabPane.closeActiveTab());

        MenuItem exitItem = new MenuItem("Sair");
        exitItem.setOnAction(e -> {
            if (tabPane.confirmCloseAllTabs()) {
                stage.close();
            }
        });

        fileMenu.getItems().addAll(
                newMenu,
                openItem,
                recentFilesMenu,
                new SeparatorMenuItem(),
                saveItem,
                saveAsItem,
                new SeparatorMenuItem(),
                exportImgItem,
                copyImgItem,
                new SeparatorMenuItem(),
                closeTabItem,
                exitItem
        );

        // ---------------- MENU EDITAR ----------------
        Menu editMenu = new Menu("_Editar");

        MenuItem undoItem = new MenuItem("Desfazer (Undo)");
        undoItem.setAccelerator(new KeyCodeCombination(KeyCode.Z, KeyCombination.CONTROL_DOWN));
        undoItem.setOnAction(e -> tabPane.getActiveContext().ifPresent(ctx -> ctx.commandManager.undo()));

        MenuItem redoItem = new MenuItem("Refazer (Redo)");
        redoItem.setAccelerator(new KeyCodeCombination(KeyCode.Y, KeyCombination.CONTROL_DOWN));
        redoItem.setOnAction(e -> tabPane.getActiveContext().ifPresent(ctx -> ctx.commandManager.redo()));

        MenuItem cutItem = new MenuItem("Recortar");
        cutItem.setAccelerator(new KeyCodeCombination(KeyCode.X, KeyCombination.CONTROL_DOWN));
        cutItem.setOnAction(e -> performCut());

        MenuItem copyItem = new MenuItem("Copiar");
        copyItem.setAccelerator(new KeyCodeCombination(KeyCode.C, KeyCombination.CONTROL_DOWN));
        copyItem.setOnAction(e -> performCopy());

        MenuItem pasteItem = new MenuItem("Colar");
        pasteItem.setAccelerator(new KeyCodeCombination(KeyCode.V, KeyCombination.CONTROL_DOWN));
        pasteItem.setOnAction(e -> performPaste());

        MenuItem selectAllItem = new MenuItem("Selecionar Tudo");
        selectAllItem.setAccelerator(new KeyCodeCombination(KeyCode.A, KeyCombination.CONTROL_DOWN));
        selectAllItem.setOnAction(e -> tabPane.getActiveContext().ifPresent(ctx -> ctx.diagram.selectAll()));

        MenuItem propertiesItem = new MenuItem("Propriedades do Elemento...");
        propertiesItem.setOnAction(e -> editSelectedElementProperties());

        MenuItem deleteItem = new MenuItem("Excluir Selecionados");
        deleteItem.setAccelerator(new KeyCodeCombination(KeyCode.DELETE));
        deleteItem.setOnAction(e -> deleteSelectedElements());

        editMenu.getItems().addAll(
                undoItem,
                redoItem,
                new SeparatorMenuItem(),
                cutItem,
                copyItem,
                pasteItem,
                new SeparatorMenuItem(),
                selectAllItem,
                propertiesItem,
                deleteItem
        );

        // ---------------- MENU EXIBIR ----------------
        Menu viewMenu = new Menu("E_xibir");

        MenuItem zoomInItem = new MenuItem("Aumentar Zoom (+)");
        zoomInItem.setAccelerator(new KeyCodeCombination(KeyCode.EQUALS, KeyCombination.CONTROL_DOWN));
        zoomInItem.setOnAction(e -> adjustZoom(0.10));

        MenuItem zoomOutItem = new MenuItem("Diminuir Zoom (-)");
        zoomOutItem.setAccelerator(new KeyCodeCombination(KeyCode.MINUS, KeyCombination.CONTROL_DOWN));
        zoomOutItem.setOnAction(e -> adjustZoom(-0.10));

        MenuItem zoomResetItem = new MenuItem("Restaurar Zoom (100%)");
        zoomResetItem.setAccelerator(new KeyCodeCombination(KeyCode.DIGIT0, KeyCombination.CONTROL_DOWN));
        zoomResetItem.setOnAction(e -> setZoom(1.00));

        CheckMenuItem showGridItem = new CheckMenuItem("Exibir Grade");
        showGridItem.setSelected(UserPreferences.getInstance().isShowGrid());
        showGridItem.setAccelerator(new KeyCodeCombination(KeyCode.G, KeyCombination.CONTROL_DOWN));
        showGridItem.setOnAction(e -> {
            UserPreferences.getInstance().setShowGrid(showGridItem.isSelected());
            tabPane.redrawAllTabs();
        });

        CheckMenuItem darkModeItem = new CheckMenuItem("Modo Escuro");
        darkModeItem.setSelected(UserPreferences.getInstance().isDarkMode());
        darkModeItem.setOnAction(e -> {
            UserPreferences.getInstance().setDarkMode(darkModeItem.isSelected());
            applyThemeStyles(stage.getScene());
            tabPane.redrawAllTabs();
        });

        MenuItem diagramSizeItem = new MenuItem("Dimensao do Diagrama...");
        diagramSizeItem.setOnAction(e -> tabPane.getActiveContext().ifPresent(ctx -> {
            if (PreferencesDialog.showDiagramSizeDialog(ctx.diagram)) {
                updateStatusBar(ctx);
            }
        }));

        MenuItem fontItem = new MenuItem("Fonte...");
        fontItem.setOnAction(e -> {
            if (PreferencesDialog.showFontDialog()) {
                tabPane.redrawAllTabs();
            }
        });

        MenuItem prefsItem = new MenuItem("Preferencias...");
        prefsItem.setOnAction(e -> {
            if (PreferencesDialog.showGeneralPreferencesDialog()) {
                showGridItem.setSelected(UserPreferences.getInstance().isShowGrid());
                darkModeItem.setSelected(UserPreferences.getInstance().isDarkMode());
                applyThemeStyles(stage.getScene());
                tabPane.redrawAllTabs();
            }
        });

        viewMenu.getItems().addAll(
                zoomInItem,
                zoomOutItem,
                zoomResetItem,
                new SeparatorMenuItem(),
                showGridItem,
                darkModeItem,
                new SeparatorMenuItem(),
                diagramSizeItem,
                fontItem,
                prefsItem
        );

        // ---------------- MENU AJUDA ----------------
        Menu helpMenu = new Menu("A_juda");
        MenuItem tipItem = new MenuItem("Dica do Dia...");
        tipItem.setOnAction(e -> TipsAndShortcutsDialog.showTipOfTheDay());

        MenuItem shortcutsItem = new MenuItem("Atalhos de Teclado e Ajuda...");
        shortcutsItem.setOnAction(e -> TipsAndShortcutsDialog.showKeyboardShortcutsGuide());

        helpMenu.getItems().addAll(tipItem, shortcutsItem);

        menuBar.getMenus().addAll(fileMenu, editMenu, viewMenu, helpMenu);
        return menuBar;
    }

    private ToolBar buildTopQuickBar() {
        Button newClassBtn = new Button("+ Classe");
        newClassBtn.setOnAction(e -> createNewDiagram(DiagramType.CLASS));

        Button newSeqBtn = new Button("+ Sequencia");
        newSeqBtn.setOnAction(e -> createNewDiagram(DiagramType.SEQUENCE));

        Button newStateBtn = new Button("+ Estados");
        newStateBtn.setOnAction(e -> createNewDiagram(DiagramType.STATE));

        Button newObjBtn = new Button("+ Objetos");
        newObjBtn.setOnAction(e -> createNewDiagram(DiagramType.OBJECT));

        Button newUcBtn = new Button("+ Casos de Uso");
        newUcBtn.setOnAction(e -> createNewDiagram(DiagramType.USE_CASE));

        Button saveBtn = new Button("Salvar");
        saveBtn.setOnAction(e -> tabPane.getActiveContext().ifPresent(ctx -> saveDiagram(ctx.diagram)));

        Button undoBtn = new Button("Desfazer");
        undoBtn.setOnAction(e -> tabPane.getActiveContext().ifPresent(ctx -> ctx.commandManager.undo()));

        Button redoBtn = new Button("Refazer");
        redoBtn.setOnAction(e -> tabPane.getActiveContext().ifPresent(ctx -> ctx.commandManager.redo()));

        Button zoomOutBtn = new Button("Zoom -");
        zoomOutBtn.setOnAction(e -> adjustZoom(-0.10));

        Button zoomInBtn = new Button("Zoom +");
        zoomInBtn.setOnAction(e -> adjustZoom(0.10));

        return new ToolBar(
                newClassBtn, newSeqBtn, newStateBtn, newObjBtn, newUcBtn,
                new Separator(Orientation.VERTICAL),
                saveBtn, undoBtn, redoBtn,
                new Separator(Orientation.VERTICAL),
                zoomOutBtn, zoomInBtn
        );
    }

    private HBox buildStatusBar() {
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox bar = new HBox(16, statusDiagramLabel, spacer, statusZoomLabel);
        bar.setPadding(new Insets(5, 12, 5, 12));
        bar.getStyleClass().add("status-bar");
        return bar;
    }

    private void rebuildSideToolbarFor(DiagramType type) {
        sideToolBar.getChildren().clear();
        currentToolButtons.clear();
        toolToggleGroup.getToggles().clear();

        Label header = new Label(type != null ? type.getDisplayName() : "Ferramentas");
        header.setStyle("-fx-font-weight: bold; -fx-font-size: 12.5px;");
        sideToolBar.getChildren().addAll(header, new Separator());

        if (type == null) {
            Label hint = new Label("Crie ou abra um diagrama para exibir as ferramentas.");
            hint.setWrapText(true);
            hint.setStyle("-fx-font-size: 11.5px; -fx-opacity: 0.7;");
            sideToolBar.getChildren().add(hint);
            return;
        }

        List<ToolType> tools = type.getAvailableTools();
        for (int i = 0; i < tools.size(); i++) {
            ToolType tool = tools.get(i);
            int shortcutNum = i + 1;
            String labelText = (shortcutNum <= 9)
                    ? "[" + shortcutNum + "] " + tool.getDisplayName()
                    : tool.getDisplayName();

            ToggleButton btn = new ToggleButton(labelText);
            btn.setMaxWidth(Double.MAX_VALUE);
            btn.setStyle("-fx-alignment: CENTER_LEFT; -fx-padding: 6 10 6 10;");
            btn.setToggleGroup(toolToggleGroup);
            btn.setUserData(tool);
            btn.setTooltip(new Tooltip(tool.getDisplayName() + (shortcutNum <= 9 ? " (Tecla " + shortcutNum + ")" : "")));

            btn.setOnAction(e -> {
                if (btn.isSelected()) {
                    activeTool = tool;
                } else {
                    selectDefaultTool();
                }
            });

            if (tool == ToolType.SELECT) {
                btn.setSelected(true);
                activeTool = ToolType.SELECT;
            }

            currentToolButtons.add(btn);
            sideToolBar.getChildren().add(btn);
        }
    }

    private void selectTool(ToolType tool) {
        if (tool == null) {
            selectDefaultTool();
            return;
        }
        this.activeTool = tool;
        for (ToggleButton btn : currentToolButtons) {
            if (btn.getUserData() == tool) {
                btn.setSelected(true);
                break;
            }
        }
    }

    private void selectDefaultTool() {
        selectTool(ToolType.SELECT);
    }

    private void registerGlobalKeyShortcuts(Scene scene) {
        scene.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            // Ignorar atalhos de caractere unico se algum campo de texto tiver foco
            if (scene.getFocusOwner() instanceof javafx.scene.control.TextInputControl) {
                return;
            }

            // Atalhos numericos 1..9 para selecionar ferramentas da barra lateral (RF10)
            if (!event.isControlDown() && !event.isAltDown() && !event.isMetaDown()) {
                int idx = mapDigitKeyToIndex(event.getCode());
                if (idx >= 0 && idx < currentToolButtons.size()) {
                    ToggleButton btn = currentToolButtons.get(idx);
                    btn.setSelected(true);
                    activeTool = (ToolType) btn.getUserData();
                    event.consume();
                    return;
                }
                if (event.getCode() == KeyCode.ESCAPE) {
                    selectDefaultTool();
                    tabPane.getActiveContext().ifPresent(ctx -> ctx.diagram.clearSelection());
                    event.consume();
                    return;
                }
                if (event.getCode() == KeyCode.BACK_SPACE || event.getCode() == KeyCode.DELETE) {
                    deleteSelectedElements();
                    event.consume();
                    return;
                }
            }

            // Atalhos Ctrl+1 .. Ctrl+5 para abrir arquivos recentes (RF06)
            if (event.isControlDown() && !event.isShiftDown() && !event.isAltDown()) {
                int recentIdx = mapDigitKeyToIndex(event.getCode());
                if (recentIdx >= 0 && recentIdx < 5) {
                    List<Path> recents = UserPreferences.getInstance().getRecentFiles();
                    if (recentIdx < recents.size()) {
                        openDiagramFile(recents.get(recentIdx));
                        event.consume();
                    }
                }
            }
        });
    }

    private int mapDigitKeyToIndex(KeyCode code) {
        return switch (code) {
            case DIGIT1, NUMPAD1 -> 0;
            case DIGIT2, NUMPAD2 -> 1;
            case DIGIT3, NUMPAD3 -> 2;
            case DIGIT4, NUMPAD4 -> 3;
            case DIGIT5, NUMPAD5 -> 4;
            case DIGIT6, NUMPAD6 -> 5;
            case DIGIT7, NUMPAD7 -> 6;
            case DIGIT8, NUMPAD8 -> 7;
            case DIGIT9, NUMPAD9 -> 8;
            default -> -1;
        };
    }

    private void onTabChanged(DiagramTabContext ctx) {
        if (ctx == null) {
            rebuildSideToolbarFor(null);
            statusDiagramLabel.setText("Nenhum diagrama aberto");
            statusZoomLabel.setText("Zoom: 100%");
            return;
        }
        rebuildSideToolbarFor(ctx.diagram.getType());
        updateStatusBar(ctx);
    }

    private void updateStatusBar(DiagramTabContext ctx) {
        statusDiagramLabel.setText(
                ctx.diagram.getType().getDisplayName()
                        + " | Dimensao: " + ctx.diagram.getWidth() + " x " + ctx.diagram.getHeight() + " px"
        );
        statusZoomLabel.setText(String.format("Zoom: %.0f%%", ctx.getZoomFactor() * 100));
    }

    public void createNewDiagram(DiagramType type) {
        Diagram diagram = new Diagram(type);
        DiagramTabContext ctx = tabPane.openDiagram(diagram);
        rebuildSideToolbarFor(type);
        updateStatusBar(ctx);
    }

    private void openDiagramFromDialog() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Abrir Diagrama JetUML");
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Diagramas JetUML (*.json, *.jet)", "*.json", "*.jet"),
                new FileChooser.ExtensionFilter("Todos os Arquivos", "*.*")
        );
        File selected = chooser.showOpenDialog(stage);
        if (selected != null) {
            openDiagramFile(selected.toPath());
        }
    }

    private void openDiagramFile(Path path) {
        try {
            Diagram diagram = DiagramJsonSerializer.loadFromFile(path);
            DiagramTabContext ctx = tabPane.openDiagram(diagram);
            UserPreferences.getInstance().recordRecentFile(path);
            refreshRecentFilesMenu();
            updateStatusBar(ctx);
            notificationOverlay.showConfirmation("Diagrama carregado: " + path.getFileName());
        } catch (IOException ex) {
            notificationOverlay.showError("Erro ao abrir arquivo: " + ex.getMessage());
        }
    }

    private void saveDiagram(Diagram diagram) {
        if (diagram == null) return;
        if (diagram.getFilePath().isEmpty()) {
            saveDiagramAs(diagram);
            return;
        }
        try {
            Path path = diagram.getFilePath().get();
            DiagramJsonSerializer.saveToFile(diagram, path);
            UserPreferences.getInstance().recordRecentFile(path);
            refreshRecentFilesMenu();
            notificationOverlay.showConfirmation("Diagrama salvo com sucesso: " + path.getFileName());
        } catch (IOException ex) {
            notificationOverlay.showError("Falha ao salvar diagrama: " + ex.getMessage());
        }
    }

    private void saveDiagramAs(Diagram diagram) {
        if (diagram == null) return;
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Salvar Diagrama Como");
        chooser.setInitialFileName(diagram.getType().name().toLowerCase() + "_diagram.json");
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Arquivo JetUML JSON (*.json)", "*.json"),
                new FileChooser.ExtensionFilter("Arquivo JetUML (*.jet)", "*.jet")
        );
        File target = chooser.showSaveDialog(stage);
        if (target != null) {
            try {
                Path path = target.toPath();
                DiagramJsonSerializer.saveToFile(diagram, path);
                UserPreferences.getInstance().recordRecentFile(path);
                refreshRecentFilesMenu();
                notificationOverlay.showConfirmation("Diagrama salvo em: " + path.getFileName());
            } catch (IOException ex) {
                notificationOverlay.showError("Falha ao salvar arquivo: " + ex.getMessage());
            }
        }
    }

    private void exportDiagramImage() {
        Optional<DiagramTabContext> ctxOpt = tabPane.getActiveContext();
        if (ctxOpt.isEmpty()) return;

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Exportar Diagrama como Imagem");
        chooser.setInitialFileName("diagrama.svg");
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Imagem Vetorial SVG (*.svg)", "*.svg"),
                new FileChooser.ExtensionFilter("Imagem PNG (*.png)", "*.png"),
                new FileChooser.ExtensionFilter("Imagem JPEG (*.jpg, *.jpeg)", "*.jpg", "*.jpeg"),
                new FileChooser.ExtensionFilter("Imagem GIF (*.gif)", "*.gif"),
                new FileChooser.ExtensionFilter("Imagem BMP (*.bmp)", "*.bmp")
        );

        File file = chooser.showSaveDialog(stage);
        if (file != null) {
            try {
                DiagramImageExporter.exportDiagramToFile(ctxOpt.get().diagram, file.toPath());
                notificationOverlay.showConfirmation("Imagem exportada: " + file.getName());
            } catch (IOException ex) {
                notificationOverlay.showError("Erro ao exportar imagem: " + ex.getMessage());
            }
        }
    }

    private void copyDiagramImageToClipboard() {
        Optional<DiagramTabContext> ctxOpt = tabPane.getActiveContext();
        if (ctxOpt.isEmpty()) return;
        DiagramImageExporter.copyDiagramToSystemClipboard(ctxOpt.get().diagram);
        notificationOverlay.showConfirmation("Imagem do diagrama copiada para a area de transferencia.");
    }

    private void refreshRecentFilesMenu() {
        recentFilesMenu.getItems().clear();
        List<Path> recents = UserPreferences.getInstance().getRecentFiles();
        if (recents.isEmpty()) {
            MenuItem empty = new MenuItem("(Nenhum arquivo recente)");
            empty.setDisable(true);
            recentFilesMenu.getItems().add(empty);
        } else {
            for (int i = 0; i < recents.size(); i++) {
                Path p = recents.get(i);
                MenuItem item = new MenuItem((i + 1) + ". " + p.getFileName() + "  [" + p + "]");
                item.setOnAction(e -> openDiagramFile(p));
                recentFilesMenu.getItems().add(item);
            }
        }
        if (welcomePane != null) {
            welcomePane.refreshRecentFiles();
        }
    }

    private void performCopy() {
        tabPane.getActiveContext().ifPresent(ctx -> {
            if (!ctx.diagram.getSelectedElements().isEmpty()) {
                DiagramClipboard.getInstance().copy(ctx.diagram);
                notificationOverlay.showConfirmation("Elementos copiados.");
            }
        });
    }

    private void performCut() {
        tabPane.getActiveContext().ifPresent(ctx -> {
            if (!ctx.diagram.getSelectedElements().isEmpty()) {
                ctx.commandManager.execute(DiagramClipboard.getInstance().createCutCommand(ctx.diagram));
                notificationOverlay.showConfirmation("Elementos recortados.");
            }
        });
    }

    private void performPaste() {
        tabPane.getActiveContext().ifPresent(ctx -> {
            DiagramClipboard clip = DiagramClipboard.getInstance();
            if (clip.isEmpty()) return;
            ValidationResult res = clip.validatePaste(ctx.diagram);
            if (!res.isValid()) {
                notificationOverlay.showError(res.getErrorMessage());
                return;
            }
            ctx.commandManager.execute(clip.createPasteCommand(ctx.diagram));
        });
    }

    private void deleteSelectedElements() {
        tabPane.getActiveContext().ifPresent(ctx -> {
            if (!ctx.diagram.getSelectedElements().isEmpty()) {
                ctx.commandManager.execute(new DeleteElementsCommand(ctx.diagram, ctx.diagram.getSelectedElements()));
            }
        });
    }

    private void editSelectedElementProperties() {
        tabPane.getActiveContext().ifPresent(ctx -> {
            if (ctx.diagram.getSelectedElements().size() == 1) {
                DiagramElement el = ctx.diagram.getSelectedElements().iterator().next();
                PropertyEditorDialog.showAndBuildCommand(ctx.diagram, el)
                        .ifPresent(ctx.commandManager::execute);
            } else {
                notificationOverlay.showError("Selecione exatamente um elemento para editar suas propriedades.");
            }
        });
    }

    private void adjustZoom(double delta) {
        tabPane.getActiveContext().ifPresent(ctx -> {
            ctx.setZoomFactor(ctx.getZoomFactor() + delta);
            updateStatusBar(ctx);
        });
    }

    private void setZoom(double exactFactor) {
        tabPane.getActiveContext().ifPresent(ctx -> {
            ctx.setZoomFactor(exactFactor);
            updateStatusBar(ctx);
        });
    }

    private void applyThemeStyles(Scene scene) {
        if (scene == null) return;
        boolean dark = UserPreferences.getInstance().isDarkMode();
        if (dark) {
            rootPane.setStyle("-fx-base: #262A33; -fx-background: #1E2025; -fx-control-inner-background: #2C303A;");
            sideToolBar.setStyle("-fx-background-color: #252932; -fx-border-color: #383E4A; -fx-border-width: 0 1 0 0;");
        } else {
            rootPane.setStyle("");
            sideToolBar.setStyle("-fx-background-color: #F4F6F9; -fx-border-color: #D8DEE6; -fx-border-width: 0 1 0 0;");
        }
    }
}