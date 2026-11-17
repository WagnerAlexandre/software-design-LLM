package org.jetuml.layout;

import org.jetuml.geom.Point2DInt;
import org.jetuml.geom.Rectangle2DInt;
import org.jetuml.model.AbstractEdge;
import org.jetuml.model.Diagram;
import org.jetuml.model.Edge;
import org.jetuml.model.Node;
import org.jetuml.model.edges.CallEdge;
import org.jetuml.model.edges.ReferenceEdge;
import org.jetuml.model.edges.ReturnEdge;
import org.jetuml.model.nodes.CallNode;
import org.jetuml.model.nodes.FieldNode;

import java.util.List;

/**
 * Calcula automaticamente o caminho (pontos de roteamento) de todas as arestas
 * sempre que os nos de origem ou destino sao movidos ou criados (RF17).
 */
public final class EdgeRouter {

    private EdgeRouter() {
    }

    public static void routeAll(Diagram diagram) {
        if (diagram == null) return;
        List<Edge> edges = diagram.getEdges();
        for (int i = 0; i < edges.size(); i++) {
            Edge edge = edges.get(i);
            int parallelIndex = countPrecedingParallelEdges(edges, i, edge);
            List<Point2DInt> points = computeRoute(edge, parallelIndex);
            if (edge instanceof AbstractEdge abstractEdge) {
                abstractEdge.setRoutedPoints(points);
            }
        }
    }

    private static int countPrecedingParallelEdges(List<Edge> edges, int currentIndex, Edge target) {
        int count = 0;
        for (int i = 0; i < currentIndex; i++) {
            Edge other = edges.get(i);
            boolean samePair = (other.getStartNode().equals(target.getStartNode()) && other.getEndNode().equals(target.getEndNode()))
                    || (other.getStartNode().equals(target.getEndNode()) && other.getEndNode().equals(target.getStartNode()));
            if (samePair) {
                count++;
            }
        }
        return count;
    }

    public static List<Point2DInt> computeRoute(Edge edge, int parallelIndex) {
        Node start = edge.getStartNode();
        Node end = edge.getEndNode();
        if (start == null || end == null) {
            return List.of();
        }

        // Caso 1: Autoaresta (self-loop) (RF19, RF28)
        if (start.equals(end)) {
            if (edge instanceof CallEdge && start instanceof CallNode callNode) {
                return computeSelfCallRoute(callNode);
            }
            return computeSelfLoopRoute(start.getBounds(), parallelIndex);
        }

        // Caso 2: Chamadas e Retornos no Diagrama de Sequencia (RF27, RF29)
        if (edge instanceof CallEdge callEdge && start instanceof CallNode caller && end instanceof CallNode callee) {
            return computeSequenceCallRoute(caller, callee, callEdge.isCreateCall());
        }
        if (edge instanceof ReturnEdge && start instanceof CallNode callee && end instanceof CallNode caller) {
            return computeSequenceReturnRoute(callee, caller);
        }

        // Caso 3: Referencia em Diagrama de Objetos (RF26: sai do centro da parte Value do FieldNode)
        if (edge instanceof ReferenceEdge && start instanceof FieldNode field) {
            Point2DInt startPt = field.getValueCenterPoint();
            Point2DInt endPt = end.getConnectionPoint(startPt);
            int midX = (startPt.x() + endPt.x()) / 2;
            return List.of(
                    startPt,
                    new Point2DInt(midX, startPt.y()),
                    new Point2DInt(midX, endPt.y()),
                    endPt
            );
        }

        // Caso 4: Aresta padrao entre dois nos distintos (com deslocamento paralelo se houver multiplas transicoes)
        Point2DInt startCenter = start.getBounds().getCenter();
        Point2DInt endCenter = end.getBounds().getCenter();

        if (parallelIndex == 0) {
            Point2DInt p1 = start.getConnectionPoint(endCenter);
            Point2DInt p2 = end.getConnectionPoint(startCenter);
            return List.of(p1, p2);
        } else {
            double dx = endCenter.x() - startCenter.x();
            double dy = endCenter.y() - startCenter.y();
            double len = Math.hypot(dx, dy);
            if (len < 1.0) len = 1.0;
            double nx = -dy / len;
            double ny = dx / len;
            int offset = ((parallelIndex % 2 == 1) ? 1 : -1) * ((parallelIndex + 1) / 2) * 26;

            Point2DInt mid = new Point2DInt(
                    (int) Math.round((startCenter.x() + endCenter.x()) / 2.0 + nx * offset),
                    (int) Math.round((startCenter.y() + endCenter.y()) / 2.0 + ny * offset)
            );
            Point2DInt p1 = start.getConnectionPoint(mid);
            Point2DInt p2 = end.getConnectionPoint(mid);
            return List.of(p1, mid, p2);
        }
    }

