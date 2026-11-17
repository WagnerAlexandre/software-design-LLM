package org.jetuml.model;

import org.jetuml.geom.Dimension2DInt;
import org.jetuml.geom.Point2DInt;
import org.jetuml.geom.Rectangle2DInt;

import java.util.UUID;

/**
 * Implementacao base para os nos UML com identificacao unica, geometria e pontos de ancoragem.
 */
public abstract class AbstractNode implements Node {
    private String id;
    private Point2DInt position;
    private Dimension2DInt size;

    protected AbstractNode(Point2DInt position, Dimension2DInt defaultSize) {
        this.id = UUID.randomUUID().toString();
        this.position = position != null ? position : new Point2DInt(0, 0);
        this.size = defaultSize != null ? defaultSize : new Dimension2DInt(100, 60);
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
    public Point2DInt getPosition() {
        return position;
    }

    @Override
    public void setPosition(Point2DInt position) {
        this.position = position;
    }

    @Override
    public void translate(int dx, int dy) {
        this.position = this.position.translate(dx, dy);
    }

    @Override
    public Dimension2DInt getSize() {
        return size;
    }

    @Override
    public void setSize(Dimension2DInt size) {
        if (size != null) {
            this.size = size;
        }
    }

    @Override
    public Rectangle2DInt getBounds() {
        return new Rectangle2DInt(position.x(), position.y(), size.width(), size.height());
    }

    @Override
    public boolean contains(Point2DInt point) {
        return getBounds().contains(point);
    }

    @Override
    public boolean hasEditableProperties() {
        return true;
    }

    /**
     * Calcula a intersecao entre o segmento que parte do centro deste no ate targetPoint
     * e a borda retangular do no.
     */
    @Override
    public Point2DInt getConnectionPoint(Point2DInt targetPoint) {
        Rectangle2DInt bounds = getBounds();
        Point2DInt center = bounds.getCenter();
        int cx = center.x();
        int cy = center.y();
        int dx = targetPoint.x() - cx;
        int dy = targetPoint.y() - cy;

        if (dx == 0 && dy == 0) {
            return new Point2DInt(bounds.getMaxX(), cy);
        }

        double halfW = bounds.width() / 2.0;
        double halfH = bounds.height() / 2.0;

        if (halfW <= 0 || halfH <= 0) {
            return center;
        }

        double scaleX = dx != 0 ? Math.abs(halfW / dx) : Double.POSITIVE_INFINITY;
        double scaleY = dy != 0 ? Math.abs(halfH / dy) : Double.POSITIVE_INFINITY;
        double scale = Math.min(scaleX, scaleY);

        int ix = (int) Math.round(cx + dx * scale);
        int iy = (int) Math.round(cy + dy * scale);
        return new Point2DInt(ix, iy);
    }
}
