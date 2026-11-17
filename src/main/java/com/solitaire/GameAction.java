package solitaire;

import java.util.*;

public interface GameAction {
    void execute();
    void undo();
    default boolean isNull() { return false; }

    final class NullAction implements GameAction {
        public static final NullAction INSTANCE = new NullAction();
        private NullAction() {}
        @Override public void execute() {}
        @Override public void undo() {}
        @Override public boolean isNull() { return true; }
    }

    final class DrawAction implements GameAction {
        private final StockPile stock;
        private final WastePile waste;
        private Card drawnCard;

        public DrawAction(StockPile stock, WastePile waste) {
            this.stock = stock;
            this.waste = waste;
        }

        @Override
        public void execute() {
            drawnCard = stock.pop();
            waste.push(drawnCard);
        }

        @Override
        public void undo() {
            Card topWaste = waste.pop();
            if (topWaste != drawnCard) throw new IllegalStateException("Inconsistência ao desfazer compra.");
            stock.push(drawnCard);
        }
    }

    final class MoveAction implements GameAction {
        private final CardPile source;
        private final CardPile target;
        private final List<Card> movedCards;

        public MoveAction(CardPile source, CardPile target, List<Card> sequence) {
            this.source = source;
            this.target = target;
            this.movedCards = List.copyOf(sequence);
        }

        @Override
        public void execute() {
            List<Card> removed = source.popSequenceFrom(movedCards.get(0));
            target.pushAll(removed);
        }

        @Override
        public void undo() {
            List<Card> removed = target.popSequenceFrom(movedCards.get(0));
            source.pushAll(removed);
        }
    }

    final class RevealCardAction implements GameAction {
        private final TableauPile column;
        public RevealCardAction(TableauPile column) { this.column = column; }
        @Override public void execute() { column.revealTop(); }
        @Override public void undo() { column.hideTop(); }
    }

    final class CompositeAction implements GameAction {
        private final List<GameAction> actions;

        public CompositeAction(List<GameAction> actions) {
            this.actions = Collections.unmodifiableList(new ArrayList<>(actions));
        }

        @Override
        public void execute() {
            for (GameAction action : actions) action.execute();
        }

        @Override
        public void undo() {
            for (int i = actions.size() - 1; i >= 0; i--) actions.get(i).undo();
        }
    }
}
