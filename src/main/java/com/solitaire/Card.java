package solitaire;

import java.util.*;

public final class Card {
    private static final Map<Suit, Map<Rank, Card>> CACHE = new EnumMap<>(Suit.class);
    private static final List<Card> ALL_CARDS;

    static {
        List<Card> list = new ArrayList<>(52);
        for (Suit suit : Suit.values()) {
            Map<Rank, Card> byRank = new EnumMap<>(Rank.class);
            for (Rank rank : Rank.values()) {
                Card card = new Card(suit, rank);
                byRank.put(rank, card);
                list.add(card);
            }
            CACHE.put(suit, Collections.unmodifiableMap(byRank));
        }
        ALL_CARDS = Collections.unmodifiableList(list);
    }

    private final Suit suit;
    private final Rank rank;

    private Card(Suit suit, Rank rank) {
        this.suit = suit;
        this.rank = rank;
    }

    public static Card of(Suit suit, Rank rank) {
        Objects.requireNonNull(suit, "Naipe obrigatório");
        Objects.requireNonNull(rank, "Rank obrigatório");
        return CACHE.get(suit).get(rank);
    }

    public static List<Card> allCards() { return ALL_CARDS; }
    public Suit getSuit() { return suit; }
    public Rank getRank() { return rank; }
    public String serialize() { return suit.name() + ":" + rank.name(); }

    public static Card deserialize(String token) {
        String[] parts = token.trim().split(":");
        if (parts.length != 2) throw new IllegalArgumentException("Token de carta inválido: " + token);
        return of(Suit.valueOf(parts[0]), Rank.valueOf(parts[1]));
    }

    public static String serializeSequence(List<Card> cards) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < cards.size(); i++) {
            if (i > 0) sb.append(";");
            sb.append(cards.get(i).serialize());
        }
        return sb.toString();
    }

    public static List<Card> deserializeSequence(String serialized) {
        if (serialized == null || serialized.isBlank()) return Collections.emptyList();
        String[] tokens = serialized.split(";");
        List<Card> result = new ArrayList<>(tokens.length);
        for (String token : tokens) result.add(deserialize(token));
        return Collections.unmodifiableList(result);
    }

    @Override
    public String toString() { return rank.getLabel() + suit.getSymbol(); }
}
