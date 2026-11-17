package solitaire.model;

public enum Suit {
    COPAS(Cor.VERMELHO),
    OUROS(Cor.VERMELHO),
    PAUS(Cor.PRETO),
    ESPADAS(Cor.PRETO);

    private final Cor cor;

    Suit(Cor cor) { this.cor = cor; }

    public Cor color() { return cor; }
    public boolean isRed() { return cor == Cor.VERMELHO; }
}