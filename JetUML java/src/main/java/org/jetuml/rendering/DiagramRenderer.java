package org.jetuml.rendering;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.TextAlignment;
import org.jetuml.config.UserPreferences;
import org.jetuml.geom.Point2DInt;
import org.jetuml.geom.Rectangle2DInt;
import org.jetuml.model.*;
import org.jetuml.model.edges.*;
import org.jetuml.model.nodes.*;

import java.util.List;
import java.util.Set;

/**
 * Renderizador vetorial completo dos 5 tipos de diagramas UML em JavaFX GraphicsContext,
 * respeitando fonte configurada (RF39), grade (RF36), modo escuro (RF40) e formatacao UML (RF24).
 */
public final class DiagramRenderer {

    private DiagramRenderer() {
    }

    public static void render(
            GraphicsContext gc,
            Diagram diagram,
            boolean includeGrid,
            boolean includeSelectionAndOverlays,
            OverlayState overlay
    ) {
        if (gc == null || diagram == null) return;

        UserPreferences prefs = UserPreferences.getInstance();
        ThemePalette palette = ThemePalette.forDarkMode(prefs.isDarkMode());
        String fontFamily = prefs.getFontFamily();
        double fontSize = prefs.getFontSize();

        double width = diagram.getWidth();
        double height = diagram.getHeight();

        // Fundo do canvas
        gc.save();
        gc.setFill(palette.canvasBackground);
        gc.fillRect(0, 0, width, height);

        // Grade opcional (RF36)
        if (includeGrid && prefs.isShowGrid()) {
            drawGrid(gc, width, height, diagram.getGrid().gridSize(), palette);
        }

        // Renderizar nos em ordem hierarquica (pais antes dos filhos)
        for (Node root : diagram.getRootNodes()) {
            drawNodeRecursive(gc, root, palette, fontFamily, fontSize);
        }

        // Renderizar arestas
        for (Edge edge : diagram.getEdges()) {
            drawEdge(gc, edge, palette, fontFamily, fontSize);
        }

        // Destaque de selecao e sobreposicoes interativas (nao incluidas na exportacao/clipboard, RF04, RF05)
        if (includeSelectionAndOverlays) {
            drawSelectionHighlights(gc, diagram.getSelectedElements(), palette);
            if (overlay != null) {
                drawOverlays(gc, overlay, palette);
            }
        }

        gc.restore();
    }

    private static void drawGrid(GraphicsContext gc, double width, double height, int step, ThemePalette palette) {
        gc.save();
        gc.setStroke(palette.gridLine);
        gc.setLineWidth(1.0);
        for (double x = 0; x <= width; x += step) {
            gc.strokeLine(x + 0.5, 0, x + 0.5, height);
        }
        for (double y = 0; y <= height; y += step) {
            gc.strokeLine(0, y + 0.5, width, y + 0.5);
        }
        gc.restore();
    }

    private static void drawNodeRecursive(
            GraphicsContext gc,
            Node node,
            ThemePalette palette,
            String fontFamily,
            double fontSize
    ) {
        if (node instanceof PackageNode pkg) {
            drawPackageNode(gc, pkg, palette, fontFamily, fontSize);
            for (Node child : pkg.getChildren()) {
                drawNodeRecursive(gc, child, palette, fontFamily, fontSize);
            }
        } else if (node instanceof ClassNode cls) {
            drawClassNode(gc, cls, palette, fontFamily, fontSize);
        } else if (node instanceof InterfaceNode iface) {
            drawInterfaceNode(gc, iface, palette, fontFamily, fontSize);
        } else if (node instanceof PackageDescriptionNode pkgDesc) {
            drawPackageDescriptionNode(gc, pkgDesc, palette, fontFamily, fontSize);
        } else if (node instanceof ImplicitParameterNode lifeline) {
            drawImplicitParameterNode(gc, lifeline, palette, fontFamily, fontSize);
            for (Node child : lifeline.getChildren()) {
                drawNodeRecursive(gc, child, palette, fontFamily, fontSize);
            }
        } else if (node instanceof CallNode call) {
            drawCallNode(gc, call, palette);
            for (Node subCall : call.getChildren()) {
                drawNodeRecursive(gc, subCall, palette, fontFamily, fontSize);
            }
        } else if (node instanceof StateNode state) {
            drawStateNode(gc, state, palette, fontFamily, fontSize);
        } else if (node instanceof InitialStateNode init) {
            drawInitialStateNode(gc, init, palette);
        } else if (node instanceof FinalStateNode fin) {
            drawFinalStateNode(gc, fin, palette);
        } else if (node instanceof ObjectNode obj) {
            drawObjectNode(gc, obj, palette, fontFamily, fontSize);
            for (Node field : obj.getChildren()) {
                drawNodeRecursive(gc, field, palette, fontFamily, fontSize);
            }
        } else if (node instanceof FieldNode field) {
            drawFieldNode(gc, field, palette, fontFamily, fontSize);
        } else if (node instanceof ActorNode actor) {
            drawActorNode(gc, actor, palette, fontFamily, fontSize);
        } else if (node instanceof UseCaseNode uc) {
            drawUseCaseNode(gc, uc, palette, fontFamily, fontSize);
        } else if (node instanceof NoteNode note) {
            drawNoteNode(gc, note, palette, fontFamily, fontSize);
        } else if (node instanceof PointNode pt) {
            drawPointNode(gc, pt, palette);
        }
    }

