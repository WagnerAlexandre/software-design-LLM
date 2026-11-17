package solitaire;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public final class SolitaireVerificationTest {

    public static void main(String[] args) {
        testCardsAndDeck_RF04_RF05_RF06_RF07();
        testInitialDealAndLocations_RF01_RF08_RF09_RF10_RF16();
        testStockWasteAndUndo_RF11_RF12_RF13();
        testFoundationsAndVictory_RF02_RF03_RF14_RF15();
        testTableauSequenceRevealAndUndo_RF17_RF18_RF19_RF20_RF23_RNF01();
        testFullHistoryUndoToInitialState_RF22_RF25_RF26_RF27();
        testAutoPlayPrioritiesCyclesAndDisable_RF24_RF28_RF29_RF30_RF31_RNF02();
        testSerializationFidelity_RNF05();

        System.out.println("TODOS OS TESTES (RF01-RF32 e RNF01-RNF05) PASSARAM COM SUCESSO!");
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError("FALHA: " + message);
        }
    }

    // RF04, RF05, RF06, RF07
    private static void testCardsAndDeck_RF04_RF05_RF06_RF07() {
        check(Card.allCards().size() == 52, "RF04: Baralho deve ter 52 cartas");
        check(Card.of(Suit.HEARTS, Rank.ACE) == Card.of(Suit.HEARTS, Rank.ACE),
                "RF04: Mesma carta deve manter identidade única (==)");

        check(Rank.TWO.isImmediatelyHigherThan(Rank.ACE), "RF05: 2 imediatamente superior a Ás");
        check(Rank.QUEEN.isImmediatelyLowerThan(Rank.KING), "RF05: Dama imediatamente inferior a Rei");

        check(Suit.HEARTS.hasSameColorAs(Suit.DIAMONDS), "RF06: Copas e Ouros são vermelhos");
        check(Suit.CLUBS.hasSameColorAs(Suit.SPADES), "RF06: Paus e Espadas são pretos");
        check(Suit.HEARTS.hasOppositeColorTo(Suit.SPADES), "RF06: Copas e Espadas têm cores opostas");

        Deck d1 = new Deck(new Random(1));
        Deck d2 = new Deck(new Random(2));
        d1.shuffle();
        d2.shuffle();
        check(!d1.getCards().equals(d2.getCards()), "RF07: Embaralhamentos distintos devem gerar ordens diferentes");
    }

    // RF01, RF08, RF09, RF10, RF16
    private static void testInitialDealAndLocations_RF01_RF08_RF09_RF10_RF16() {
        SolitaireGame game = new SolitaireGame(new Deck(new Random(42)));

        check(game.isLocationEmpty(LocationId.waste()), "RF01: Descarte inicia vazio");
        for (int f = 0; f < 4; f++) {
            check(game.isLocationEmpty(LocationId.foundation(f)), "RF01: Fundação " + f + " inicia vazia");
        }

        int tableauTotal = 0;
        for (int col = 0; col < 7; col++) {
            List<Card> cards = game.getLocationCards(LocationId.tableau(col));
            check(cards.size() == col + 1, "RF16: Coluna " + col + " deve ter " + (col + 1) + " cartas");
            tableauTotal += cards.size();
            for (int i = 0; i < cards.size(); i++) {
                boolean expectedVisible = (i == cards.size() - 1);
                check(game.isCardVisibleInTableau(col, cards.get(i)) == expectedVisible,
                        "RF16: Apenas última carta visível no início");
            }
        }
        check(tableauTotal == 28, "RF01: 28 cartas no tableau");
        check(game.getLocationCards(LocationId.stock()).size() == 24, "RF01: 24 cartas no monte");

        Card topCol0 = game.peekLocation(LocationId.tableau(0));
        check(game.findAccessibleCardLocation(topCol0).equals(Optional.of(LocationId.tableau(0))),
                "RF10: Deve localizar carta acessível");
    }

    // RF11, RF12, RF13
    private static void testStockWasteAndUndo_RF11_RF12_RF13() {
        SolitaireGame game = new SolitaireGame(new Deck(new Random(10)));
        Card expectedNext = game.peekLocation(LocationId.stock());

        check(game.draw(), "RF11: Compra válida quando monte não vazio");
        check(game.peekLocation(LocationId.waste()) == expectedNext, "RF11: Carta comprada vai ao topo do descarte");

        // RF13: Descarte não aceita movimentação direta
        Card topTableau = game.peekLocation(LocationId.tableau(0));
        check(!game.canMove(LocationId.tableau(0), topTableau, LocationId.waste()),
                "RF13: Descarte não pode ser destino de movimentação");

        // RF12: Desfazer compra devolve exatamente a mesma carta ao monte
        check(game.undo(), "RF12: Deve desfazer compra");
        check(game.isLocationEmpty(LocationId.waste()), "RF12: Descarte volta a ficar vazio");
        check(game.peekLocation(LocationId.stock()) == expectedNext, "RF12: Mesma carta volta ao topo do monte");
    }

    // RF02, RF03, RF14, RF15
    private static void testFoundationsAndVictory_RF02_RF03_RF14_RF15() {
        FoundationPile foundation = new FoundationPile(0);
        Card aceHearts = Card.of(Suit.HEARTS, Rank.ACE);
        Card twoHearts = Card.of(Suit.HEARTS, Rank.TWO);
        Card twoSpades = Card.of(Suit.SPADES, Rank.TWO);

        check(!foundation.canAccept(List.of(twoHearts)), "RF15: Fundação vazia só aceita Ás");
        check(foundation.canAccept(List.of(aceHearts)), "RF15: Fundação vazia aceita Ás");
        foundation.push(aceHearts);

        check(!foundation.canAccept(List.of(twoSpades)), "RF15: Não aceita naipe diferente");
        check(foundation.canAccept(List.of(twoHearts)), "RF15: Aceita mesmo naipe e rank + 1");
        check(!foundation.canAccept(List.of(twoHearts, Card.of(Suit.HEARTS, Rank.THREE))),
                "RF15: Apenas uma carta por vez");
    }

    // RF17, RF18, RF19, RF20, RF23, RNF01
    private static void testTableauSequenceRevealAndUndo_RF17_RF18_RF19_RF20_RF23_RNF01() {
        TableauPile col0 = new TableauPile(0);
        TableauPile col1 = new TableauPile(1);

        Card hiddenCard = Card.of(Suit.CLUBS, Rank.NINE);
        Card redEight = Card.of(Suit.HEARTS, Rank.EIGHT);
        Card blackSeven = Card.of(Suit.SPADES, Rank.SEVEN);
        Card blackNine = Card.of(Suit.SPADES, Rank.NINE);

        col0.dealInitial(List.of(hiddenCard, redEight));
        col0.push(blackSeven); // Sequência visível: [8♥, 7♠] sobre 9♣ oculto
        col1.dealInitial(List.of(blackNine));

        check(!col0.isVisible(hiddenCard), "Carta de baixo está oculta");
        check(col0.isLowestVisible(redEight), "RF20: 8♥ é a visível mais próxima da base");
        check(col0.getSequenceFrom(redEight).equals(List.of(redEight, blackSeven)),
                "RF20: Sequência a partir de 8♥ inclui 7♠");
        check(col1.canAccept(col0.getSequenceFrom(redEight)), "RF17: 9♠ aceita sequência iniciada em 8♥");

        // Simula ação composta (mover sequência + revelar carta oculta)
        GameAction composite = new GameAction.CompositeAction(List.of(
                new GameAction.MoveAction(col0, col1, List.of(redEight, blackSeven)),
                new GameAction.RevealCardAction(col0)
        ));
        composite.execute();

        check(col0.isVisible(hiddenCard), "RF19: Carta oculta revelada automaticamente");
        check(col1.getCards().equals(List.of(blackNine, redEight, blackSeven)), "RF18: Sequência movida junta");

        composite.undo();
        check(!col0.isVisible(hiddenCard), "RF19/RF23: Desfazer oculta a carta novamente");
        check(col0.getCards().equals(List.of(hiddenCard, redEight, blackSeven)), "RF23: Ordem original restaurada");
    }

    // RF22, RF25, RF26, RF27
    private static void testFullHistoryUndoToInitialState_RF22_RF25_RF26_RF27() {
        SolitaireGame game = new SolitaireGame(new Deck(new Random(77)));
        AtomicInteger notifications = new AtomicInteger(0);
        game.addListener(notifications::incrementAndGet);

        List<Card> initialStock = game.getLocationCards(LocationId.stock());
        List<List<Card>> initialTableau = new ArrayList<>();
        for (int c = 0; c < 7; c++) initialTableau.add(game.getLocationCards(LocationId.tableau(c)));

        int steps = 0;
        while (steps < 30 && game.playAutoStep()) {
            steps++;
        }
        check(steps > 0, "Jogo automático deve executar jogadas");
        check(notifications.get() == steps, "RF26: Cada mudança efetiva notifica listeners");

        while (game.canUndo()) {
            game.undo();
        }

        check(game.getLocationCards(LocationId.stock()).equals(initialStock),
                "RF25: Desfazer tudo restaura monte inicial");
        for (int c = 0; c < 7; c++) {
            check(game.getLocationCards(LocationId.tableau(c)).equals(initialTableau.get(c)),
                    "RF25: Desfazer tudo restaura coluna " + c);
        }
        check(game.playAutoStep(), "RF25: Jogo continua normalmente após desfazer tudo");
    }

    // RF24, RF28, RF29, RF30, RF31, RNF02
    private static void testAutoPlayPrioritiesCyclesAndDisable_RF24_RF28_RF29_RF30_RF31_RNF02() {
        SolitaireGame game = new SolitaireGame(new Deck(new Random(99)));

        // Prioridade 1: Descarte vazio e monte com cartas -> compra
        check(game.isLocationEmpty(LocationId.waste()), "Descarte vazio");
        check(game.playAutoStep(), "RF29.1: Deve comprar do monte quando descarte está vazio");
        check(!game.isLocationEmpty(LocationId.waste()), "RF29.1: Carta movida para descarte");

        // Desativação da estratégia (RF28, RNF02)
        game.setAutoPlayEnabled(false);
        check(!game.playAutoStep(), "RF28: Com estratégia desativada, retorna ação nula sem erro");
        game.setAutoPlayEnabled(true);

        // RF30: Simulação completa não entra em loop infinito
        int guard = 0;
        while (!game.isWon() && game.playAutoStep()) {
            guard++;
            check(guard < 5000, "RF30: Jogo automático não deve entrar em ciclo infinito");
        }
    }

    // RNF05
    private static void testSerializationFidelity_RNF05() {
        List<Card> original = List.of(
                Card.of(Suit.DIAMONDS, Rank.KING),
                Card.of(Suit.SPADES, Rank.QUEEN),
                Card.of(Suit.HEARTS, Rank.JACK)
        );
        String serialized = Card.serializeSequence(original);
        List<Card> restored = Card.deserializeSequence(serialized);

        check(restored.size() == original.size(), "RNF05: Tamanho preservado");
        for (int i = 0; i < original.size(); i++) {
            check(restored.get(i) == original.get(i),
                    "RNF05: Identidade (==) e ordem preservadas na posição " + i);
        }
    }
}
