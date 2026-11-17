package com.solitaire.model;

import java.util.ArrayList;
import java.util.List;

/** Pilha genérica. Usada por fundações, descarte e monte. */
public class Pile {
    protected final List<Card> cards = new ArrayList<>();

    public int size()             { return cards.size(); }
    public boolean isEmpty()      { return cards.isEmpty(); }
    public Card top()             { return cards.isEmpty() ? null : cards.get(cards.size() - 1); }
    public List<Card> cards()     { return List.copyOf(cards); }

    public void push(Card c)      { cards.add(c); }

    public Card pop() {
        if (cards.isEmpty()) throw new IllegalStateException("pile vazia");
        return cards.remove(cards.size() - 1);
    }

    /** Por padrão apenas o topo é retirável. Column sobrescreve. */
    public List<Card> popSequence(int fromIndex) {
        if (fromIndex != cards.size() - 1) throw new IllegalArgumentException("apenas topo");
        return List.of(pop());
    }
}