    // ------------------------------------------------------------------------
    // Desenho dos Nos UML
    // ------------------------------------------------------------------------
    private static void drawClassNode(GraphicsContext gc, ClassNode node, ThemePalette palette, String fontFamily, double fontSize) {
        Rectangle2DInt b = node.getBounds();
        int headerH = 28;
        String[] attrLines = node.getAttributes().isEmpty() ? new String[0] : node.getAttributes().split("\\R", -1);
        int attrH = Math.max(22, attrLines.length * 17 + 10);

        gc.save();
        gc.setFill(palette.nodeFill);
        gc.setStroke(palette.nodeStroke);
        gc.setLineWidth(1.4);
        gc.fillRect(b.x(), b.y(), b.width(), b.height());
        gc.strokeRect(b.x(), b.y(), b.width(), b.height());

        // Divisorias de compartimentos
        double y1 = b.y() + headerH;
        double y2 = y1 + attrH;
        gc.strokeLine(b.x(), y1, b.getMaxX(), y1);
        gc.strokeLine(b.x(), y2, b.getMaxX(), y2);

        // Nome da classe
        FormattedTextRenderer.drawFormattedLine(
                gc, node.getName(), b.x() + 6, b.y() + headerH / 2.0,
                b.width() - 12, fontFamily, fontSize, true, TextAlignment.CENTER, palette.textPrimary
        );

        // Atributos
        double curY = y1 + 12;
        for (String line : attrLines) {
            FormattedTextRenderer.drawFormattedLine(
                    gc, line, b.x() + 8, curY,
                    b.width() - 16, fontFamily, fontSize, false, TextAlignment.LEFT, palette.textPrimary
            );
            curY += 17;
        }

        // Metodos
        String[] methLines = node.getMethods().isEmpty() ? new String[0] : node.getMethods().split("\\R", -1);
        curY = y2 + 12;
        for (String line : methLines) {
            FormattedTextRenderer.drawFormattedLine(
                    gc, line, b.x() + 8, curY,
                    b.width() - 16, fontFamily, fontSize, false, TextAlignment.LEFT, palette.textPrimary
            );
            curY += 17;
        }
        gc.restore();
    }

    private static void drawInterfaceNode(GraphicsContext gc, InterfaceNode node, ThemePalette palette, String fontFamily, double fontSize) {
        Rectangle2DInt b = node.getBounds();
        int headerH = 38;

        gc.save();
        gc.setFill(palette.nodeFill);
        gc.setStroke(palette.nodeStroke);
        gc.setLineWidth(1.4);
        gc.fillRect(b.x(), b.y(), b.width(), b.height());
        gc.strokeRect(b.x(), b.y(), b.width(), b.height());

        double y1 = b.y() + headerH;
        gc.strokeLine(b.x(), y1, b.getMaxX(), y1);

        FormattedTextRenderer.drawFormattedLine(
                gc, "«interface»", b.x() + 6, b.y() + 11,
                b.width() - 12, fontFamily, Math.max(9, fontSize - 1), false, TextAlignment.CENTER, palette.textSecondary
        );
        FormattedTextRenderer.drawFormattedLine(
                gc, node.getName(), b.x() + 6, b.y() + 26,
                b.width() - 12, fontFamily, fontSize, true, TextAlignment.CENTER, palette.textPrimary
        );

        String[] methLines = node.getMethods().isEmpty() ? new String[0] : node.getMethods().split("\\R", -1);
        double curY = y1 + 12;
        for (String line : methLines) {
            FormattedTextRenderer.drawFormattedLine(
                    gc, line, b.x() + 8, curY,
                    b.width() - 16, fontFamily, fontSize, false, TextAlignment.LEFT, palette.textPrimary
            );
            curY += 17;
        }
        gc.restore();
    }

