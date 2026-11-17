package solitaire;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

public final class SolitaireGame implements ReadOnlyGame {
    private final Deck deck;
    private final StockPile stock = new StockPile();
    private final WastePile waste = new WastePile();
    private final FoundationPile[] foundations = new FoundationPile[4];
    private final TableauPile[] tableau = new TableauPile[7];

    private final Deque<GameAction> history = new ArrayDeque<>();
    private final List<GameListener> listeners = new CopyOnWriteArrayList<>();

    private AutoPlayStrategy autoPlayStrategy = new DefaultAutoPlayStrategy();
    private boolean autoPlayEnabled = true;

    public SolitaireGame() { this(new Deck()); }

    public SolitaireGame(Deck deck) {
        this.deck = deck;
        for (int i = 0; i < 4; i++) foundations[i] = new FoundationPile(i);
        for (int i = 0; i < 7; i++) tableau[i] = new TableauPile(i);
        newGame();
    }

    public void newGame() {
        deck.shuffle();
        List<Card> shuffled = deck.getCards();

        history.clear();
        stock.clear();
        waste.clear();
        for (FoundationPile f : foundations) f.clear();

        int cursor = 0;
        for (int col = 0; col < 7; col++) {
            int count = col + 1;
            tableau[col].dealInitial(shuffled.subList(cursor, cursor + count));
            cursor += count;
        }
        while (cursor < shuffled.size()) stock.push(shuffled.get(cursor++));
        notifyListeners();
    }

    public void addListener(GameListener listener) {
        if (listener != null) listeners.add(listener);
    }

    public void removeListener(GameListener listener) { listeners.remove(listener); }

    private void notifyListeners() {
        for (GameListener listener : listeners) listener.onGameStateChanged();
    }

    @Override
    public int getScore() {
        int total = 0;
        for (FoundationPile f : foundations) total += f.size();
        return total;
    }

    @Override
    public boolean isWon() { return getScore() == 52; }

    private CardPile getPile(LocationId id) {
        return switch (id.getType()) {
            case STOCK -> stock;
            case WASTE -> waste;
            case FOUNDATION -> foundations[id.getIndex()];
            case TABLEAU -> tableau[id.getIndex()];
        };
    }

    @Override public boolean isLocationEmpty(LocationId location) { return getPile(location).isEmpty(); }
    @Override public Card peekLocation(LocationId location) { return getPile(location).peek(); }
    @Override public List<Card> getLocationCards(LocationId location) { return getPile(location).getCards(); }

    @Override
    public Optional<LocationId> findAccessibleCardLocation(Card card) {
        if (card == null) return Optional.empty();
        if (waste.peek() == card) return Optional.of(waste.getLocationId());
        for (FoundationPile f : foundations) {
            if (f.peek() == card) return Optional.of(f.getLocationId());
        }
        for (TableauPile t : tableau) {
            if (t.isVisible(card)) return Optional.of(t.getLocationId());
        }
        return Optional.empty();
    }

    @Override public boolean isCardVisibleInTableau(int columnIndex, Card card) { return tableau[columnIndex].isVisible(card); }
    @Override public boolean isLowestVisibleInTableau(int columnIndex, Card card) { return tableau[columnIndex].isLowestVisible(card); }
    @Override public boolean isKingAtBaseInTableau(int columnIndex, Card card) { return tableau[columnIndex].isKingAtBase(card); }
    @Override public List<Card> getTableauSequenceFrom(int columnIndex, Card card) { return tableau[columnIndex].getSequenceFrom(card); }
    @Override public boolean canDraw() { return !stock.isEmpty(); }

    @Override
    public boolean canMove(LocationId source, Card card, LocationId target) {
        if (source == null || card == null || target == null || source.equals(target)) return false;
        if (source.getType() == LocationId.Type.STOCK || target.getType() == LocationId.Type.STOCK) return false;
        if (target.getType() == LocationId.Type.WASTE) return false;

        List<Card> movingSequence = extractMovableSequence(source, card);
        if (movingSequence.isEmpty()) return false;
        return getPile(target).canAccept(movingSequence);
    }

    private List<Card> extractMovableSequence(LocationId source, Card card) {
        CardPile pile = getPile(source);
        if (pile instanceof TableauPile tab) return tab.getSequenceFrom(card);
        if (pile.peek() == card) return List.of(card);
        return List.of();
    }

    public boolean draw() {
        if (!canDraw()) return false;
        executeAction(new GameAction.DrawAction(stock, waste));
        return true;
    }

    public boolean move(LocationId source, Card card, LocationId target) {
        if (!canMove(source, card, target)) return false;

        CardPile sourcePile = getPile(source);
        CardPile targetPile = getPile(target);
        List<Card> sequence = extractMovableSequence(source, card);

        List<GameAction> steps = new ArrayList<>(2);
        steps.add(new GameAction.MoveAction(sourcePile, targetPile, sequence));

        if (sourcePile instanceof TableauPile tab && tab.size() > sequence.size()) {
            int remainingSize = tab.size() - sequence.size();
            Card cardBelow = tab.getCards().get(remainingSize - 1);
            if (!tab.isVisible(cardBelow)) {
                steps.add(new GameAction.RevealCardAction(tab));
            }
        }

        GameAction action = (steps.size() == 1) ? steps.get(0) : new GameAction.CompositeAction(steps);
        executeAction(action);
        return true;
    }

    private void executeAction(GameAction action) {
        action.execute();
        if (!action.isNull()) {
            history.push(action);
            notifyListeners();
        }
    }

    @Override
    public boolean canUndo() { return !history.isEmpty(); }

    public boolean undo() {
        if (!canUndo()) return false;
        history.pop().undo();
        notifyListeners();
        return true;
    }

    public void setAutoPlayEnabled(boolean enabled) {
        this.autoPlayEnabled = enabled;
        this.autoPlayStrategy = enabled ? new DefaultAutoPlayStrategy() : new DisabledAutoPlayStrategy();
        notifyListeners();
    }

    @Override
    public boolean isAutoPlayEnabled() { return autoPlayEnabled; }

    public boolean playAutoStep() {
        SuggestedMove suggestion = autoPlayStrategy.decide(this);
        return switch (suggestion.type()) {
            case DRAW -> draw();
            case MOVE -> move(suggestion.source(), suggestion.card(), suggestion.target());
            case NONE -> {
                executeAction(GameAction.NullAction.INSTANCE);
                yield false;
            }
        };
    }
}
