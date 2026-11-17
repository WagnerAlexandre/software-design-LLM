package solitaire.model;

public record Card(Suit suit, Rank rank) {
    public Card {
        if (suit == null || rank == null) throw new NullPointerException();
    }

    public Cor color() { return suit.color(); }
    public boolean isRed() { return suit.isRed(); }
    public boolean sameColorAs(Card other) { return color() == other.color(); }

    @Override public String toString() { return rank + "/" + suit; }
}