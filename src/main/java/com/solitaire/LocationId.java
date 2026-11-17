package solitaire;

import java.util.Objects;

public final class LocationId {
    public enum Type { STOCK, WASTE, FOUNDATION, TABLEAU }

    private final Type type;
    private final int index;

    private LocationId(Type type, int index) {
        this.type = type;
        this.index = index;
    }

    public static LocationId stock() { return new LocationId(Type.STOCK, 0); }
    public static LocationId waste() { return new LocationId(Type.WASTE, 0); }

    public static LocationId foundation(int index) {
        if (index < 0 || index >= 4) throw new IndexOutOfBoundsException("Fundação inválida: " + index);
        return new LocationId(Type.FOUNDATION, index);
    }

    public static LocationId tableau(int index) {
        if (index < 0 || index >= 7) throw new IndexOutOfBoundsException("Coluna inválida: " + index);
        return new LocationId(Type.TABLEAU, index);
    }

    public Type getType() { return type; }
    public int getIndex() { return index; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof LocationId that)) return false;
        return index == that.index && type == that.type;
    }

    @Override
    public int hashCode() { return Objects.hash(type, index); }

    @Override
    public String toString() { return type + "[" + index + "]"; }
}
