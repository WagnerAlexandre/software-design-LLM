package org.jetuml.geom;

/**
 * Representa dimensoes inteiras (largura e altura) imutaveis.
 */
public record Dimension2DInt(int width, int height) {
    public Dimension2DInt(int width, int height) {
        this.width = Math.max(0, width);
        this.height = Math.max(0, height);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Dimension2DInt that)) return false;
        return width == that.width && height == that.height;
    }

    @Override
    public String toString() {
        return width + "x" + height;
    }
}
