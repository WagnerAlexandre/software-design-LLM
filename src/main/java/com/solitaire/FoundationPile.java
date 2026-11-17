package solitaire;

import java.util.List;

public final class FoundationPile extends CardPile {
    public FoundationPile(int index) { super(LocationId.foundation(index)); }

    @Override
    public boolean canAccept(List<Card> sequence) {
        if (sequence == null || sequence.size() != 1) return false;
        Card incoming = sequence.get(0);
        if (isEmpty()) return incoming.getRank() == Rank.ACE;
        Card top = peek();
        return incoming.getSuit() == top.getSuit()
                && incoming.getRank().isImmediatelyHigherThan(top.getRank());
    }
}
