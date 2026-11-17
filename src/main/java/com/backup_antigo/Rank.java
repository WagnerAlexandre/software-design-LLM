package solitaire.model;

public enum Rank {
    AS(1), DOIS(2), TRES(3), QUATRO(4), CINCO(5), SEIS(6), SETE(7),
    OITO(8), NOVE(9), DEZ(10), VALETE(11), DAMA(12), REI(13);

    private final int value;

    Rank(int value) { this.value = value; }

    public int value() { return value; }
    public boolean isAce()  { return this == AS; }
    public boolean isKing() { return this == REI; }

    public Rank next() {
        if (this == REI) throw new IllegalStateException("REI não tem próximo");
        return values()[value];               // AS é índice 0, value 1 → índice 1 = DOIS
    }

    public Rank previous() {
        if (this == AS) throw new IllegalStateException("AS não tem anterior");
        return values()[value - 2];
    }
}