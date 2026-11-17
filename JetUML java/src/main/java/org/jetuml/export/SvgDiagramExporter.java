package org.jetuml.export;

import org.jetuml.geom.Point2DInt;
import org.jetuml.geom.Rectangle2DInt;
import org.jetuml.model.AbstractEdge;
import org.jetuml.model.Diagram;
import org.jetuml.model.Edge;
import org.jetuml.model.Node;
import org.jetuml.model.edges.*;
import org.jetuml.model.nodes.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Gera exportacao vetorial SVG limpa, sem grade e ajustada aos limites do conteudo (RF04).
 */
public final class SvgDiagramExporter {
    private static final int MARGIN = 16;

    private SvgDiagramExporter() {
    }

    public static void exportToSvg(Diagram diagram, Path path) throws IOException {
        String svg = generateSvg(diagram);
        Files.writeString(path, svg, StandardCharsets.UTF_8);
    }

    public static String generateSvg(Diagram diagram) {
        Rectangle2DInt content = diagram.getContentBounds();
        int offsetX = content.x() - MARGIN;
        int offsetY = content.y() - MARGIN;
        int width = Math.max(100, content.width() + MARGIN * 2);
        int height = Math.max(80, content.height() + MARGIN * 2);

        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        sb.append(String.format(
                "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"%d\" height=\"%d\" viewBox=\"0 0 %d %d\">\n",
                width, height, width, height
        ));
        sb.append("  <style>\n");
        sb.append("    .uml-box { fill: #ffffff; stroke: #22252a; stroke-width: 1.4; }\n");
        sb.append("    .uml-pkg { fill: #f7f8fa; stroke: #22252a; stroke-width: 1.4; }\n");
        sb.append("    .uml-text { font-family: sans-serif; font-size: 12px; fill: #1a1d21; }\n");
        sb.append("    .uml-bold { font-family: sans-serif; font-size: 12px; font-weight: bold; fill: #1a1d21; }\n");
        sb.append("    .uml-edge { fill: none; stroke: #22252a; stroke-width: 1.4; }\n");
        sb.append("    .uml-dashed { fill: none; stroke: #22252a; stroke-width: 1.4; stroke-dasharray: 6,5;\n");
        sb.append("  </style>\n");
        sb.append(String.format("  <rect width=\"%d\" height=\"%d\" fill=\"#ffffff\"/>\n", width, height));
        sb.append(String.format("  <g transform=\"translate(%d,%d)\">\n", -offsetX, -offsetY));

        for (Node node : diagram.getAllNodes()) {
            appendNodeSvg(sb, node);
        }
        for (Edge edge : diagram.getEdges()) {
            appendEdgeSvg(sb, edge);
        }

        sb.append("  </g>");
        sb.append("</svg>");
        return sb.toString();
    }