    private static void drawPackageNode(GraphicsContext gc, PackageNode node, ThemePalette palette, String fontFamily, double fontSize) {
        Rectangle2DInt b = node.getBounds();
        int tabH = PackageNode.TAB_HEIGHT;
        int tabW = Math.min(b.width() - 20, Math.max(70, node.getName().length() * 8 + 24));

        gc.save();
        gc.setFill(palette.nodeSecondaryFill);
        gc.setStroke(palette.nodeStroke);
        gc.setLineWidth(1.4);

        // Aba superior do pacote
        gc.fillRect(b.x(), b.y(), tabW, tabH);
        gc.strokeRect(b.x(), b.y(), tabW, tabH);

        // Corpo do pacote
        gc.fillRect(b.x(), b.y() + tabH, b.width(), b.height() - tabH);
        gc.strokeRect(b.x(), b.y() + tabH, b.width(), b.height() - tabH);

        FormattedTextRenderer.drawFormattedLine(
                gc, node.getName(), b.x() + 8, b.y() + tabH / 2.0,
                tabW - 14, fontFamily, fontSize, true, TextAlignment.LEFT, palette.textPrimary
        );
        gc.restore();
    }

    private static void drawPackageDescriptionNode(GraphicsContext gc, PackageDescriptionNode node, ThemePalette palette, String fontFamily, double fontSize) {
        Rectangle2DInt b = node.getBounds();
        int tabH = 22;
        int tabW = Math.min(b.width() - 16, Math.max(70, node.getName().length() * 8 + 20));

        gc.save();
        gc.setFill(palette.nodeFill);
        gc.setStroke(palette.nodeStroke);
        gc.setLineWidth(1.4);

        gc.fillRect(b.x(), b.y(), tabW, tabH);
        gc.strokeRect(b.x(), b.y(), tabW, tabH);

        gc.fillRect(b.x(), b.y() + tabH, b.width(), b.height() - tabH);
        gc.strokeRect(b.x(), b.y() + tabH, b.width(), b.height() - tabH);

        FormattedTextRenderer.drawFormattedLine(
                gc, node.getName(), b.x() + 8, b.y() + tabH / 2.0,
                tabW - 12, fontFamily, fontSize, true, TextAlignment.LEFT, palette.textPrimary
        );

        String[] lines = node.getContents().isEmpty() ? new String[0] : node.getContents().split("\\R", -1);
        double curY = b.y() + tabH + 14;
        for (String line : lines) {
            FormattedTextRenderer.drawFormattedLine(
                    gc, line, b.x() + 8, curY,
                    b.width() - 16, fontFamily, fontSize, false, TextAlignment.LEFT, palette.textPrimary
            );
            curY += 18;
        }
        gc.restore();
    }

    private static void drawImplicitParameterNode(GraphicsContext gc, ImplicitParameterNode node, ThemePalette palette, String fontFamily, double fontSize) {
        Rectangle2DInt header = node.getHeaderBounds();
        int centerX = node.getLifelineCenterX();
        int bottomY = node.getBounds().getMaxY();

        gc.save();
        gc.setFill(palette.nodeFill);
        gc.setStroke(palette.nodeStroke);
        gc.setLineWidth(1.4);

        gc.fillRect(header.x(), header.y(), header.width(), header.height());
        gc.strokeRect(header.x(), header.y(), header.width(), header.height());

        // Nome sublinhado por convencao UML para objetos em Diagrama de Sequencia
        FormattedTextRenderer.drawFormattedLine(
                gc, "_" + node.getName() + "_", header.x() + 6, header.getCenter().y(),
                header.width() - 12, fontFamily, fontSize, false, TextAlignment.CENTER, palette.textPrimary
        );

        // Linha de vida tracejada
        gc.setLineDashes(6, 5);
        gc.strokeLine(centerX, header.getMaxY(), centerX, bottomY);
        gc.restore();
    }

    private static void drawCallNode(GraphicsContext gc, CallNode call, ThemePalette palette) {
        Rectangle2DInt b = call.getBounds();
        gc.save();
        gc.setFill(palette.nodeFill);
        gc.setStroke(palette.nodeStroke);
        gc.setLineWidth(1.3);
        gc.fillRect(b.x(), b.y(), b.width(), b.height());

        if (call.isOpenBottom()) {
            // RF31: Sequencia incompleta exibe borda inferior aberta/tracejada
            gc.strokeLine(b.x(), b.y(), b.getMaxX(), b.y());
            gc.strokeLine(b.x(), b.y(), b.x(), b.getMaxY());
            gc.strokeLine(b.getMaxX(), b.y(), b.getMaxX(), b.getMaxY());
            gc.setLineDashes(3, 3);
            gc.strokeLine(b.x(), b.getMaxY(), b.getMaxX(), b.getMaxY());
        } else {
            gc.strokeRect(b.x(), b.y(), b.width(), b.height());
        }
        gc.restore();
    }

