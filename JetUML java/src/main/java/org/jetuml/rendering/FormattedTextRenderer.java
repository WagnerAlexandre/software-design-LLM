package org.jetuml.rendering;

import javafx.geometry.VPos;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Renderiza linhas de texto UML com suporte as marcacoes definidas no RF24:
 * - entre barras (/texto/): exibido em italico (abstrato);
 * - entre sublinhados (_texto_): exibido sublinhado (estatico).
 */
public final class FormattedTextRenderer {

    private FormattedTextRenderer() {
    }

    public static void drawFormattedLine(
            GraphicsContext gc,
            String rawLine,
            double x,
            double y,
            double maxWidth,
            String fontFamily,
            double fontSize,
            boolean boldBase,
            TextAlignment alignment,
            Color color
    ) {
        if (rawLine == null || rawLine.isEmpty()) return;

        List<TextSegment> segments = parseSegments(rawLine);
        FontWeight weight = boldBase ? FontWeight.BOLD : FontWeight.NORMAL;

        double totalWidth = 0;
        double[] segWidths = new double[segments.size()];
        Font[] segFonts = new Font[segments.size()];

        for (int i = 0; i < segments.size(); i++) {
            TextSegment seg = segments.get(i);
            FontPosture posture = seg.italic() ? FontPosture.ITALIC : FontPosture.REGULAR;
            Font font = Font.font(fontFamily, weight, posture, fontSize);
            segFonts[i] = font;
            segWidths[i] = measureTextWidth(seg.content(), font);
            totalWidth += segWidths[i];
        }

        double drawX = switch (alignment) {
            case CENTER -> x + (maxWidth - totalWidth) / 2.0;
            case RIGHT -> x + (maxWidth - totalWidth);
            default -> x;
        };

        gc.save();
        gc.setFill(color);
        gc.setStroke(color);
        gc.setLineWidth(1.0);
        gc.setTextBaseline(VPos.CENTER);

        double cursorX = drawX;
        for (int i = 0; i < segments.size(); i++) {
            TextSegment seg = segments.get(i);
            gc.setFont(segFonts[i]);
            gc.fillText(seg.content(), cursorX, y);
            if (seg.underline()) {
                double underlineY = y + fontSize * 0.42;
                gc.strokeLine(cursorX, underlineY, cursorX + segWidths[i], underlineY);
            }
            cursorX += segWidths[i];
        }
        gc.restore();
    }

    public static double measureFormattedWidth(String rawLine, String fontFamily, double fontSize, boolean boldBase) {
        if (rawLine == null || rawLine.isEmpty()) return 0;
        List<TextSegment> segments = parseSegments(rawLine);
        FontWeight weight = boldBase ? FontWeight.BOLD : FontWeight.NORMAL;
        double total = 0;
        for (TextSegment seg : segments) {
            FontPosture posture = seg.italic() ? FontPosture.ITALIC : FontPosture.REGULAR;
            total += measureTextWidth(seg.content(), Font.font(fontFamily, weight, posture, fontSize));
        }
        return total;
    }

    private static double measureTextWidth(String text, Font font) {
        Text helper = new Text(text);
        helper.setFont(font);
        return helper.getLayoutBounds().getWidth();
    }

    private static List<TextSegment> parseSegments(String line) {
        List<TextSegment> result = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean italic = false;
        boolean underline = false;

        int i = 0;
        while (i < line.length()) {
            char c = line.charAt(i);
            if (c == '/' && hasClosingMarker(line, i + 1, '/')) {
                flushSegment(result, current, italic, underline);
                italic = !italic;
                i++;
            } else if (c == '_' && hasClosingMarker(line, i + 1, '_')) {
                flushSegment(result, current, italic, underline);
                underline = !underline;
                i++;
            } else {
                current.append(c);
                i++;
            }
        }
        flushSegment(result, current, italic, underline);
        return result;
    }

    private static boolean hasClosingMarker(String text, int startIdx, char marker) {
        int next = text.indexOf(marker, startIdx);
        return next > startIdx;
    }

    private static void flushSegment(List<TextSegment> list, StringBuilder sb, boolean italic, boolean underline) {
        if (sb.length() > 0) {
            list.add(new TextSegment(sb.toString(), italic, underline));
            sb.setLength(0);
        }
    }

    private record TextSegment(String content, boolean italic, boolean underline) {
    }
}
