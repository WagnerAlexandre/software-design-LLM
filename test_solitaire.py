"""Testes automatizados do motor de Solitaire: python -m unittest -v"""
import unittest

from solitaire import (
    AutoStrategy,
    CardRegistry,
    MoveSuggestion,
    NoOpAction,
    PileKind,
    Rank,
    SolitaireGame,
    Suit,
)


class SolitaireEngineTests(unittest.TestCase):
    def test_initial_deal_has_28_tableau_and_24_stock(self):
        game = SolitaireGame(seed=123)
        self.assertEqual(sum(p.size for p in game.tableau), 28)
        self.assertEqual(game.stock.size, 24)
        self.assertTrue(game.waste.is_empty)
        self.assertEqual(game.score, 0)
        self.assertEqual([p.size for p in game.tableau], [1, 2, 3, 4, 5, 6, 7])
        for column in game.tableau:
            self.assertEqual(sum(card.face_up for card in column.cards), 1)
        game.assert_invariants()

    def test_deck_has_52_unique_card_entities(self):
        game = SolitaireGame(seed=10)
        cards = [card for pile in game.all_piles() for card in pile.cards]
        self.assertEqual(len(cards), 52)
        self.assertEqual(len({id(card) for card in cards}), 52)
        self.assertEqual(len({card.uid for card in cards}), 52)

    def test_draw_and_undo_restore_exact_state(self):
        game = SolitaireGame(seed=77)
        before = game.state_fingerprint()
        stock_top = game.stock.top
        action = game.draw_or_recycle()
        self.assertTrue(action.changed)
        self.assertIs(game.waste.top, stock_top)
        self.assertTrue(stock_top.face_up)
        self.assertEqual(game.stock.size, 23)
        self.assertTrue(game.undo())
        self.assertEqual(game.state_fingerprint(), before)
        self.assertIs(game.stock.top, stock_top)

    def test_recycle_and_undo_preserve_order_identity_and_visibility(self):
        game = SolitaireGame(seed=9)
        for _ in range(24):
            game.draw_or_recycle()
        self.assertTrue(game.stock.is_empty)
        original_waste = game.waste.cards
        self.assertEqual(len(original_waste), 24)
        recycled = game.draw_or_recycle()
        self.assertTrue(recycled.changed)
        self.assertEqual(game.stock.size, 24)
        self.assertTrue(game.waste.is_empty)
        # A nova compra deve devolver a carta que estava na base do descarte.
        expected_next = original_waste[0]
        game.draw_or_recycle()
        self.assertIs(game.waste.top, expected_next)
        self.assertTrue(game.undo())  # desfaz a compra
        self.assertTrue(game.undo())  # desfaz a reciclagem
        self.assertEqual(game.waste.cards, original_waste)
        self.assertTrue(all(card.face_up for card in game.waste.cards))
        self.assertTrue(game.stock.is_empty)
        game.assert_invariants()

    def test_serialization_keeps_same_entities_and_sequence_order(self):
        game = SolitaireGame(seed=321)
        sequence = game.tableau[6].cards[-3:]
        payload = game.registry.serialize_sequence(sequence)
        restored = game.registry.deserialize_sequence(payload)
        self.assertEqual(restored, sequence)
        for a, b in zip(restored, sequence):
            self.assertIs(a, b)

    def test_read_only_strategy_decision_does_not_change_state(self):
        game = SolitaireGame(seed=99)
        before = game.state_fingerprint()
        suggestion = game.strategy.choose(game)
        self.assertIsInstance(suggestion, MoveSuggestion)
        self.assertEqual(suggestion.kind, "draw")
        self.assertEqual(game.state_fingerprint(), before)

    def test_disabled_strategy_returns_noop(self):
        game = SolitaireGame(seed=1)
        game.strategy.enabled = False
        before = game.state_fingerprint()
        result = game.auto_step()
        self.assertIsInstance(result, NoOpAction)
        self.assertFalse(result.changed)
        self.assertEqual(game.state_fingerprint(), before)
        self.assertFalse(game.can_undo)

    def test_undo_all_returns_to_initial_state_after_many_draws(self):
        game = SolitaireGame(seed=5)
        initial = game.state_fingerprint()
        for _ in range(12):
            game.draw_or_recycle()
        game.undo_all()
        self.assertEqual(game.state_fingerprint(), initial)
        self.assertFalse(game.can_undo)


    def test_tableau_sequence_move_reveals_card_and_undo_restores_it(self):
        game = SolitaireGame(seed=12)
        # Monta um estado controlado mantendo as 52 cartas originais e únicas.
        for pile in game.all_piles():
            pile._cards.clear()
        for card in game._all_cards:
            card.face_up = False

        hidden = game.registry.get("DIAMONDS:2")
        eight_hearts = game.registry.get("HEARTS:8")
        seven_clubs = game.registry.get("CLUBS:7")
        six_hearts = game.registry.get("HEARTS:6")
        nine_spades = game.registry.get("SPADES:9")
        hidden.face_up = False
        eight_hearts.face_up = True
        seven_clubs.face_up = True
        six_hearts.face_up = True
        nine_spades.face_up = True
        game.tableau[0]._append_many((hidden, eight_hearts, seven_clubs, six_hearts))
        game.tableau[1]._push(nine_spades)

        used = {hidden, eight_hearts, seven_clubs, six_hearts, nine_spades}
        for card in game._all_cards:
            if card not in used:
                card.face_up = False
                game.stock._push(card)
        game.history.clear()
        game.assert_invariants()
        before = game.state_fingerprint()

        # Move o 8, 7 e 6 como uma única sequência para cima do 9.
        self.assertTrue(game.can_move(game.tableau[0], game.tableau[1], 1))
        action = game.move(game.tableau[0], game.tableau[1], 1)
        self.assertTrue(action.changed)
        self.assertEqual(game.tableau[1].cards, (nine_spades, eight_hearts, seven_clubs, six_hearts))
        self.assertEqual(game.tableau[0].cards, (hidden,))
        self.assertTrue(hidden.face_up)

        self.assertTrue(game.undo())
        self.assertEqual(game.state_fingerprint(), before)
        self.assertFalse(hidden.face_up)

    def test_ace_can_move_to_its_empty_foundation_and_undo(self):
        game = SolitaireGame(seed=13)
        for pile in game.all_piles():
            pile._cards.clear()
        for card in game._all_cards:
            card.face_up = False
        ace = game.registry.get("HEARTS:1")
        ace.face_up = True
        game.waste._push(ace)
        for card in game._all_cards:
            if card is not ace:
                card.face_up = False
                game.stock._push(card)
        game.history.clear()
        game.assert_invariants()

        foundation = game.foundations[Suit.HEARTS]
        self.assertTrue(game.can_move(game.waste, foundation))
        self.assertFalse(game.can_move(game.waste, game.foundations[Suit.SPADES]))
        game.move(game.waste, foundation)
        self.assertIs(foundation.top, ace)
        self.assertEqual(game.score, 1)
        self.assertTrue(game.undo())
        self.assertIs(game.waste.top, ace)
        self.assertEqual(game.score, 0)

    def test_foundation_rule_accepts_only_matching_ace_on_empty_pile(self):
        game = SolitaireGame(seed=8)
        # Consulta de destino vazia: não altera o jogo.
        hearts_foundation = game.foundations[Suit.HEARTS]
        self.assertTrue(hearts_foundation.is_empty)
        self.assertFalse(game.can_move(game.stock, hearts_foundation))
        self.assertEqual(hearts_foundation.kind, PileKind.FOUNDATION)


if __name__ == "__main__":
    unittest.main()