    private static void drawStateNode(GraphicsContext gc, StateNode state, ThemePalette palette, String fontFamily, double fontSize) {
        Rectangle2DInt b = state.getBounds();
        gc.save();
        gc.setFill(palette.nodeFill);
        gc.setStroke(palette.nodeStroke);
        gc.setLineWidth(1.4);
        gc.fillRoundRect(b.x(), b.y(), b.width(), b.height(), 22, 22);
        gc.strokeRoundRect(b.x(), b.y(), b.width(), b.height(), 22, 22);

        FormattedTextRenderer.drawFormattedLine(
                gc, state.getName(), b.x() + 8, b.getCenter().y(),
                b.width() - 16, fontFamily, fontSize, false, TextAlignment.CENTER, palette.textPrimary
        );
        gc.restore();
    }

    private static void drawInitialStateNode(GraphicsContext gc, InitialStateNode init, ThemePalette palette) {
        Rectangle2DInt b = init.getBounds();
        gc.save();
        gc.setFill(palette.nodeStroke);
        gc.fillOval(b.x(), b.y(), b.width(), b.height());
        gc.restore();
    }

    private static void drawFinalStateNode(GraphicsContext gc, FinalStateNode fin, ThemePalette palette) {
        Rectangle2DInt b = fin.getBounds();
        gc.save();
        gc.setFill(palette.canvasBackground);
        gc.setStroke(palette.nodeStroke);
        gc.setLineWidth(1.6);
        gc.fillOval(b.x(), b.y(), b.width(), b.height());
        gc.strokeOval(b.x(), b.y(), b.width(), b.height());

        gc.setFill(palette.nodeStroke);
        gc.fillOval(b.x() + 5, b.y() + 5, b.width() - 10, b.height() - 10);
        gc.restore();
    }

    private static void drawObjectNode(GraphicsContext gc, ObjectNode obj, ThemePalette palette, String fontFamily, double fontSize) {
        Rectangle2DInt b = obj.getBounds();
        gc.save();
        gc.setFill(palette.nodeFill);
        gc.setStroke(palette.nodeStroke);
        gc.setLineWidth(1.4);
        gc.fillRect(b.x(), b.y(), b.width(), b.height());
        gc.strokeRect(b.x(), b.y(), b.width(), b.height());

        double headerBottom = b.y() + ObjectNode.HEADER_HEIGHT;
        gc.strokeLine(b.x(), headerBottom, b.getMaxX(), headerBottom);

        FormattedTextRenderer.drawFormattedLine(
                gc, "_" + obj.getName() + "_", b.x() + 6, b.y() + ObjectNode.HEADER_HEIGHT / 2.0,
                b.width() - 12, fontFamily, fontSize, true, TextAlignment.CENTER, palette.textPrimary
        );
        gc.restore();
    }

    private static void drawFieldNode(GraphicsContext gc, FieldNode field, ThemePalette palette, String fontFamily, double fontSize) {
        Rectangle2DInt b = field.getBounds();
        Rectangle2DInt valB = field.getValueBounds();
        gc.save();
        String text = field.getName() + " = " + field.getValue();
        FormattedTextRenderer.drawFormattedLine(
                gc, text, b.x() + 4, b.getCenter().y(),
                b.width() - 8, fontFamily, fontSize, false, TextAlignment.LEFT, palette.textPrimary
        );
        // Indicador sutil da regiao Value (ponto de ancoragem para ReferenceEdge, RF26)
        gc.setFill(palette.textSecondary);
        gc.fillOval(valB.getCenter().x() - 2, valB.getCenter().y() - 2, 4, 4);
        gc.restore();
    }

    private static void drawActorNode(GraphicsContext gc, ActorNode actor, ThemePalette palette, String fontFamily, double fontSize) {
        Rectangle2DInt b = actor.getBounds();
        double cx = b.getCenter().x();
        double topY = b.y() + 6;

        gc.save();
        gc.setStroke(palette.nodeStroke);
        gc.setFill(palette.nodeFill);
        gc.setLineWidth(1.5);

        // Cabeca
        gc.fillOval(cx - 10, topY, 20, 20);
        gc.strokeOval(cx - 10, topY, 20, 20);
        // Tronco
        gc.strokeLine(cx, topY + 20, cx, topY + 46);
        // Bracos
        gc.strokeLine(cx - 16, topY + 28, cx + 16, topY + 28);
        // Pernas
        gc.strokeLine(cx, topY + 46, cx - 14, topY + 66);
        gc.strokeLine(cx, topY + 46, cx + 14, topY + 66);

        FormattedTextRenderer.drawFormattedLine(
                gc, actor.getName(), b.x() + 2, b.getMaxY() - 10,
                b.width() - 4, fontFamily, fontSize, false, TextAlignment.CENTER, palette.textPrimary
        );
        gc.restore();
    }

