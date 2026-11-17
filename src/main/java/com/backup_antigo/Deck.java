package solitaire.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public final class Deck {
    private Deck() {}

    public static List<Card> full() {
        List<Card> cards = new ArrayList<>(52);
        for (Suit s : Suit.values())
            for (Rank r : Rank.values())
                cards.add(new Card(s, r));   // uma única instância por (suit, rank)
        return cards;
    }

    /**
     * Fisher–Yates via Collections.shuffle(lista, new Random(seed)).
     * Mesma seed ⇒ mesma permutação.
     */
    public static List<Card> shuffled(long seed) {
        List<Card> cards = full();
        Collections.shuffle(cards, new Random(seed));
        return cards;
    }
}