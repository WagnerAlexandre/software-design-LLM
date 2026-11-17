package org.jetuml.geom;

/**
 * Representa um ponto imutavel em coordenadas inteiras 2D no canvas do diagrama.
 */
public record Point2DInt(int x, int y) {

    public Point2DInt translate(int dx, int dy) {
        return new Point2DInt(this.x + dx, this.y + dy);
    }

    public double distanceTo(Point2DInt other) {
        double dx = this.x - other.x;
        double dy = this.y - other.y;
        return Math.hypot(dx, dy);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Point2DInt that)) return false;
        return x == that.x && y == that.y;
    }

    @Override
    public String toString() {
        return "(" + x + ", " + y + ")";
    }
}