    private static void appendNodeSvg(StringBuilder sb, Node node) {
        Rectangle2DInt b = node.getBounds();
        if (node instanceof ClassNode cls) {
            sb.append(String.format("    <rect class=\"uml-box\" x=\"%d\" y=\"%d\" width=\"%d\" height=\"%d\"/>\n",
                    b.x(), b.y(), b.width(), b.height()));
            sb.append(String.format("    <text class=\"uml-bold\" x=\"%d\" y=\"%d\" text-anchor=\"middle\">%s</text>\n",
                    b.getCenter().x(), b.y() + 18, escapeXml(cls.getName())));
        } else if (node instanceof InterfaceNode iface) {
            sb.append(String.format("    <rect class=\"uml-box\" x=\"%d\" y=\"%d\" width=\"%d\" height=\"%d\"/>\n",
                    b.x(), b.y(), b.width(), b.height()));
            sb.append(String.format("    <text class=\"uml-text\" x=\"%d\" y=\"%d\" text-anchor=\"middle\">«interface»</text>\n",
                    b.getCenter().x(), b.y() + 14));
            sb.append(String.format("    <text class=\"uml-bold\" x=\"%d\" y=\"%d\" text-anchor=\"middle\">%s</text>\n",
                    b.getCenter().x(), b.y() + 30, escapeXml(iface.getName())));
        } else if (node instanceof PackageNode pkg) {
            sb.append(String.format("    <rect class=\"uml-pkg\" x=\"%d\" y=\"%d\" width=\"%d\" height=\"%d\"/>\n",
                    b.x(), b.y() + 22, b.width(), b.height() - 22));
            sb.append(String.format("    <text class=\"uml-bold\" x=\"%d\" y=\"%d\">%s</text>\n",
                    b.x() + 8, b.y() + 15, escapeXml(pkg.getName())));
        } else if (node instanceof PackageDescriptionNode pd) {
            sb.append(String.format("    <rect class=\"uml-box\" x=\"%d\" y=\"%d\" width=\"%d\" height=\"%d\"/>\n",
                    b.x(), b.y() + 22, b.width(), b.height() - 22));
            sb.append(String.format("    <text class=\"uml-bold\" x=\"%d\" y=\"%d\">%s</text>\n",
                    b.x() + 8, b.y() + 15, escapeXml(pd.getName())));
        } else if (node instanceof ImplicitParameterNode ip) {
            Rectangle2DInt h = ip.getHeaderBounds();
            sb.append(String.format("    <rect class=\"uml-box\" x=\"%d\" y=\"%d\" width=\"%d\" height=\"%d\"/>\n",
                    h.x(), h.y(), h.width(), h.height()));
            sb.append(String.format("    <text class=\"uml-text\" x=\"%d\" y=\"%d\" text-anchor=\"middle\" text-decoration=\"underline\">%s</text>\n",
                    h.getCenter().x(), h.getCenter().y() + 4, escapeXml(ip.getName())));
            sb.append(String.format("    <line class=\"uml-dashed\" x1=\"%d\" y1=\"%d\" x2=\"%d\" y2=\"%d\"/>\n",
                    ip.getLifelineCenterX(), h.getMaxY(), ip.getLifelineCenterX(), b.getMaxY()));
        } else if (node instanceof CallNode call) {
            sb.append(String.format("    <rect class=\"uml-box\" x=\"%d\" y=\"%d\" width=\"%d\" height=\"%d\"/>\n",
                    b.x(), b.y(), b.width(), b.height()));
        } else if (node instanceof StateNode st) {
            sb.append(String.format("    <rect class=\"uml-box\" rx=\"12\" ry=\"12\" x=\"%d\" y=\"%d\" width=\"%d\" height=\"%d\"/>\n",
                    b.x(), b.y(), b.width(), b.height()));
            sb.append(String.format("    <text class=\"uml-text\" x=\"%d\" y=\"%d\" text-anchor=\"middle\">%s</text>\n",
                    b.getCenter().x(), b.getCenter().y() + 4, escapeXml(st.getName())));
        } else if (node instanceof InitialStateNode) {
            sb.append(String.format("    <circle cx=\"%d\" cy=\"%d\" r=\"%d\" fill=\"#22252a\"/>\n",
                    b.getCenter().x(), b.getCenter().y(), b.width() / 2));
        } else if (node instanceof FinalStateNode) {
            sb.append(String.format("    <circle class=\"uml-box\" cx=\"%d\" cy=\"%d\" r=\"%d\"/>\n",
                    b.getCenter().x(), b.getCenter().y(), b.width() / 2));
            sb.append(String.format("    <circle cx=\"%d\" cy=\"%d\" r=\"%d\" fill=\"#22252a\"/>\n",
                    b.getCenter().x(), b.getCenter().y(), Math.max(4, b.width() / 2 - 5)));
        } else if (node instanceof ObjectNode obj) {
            sb.append(String.format("    <rect class=\"uml-box\" x=\"%d\" y=\"%d\" width=\"%d\" height=\"%d\"/>\n",
                    b.x(), b.y(), b.width(), b.height()));
            sb.append(String.format("    <text class=\"uml-bold\" x=\"%d\" y=\"%d\" text-anchor=\"middle\" text-decoration=\"underline\">%s</text>\n",
                    b.getCenter().x(), b.y() + 20, escapeXml(obj.getName())));
        } else if (node instanceof FieldNode field) {
            sb.append(String.format("    <text class=\"uml-text\" x=\"%d\" y=\"%d\">%s = %s</text>\n",
                    b.x() + 4, b.getCenter().y() + 4, escapeXml(field.getName()), escapeXml(field.getValue())));
        } else if (node instanceof ActorNode actor) {
            int cx = b.getCenter().x();
            int y = b.y() + 6;
            sb.append(String.format("    <circle class=\"uml-box\" cx=\"%d\" cy=\"%d\" r=\"10\"/>\n", cx, y + 10));
            sb.append(String.format("    <line class=\"uml-edge\" x1=\"%d\" y1=\"%d\" x2=\"%d\" y2=\"%d\"/>\n", cx, y + 20, cx, y + 46));
            sb.append(String.format("    <line class=\"uml-edge\" x1=\"%d\" y1=\"%d\" x2=\"%d\" y2=\"%d\"/>\n", cx - 16, y + 28, cx + 16, y + 28));
            sb.append(String.format("    <line class=\"uml-edge\" x1=\"%d\" y1=\"%d\" x2=\"%d\" y2=\"%d\"/>\n", cx, y + 46, cx - 14, y + 66));
            sb.append(String.format("    <line class=\"uml-edge\" x1=\"%d\" y1=\"%d\" x2=\"%d\" y2=\"%d\"/>\n", cx, y + 46, cx + 14, y + 66));
            sb.append(String.format("    <text class=\"uml-text\" x=\"%d\" y=\"%d\" text-anchor=\"middle\">%s</text>\n",
                    cx, b.getMaxY() - 6, escapeXml(actor.getName())));
        } else if (node instanceof UseCaseNode uc) {
            sb.append(String.format("    <ellipse class=\"uml-box\" cx=\"%d\" cy=\"%d\" rx=\"%d\" ry=\"%d\"/>\n",
                    b.getCenter().x(), b.getCenter().y(), b.width() / 2, b.height() / 2));
            sb.append(String.format("    <text class=\"uml-text\" x=\"%d\" y=\"%d\" text-anchor=\"middle\">%s</text>\n",
                    b.getCenter().x(), b.getCenter().y() + 4, escapeXml(uc.getName())));
        } else if (node instanceof NoteNode note) {
            sb.append(String.format("    <rect class=\"uml-pkg\" x=\"%d\" y=\"%d\" width=\"%d\" height=\"%d\"/>\n",
                    b.x(), b.y(), b.width(), b.height()));
            sb.append(String.format("    <text class=\"uml-text\" x=\"%d\" y=\"%d\">%s</text>\n",
                    b.x() + 8, b.y() + 20, escapeXml(note.getText())));
        }
    }

