package solitaire;

import java.util.List;
import java.util.Optional;

public interface ReadOnlyGame {
    boolean isWon();
    int getScore();
    boolean isLocationEmpty(LocationId location);
    Card peekLocation(LocationId location);
    List<Card> getLocationCards(LocationId location);
    Optional<LocationId> findAccessibleCardLocation(Card card);
    boolean isCardVisibleInTableau(int columnIndex, Card card);
    boolean isLowestVisibleInTableau(int columnIndex, Card card);
    boolean isKingAtBaseInTableau(int columnIndex, Card card);
    List<Card> getTableauSequenceFrom(int columnIndex, Card card);
    boolean canDraw();
    boolean canMove(LocationId source, Card card, LocationId target);
    boolean canUndo();
    boolean isAutoPlayEnabled();
}