    private static void drawUseCaseNode(GraphicsContext gc, UseCaseNode uc, ThemePalette palette, String fontFamily, double fontSize) {
        Rectangle2DInt b = uc.getBounds();
        gc.save();
        gc.setFill(palette.nodeFill);
        gc.setStroke(palette.nodeStroke);
        gc.setLineWidth(1.4);
        gc.fillOval(b.x(), b.y(), b.width(), b.height());
        gc.strokeOval(b.x(), b.y(), b.width(), b.height());

        FormattedTextRenderer.drawFormattedLine(
                gc, uc.getName(), b.x() + 12, b.getCenter().y(),
                b.width() - 24, fontFamily, fontSize, false, TextAlignment.CENTER, palette.textPrimary
        );
        gc.restore();
    }

    private static void drawNoteNode(GraphicsContext gc, NoteNode note, ThemePalette palette, String fontFamily, double fontSize) {
        Rectangle2DInt b = note.getBounds();
        int fold = 12;
        double x0 = b.x();
        double y0 = b.y();
        double x1 = b.getMaxX();
        double y1 = b.getMaxY();

        gc.save();
        gc.setFill(palette.nodeSecondaryFill);
        gc.setStroke(palette.nodeStroke);
        gc.setLineWidth(1.3);

        double[] xs = {x0, x1 - fold, x1, x1, x0};
        double[] ys = {y0, y0, y0 + fold, y1, y1};
        gc.fillPolygon(xs, ys, 5);
        gc.strokePolygon(xs, ys, 5);

        // Dobra superior direita
        gc.strokeLine(x1 - fold, y0, x1 - fold, y0 + fold);
        gc.strokeLine(x1 - fold, y0 + fold, x1, y0 + fold);

        String[] lines = note.getText().split("\\R", -1);
        double curY = y0 + 16;
        for (String line : lines) {
            FormattedTextRenderer.drawFormattedLine(
                    gc, line, x0 + 8, curY,
                    b.width() - 18, fontFamily, fontSize, false, TextAlignment.LEFT, palette.textPrimary
            );
            curY += 18;
        }
        gc.restore();
    }

    private static void drawPointNode(GraphicsContext gc, PointNode pt, ThemePalette palette) {
        Point2DInt c = pt.getBounds().getCenter();
        gc.save();
        gc.setFill(palette.nodeStroke);
        gc.fillOval(c.x() - 2.5, c.y() - 2.5, 5, 5);
        gc.restore();
    }

    // ------------------------------------------------------------------------
    // Desenho das Arestas UML
    // ------------------------------------------------------------------------
    private static void drawEdge(GraphicsContext gc, Edge edge, ThemePalette palette, String fontFamily, double fontSize) {
        List<Point2DInt> points = (edge instanceof AbstractEdge ae && !ae.getRoutedPoints().isEmpty())
                ? ae.getRoutedPoints()
                : List.of(
                edge.getStartNode().getConnectionPoint(edge.getEndNode().getBounds().getCenter()),
                edge.getEndNode().getConnectionPoint(edge.getStartNode().getBounds().getCenter())
        );
        if (points.size() < 2) return;

        gc.save();
        gc.setStroke(palette.edgeStroke);
        gc.setFill(palette.edgeStroke);
        gc.setLineWidth(1.4);

        boolean dashed = isDashedEdge(edge);
        if (dashed) {
            gc.setLineDashes(6, 5);
        }

        for (int i = 0; i < points.size() - 1; i++) {
            Point2DInt a = points.get(i);
            Point2DInt b = points.get(i + 1);
            gc.strokeLine(a.x(), a.y(), b.x(), b.y());
        }
        gc.setLineDashes((double[]) null);

        Point2DInt pStart = points.get(0);
        Point2DInt pSecond = points.get(1);
        Point2DInt pPenultimate = points.get(points.size() - 2);
        Point2DInt pEnd = points.get(points.size() - 1);

        drawEdgeDecorations(gc, edge, pStart, pSecond, pPenultimate, pEnd, palette, fontFamily, fontSize);
        drawEdgeLabels(gc, edge, points, palette, fontFamily, fontSize);

        gc.restore();
    }

