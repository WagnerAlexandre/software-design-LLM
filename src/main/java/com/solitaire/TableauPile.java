package solitaire;

import java.util.*;

public final class TableauPile extends CardPile {
    private int firstVisibleIndex = 0;

    public TableauPile(int index) { super(LocationId.tableau(index)); }

    void dealInitial(List<Card> initialCards) {
        cards.clear();
        cards.addAll(initialCards);
        firstVisibleIndex = initialCards.isEmpty() ? 0 : initialCards.size() - 1;
    }

    @Override
    public boolean canAccept(List<Card> sequence) {
        if (sequence == null || sequence.isEmpty()) return false;
        Card bottomOfIncoming = sequence.get(0);
        if (isEmpty()) return bottomOfIncoming.getRank() == Rank.KING;
        Card top = peek();
        return bottomOfIncoming.getRank().isImmediatelyLowerThan(top.getRank())
                && bottomOfIncoming.getSuit().hasOppositeColorTo(top.getSuit());
    }

    public boolean isVisible(Card card) {
        int idx = cards.indexOf(card);
        return idx >= 0 && idx >= firstVisibleIndex;
    }

    public boolean isLowestVisible(Card card) {
        int idx = cards.indexOf(card);
        return idx >= 0 && idx == firstVisibleIndex;
    }

    public boolean isKingAtBase(Card card) {
        return card != null
                && card.getRank() == Rank.KING
                && !cards.isEmpty()
                && cards.get(0) == card
                && firstVisibleIndex == 0;
    }

    public List<Card> getSequenceFrom(Card card) {
        int idx = cards.indexOf(card);
        if (idx < 0 || idx < firstVisibleIndex) return Collections.emptyList();
        return Collections.unmodifiableList(new ArrayList<>(cards.subList(idx, cards.size())));
    }

    void revealTop() {
        if (!cards.isEmpty() && firstVisibleIndex > cards.size() - 1) {
            firstVisibleIndex = cards.size() - 1;
        }
    }

    void hideTop() {
        if (firstVisibleIndex < cards.size()) {
            firstVisibleIndex++;
        }
    }

    @Override
    List<Card> popSequenceFrom(Card startCard) {
        List<Card> removed = super.popSequenceFrom(startCard);
        if (cards.isEmpty()) firstVisibleIndex = 0;
        return removed;
    }
}
