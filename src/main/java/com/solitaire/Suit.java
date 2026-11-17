package solitaire;

public enum Suit {
    HEARTS("♥", "Copas", true),
    DIAMONDS("♦", "Ouros", true),
    CLUBS("♣", "Paus", false),
    SPADES("♠", "Espadas", false);

    private final String symbol;
    private final String displayName;
    private final boolean red;

    Suit(String symbol, String displayName, boolean red) {
        this.symbol = symbol;
        this.displayName = displayName;
        this.red = red;
    }

    public String getSymbol() { return symbol; }
    public String getDisplayName() { return displayName; }
    public boolean isRed() { return red; }
    public boolean hasSameColorAs(Suit other) { return other != null && this.red == other.red; }
    public boolean hasOppositeColorTo(Suit other) { return other != null && this.red != other.red; }
}
