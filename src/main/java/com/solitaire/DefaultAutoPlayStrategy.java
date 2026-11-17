package solitaire;

import java.util.List;

public final class DefaultAutoPlayStrategy implements AutoPlayStrategy {
    @Override
    public SuggestedMove decide(ReadOnlyGame game) {
        if (game.isLocationEmpty(LocationId.waste()) && game.canDraw()) {
            return SuggestedMove.draw();
        }

        Card wasteTop = game.peekLocation(LocationId.waste());

        if (wasteTop != null) {
            for (int f = 0; f < 4; f++) {
                LocationId target = LocationId.foundation(f);
                if (game.canMove(LocationId.waste(), wasteTop, target)) {
                    return SuggestedMove.move(LocationId.waste(), wasteTop, target);
                }
            }
            for (int col = 0; col < 7; col++) {
                LocationId target = LocationId.tableau(col);
                if (game.canMove(LocationId.waste(), wasteTop, target)) {
                    return SuggestedMove.move(LocationId.waste(), wasteTop, target);
                }
            }
        }

        for (int col = 0; col < 7; col++) {
            LocationId source = LocationId.tableau(col);
            Card top = game.peekLocation(source);
            if (top != null) {
                for (int f = 0; f < 4; f++) {
                    LocationId target = LocationId.foundation(f);
                    if (game.canMove(source, top, target)) {
                        return SuggestedMove.move(source, top, target);
                    }
                }
            }
        }

        for (int srcCol = 0; srcCol < 7; srcCol++) {
            LocationId source = LocationId.tableau(srcCol);
            List<Card> cards = game.getLocationCards(source);
            for (Card card : cards) {
                if (!game.isLowestVisibleInTableau(srcCol, card)) continue;
                if (game.isKingAtBaseInTableau(srcCol, card)) continue;
                for (int dstCol = 0; dstCol < 7; dstCol++) {
                    if (srcCol == dstCol) continue;
                    LocationId target = LocationId.tableau(dstCol);
                    if (game.canMove(source, card, target)) {
                        return SuggestedMove.move(source, card, target);
                    }
                }
            }
        }

        if (game.canDraw()) return SuggestedMove.draw();
        return SuggestedMove.none();
    }
}
