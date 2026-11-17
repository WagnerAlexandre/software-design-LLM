package org.jetuml.geom;

/**
 * Representa um retangulo delimitador (bounding box) imutavel em coordenadas inteiras.
 */
public record Rectangle2DInt(int x, int y, int width, int height) {
    public Rectangle2DInt(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = Math.max(0, width);
        this.height = Math.max(0, height);
    }

    public int getMaxX() {
        return x + width;
    }

    public int getMaxY() {
        return y + height;
    }

    public Point2DInt getCenter() {
        return new Point2DInt(x + width / 2, y + height / 2);
    }

    public Point2DInt getTopLeft() {
        return new Point2DInt(x, y);
    }

    public boolean contains(Point2DInt p) {
        return p.x() >= x && p.x() <= getMaxX()
                && p.y() >= y && p.y() <= getMaxY();
    }

    public boolean contains(Rectangle2DInt other) {
        return other.x >= this.x
                && other.y >= this.y
                && other.getMaxX() <= this.getMaxX()
                && other.getMaxY() <= this.getMaxY();
    }

    public boolean intersects(Rectangle2DInt other) {
        return this.x < other.getMaxX()
                && this.getMaxX() > other.x
                && this.y < other.getMaxY()
                && this.getMaxY() > other.y;
    }

    public Rectangle2DInt union(Rectangle2DInt other) {
        int minX = Math.min(this.x, other.x);
        int minY = Math.min(this.y, other.y);
        int maxX = Math.max(this.getMaxX(), other.getMaxX());
        int maxY = Math.max(this.getMaxY(), other.getMaxY());
        return new Rectangle2DInt(minX, minY, maxX - minX, maxY - minY);
    }

    public Rectangle2DInt expand(int padding) {
        return new Rectangle2DInt(
                x - padding,
                y - padding,
                width + padding * 2,
                height + padding * 2
        );
    }

    public Rectangle2DInt translate(int dx, int dy) {
        return new Rectangle2DInt(x + dx, y + dy, width, height);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Rectangle2DInt that)) return false;
        return x == that.x && y == that.y && width == that.width && height == that.height;
    }

    @Override
    public String toString() {
        return "Rect[" + x + "," + y + "," + width + "x" + height + "]";
    }
}
