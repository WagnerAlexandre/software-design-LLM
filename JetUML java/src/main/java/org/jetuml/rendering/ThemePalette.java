package org.jetuml.rendering;

import javafx.scene.paint.Color;

/**
 * Paleta de cores para os modos Claro e Escuro do diagrama e da interface (RF40).
 */
public final class ThemePalette {
    public final Color canvasBackground;
    public final Color gridLine;
    public final Color nodeFill;
    public final Color nodeSecondaryFill;
    public final Color nodeStroke;
    public final Color textPrimary;
    public final Color textSecondary;
    public final Color edgeStroke;
    public final Color selectionHighlight;
    public final Color rubberBandStroke;
    public final Color lassoFill;
    public final Color lassoStroke;

    private ThemePalette(
            Color canvasBackground,
            Color gridLine,
            Color nodeFill,
            Color nodeSecondaryFill,
            Color nodeStroke,
            Color textPrimary,
            Color textSecondary,
            Color edgeStroke,
            Color selectionHighlight,
            Color rubberBandStroke,
            Color lassoFill,
            Color lassoStroke
    ) {
        this.canvasBackground = canvasBackground;
        this.gridLine = gridLine;
        this.nodeFill = nodeFill;
        this.nodeSecondaryFill = nodeSecondaryFill;
        this.nodeStroke = nodeStroke;
        this.textPrimary = textPrimary;
        this.textSecondary = textSecondary;
        this.edgeStroke = edgeStroke;
        this.selectionHighlight = selectionHighlight;
        this.rubberBandStroke = rubberBandStroke;
        this.lassoFill = lassoFill;
        this.lassoStroke = lassoStroke;
    }

    public static ThemePalette forDarkMode(boolean darkMode) {
        if (darkMode) {
            return new ThemePalette(
                    Color.web("#1E2025"),
                    Color.web("#2C3038"),
                    Color.web("#2A2E37"),
                    Color.web("#333842"),
                    Color.web("#AEB7C4"),
                    Color.web("#ECEFF4"),
                    Color.web("#B8C0CC"),
                    Color.web("#CBD2DC"),
                    Color.web("#4EA1FF"),
                    Color.web("#66B2FF"),
                    Color.color(0.31, 0.63, 1.0, 0.16),
                    Color.web("#4EA1FF")
            );
        }
        return new ThemePalette(
                Color.WHITE,
                Color.web("#EAECEF"),
                Color.WHITE,
                Color.web("#F7F8FA"),
                Color.web("#22252A"),
                Color.web("#1A1D21"),
                Color.web("#4B525C"),
                Color.web("#22252A"),
                Color.web("#1976D2"),
                Color.web("#1976D2"),
                Color.color(0.10, 0.46, 0.82, 0.12),
                Color.web("#1976D2")
        );
    }
}
