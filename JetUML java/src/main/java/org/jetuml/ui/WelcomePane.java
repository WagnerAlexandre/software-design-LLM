package org.jetuml.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.jetuml.config.UserPreferences;
import org.jetuml.model.DiagramType;

import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;

/**
 * Tela de Boas-Vindas exibida na inicializacao e sempre que nao houver abas abertas (RF01, RF03, RF06).
 * Permite criar qualquer um dos 5 tipos de diagrama UML ou abrir arquivos existentes/recentes.
 */
public class WelcomePane extends VBox {

    private final Consumer<DiagramType> onCreateDiagram;
    private final Runnable onOpenDiagramDialog;
    private final Consumer<Path> onOpenRecentFile;
    private final VBox recentFilesBox = new VBox(6);

    public WelcomePane(
            Consumer<DiagramType> onCreateDiagram,
            Runnable onOpenDiagramDialog,
            Consumer<Path> onOpenRecentFile
    ) {
        this.onCreateDiagram = onCreateDiagram;
        this.onOpenDiagramDialog = onOpenDiagramDialog;
        this.onOpenRecentFile = onOpenRecentFile;

        setAlignment(Pos.CENTER);
        setSpacing(20);
        setPadding(new Insets(36));
        setStyle("-fx-background-color: transparent;");

        Label titleLabel = new Label("JetUML");
        titleLabel.setStyle("-fx-font-size: 32px; -fx-font-weight: bold;");

        Label subtitleLabel = new Label("Selecione o tipo de diagrama UML para iniciar ou abra um arquivo existente:");
        subtitleLabel.setStyle("-fx-font-size: 14px; -fx-opacity: 0.85;");

        FlowPane diagramCardsPane = new FlowPane();
        diagramCardsPane.setHgap(14);
        diagramCardsPane.setVgap(14);
        diagramCardsPane.setAlignment(Pos.CENTER);
        diagramCardsPane.setPrefWrapLength(780);

        diagramCardsPane.getChildren().addAll(
                buildDiagramCard(DiagramType.CLASS, "Class Diagram", "Classes, interfaces, pacotes e relacionamentos estruturais"),
                buildDiagramCard(DiagramType.SEQUENCE, "Sequence Diagram", "Participantes, linhas de vida, chamadas e retornos"),
                buildDiagramCard(DiagramType.STATE, "State Diagram", "Estados inicial, intermediarios, final e transicoes"),
                buildDiagramCard(DiagramType.OBJECT, "Object Diagram", "Instancias de objetos, campos e referencias"),
                buildDiagramCard(DiagramType.USE_CASE, "Use Case Diagram", "Atores, casos de uso, associacoes, <<include>> e <<extend>>")
        );

        Button openFileBtn = new Button("Abrir Diagrama Existente (.json / .jet)...");
        openFileBtn.setStyle("-fx-font-size: 13px; -fx-padding: 8 18 8 18; -fx-cursor: hand;");
        openFileBtn.setOnAction(e -> {
            if (onOpenDiagramDialog != null) {
                onOpenDiagramDialog.run();
            }
        });

        HBox actionsRow = new HBox(12, openFileBtn);
        actionsRow.setAlignment(Pos.CENTER);

        recentFilesBox.setAlignment(Pos.CENTER);
        recentFilesBox.setMaxWidth(520);

        getChildren().addAll(
                titleLabel,
                subtitleLabel,
                diagramCardsPane,
                actionsRow,
                new Separator(),
                recentFilesBox
        );

        refreshRecentFiles();
    }

    private Button buildDiagramCard(DiagramType type, String englishTitle, String description) {
        Label header = new Label(englishTitle);
        header.setStyle("-fx-font-size: 15px; -fx-font-weight: bold;");

        Label ptName = new Label(type.getDisplayName());
        ptName.setStyle("-fx-font-size: 12px; -fx-opacity: 0.75;");

        Label desc = new Label(description);
        desc.setWrapText(true);
        desc.setMaxWidth(210);
        desc.setStyle("-fx-font-size: 11.5px; -fx-opacity: 0.85;");

        VBox content = new VBox(5, header, ptName, desc);
        content.setAlignment(Pos.TOP_LEFT);
        content.setPadding(new Insets(12));

        Button cardButton = new Button();
        cardButton.setGraphic(content);
        cardButton.setPrefSize(240, 118);
        cardButton.setStyle(
                "-fx-alignment: TOP_LEFT; " +
                        "-fx-background-radius: 8; " +
                        "-fx-border-radius: 8; " +
                        "-fx-border-color: rgba(128, 128, 128, 0.35); " +
                        "-fx-cursor: hand;"
        );
        cardButton.setOnAction(e -> {
            if (onCreateDiagram != null) {
                onCreateDiagram.accept(type);
            }
        });

        return cardButton;
    }

    public void refreshRecentFiles() {
        recentFilesBox.getChildren().clear();
        List<Path> recents = UserPreferences.getInstance().getRecentFiles();
        if (recents.isEmpty()) {
            return;
        }

        Label recentHeader = new Label("Arquivos Recentes:");
        recentHeader.setStyle("-fx-font-weight: bold; -fx-font-size: 12.5px;");
        recentFilesBox.getChildren().add(recentHeader);

        for (Path path : recents) {
            Hyperlink link = new Hyperlink(path.getFileName() + "  (" + path + ")");
            link.setOnAction(e -> {
                if (onOpenRecentFile != null) {
                    onOpenRecentFile.accept(path);
                }
            });
            recentFilesBox.getChildren().add(link);
        }
    }
}