package solitaire;

import java.util.*;

public abstract class CardPile {
    private final LocationId locationId;
    protected final List<Card> cards = new ArrayList<>();

    protected CardPile(LocationId locationId) { this.locationId = locationId; }

    public LocationId getLocationId() { return locationId; }
    public boolean isEmpty() { return cards.isEmpty(); }
    public int size() { return cards.size(); }
    public Card peek() { return cards.isEmpty() ? null : cards.get(cards.size() - 1); }
    public boolean contains(Card card) { return cards.contains(card); }
    public List<Card> getCards() { return Collections.unmodifiableList(new ArrayList<>(cards)); }

    public abstract boolean canAccept(List<Card> sequence);

    void clear() { cards.clear(); }
    void push(Card card) { cards.add(card); }

    Card pop() {
        if (cards.isEmpty()) throw new NoSuchElementException("Pilha vazia: " + locationId);
        return cards.remove(cards.size() - 1);
    }

    void pushAll(List<Card> sequence) { cards.addAll(sequence); }

    List<Card> popSequenceFrom(Card startCard) {
        int idx = cards.indexOf(startCard);
        if (idx < 0) throw new IllegalArgumentException("Carta não encontrada na pilha: " + startCard);
        List<Card> sub = new ArrayList<>(cards.subList(idx, cards.size()));
        cards.subList(idx, cards.size()).clear();
        return sub;
    }
}
