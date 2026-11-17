package org.jetuml.config;

import javafx.geometry.Rectangle2D;
import javafx.stage.Screen;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.prefs.Preferences;

/**
 * Armazena e persiste preferencias globais do usuario e parametros de visualizacao
 * exigidos pelos requisitos RF02, RF10, RF12, RF36, RF38, RF39, RF40, RF42 e RNF01.
 */
public final class UserPreferences {
    public static final int MIN_DIAGRAM_SIZE = 250;
    public static final int MAX_DIAGRAM_SIZE = 4000;
    public static final int MIN_NOTIFICATION_SECONDS = 0;
    public static final int MAX_NOTIFICATION_SECONDS = 10;
    public static final int DEFAULT_NOTIFICATION_SECONDS = 5;
    public static final String DEFAULT_FONT_FAMILY = "System";
    public static final int DEFAULT_FONT_SIZE = 12;
    private static final int MAX_RECENT_FILES = 10;

    private static final UserPreferences INSTANCE = new UserPreferences();
    private final Preferences prefs;
    private final List<Path> recentFiles;
    private final List<Runnable> listeners = new ArrayList<>();
    private boolean showGrid;
    private boolean autoEditNode;
    private boolean showToolHint;
    private boolean verboseTooltips;
    private boolean darkMode;
    private int diagramWidth;
    private int diagramHeight;
    private String fontFamily;
    private double fontSize;
    private int notificationDurationSeconds;
    private int defaultDiagramWidth;
    private int defaultDiagramHeight;


    private UserPreferences() {
        this.prefs = Preferences.userNodeForPackage(UserPreferences.class);
        this.showGrid = prefs.getBoolean("showGrid", true);
        this.autoEditNode = prefs.getBoolean("autoEditNode", false);
        this.showToolHint = prefs.getBoolean("showToolHint", false);
        this.verboseTooltips = prefs.getBoolean("verboseTooltips", false);
        this.darkMode = prefs.getBoolean("darkMode", false);

        int[] defaultDims = computeScreenProportionalSize();
        this.diagramWidth = clampDiagramSize(prefs.getInt("diagramWidth", defaultDims[0]));
        this.diagramHeight = clampDiagramSize(prefs.getInt("diagramHeight", defaultDims[1]));

        this.fontFamily = prefs.get("fontFamily", DEFAULT_FONT_FAMILY);
        this.fontSize = prefs.getInt("fontSize", DEFAULT_FONT_SIZE);
        this.notificationDurationSeconds = clampNotificationDuration(
                prefs.getInt("notificationDuration", DEFAULT_NOTIFICATION_SECONDS)
        );
        this.recentFiles = loadRecentFiles();
    }

    public static UserPreferences getInstance() {
        return INSTANCE;
    }

    /**
     * Calcula dimensoes padrao proporcionais a resolucao da tela principal (RF38, RNF01).
     */
    public static int[] computeScreenProportionalSize() {
        try {
            Rectangle2D bounds = Screen.getPrimary().getVisualBounds();
            int w = clampDiagramSize((int) Math.round(bounds.getWidth() * 0.72));
            int h = clampDiagramSize((int) Math.round(bounds.getHeight() * 0.72));
            return new int[]{w, h};
        } catch (Exception ignored) {
            return new int[]{1100, 750};
        }
    }

    public static int clampDiagramSize(int value) {
        return Math.max(MIN_DIAGRAM_SIZE, Math.min(MAX_DIAGRAM_SIZE, value));
    }

    public static int clampNotificationDuration(int seconds) {
        return Math.max(MIN_NOTIFICATION_SECONDS, Math.min(MAX_NOTIFICATION_SECONDS, seconds));
    }

    public void recordRecentFile(java.nio.file.Path path) {
        addRecentFile(path); // ou se o metodo se chamar addRecentFile, delega diretamente para ele
    }

    public void setDefaultDiagramWidth(int width) {
        this.defaultDiagramWidth = Math.max(400, width);
    }

    public void setDefaultDiagramHeight(int height) {
        this.defaultDiagramHeight = Math.max(300, height);
    }

    public void addListener(Runnable listener) {
        listeners.add(listener);
    }

    public void removeListener(Runnable listener) {
        listeners.remove(listener);
    }

    private void notifyListeners() {
        for (Runnable listener : List.copyOf(listeners)) {
            listener.run();
        }
    }

    public boolean isShowGrid() {
        return showGrid;
    }

    public void setShowGrid(boolean showGrid) {
        this.showGrid = showGrid;
        prefs.putBoolean("showGrid", showGrid);
        notifyListeners();
    }