    private static boolean isDashedEdge(Edge edge) {
        if (edge instanceof ClassRelationshipEdge rel) {
            return rel.getKind() == ClassRelationshipEdge.RelationshipKind.DEPENDENCY
                    || rel.getKind() == ClassRelationshipEdge.RelationshipKind.REALIZATION;
        }
        if (edge instanceof UseCaseEdge uc) {
            return uc.getKind() == UseCaseEdge.UseCaseKind.INCLUDE
                    || uc.getKind() == UseCaseEdge.UseCaseKind.EXTEND;
        }
        return edge instanceof ReturnEdge || edge instanceof NoteConnectorEdge;
    }

    private static void drawEdgeDecorations(
            GraphicsContext gc,
            Edge edge,
            Point2DInt pStart,
            Point2DInt pSecond,
            Point2DInt pPenultimate,
            Point2DInt pEnd,
            ThemePalette palette,
            String fontFamily,
            double fontSize
    ) {
        if (edge instanceof ClassRelationshipEdge rel) {
            switch (rel.getKind()) {
                case INHERITANCE, REALIZATION -> {
                    if (rel.getDirectionality() != Directionality.UNSPECIFIED) {
                        drawHollowTriangle(gc, pPenultimate, pEnd, palette.canvasBackground, palette.edgeStroke);
                    }
                    if (rel.getDirectionality() == Directionality.BIDIRECTIONAL) {
                        drawHollowTriangle(gc, pSecond, pStart, palette.canvasBackground, palette.edgeStroke);
                    }
                }
                case DEPENDENCY -> {
                    if (rel.getDirectionality() != Directionality.UNSPECIFIED) {
                        drawOpenArrow(gc, pPenultimate, pEnd);
                    }
                    if (rel.getDirectionality() == Directionality.BIDIRECTIONAL) {
                        drawOpenArrow(gc, pSecond, pStart);
                    }
                }
                case ASSOCIATION -> {
                    if (rel.getDirectionality() == Directionality.UNIDIRECTIONAL
                            || rel.getDirectionality() == Directionality.BIDIRECTIONAL) {
                        drawOpenArrow(gc, pPenultimate, pEnd);
                    }
                    if (rel.getDirectionality() == Directionality.BIDIRECTIONAL) {
                        drawOpenArrow(gc, pSecond, pStart);
                    }
                }
                case AGGREGATION -> {
                    drawDiamond(gc, pSecond, pStart, false, palette.canvasBackground, palette.edgeStroke);
                    if (rel.getDirectionality() == Directionality.UNIDIRECTIONAL
                            || rel.getDirectionality() == Directionality.BIDIRECTIONAL) {
                        drawOpenArrow(gc, pPenultimate, pEnd);
                    }
                }
                case COMPOSITION -> {
                    drawDiamond(gc, pSecond, pStart, true, palette.edgeStroke, palette.edgeStroke);
                    if (rel.getDirectionality() == Directionality.UNIDIRECTIONAL
                            || rel.getDirectionality() == Directionality.BIDIRECTIONAL) {
                        drawOpenArrow(gc, pPenultimate, pEnd);
                    }
                }
            }
        } else if (edge instanceof CallEdge) {
            drawFilledTriangle(gc, pPenultimate, pEnd);
        } else if (edge instanceof ReturnEdge || edge instanceof TransitionEdge || edge instanceof ReferenceEdge) {
            drawOpenArrow(gc, pPenultimate, pEnd);
        } else if (edge instanceof UseCaseEdge uc) {
            if (uc.getKind() == UseCaseEdge.UseCaseKind.GENERALIZATION) {
                drawHollowTriangle(gc, pPenultimate, pEnd, palette.canvasBackground, palette.edgeStroke);
            } else if (uc.getKind() == UseCaseEdge.UseCaseKind.INCLUDE || uc.getKind() == UseCaseEdge.UseCaseKind.EXTEND) {
                drawOpenArrow(gc, pPenultimate, pEnd);
            }
        }
    }

