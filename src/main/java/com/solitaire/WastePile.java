package solitaire;

import java.util.List;

public final class WastePile extends CardPile {
    public WastePile() { super(LocationId.waste()); }

    @Override
    public boolean canAccept(List<Card> sequence) { return false; }
}