    private static List<Point2DInt> computeSelfLoopRoute(Rectangle2DInt bounds, int parallelIndex) {
        int spread = 28 + parallelIndex * 18;
        if (parallelIndex % 2 == 0) {
            // Laco no canto superior direito
            Point2DInt p1 = new Point2DInt(bounds.getMaxX(), bounds.y() + 14);
            Point2DInt p2 = new Point2DInt(bounds.getMaxX() + spread, bounds.y() + 14);
            Point2DInt p3 = new Point2DInt(bounds.getMaxX() + spread, bounds.y() - spread);
            Point2DInt p4 = new Point2DInt(bounds.getMaxX() - 18, bounds.y() - spread);
            Point2DInt p5 = new Point2DInt(bounds.getMaxX() - 18, bounds.y());
            return List.of(p1, p2, p3, p4, p5);
        } else {
            // Laco no canto inferior esquerdo (para 2a autotransicao em Diagrama de Estados, RF19)
            Point2DInt p1 = new Point2DInt(bounds.x(), bounds.getMaxY() - 14);
            Point2DInt p2 = new Point2DInt(bounds.x() - spread, bounds.getMaxY() - 14);
            Point2DInt p3 = new Point2DInt(bounds.x() - spread, bounds.getMaxY() + spread);
            Point2DInt p4 = new Point2DInt(bounds.x() + 18, bounds.getMaxY() + spread);
            Point2DInt p5 = new Point2DInt(bounds.x() + 18, bounds.getMaxY());
            return List.of(p1, p2, p3, p4, p5);
        }
    }

    private static List<Point2DInt> computeSelfCallRoute(CallNode caller) {
        Rectangle2DInt b = caller.getBounds();
        int y1 = b.y() + 8;
        int y2 = y1 + 18;
        int rightX = b.getMaxX() + 30;
        return List.of(
                new Point2DInt(b.getMaxX(), y1),
                new Point2DInt(rightX, y1),
                new Point2DInt(rightX, y2),
                new Point2DInt(b.getMaxX() + 6, y2)
        );
    }

    private static List<Point2DInt> computeSequenceCallRoute(CallNode caller, CallNode callee, boolean isCreate) {
        Rectangle2DInt b1 = caller.getBounds();
        Rectangle2DInt b2 = callee.getBounds();
        int y = b2.y() + 4;

        if (isCreate && callee.getImplicitParameter() != null) {
            Rectangle2DInt header = callee.getImplicitParameter().getHeaderBounds();
            y = header.getCenter().y();
            boolean leftToRight = b1.getCenter().x() <= header.getCenter().x();
            int startX = leftToRight ? b1.getMaxX() : b1.x();
            int endX = leftToRight ? header.x() : header.getMaxX();
            return List.of(new Point2DInt(startX, y), new Point2DInt(endX, y));
        }

        boolean leftToRight = b1.getCenter().x() <= b2.getCenter().x();
        int startX = leftToRight ? b1.getMaxX() : b1.x();
        int endX = leftToRight ? b2.x() : b2.getMaxX();
        return List.of(new Point2DInt(startX, y), new Point2DInt(endX, y));
    }

    private static List<Point2DInt> computeSequenceReturnRoute(CallNode callee, CallNode caller) {
        Rectangle2DInt bCallee = callee.getBounds();
        Rectangle2DInt bCaller = caller.getBounds();
        int y = bCallee.getMaxY() - 4;
        boolean rightToLeft = bCallee.getCenter().x() >= bCaller.getCenter().x();
        int startX = rightToLeft ? bCallee.x() : bCallee.getMaxX();
        int endX = rightToLeft ? bCaller.getMaxX() : bCaller.x();
        return List.of(new Point2DInt(startX, y), new Point2DInt(endX, y));
    }
}