    private static void drawEdgeLabels(
            GraphicsContext gc,
            Edge edge,
            List<Point2DInt> points,
            ThemePalette palette,
            String fontFamily,
            double fontSize
    ) {
        Point2DInt mid = getMidPoint(points);
        double smallFont = Math.max(9, fontSize - 1);

        if (edge instanceof ClassRelationshipEdge rel) {
            if (!rel.getStartLabel().isEmpty()) {
                Point2DInt p0 = points.get(0);
                Point2DInt p1 = points.get(1);
                drawLabelAt(gc, rel.getStartLabel(), (p0.x() * 0.75 + p1.x() * 0.25), (p0.y() * 0.75 + p1.y() * 0.25) - 10, fontFamily, smallFont, palette);
            }
            if (!rel.getMiddleLabel().isEmpty()) {
                drawLabelAt(gc, rel.getMiddleLabel(), mid.x(), mid.y() - 10, fontFamily, smallFont, palette);
            }
            if (!rel.getEndLabel().isEmpty()) {
                Point2DInt pn1 = points.get(points.size() - 2);
                Point2DInt pn = points.get(points.size() - 1);
                drawLabelAt(gc, rel.getEndLabel(), (pn.x() * 0.75 + pn1.x() * 0.25), (pn.y() * 0.75 + pn1.y() * 0.25) - 10, fontFamily, smallFont, palette);
            }
        } else if (edge instanceof CallEdge call && !call.getMiddleLabel().isEmpty()) {
            drawLabelAt(gc, call.getMiddleLabel(), mid.x(), mid.y() - 10, fontFamily, smallFont, palette);
        } else if (edge instanceof ReturnEdge ret && !ret.getMiddleLabel().isEmpty()) {
            drawLabelAt(gc, ret.getMiddleLabel(), mid.x(), mid.y() - 10, fontFamily, smallFont, palette);
        } else if (edge instanceof TransitionEdge tr && !tr.getMiddleLabel().isEmpty()) {
            drawLabelAt(gc, tr.getMiddleLabel(), mid.x(), mid.y() - 10, fontFamily, smallFont, palette);
        } else if (edge instanceof CollaborationEdge col && !col.getMiddleLabel().isEmpty()) {
            drawLabelAt(gc, col.getMiddleLabel(), mid.x(), mid.y() - 10, fontFamily, smallFont, palette);
        } else if (edge instanceof UseCaseEdge uc) {
            if (uc.getDependencyType() == UseCaseEdge.DependencyType.INCLUDE) {
                drawLabelAt(gc, "«include»", mid.x(), mid.y() - 10, fontFamily, smallFont, palette);
            } else if (uc.getDependencyType() == UseCaseEdge.DependencyType.EXTEND) {
                drawLabelAt(gc, "«extend»", mid.x(), mid.y() - 10, fontFamily, smallFont, palette);
            }
        }
    }

    private static void drawLabelAt(
            GraphicsContext gc,
            String text,
            double cx,
            double cy,
            String fontFamily,
            double fontSize,
            ThemePalette palette
    ) {
        double w = FormattedTextRenderer.measureFormattedWidth(text, fontFamily, fontSize, false) + 8;
        gc.save();
        gc.setFill(palette.canvasBackground);
        gc.fillRect(cx - w / 2.0, cy - fontSize * 0.65, w, fontSize * 1.3);
        FormattedTextRenderer.drawFormattedLine(
                gc, text, cx - w / 2.0, cy, w, fontFamily, fontSize, false, TextAlignment.CENTER, palette.textPrimary
        );
        gc.restore();
    }

    private static Point2DInt getMidPoint(List<Point2DInt> points) {
        if (points.size() % 2 == 1) {
            return points.get(points.size() / 2);
        }
        Point2DInt a = points.get(points.size() / 2 - 1);
        Point2DInt b = points.get(points.size() / 2);
        return new Point2DInt((a.x() + b.x()) / 2, (a.y() + b.y()) / 2);
    }

    private static void drawOpenArrow(GraphicsContext gc, Point2DInt from, Point2DInt to) {
        double angle = Math.atan2(to.y() - from.y(), to.x() - from.x());
        double len = 11.0;
        double spread = Math.toRadians(28);
        double x1 = to.x() - len * Math.cos(angle - spread);
        double y1 = to.y() - len * Math.sin(angle - spread);
        double x2 = to.x() - len * Math.cos(angle + spread);
        double y2 = to.y() - len * Math.sin(angle + spread);
        gc.strokeLine(to.x(), to.y(), x1, y1);
        gc.strokeLine(to.x(), to.y(), x2, y2);
    }

    private static void drawFilledTriangle(GraphicsContext gc, Point2DInt from, Point2DInt to) {
        double angle = Math.atan2(to.y() - from.y(), to.x() - from.x());
        double len = 11.0;
        double spread = Math.toRadians(24);
        double[] xs = {
                to.x(),
                to.x() - len * Math.cos(angle - spread),
                to.x() - len * Math.cos(angle + spread)
        };
        double[] ys = {
                to.y(),
                to.y() - len * Math.sin(angle - spread),
                to.y() - len * Math.sin(angle + spread)
        };
        gc.fillPolygon(xs, ys, 3);
    }