    private static void appendEdgeSvg(StringBuilder sb, Edge edge) {
        if (!(edge instanceof AbstractEdge ae)) return;
        List<Point2DInt> pts = ae.getRoutedPoints();
        if (pts.size() < 2) return;

        StringBuilder pointsAttr = new StringBuilder();
        for (Point2DInt p : pts) {
            pointsAttr.append(p.x()).append(",").append(p.y()).append(" ");
        }
        boolean dashed = (edge instanceof ReturnEdge) || (edge instanceof NoteConnectorEdge)
                || (edge instanceof ClassRelationshipEdge rel && (rel.getKind() == ClassRelationshipEdge.RelationshipKind.DEPENDENCY || rel.getKind() == ClassRelationshipEdge.RelationshipKind.REALIZATION))
                || (edge instanceof UseCaseEdge uc && (uc.getKind() == UseCaseEdge.UseCaseKind.INCLUDE || uc.getKind() == UseCaseEdge.UseCaseKind.EXTEND));

        sb.append(String.format("    <polyline class=\"%s\" points=\"%s\"/>\n",
                dashed ? "uml-dashed" : "uml-edge", pointsAttr.toString().trim()));

        String label = extractEdgeLabel(edge);
        if (!label.isEmpty()) {
            Point2DInt mid = pts.get(pts.size() / 2);
            sb.append(String.format("    <text class=\"uml-text\" x=\"%d\" y=\"%d\" text-anchor=\"middle\">%s</text>\n",
                    mid.x(), mid.y() - 6, escapeXml(label)));
        }
    }

    private static String extractEdgeLabel(Edge edge) {
        if (edge instanceof ClassRelationshipEdge rel) return rel.getMiddleLabel();
        if (edge instanceof CallEdge call) return call.getMiddleLabel();
        if (edge instanceof ReturnEdge ret) return ret.getMiddleLabel();
        if (edge instanceof TransitionEdge tr) return tr.getMiddleLabel();
        if (edge instanceof CollaborationEdge col) return col.getMiddleLabel();
        if (edge instanceof UseCaseEdge uc) {
            if (uc.getDependencyType() == UseCaseEdge.DependencyType.INCLUDE) return "«include»";
            if (uc.getDependencyType() == UseCaseEdge.DependencyType.EXTEND) return "«extend»";
        }
        return "";
    }

    private static String escapeXml(String raw) {
        if (raw == null) return "";
        return raw.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
