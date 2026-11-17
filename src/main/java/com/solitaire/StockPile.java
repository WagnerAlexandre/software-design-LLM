package solitaire;

import java.util.List;

public final class StockPile extends CardPile {
    public StockPile() { super(LocationId.stock()); }

    @Override
    public boolean canAccept(List<Card> sequence) { return false; }
}