    private static void drawHollowTriangle(GraphicsContext gc, Point2DInt from, Point2DInt to, Color fill, Color stroke) {
        double angle = Math.atan2(to.y() - from.y(), to.x() - from.x());
        double len = 13.0;
        double spread = Math.toRadians(26);
        double[] xs = {
                to.x(),
                to.x() - len * Math.cos(angle - spread),
                to.x() - len * Math.cos(angle + spread)
        };
        double[] ys = {
                to.y(),
                to.y() - len * Math.sin(angle - spread),
                to.y() - len * Math.sin(angle + spread)
        };
        gc.setFill(fill);
        gc.setStroke(stroke);
        gc.fillPolygon(xs, ys, 3);
        gc.strokePolygon(xs, ys, 3);
    }

    private static void drawDiamond(GraphicsContext gc, Point2DInt from, Point2DInt tip, boolean filled, Color fill, Color stroke) {
        double angle = Math.atan2(tip.y() - from.y(), tip.x() - from.x());
        double halfLen = 8.0;
        double halfWidth = 5.0;

        double midX = tip.x() - halfLen * Math.cos(angle);
        double midY = tip.y() - halfLen * Math.sin(angle);
        double backX = tip.x() - 2 * halfLen * Math.cos(angle);
        double backY = tip.y() - 2 * halfLen * Math.sin(angle);

        double perpX = -Math.sin(angle) * halfWidth;
        double perpY = Math.cos(angle) * halfWidth;

        double[] xs = {tip.x(), midX + perpX, backX, midX - perpX};
        double[] ys = {tip.y(), midY + perpY, backY, midY - perpY};

        gc.setFill(filled ? stroke : fill);
        gc.setStroke(stroke);
        gc.fillPolygon(xs, ys, 4);
        gc.strokePolygon(xs, ys, 4);
    }

    private static void drawSelectionHighlights(GraphicsContext gc, Set<DiagramElement> selected, ThemePalette palette) {
        if (selected == null || selected.isEmpty()) return;
        gc.save();
        gc.setStroke(palette.selectionHighlight);
        gc.setFill(palette.selectionHighlight);
        gc.setLineWidth(1.5);

        for (DiagramElement el : selected) {
            if (el instanceof Node node) {
                Rectangle2DInt b = node.getBounds().expand(3);
                gc.setLineDashes(4, 3);
                gc.strokeRect(b.x(), b.y(), b.width(), b.height());
                gc.setLineDashes((double[]) null);
                // Alcas nos 4 cantos
                drawHandle(gc, b.x(), b.y());
                drawHandle(gc, b.getMaxX(), b.y());
                drawHandle(gc, b.x(), b.getMaxY());
                drawHandle(gc, b.getMaxX(), b.getMaxY());
            } else if (el instanceof AbstractEdge edge) {
                List<Point2DInt> pts = edge.getRoutedPoints();
                for (Point2DInt p : pts) {
                    drawHandle(gc, p.x(), p.y());
                }
            }
        }
        gc.restore();
    }

    private static void drawHandle(GraphicsContext gc, double cx, double cy) {
        gc.fillRect(cx - 3.5, cy - 3.5, 7, 7);
    }

    private static void drawOverlays(GraphicsContext gc, OverlayState overlay, ThemePalette palette) {
        gc.save();
        if (overlay.dropTargetHighlight != null) {
            Rectangle2DInt r = overlay.dropTargetHighlight;
            gc.setStroke(palette.selectionHighlight);
            gc.setLineWidth(2.2);
            gc.setLineDashes(6, 4);
            gc.strokeRect(r.x(), r.y(), r.width(), r.height());
            gc.setLineDashes((double[]) null);
        }
        if (overlay.rubberBandStart != null && overlay.rubberBandEnd != null) {
            gc.setStroke(palette.rubberBandStroke);
            gc.setLineWidth(1.6);
            gc.setLineDashes(5, 4);
            gc.strokeLine(
                    overlay.rubberBandStart.x(),
                    overlay.rubberBandStart.y(),
                    overlay.rubberBandEnd.x(),
                    overlay.rubberBandEnd.y()
            );
            gc.setLineDashes((double[]) null);
        }
        if (overlay.selectionLasso != null) {
            Rectangle2DInt l = overlay.selectionLasso;
            gc.setFill(palette.lassoFill);
            gc.setStroke(palette.lassoStroke);
            gc.setLineWidth(1.2);
            gc.setLineDashes(4, 4);
            gc.fillRect(l.x(), l.y(), l.width(), l.height());
            gc.strokeRect(l.x(), l.y(), l.width(), l.height());
            gc.setLineDashes((double[]) null);
        }
        gc.restore();
    }

    public static final class OverlayState {
        public Point2DInt rubberBandStart;
        public Point2DInt rubberBandEnd;
        public Rectangle2DInt selectionLasso;
        public Rectangle2DInt dropTargetHighlight;
    }
}
