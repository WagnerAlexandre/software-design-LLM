package org.jetuml.ui;

import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.text.Font;
import org.jetuml.config.UserPreferences;
import org.jetuml.model.Diagram;

/**
 * Dialogos de configuracao para Dimensao do Diagrama (RF37), Fonte (RF39),
 * Modo Escuro (RF40) e Duracao de Notificacoes (RF42).
 */
public final class PreferencesDialog {

    private PreferencesDialog() {
    }

    public static boolean showDiagramSizeDialog(Diagram diagram) {
        if (diagram == null) return false;

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Dimensao do Diagrama");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        Spinner<Integer> widthSpinner = new Spinner<>(400, 6000, diagram.getWidth(), 50);
        widthSpinner.setEditable(true);
        Spinner<Integer> heightSpinner = new Spinner<>(300, 6000, diagram.getHeight(), 50);
        heightSpinner.setEditable(true);

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(10);
        grid.setPadding(new Insets(16));
        grid.addRow(0, new Label("Largura (px):"), widthSpinner);
        grid.addRow(1, new Label("Altura (px):"), heightSpinner);
        dialog.getDialogPane().setContent(grid);

        if (dialog.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            diagram.setCanvasSize(widthSpinner.getValue(), heightSpinner.getValue());
            UserPreferences.getInstance().setDefaultDiagramWidth(widthSpinner.getValue());
            UserPreferences.getInstance().setDefaultDiagramHeight(heightSpinner.getValue());
            diagram.setDirty(true);
            diagram.notifyListeners();
            return true;
        }
        return false;
    }

    public static boolean showFontDialog() {
        UserPreferences prefs = UserPreferences.getInstance();
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Configurar Fonte do Diagrama");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        ComboBox<String> familyBox = new ComboBox<>();
        familyBox.getItems().addAll(Font.getFamilies());
        familyBox.setValue(prefs.getFontFamily());

        Spinner<Integer> sizeSpinner = new Spinner<>(8, 36, (int) Math.round(prefs.getFontSize()), 1);
        sizeSpinner.setEditable(true);

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(10);
        grid.setPadding(new Insets(16));
        grid.addRow(0, new Label("Familia da Fonte:"), familyBox);
        grid.addRow(1, new Label("Tamanho (pt):"), sizeSpinner);
        dialog.getDialogPane().setContent(grid);

        if (dialog.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            prefs.setFontFamily(familyBox.getValue());
            prefs.setFontSize(sizeSpinner.getValue());
            return true;
        }
        return false;
    }

    public static boolean showGeneralPreferencesDialog() {
        UserPreferences prefs = UserPreferences.getInstance();
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Preferencias do JetUML");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        CheckBox showGridBox = new CheckBox("Exibir grade na area de desenho");
        showGridBox.setSelected(prefs.isShowGrid());

        CheckBox darkModeBox = new CheckBox("Ativar Modo Escuro");
        darkModeBox.setSelected(prefs.isDarkMode());

        Spinner<Integer> durationSpinner = new Spinner<>(0, 10, prefs.getNotificationDurationSeconds(), 1);
        durationSpinner.setEditable(true);

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(10);
        grid.setPadding(new Insets(16));
        grid.addRow(0, showGridBox);
        grid.addRow(1, darkModeBox);
        grid.addRow(2, new Label("Duracao das notificacoes (0 a 10s):"), durationSpinner);
        dialog.getDialogPane().setContent(grid);

        if (dialog.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            prefs.setShowGrid(showGridBox.isSelected());
            prefs.setDarkMode(darkModeBox.isSelected());
            prefs.setNotificationDurationSeconds(durationSpinner.getValue());
            return true;
        }
        return false;
    }
}
