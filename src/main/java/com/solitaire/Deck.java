package solitaire;

import java.util.*;

public final class Deck {
    private final List<Card> cards;
    private final Random random;

    public Deck() { this(new Random()); }

    public Deck(Random random) {
        this.cards = new ArrayList<>(Card.allCards());
        this.random = random;
    }

    public void shuffle() {
        cards.clear();
        cards.addAll(Card.allCards());
        Collections.shuffle(cards, random);
    }

    public List<Card> getCards() { return Collections.unmodifiableList(cards); }
}
