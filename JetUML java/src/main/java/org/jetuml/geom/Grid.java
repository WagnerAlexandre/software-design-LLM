package org.jetuml.geom;

/**
 * Gerencia o alinhamento (snap) a grade do diagrama (RF36).
 * Os elementos sempre se alinham a grade, esteja ela visivel ou oculta.
 */
public record Grid(int gridSize) {
    public static final int DEFAULT_GRID_SIZE = 10;

    public Grid() {
        this(DEFAULT_GRID_SIZE);
    }

    public Grid(int gridSize) {
        this.gridSize = Math.max(1, gridSize);
    }

    public int snap(int value) {
        return Math.round((float) value / gridSize) * gridSize;
    }

    public int snap(double value) {
        return (int) Math.round(value / gridSize) * gridSize;
    }

    public Point2DInt snap(Point2DInt point) {
        return new Point2DInt(snap(point.x()), snap(point.y()));
    }

    public Rectangle2DInt snap(Rectangle2DInt rect) {
        return new Rectangle2DInt(
                snap(rect.x()),
                snap(rect.y()),
                Math.max(gridSize, snap(rect.width())),
                Math.max(gridSize, snap(rect.height()))
        );
    }
}
