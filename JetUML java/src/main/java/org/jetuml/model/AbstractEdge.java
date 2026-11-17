package org.jetuml.model;

import org.jetuml.geom.Point2DInt;
import org.jetuml.geom.Rectangle2DInt;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Implementacao base para arestas UML com suporte a pontos roteados e deteccao de clique (hit-testing).
 */
public abstract class AbstractEdge implements Edge {
    private static final int HIT_TOLERANCE_PX = 6;

    private String id;
    private Node startNode;
    private Node endNode;
    private List<Point2DInt> routedPoints = new ArrayList<>();

    protected AbstractEdge(Node startNode, Node endNode) {
        this.id = UUID.randomUUID().toString();
        this.startNode = startNode;
        this.endNode = endNode;
    }

    private static double distanceToSegment(Point2DInt p, Point2DInt a, Point2DInt b) {
        double vx = b.x() - a.x();
        double vy = b.y() - a.y();
        double wx = p.x() - a.x();
        double wy = p.y() - a.y();

        double c1 = vx * wx + vy * wy;
        if (c1 <= 0) return p.distanceTo(a);

        double c2 = vx * vx + vy * vy;
        if (c2 <= c1) return p.distanceTo(b);

        double t = c1 / c2;
        double projX = a.x() + t * vx;
        double projY = a.y() + t * vy;
        return Math.hypot(p.x() - projX, p.y() - projY);
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String id) {
        if (id != null && !id.isBlank()) {
            this.id = id;
        }
    }

    @Override
    public Node getStartNode() {
        return startNode;
    }

    @Override
    public void setStartNode(Node startNode) {
        this.startNode = startNode;
    }

    @Override
    public Node getEndNode() {
        return endNode;
    }

    @Override
    public void setEndNode(Node endNode) {
        this.endNode = endNode;
    }

    public List<Point2DInt> getRoutedPoints() {
        return Collections.unmodifiableList(routedPoints);
    }

    public void setRoutedPoints(List<Point2DInt> points) {
        this.routedPoints = points == null ? new ArrayList<>() : new ArrayList<>(points);
    }

    @Override
    public Rectangle2DInt getBounds() {
        if (routedPoints.isEmpty()) {
            if (startNode != null && endNode != null) {
                return startNode.getBounds().union(endNode.getBounds());
            }
            return new Rectangle2DInt(0, 0, 1, 1);
        }
        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxY = Integer.MIN_VALUE;
        for (Point2DInt p : routedPoints) {
            minX = Math.min(minX, p.x());
            minY = Math.min(minY, p.y());
            maxX = Math.max(maxX, p.x());
            maxY = Math.max(maxY, p.y());
        }
        return new Rectangle2DInt(minX, minY, Math.max(1, maxX - minX), Math.max(1, maxY - minY));
    }

    @Override
    public boolean contains(Point2DInt point) {
        List<Point2DInt> pts = getEffectivePoints();
        if (pts.size() < 2) return false;
        for (int i = 0; i < pts.size() - 1; i++) {
            if (distanceToSegment(point, pts.get(i), pts.get(i + 1)) <= HIT_TOLERANCE_PX) {
                return true;
            }
        }
        return false;
    }

    private List<Point2DInt> getEffectivePoints() {
        if (!routedPoints.isEmpty()) {
            return routedPoints;
        }
        if (startNode != null && endNode != null) {
            Point2DInt p1 = startNode.getConnectionPoint(endNode.getBounds().getCenter());
            Point2DInt p2 = endNode.getConnectionPoint(startNode.getBounds().getCenter());
            return List.of(p1, p2);
        }
        return List.of();
    }

    @Override
    public boolean hasEditableProperties() {
        return true;
    }
}