    public boolean isAutoEditNode() {
        return autoEditNode;
    }

    public void setAutoEditNode(boolean autoEditNode) {
        this.autoEditNode = autoEditNode;
        prefs.putBoolean("autoEditNode", autoEditNode);
        notifyListeners();
    }

    public boolean isShowToolHint() {
        return showToolHint;
    }

    public void setShowToolHint(boolean showToolHint) {
        this.showToolHint = showToolHint;
        prefs.putBoolean("showToolHint", showToolHint);
        notifyListeners();
    }

    public boolean isVerboseTooltips() {
        return verboseTooltips;
    }

    public void setVerboseTooltips(boolean verboseTooltips) {
        this.verboseTooltips = verboseTooltips;
        prefs.putBoolean("verboseTooltips", verboseTooltips);
        notifyListeners();
    }

    public boolean isDarkMode() {
        return darkMode;
    }

    public void setDarkMode(boolean darkMode) {
        this.darkMode = darkMode;
        prefs.putBoolean("darkMode", darkMode);
        notifyListeners();
    }

    public int getDiagramWidth() {
        return diagramWidth;
    }

    public int getDiagramHeight() {
        return diagramHeight;
    }

    public void setDiagramSize(int width, int height) {
        this.diagramWidth = clampDiagramSize(width);
        this.diagramHeight = clampDiagramSize(height);
        prefs.putInt("diagramWidth", this.diagramWidth);
        prefs.putInt("diagramHeight", this.diagramHeight);
        notifyListeners();
    }

    public void restoreDefaultDiagramSize() {
        int[] defaults = computeScreenProportionalSize();
        setDiagramSize(defaults[0], defaults[1]);
    }

    public String getFontFamily() {
        return fontFamily;
    }

    public void setFontFamily(String fontFamily) {
        if (fontFamily != null && !fontFamily.isBlank()) {
            this.fontFamily = fontFamily;
        }
    }

    public double getFontSize() {
        return fontSize;
    }

    public void setFontSize(double fontSize) {
        this.fontSize = Math.max(8.0, Math.min(36.0, fontSize));
    }

    public void setFont(String fontFamily, int fontSize) {
        this.fontFamily = (fontFamily == null || fontFamily.isBlank()) ? DEFAULT_FONT_FAMILY : fontFamily;
        this.fontSize = Math.max(8, Math.min(36, fontSize));
        prefs.put("fontFamily", this.fontFamily);
        prefs.putInt("fontSize", (int) this.fontSize);
        notifyListeners();
    }

    public void restoreDefaultFont() {
        setFont(DEFAULT_FONT_FAMILY, DEFAULT_FONT_SIZE);
    }

    public int getNotificationDurationSeconds() {
        return notificationDurationSeconds;
    }

    public void setNotificationDurationSeconds(int seconds) {
        this.notificationDurationSeconds = clampNotificationDuration(seconds);
        prefs.putInt("notificationDuration", this.notificationDurationSeconds);
        notifyListeners();
    }

    public void restoreDefaultNotificationDuration() {
        setNotificationDurationSeconds(DEFAULT_NOTIFICATION_SECONDS);
    }

    public List<Path> getRecentFiles() {
        return Collections.unmodifiableList(recentFiles);
    }

    public void addRecentFile(Path path) {
        if (path == null) return;
        Path normalized = path.toAbsolutePath().normalize();
        recentFiles.remove(normalized);
        recentFiles.add(0, normalized);
        while (recentFiles.size() > MAX_RECENT_FILES) {
            recentFiles.remove(recentFiles.size() - 1);
        }
        saveRecentFiles();
        notifyListeners();
    }

    public void removeRecentFile(Path path) {
        if (path == null) return;
        recentFiles.remove(path.toAbsolutePath().normalize());
        saveRecentFiles();
        notifyListeners();
    }

    private List<Path> loadRecentFiles() {
        List<Path> list = new ArrayList<>();
        int count = prefs.getInt("recentCount", 0);
        for (int i = 0; i < count; i++) {
            String raw = prefs.get("recent_" + i, null);
            if (raw != null && !raw.isBlank()) {
                list.add(Paths.get(raw));
            }
        }
        return list;
    }

    private void saveRecentFiles() {
        prefs.putInt("recentCount", recentFiles.size());
        for (int i = 0; i < recentFiles.size(); i++) {
            prefs.put("recent_" + i, recentFiles.get(i).toString());
        }
    }
}
