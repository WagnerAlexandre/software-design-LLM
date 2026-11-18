"""Solitaire (Klondike) em Python.

Interface: Tkinter (biblioteca padrão do Python).
Execução: python solitaire.py

O motor do jogo não depende da interface. Regras, estratégia automática, ações,
undo, notificações e consultas somente-leitura ficam em classes próprias.
"""

from __future__ import annotations

import json
import random
import tkinter as tk
from dataclasses import dataclass
from enum import Enum, IntEnum
from tkinter import messagebox
from typing import Callable, Iterable, Optional, Sequence


# ---------------------------------------------------------------------------
# Cartas e baralho
# ---------------------------------------------------------------------------


class Suit(Enum):
    HEARTS = ("Copas", "♥", "red")
    DIAMONDS = ("Ouros", "♦", "red")
    CLUBS = ("Paus", "♣", "black")
    SPADES = ("Espadas", "♠", "black")

    @property
    def label(self) -> str:
        return self.value[0]

    @property
    def symbol(self) -> str:
        return self.value[1]

    @property
    def color(self) -> str:
        return self.value[2]


class Rank(IntEnum):
    ACE = 1
    TWO = 2
    THREE = 3
    FOUR = 4
    FIVE = 5
    SIX = 6
    SEVEN = 7
    EIGHT = 8
    NINE = 9
    TEN = 10
    JACK = 11
    QUEEN = 12
    KING = 13

    @property
    def label(self) -> str:
        return {
            Rank.ACE: "A",
            Rank.JACK: "J",
            Rank.QUEEN: "Q",
            Rank.KING: "K",
        }.get(self, str(int(self)))


@dataclass(eq=False)
class Card:
    """Uma carta é uma entidade única; a igualdade padrão é por identidade."""

    suit: Suit
    rank: Rank
    face_up: bool = False

    @property
    def uid(self) -> str:
        return f"{self.suit.name}:{int(self.rank)}"

    @property
    def color(self) -> str:
        return self.suit.color

    def __repr__(self) -> str:
        return f"{self.rank.label}{self.suit.symbol}"

    def to_dict(self) -> dict[str, object]:
        return {
            "id": self.uid,
            "suit": self.suit.name,
            "rank": int(self.rank),
            "face_up": self.face_up,
        }


class CardRegistry:
    """Resolve IDs para as mesmas instâncias de cartas do jogo.

    A serialização guarda IDs e ordem. A desserialização consulta este registro,
    de modo que não cria cópias das cartas.
    """

    def __init__(self, cards: Iterable[Card]):
        self._cards = {card.uid: card for card in cards}

    def serialize_sequence(self, cards: Sequence[Card]) -> str:
        payload = [card.to_dict() for card in cards]
        return json.dumps(payload, ensure_ascii=False)

    def deserialize_sequence(self, payload: str) -> tuple[Card, ...]:
        data = json.loads(payload)
        result: list[Card] = []
        for item in data:
            card_id = item["id"]
            if card_id not in self._cards:
                raise ValueError(f"Carta desconhecida no registro: {card_id}")
            card = self._cards[card_id]
            # Atualiza a visibilidade da entidade original, sem substituí-la.
            card.face_up = bool(item["face_up"])
            result.append(card)
        return tuple(result)

    def get(self, card_id: str) -> Card:
        return self._cards[card_id]


# ---------------------------------------------------------------------------
# Pilhas e locais
# ---------------------------------------------------------------------------


class PileKind(Enum):
    STOCK = "stock"
    WASTE = "waste"
    FOUNDATION = "foundation"
    TABLEAU = "tableau"


class Pile:
    """Pilha comum a monte, descarte, fundações e colunas.

    A ordem interna é da base para o topo. A interface externa recebe tuplas,
    portanto consultas não alteram a pilha.
    """

    def __init__(self, pile_id: str, label: str, kind: PileKind):
        self.id = pile_id
        self.label = label
        self.kind = kind
        self._cards: list[Card] = []

    @property
    def cards(self) -> tuple[Card, ...]:
        return tuple(self._cards)

    @property
    def is_empty(self) -> bool:
        return not self._cards

    @property
    def top(self) -> Optional[Card]:
        return self._cards[-1] if self._cards else None

    @property
    def size(self) -> int:
        return len(self._cards)

    def __len__(self) -> int:
        return len(self._cards)

    def __repr__(self) -> str:
        return f"Pile({self.id!r}, cards={self._cards!r})"

    # Operações internas: devem ser chamadas pelo motor ou por Actions.
    def _push(self, card: Card) -> None:
        self._cards.append(card)

    def _pop(self) -> Card:
        if not self._cards:
            raise IndexError(f"A pilha {self.id} está vazia")
        return self._cards.pop()

    def _remove_suffix(self, count: int) -> tuple[Card, ...]:
        if count < 1 or count > len(self._cards):
            raise IndexError("Quantidade inválida de cartas para remoção")
        result = tuple(self._cards[-count:])
        del self._cards[-count:]
        return result

    def _append_many(self, cards: Sequence[Card]) -> None:
        self._cards.extend(cards)


@dataclass(frozen=True)
class LocationInfo:
    pile: Pile
    index: int
    card: Card


# ---------------------------------------------------------------------------
# Ações reversíveis
# ---------------------------------------------------------------------------


class Action:
    """Contrato comum das ações do jogo."""

    @property
    def changed(self) -> bool:
        return True

    def execute(self) -> None:
        raise NotImplementedError

    def undo(self) -> None:
        raise NotImplementedError


class NoOpAction(Action):
    """Ação nula: representa uma decisão sem mudança de estado."""

    @property
    def changed(self) -> bool:
        return False

    def execute(self) -> None:
        return

    def undo(self) -> None:
        return


class TransferAction(Action):
    """Move um sufixo de uma pilha para outra, mantendo a ordem e identidade."""

    def __init__(self, source: Pile, destination: Pile, cards: Sequence[Card]):
        if not cards:
            raise ValueError("Uma transferência precisa conter cartas")
        self.source = source
        self.destination = destination
        self.cards = tuple(cards)
        self._executed = False

    def execute(self) -> None:
        if self._executed:
            raise RuntimeError("Esta ação já foi executada")
        if len(self.source._cards) < len(self.cards):
            raise RuntimeError("A pilha de origem não contém a sequência")
        if tuple(self.source._cards[-len(self.cards):]) != self.cards:
            raise RuntimeError("A sequência não está no topo da origem")
        moved = self.source._remove_suffix(len(self.cards))
        self.destination._append_many(moved)
        self._executed = True

    def undo(self) -> None:
        if not self._executed:
            raise RuntimeError("Não é possível desfazer uma ação não executada")
        if tuple(self.destination._cards[-len(self.cards):]) != self.cards:
            raise RuntimeError("O destino não contém a sequência esperada")
        del self.destination._cards[-len(self.cards):]
        self.source._append_many(self.cards)
        self._executed = False


class FlipAction(Action):
    """Altera a visibilidade de uma carta e guarda o estado anterior."""

    def __init__(self, card: Card, face_up: bool):
        self.card = card
        self.new_value = face_up
        self.old_value: Optional[bool] = None
        self._executed = False

    def execute(self) -> None:
        if self._executed:
            raise RuntimeError("Esta ação já foi executada")
        self.old_value = self.card.face_up
        self.card.face_up = self.new_value
        self._executed = True

    def undo(self) -> None:
        if not self._executed or self.old_value is None:
            raise RuntimeError("Não é possível desfazer esta revelação")
        self.card.face_up = self.old_value
        self._executed = False


class CompositeAction(Action):
    """Executa ações em ordem e desfaz na ordem inversa."""

    def __init__(self, actions: Sequence[Action], description: str = "Jogada"):
        self.actions = tuple(actions)
        self.description = description
        self._executed_count = 0

    @property
    def changed(self) -> bool:
        return any(action.changed for action in self.actions)

    def execute(self) -> None:
        if self._executed_count:
            raise RuntimeError("Esta ação composta já foi executada")
        try:
            for action in self.actions:
                action.execute()
                self._executed_count += 1
        except Exception:
            for action in reversed(self.actions[:self._executed_count]):
                action.undo()
            self._executed_count = 0
            raise

    def undo(self) -> None:
        if self._executed_count != len(self.actions):
            raise RuntimeError("Ação composta não está totalmente executada")
        for action in reversed(self.actions):
            action.undo()
        self._executed_count = 0


class RecycleWasteAction(Action):
    """Devolve o descarte ao monte na ordem correta para uma nova compra."""

    def __init__(self, stock: Pile, waste: Pile):
        self.stock = stock
        self.waste = waste
        self.moved: list[Card] = []
        self.old_visibility: dict[Card, bool] = {}
        self._executed = False

    def execute(self) -> None:
        if self._executed:
            raise RuntimeError("Esta ação já foi executada")
        if not self.stock.is_empty or self.waste.is_empty:
            raise RuntimeError("Não é possível reciclar neste estado")
        while not self.waste.is_empty:
            card = self.waste._pop()
            self.old_visibility[card] = card.face_up
            self.moved.append(card)
            card.face_up = False
            self.stock._push(card)
        self._executed = True

    def undo(self) -> None:
        if not self._executed:
            raise RuntimeError("Não é possível desfazer a reciclagem")
        # moved registra a ordem em que as cartas saíram do descarte.
        for card in reversed(self.moved):
            if self.stock._pop() is not card:
                raise RuntimeError("A ordem do monte foi alterada")
            card.face_up = self.old_visibility[card]
            self.waste._push(card)
        self.moved.clear()
        self.old_visibility.clear()
        self._executed = False


# ---------------------------------------------------------------------------
# Sugestões da estratégia automática
# ---------------------------------------------------------------------------


@dataclass(frozen=True)
class MoveSuggestion:
    kind: str
    source_id: Optional[str] = None
    destination_id: Optional[str] = None
    card_index: Optional[int] = None


# ---------------------------------------------------------------------------
# Motor do jogo: regras, histórico, consultas e notificações
# ---------------------------------------------------------------------------


class SolitaireGame:
    def __init__(self, seed: Optional[int] = None):
        self.stock = Pile("stock", "Monte", PileKind.STOCK)
        self.waste = Pile("waste", "Descarte", PileKind.WASTE)
        self.foundations: dict[Suit, Pile] = {
            suit: Pile(f"foundation:{suit.name}", f"Fundação {suit.label}", PileKind.FOUNDATION)
            for suit in Suit
        }
        self.tableau: list[Pile] = [
            Pile(f"tableau:{i}", f"Coluna {i + 1}", PileKind.TABLEAU)
            for i in range(7)
        ]
        self._all_cards = [Card(suit, rank) for suit in Suit for rank in Rank]
        self.registry = CardRegistry(self._all_cards)
        self.history: list[Action] = []
        self._observers: list[Callable[[dict[str, object]], None]] = []
        self.strategy = AutoStrategy()
        self.new_game(seed=seed)

    # ---- Eventos e estado geral -----------------------------------------

    def subscribe(self, callback: Callable[[dict[str, object]], None]) -> None:
        if callback not in self._observers:
            self._observers.append(callback)

    def unsubscribe(self, callback: Callable[[dict[str, object]], None]) -> None:
        if callback in self._observers:
            self._observers.remove(callback)

    def _notify(self, event_type: str, action: Optional[Action] = None) -> None:
        event = {
            "type": event_type,
            "action": action,
            "score": self.score,
            "won": self.has_won,
        }
        for callback in tuple(self._observers):
            callback(event)

    @property
    def score(self) -> int:
        return sum(pile.size for pile in self.foundations.values())

    @property
    def has_won(self) -> bool:
        return self.score == 52

    @property
    def can_undo(self) -> bool:
        return bool(self.history)

    def all_piles(self) -> tuple[Pile, ...]:
        return (
            self.stock,
            self.waste,
            *self.foundations.values(),
            *self.tableau,
        )

    def resolve_location(self, location_id: str) -> Optional[Pile]:
        for pile in self.all_piles():
            if pile.id == location_id:
                return pile
        return None

    def new_game(self, seed: Optional[int] = None) -> None:
        """Cria e distribui uma nova partida: 28 no tableau e 24 no monte."""
        for pile in self.all_piles():
            pile._cards.clear()
        self.history.clear()
        for card in self._all_cards:
            card.face_up = False

        deck = list(self._all_cards)
        rng = random.Random(seed)
        rng.shuffle(deck)

        # Distribuição em colunas de 1 a 7 cartas; só o topo fica visível.
        for column_index, column in enumerate(self.tableau):
            for row in range(column_index + 1):
                card = deck.pop()
                card.face_up = row == column_index
                column._push(card)

        # As 24 cartas restantes compõem o monte, todas fechadas.
        for card in deck:
            card.face_up = False
            self.stock._push(card)

        self._notify("new_game")
        self.assert_invariants()

    # ---- Consultas somente-leitura --------------------------------------

    def get_column(self, column_index: int) -> tuple[Card, ...]:
        return self.tableau[column_index].cards

    def get_sequence(self, column_index: int, card_index: int) -> tuple[Card, ...]:
        cards = self.tableau[column_index].cards
        if card_index < 0 or card_index >= len(cards):
            return ()
        return cards[card_index:]

    def is_card_visible(self, card: Card) -> bool:
        return card.face_up

    def is_visible_nearest_to_base(self, column_index: int, card_index: int) -> bool:
        cards = self.tableau[column_index].cards
        if card_index < 0 or card_index >= len(cards) or not cards[card_index].face_up:
            return False
        return card_index == 0 or not cards[card_index - 1].face_up

    def has_king_at_base(self, column_index: int) -> bool:
        cards = self.tableau[column_index].cards
        return bool(cards and cards[0].face_up and cards[0].rank == Rank.KING)

    def find_accessible_card(self, card: Card) -> Optional[LocationInfo]:
        """Localiza uma carta que pode iniciar uma movimentação."""
        for pile in (self.waste, *self.foundations.values()):
            if pile.top is card:
                return LocationInfo(pile, pile.size - 1, card)
        for pile in self.tableau:
            for index, current in enumerate(pile.cards):
                if current is card and current.face_up:
                    return LocationInfo(pile, index, current)
        return None

    def state_fingerprint(self) -> tuple[object, ...]:
        """Representação imutável útil para testes de consultas sem efeitos."""
        return tuple(
            (pile.id, tuple((card.uid, card.face_up) for card in pile.cards))
            for pile in self.all_piles()
        )

    # ---- Regras centralizadas -------------------------------------------

    @staticmethod
    def _valid_tableau_sequence(cards: Sequence[Card]) -> bool:
        if not cards or any(not card.face_up for card in cards):
            return False
        for lower, upper in zip(cards, cards[1:]):
            if lower.rank != upper.rank + 1 or lower.color == upper.color:
                return False
        return True

    @staticmethod
    def _accepts_foundation(destination: Pile, card: Card) -> bool:
        if destination.kind != PileKind.FOUNDATION:
            return False
        if destination.is_empty:
            return card.rank == Rank.ACE and destination.id == f"foundation:{card.suit.name}"
        top = destination.top
        return bool(
            top
            and top.suit == card.suit
            and int(card.rank) == int(top.rank) + 1
        )

    @staticmethod
    def _accepts_tableau(destination: Pile, first_card: Card) -> bool:
        if destination.kind != PileKind.TABLEAU:
            return False
        if destination.is_empty:
            return first_card.rank == Rank.KING
        top = destination.top
        return bool(
            top
            and top.face_up
            and int(first_card.rank) == int(top.rank) - 1
            and first_card.color != top.color
        )

    def can_move(
        self,
        source: Pile | str,
        destination: Pile | str,
        card_index: Optional[int] = None,
    ) -> bool:
        """Consulta a legalidade sem modificar o estado do jogo."""
        if isinstance(source, str):
            source = self.resolve_location(source)  # type: ignore[assignment]
        if isinstance(destination, str):
            destination = self.resolve_location(destination)  # type: ignore[assignment]
        if source is None or destination is None or source is destination:
            return False
        if destination.kind not in (PileKind.TABLEAU, PileKind.FOUNDATION):
            return False
        if source.kind in (PileKind.STOCK, PileKind.WASTE) and source.kind == PileKind.STOCK:
            return False
        if source.is_empty:
            return False

        if source.kind == PileKind.TABLEAU:
            if card_index is None or not (0 <= card_index < source.size):
                return False
            sequence = source.cards[card_index:]
            if not self._valid_tableau_sequence(sequence):
                return False
        else:
            # Descarte e fundação só permitem movimentar a carta do topo.
            if source.kind not in (PileKind.WASTE, PileKind.FOUNDATION):
                return False
            if card_index is not None and card_index != source.size - 1:
                return False
            sequence = (source.top,)  # type: ignore[assignment]
            if sequence[0] is None or not sequence[0].face_up:
                return False

        if not sequence:
            return False
        if destination.kind == PileKind.FOUNDATION:
            return len(sequence) == 1 and self._accepts_foundation(destination, sequence[0])
        return self._accepts_tableau(destination, sequence[0])

    def move(
        self,
        source: Pile | str,
        destination: Pile | str,
        card_index: Optional[int] = None,
    ) -> Action:
        """Valida e executa uma movimentação solicitada pelo jogador/estratégia."""
        if isinstance(source, str):
            source = self.resolve_location(source)  # type: ignore[assignment]
        if isinstance(destination, str):
            destination = self.resolve_location(destination)  # type: ignore[assignment]
        if source is None or destination is None:
            return NoOpAction()
        if not self.can_move(source, destination, card_index):
            return NoOpAction()

        if source.kind == PileKind.TABLEAU:
            assert card_index is not None
            sequence = source.cards[card_index:]
        else:
            sequence = (source.top,)
        # can_move assegura que a sequência não contém None.
        cards = tuple(card for card in sequence if card is not None)
        actions: list[Action] = [TransferAction(source, destination, cards)]

        # Ao retirar a sequência, revela automaticamente a carta que ficou no topo.
        if source.kind == PileKind.TABLEAU and card_index is not None and card_index > 0:
            card_below = source.cards[card_index - 1]
            if not card_below.face_up:
                actions.append(FlipAction(card_below, True))

        return self._commit(CompositeAction(actions, "Mover cartas"))

    # ---- Compra, reciclagem e histórico ---------------------------------

    def draw_or_recycle(self) -> Action:
        """A mesma ação do monte compra ou recicla quando o monte está vazio."""
        if not self.stock.is_empty:
            card = self.stock.top
            assert card is not None
            action = CompositeAction(
                [
                    TransferAction(self.stock, self.waste, (card,)),
                    FlipAction(card, True),
                ],
                "Comprar carta",
            )
            return self._commit(action)
        if not self.waste.is_empty:
            return self._commit( RecycleWasteAction(self.stock, self.waste) )
        return NoOpAction()

    def _commit(self, action: Action) -> Action:
        if not action.changed:
            return action
        action.execute()
        self.history.append(action)
        self._notify("execute", action)
        self.assert_invariants()
        return action

    def undo(self) -> bool:
        if not self.history:
            return False
        action = self.history.pop()
        action.undo()
        self._notify("undo", action)
        self.assert_invariants()
        return True

    def undo_all(self) -> None:
        while self.undo():
            pass

    # ---- Estratégia automática: decisão separada da execução ------------

    def execute_suggestion(self, suggestion: MoveSuggestion | NoOpAction) -> Action:
        """Executa uma sugestão após revalidá-la pelas regras centrais."""
        if isinstance(suggestion, NoOpAction):
            return suggestion
        if suggestion.kind == "draw":
            # A estratégia só sugere compra; reciclagem é ação explícita do jogador.
            if not self.stock.is_empty:
                return self.draw_or_recycle()
            return NoOpAction()
        if suggestion.kind == "move":
            source = self.resolve_location(suggestion.source_id or "")
            destination = self.resolve_location(suggestion.destination_id or "")
            if source is None or destination is None:
                return NoOpAction()
            return self.move(source, destination, suggestion.card_index)
        return NoOpAction()

    def auto_step(self) -> Action:
        suggestion = self.strategy.choose(self)
        return self.execute_suggestion(suggestion)

    # ---- Verificação de consistência ------------------------------------

    def assert_invariants(self) -> None:
        cards = [card for pile in self.all_piles() for card in pile.cards]
        if len(cards) != 52:
            raise AssertionError(f"Esperadas 52 cartas nas pilhas; encontradas {len(cards)}")
        if len({id(card) for card in cards}) != 52:
            raise AssertionError("Uma carta está repetida ou ausente entre as pilhas")
        if {card.uid for card in cards} != {card.uid for card in self._all_cards}:
            raise AssertionError("O conjunto de cartas não corresponde ao baralho")
        if any(card.face_up for card in self.stock.cards):
            raise AssertionError("Todas as cartas do monte devem ficar fechadas")
        if any(not card.face_up for card in self.waste.cards):
            raise AssertionError("Todas as cartas do descarte devem ficar visíveis")
        for pile in self.foundations.values():
            if any(not card.face_up for card in pile.cards):
                raise AssertionError("Cartas nas fundações devem ficar visíveis")
        for column in self.tableau:
            seen_face_up = False
            for card in column.cards:
                if card.face_up:
                    seen_face_up = True
                elif seen_face_up:
                    raise AssertionError("Há carta fechada acima de carta visível")


class AutoStrategy:
    """Estratégia determinística que apenas consulta o estado do jogo."""

    def __init__(self, enabled: bool = True):
        self.enabled = enabled

    def choose(self, game: SolitaireGame) -> MoveSuggestion | NoOpAction:
        if not self.enabled:
            return NoOpAction()

        # 1. Comprar se o descarte está vazio e o monte possui cartas.
        if game.waste.is_empty and not game.stock.is_empty:
            return MoveSuggestion("draw")

        # 2. Descarte -> fundação.
        waste_top = game.waste.top
        if waste_top is not None:
            foundation = game.foundations[waste_top.suit]
            if game.can_move(game.waste, foundation):
                return MoveSuggestion("move", game.waste.id, foundation.id)

        # 3. Descarte -> tableau.
        if waste_top is not None:
            for column in game.tableau:
                if game.can_move(game.waste, column):
                    return MoveSuggestion("move", game.waste.id, column.id)

        # 4. Topo de coluna -> fundação.
        for column in game.tableau:
            if column.top is None:
                continue
            foundation = game.foundations[column.top.suit]
            if game.can_move(column, foundation, column.size - 1):
                return MoveSuggestion("move", column.id, foundation.id, column.size - 1)

        # 5. Tableau -> tableau, apenas se revelar carta ou esvaziar coluna.
        # Primeiro procura movimentos que revelam uma carta oculta.
        for source in game.tableau:
            for index, card in enumerate(source.cards):
                if not card.face_up:
                    continue
                reveals_hidden = index > 0 and not source.cards[index - 1].face_up
                if not reveals_hidden:
                    continue
                for destination in game.tableau:
                    if destination is source:
                        continue
                    if game.can_move(source, destination, index):
                        return MoveSuggestion("move", source.id, destination.id, index)

        # Em seguida, movimentos que deixam uma coluna vazia de verdade.
        for source in game.tableau:
            if source.is_empty:
                continue
            # A sequência precisa começar na base para esvaziar a coluna.
            # Se o Rei já ocupa a base e o destino também está vazio, seria
            # apenas transferir a coluna vazia de lugar, sem progresso.
            for destination in game.tableau:
                if destination is source or destination.is_empty:
                    continue
                if game.can_move(source, destination, 0):
                    return MoveSuggestion("move", source.id, destination.id, 0)

        # 6. Comprar se nada anterior for possível e ainda houver cartas.
        if not game.stock.is_empty:
            return MoveSuggestion("draw")

        # 7. Nenhuma jogada possível.
        return NoOpAction()


# ---------------------------------------------------------------------------
# Simulação automática sem interface (útil para estatísticas)
# ---------------------------------------------------------------------------


def simulate_auto_games(
    number_of_games: int,
    max_steps_per_game: int = 5000,
    seed: Optional[int] = None,
) -> dict[str, object]:
    """Executa partidas automáticas e retorna estatísticas, sem abrir Tkinter."""
    if number_of_games < 1:
        raise ValueError("number_of_games deve ser pelo menos 1")
    rng = random.Random(seed)
    scores: list[int] = []
    wins = 0
    steps: list[int] = []
    for _ in range(number_of_games):
        game = SolitaireGame(seed=rng.randrange(0, 2**32))
        game.strategy.enabled = True
        count = 0
        while count < max_steps_per_game and not game.has_won:
            action = game.auto_step()
            if not action.changed:
                break
            count += 1
        scores.append(game.score)
        steps.append(count)
        if game.has_won:
            wins += 1
    return {
        "partidas": number_of_games,
        "vitorias": wins,
        "taxa_vitoria": wins / number_of_games,
        "pontuacao_media": sum(scores) / number_of_games,
        "passos_medios": sum(steps) / number_of_games,
        "pontuacoes": scores,
    }


# ---------------------------------------------------------------------------
# Interface gráfica: Tkinter; não contém regras de legalidade
# ---------------------------------------------------------------------------


class SolitaireUI:
    CARD_W = 82
    CARD_H = 112
    BOARD_W = 1120
    BOARD_H = 760
    TABLEAU_Y = 205
    TABLEAU_X = 25
    COLUMN_GAP = 155
    HIDDEN_GAP = 24
    FACE_UP_GAP = 31

    FELT = "#176b48"
    SLOT = "#0f563b"
    WHITE = "#fffdf8"
    RED = "#c62828"
    BLACK = "#1e2329"
    GOLD = "#e3c66b"

    def __init__(self, root: tk.Tk, game: Optional[SolitaireGame] = None):
        self.root = root
        self.game = game or SolitaireGame()
        self.game.subscribe(self._on_game_event)

        self.root.title("Solitaire — Klondike")
        self.root.configure(bg=self.FELT)
        self.root.resizable(False, False)

        toolbar = tk.Frame(root, bg="#123f30", padx=10, pady=8)
        toolbar.pack(fill="x")

        tk.Button(
            toolbar, text="Nova partida", command=self.new_game,
            bg="#e8d8a5", fg="#202820", relief="flat", padx=12, pady=5,
        ).pack(side="left", padx=(0, 8))
        self.undo_button = tk.Button(
            toolbar, text="Desfazer", command=self.undo,
            bg="#e8d8a5", fg="#202820", relief="flat", padx=12, pady=5,
        )
        self.undo_button.pack(side="left", padx=(0, 8))
        self.auto_button = tk.Button(
            toolbar, text="Jogada automática", command=self.auto_step,
            bg="#d5e8da", fg="#202820", relief="flat", padx=12, pady=5,
        )
        self.auto_button.pack(side="left", padx=(0, 12))

        self.auto_var = tk.BooleanVar(value=False)
        self.auto_check = tk.Checkbutton(
            toolbar, text="Jogo automático contínuo", variable=self.auto_var,
            command=self.toggle_auto, bg="#123f30", fg="white",
            activebackground="#123f30", activeforeground="white",
            selectcolor="#176b48", font=("Arial", 10),
        )
        self.auto_check.pack(side="left")

        self.status_var = tk.StringVar(value="Arraste uma carta ou sequência para uma pilha válida. Clique no monte para comprar.")
        tk.Label(
            root, textvariable=self.status_var, anchor="w", bg="#123f30",
            fg="white", padx=12, pady=6, font=("Arial", 9),
        ).pack(fill="x")

        self.canvas = tk.Canvas(
            root, width=self.BOARD_W, height=self.BOARD_H,
            bg=self.FELT, highlightthickness=0,
        )
        self.canvas.pack()
        self.canvas.bind("<ButtonPress-1>", self.on_press)
        self.canvas.bind("<B1-Motion>", self.on_drag)
        self.canvas.bind("<ButtonRelease-1>", self.on_release)
        self.canvas.bind("<Double-Button-1>", self.on_double_click)

        self.drag: Optional[dict[str, object]] = None
        self.auto_after_id: Optional[str] = None
        self._win_dialog_shown = False
        self.render()

    # ---- Coordenadas e desenho -----------------------------------------

    def _tableau_x(self, column_index: int) -> int:
        return self.TABLEAU_X + column_index * self.COLUMN_GAP

    def _pile_positions(self, pile: Pile) -> list[int]:
        positions: list[int] = []
        y = self.TABLEAU_Y
        cards = pile.cards
        for index, card in enumerate(cards):
            positions.append(y)
            if index < len(cards) - 1:
                y += self.FACE_UP_GAP if card.face_up else self.HIDDEN_GAP
        return positions

    def _draw_slot(self, x: int, y: int, label: str = "") -> None:
        self.canvas.create_rectangle(
            x, y, x + self.CARD_W, y + self.CARD_H,
            outline="#a9c9b7", width=2, dash=(5, 4), fill=self.SLOT,
        )
        if label:
            self.canvas.create_text(
                x + self.CARD_W / 2, y + self.CARD_H / 2,
                text=label, fill="#b7d3c3", font=("Arial", 9),
            )

    def _draw_card(self, card: Card, x: int, y: int, *, ghost: bool = False) -> None:
        if card.face_up:
            fill = self.WHITE
            outline = self.GOLD if ghost else "#263d32"
            self.canvas.create_rectangle(
                x, y, x + self.CARD_W, y + self.CARD_H,
                fill=fill, outline=outline, width=3 if ghost else 1,
            )
            color = self.RED if card.color == "red" else self.BLACK
            rank = card.rank.label
            suit = card.suit.symbol
            self.canvas.create_text(
                x + 8, y + 8, anchor="nw", text=f"{rank}\n{suit}",
                fill=color, font=("Arial", 13, "bold"), justify="left",
            )
            self.canvas.create_text(
                x + self.CARD_W / 2, y + self.CARD_H / 2,
                text=suit, fill=color, font=("Arial", 31),
            )
            self.canvas.create_text(
                x + self.CARD_W - 8, y + self.CARD_H - 8,
                anchor="se", text=rank, fill=color,
                font=("Arial", 11, "bold"),
            )
        else:
            self.canvas.create_rectangle(
                x, y, x + self.CARD_W, y + self.CARD_H,
                fill="#244d83", outline="#e9e3d1", width=2,
            )
            self.canvas.create_rectangle(
                x + 6, y + 6, x + self.CARD_W - 6, y + self.CARD_H - 6,
                outline="#b9cce4", width=1,
            )
            self.canvas.create_text(
                x + self.CARD_W / 2, y + self.CARD_H / 2,
                text="◆", fill="#b9cce4", font=("Arial", 27),
            )

    def render(self) -> None:
        self.canvas.delete("all")
        # Slots superiores: monte, descarte e quatro fundações.
        self._draw_slot(25, 28, "MONTE")
        self._draw_slot(130, 28, "DESC.")
        foundation_x = {
            Suit.HEARTS: 620,
            Suit.DIAMONDS: 725,
            Suit.CLUBS: 830,
            Suit.SPADES: 935,
        }
        for suit, pile in self.game.foundations.items():
            x = foundation_x[suit]
            self._draw_slot(x, 28, suit.symbol)
            if pile.top is not None:
                if not (self.drag and self.drag.get("source") is pile and self.drag.get("kind") == "single"):
                    self._draw_card(pile.top, x, 28)

        if self.game.stock.top is not None:
            self._draw_card(self.game.stock.top, 25, 28)
        elif not self.game.waste.is_empty:
            self.canvas.create_text(
                66, 151, text="↻", fill="#e8dfc5", font=("Arial", 16, "bold")
            )
        if self.game.waste.top is not None:
            if not (self.drag and self.drag.get("source") is self.game.waste):
                self._draw_card(self.game.waste.top, 130, 28)

        # Colunas do tableau.
        for col_index, column in enumerate(self.game.tableau):
            x = self._tableau_x(col_index)
            self._draw_slot(x, self.TABLEAU_Y, str(col_index + 1))
            positions = self._pile_positions(column)
            for card_index, (card, y) in enumerate(zip(column.cards, positions)):
                if (
                    self.drag
                    and self.drag.get("source") is column
                    and isinstance(self.drag.get("card_index"), int)
                    and card_index >= int(self.drag["card_index"])
                ):
                    continue
                self._draw_card(card, x, y)

        # Indicação do destino durante o arraste consulta o motor do jogo.
        if self.drag:
            pointer_x = int(self.drag.get("pointer_x", 0))
            pointer_y = int(self.drag.get("pointer_y", 0))
            destination = self._hit_destination(pointer_x, pointer_y)
            if destination is not None:
                source = self.drag["source"]
                card_index = self.drag.get("card_index")
                legal = self.game.can_move(source, destination, card_index)  # type: ignore[arg-type]
                color = "#75ef9c" if legal else "#ff7777"
                self._draw_destination_outline(destination, color, width=4)
            self._draw_drag_ghost()

        self.undo_button.configure(state="normal" if self.game.can_undo else "disabled")
        self.auto_button.configure(state="normal" if self.game.strategy.enabled else "disabled")
        self.canvas.update_idletasks()

    def _draw_destination_outline(self, destination: Pile, color: str, width: int = 3) -> None:
        if destination.kind == PileKind.TABLEAU:
            index = self.game.tableau.index(destination)
            x, y = self._tableau_x(index), self.TABLEAU_Y
        elif destination.kind == PileKind.FOUNDATION:
            suit = next(s for s, pile in self.game.foundations.items() if pile is destination)
            x = {
                Suit.HEARTS: 620,
                Suit.DIAMONDS: 725,
                Suit.CLUBS: 830,
                Suit.SPADES: 935,
            }[suit]
            y = 28
        else:
            return
        self.canvas.create_rectangle(
            x - 2, y - 2, x + self.CARD_W + 2, y + self.CARD_H + 2,
            outline=color, width=width,
        )

    def _draw_drag_ghost(self) -> None:
        assert self.drag is not None
        cards = self.drag["cards"]
        x = int(self.drag["pointer_x"]) - int(self.drag["grab_dx"])
        y = int(self.drag["pointer_y"]) - int(self.drag["grab_dy"])
        for index, card in enumerate(cards):  # type: ignore[union-attr]
            self._draw_card(card, x, y + index * self.FACE_UP_GAP, ghost=True)

    # ---- Identificação de origem/destino -------------------------------

    def _hit_tableau_card(self, x: int, y: int) -> Optional[tuple[Pile, int, int]]:
        if y < self.TABLEAU_Y:
            return None
        for col_index, column in enumerate(self.game.tableau):
            col_x = self._tableau_x(col_index)
            if not (col_x <= x <= col_x + self.CARD_W):
                continue
            positions = self._pile_positions(column)
            for index, card_y in enumerate(positions):
                if index + 1 < len(positions):
                    visible_height = positions[index + 1] - card_y
                else:
                    visible_height = self.CARD_H
                if card_y <= y < card_y + visible_height:
                    if column.cards[index].face_up:
                        return column, index, card_y
                    return None
            # Espaço abaixo da última carta de uma coluna vazia ou curta.
            if column.is_empty and y <= self.TABLEAU_Y + self.CARD_H:
                return column, 0, self.TABLEAU_Y
        return None

    def _hit_destination(self, x: int, y: int) -> Optional[Pile]:
        if y >= self.TABLEAU_Y - 25:
            for col_index, column in enumerate(self.game.tableau):
                col_x = self._tableau_x(col_index)
                if col_x - 10 <= x <= col_x + self.CARD_W + 10:
                    return column
        if 15 <= y <= 155:
            for suit, pile in self.game.foundations.items():
                fx = {
                    Suit.HEARTS: 620,
                    Suit.DIAMONDS: 725,
                    Suit.CLUBS: 830,
                    Suit.SPADES: 935,
                }[suit]
                if fx - 8 <= x <= fx + self.CARD_W + 8:
                    return pile
        return None

    def _source_at(self, x: int, y: int) -> Optional[tuple[Pile, Optional[int], tuple[Card, ...], int, int]]:
        # Descarte: somente a carta do topo é acessível.
        if 130 <= x <= 130 + self.CARD_W and 28 <= y <= 28 + self.CARD_H:
            if self.game.waste.top is not None:
                return self.game.waste, None, (self.game.waste.top,), 130, 28  # type: ignore[arg-type]

        # Fundações: somente o topo.
        for suit, pile in self.game.foundations.items():
            fx = {
                Suit.HEARTS: 620,
                Suit.DIAMONDS: 725,
                Suit.CLUBS: 830,
                Suit.SPADES: 935,
            }[suit]
            if fx <= x <= fx + self.CARD_W and 28 <= y <= 28 + self.CARD_H:
                if pile.top is not None:
                    return pile, None, (pile.top,), fx, 28  # type: ignore[arg-type]

        tableau_hit = self._hit_tableau_card(x, y)
        if tableau_hit:
            pile, index, card_y = tableau_hit
            cards = pile.cards[index:]
            return pile, index, cards, self._tableau_x(self.game.tableau.index(pile)), card_y
        return None

    # ---- Eventos do mouse ----------------------------------------------

    def on_press(self, event: tk.Event) -> None:
        x, y = event.x, event.y
        # Clique no monte: compra ou, quando vazio, recicla o descarte.
        if 25 <= x <= 25 + self.CARD_W and 28 <= y <= 28 + self.CARD_H:
            self.game.draw_or_recycle()
            return

        source_info = self._source_at(x, y)
        if source_info is None:
            return
        source, card_index, cards, card_x, card_y = source_info
        if not cards:
            return
        if source.kind == PileKind.TABLEAU and card_index is not None:
            kind = "sequence"
        else:
            kind = "single"
        self.drag = {
            "source": source,
            "card_index": card_index,
            "cards": cards,
            "kind": kind,
            "grab_dx": x - card_x,
            "grab_dy": y - card_y,
            "pointer_x": x,
            "pointer_y": y,
            "start_x": x,
            "start_y": y,
        }
        self.render()

    def on_drag(self, event: tk.Event) -> None:
        if not self.drag:
            return
        self.drag["pointer_x"] = event.x
        self.drag["pointer_y"] = event.y
        self.render()

    def on_release(self, event: tk.Event) -> None:
        if not self.drag:
            return
        drag = self.drag
        destination = self._hit_destination(event.x, event.y)
        source = drag["source"]
        card_index = drag.get("card_index")
        # move() revalida a jogada; a interface não decide a legalidade.
        if destination is not None:
            self.game.move(source, destination, card_index)  # type: ignore[arg-type]
        self.drag = None
        self.render()

    def on_double_click(self, event: tk.Event) -> None:
        # Atalho opcional: duplo clique tenta enviar a carta selecionada à fundação.
        source_info = self._source_at(event.x, event.y)
        if source_info is None:
            return
        source, card_index, cards, _x, _y = source_info
        if len(cards) != 1:
            return
        card = cards[0]
        destination = self.game.foundations[card.suit]
        self.game.move(source, destination, card_index)

    # ---- Controles ------------------------------------------------------

    def new_game(self) -> None:
        self._cancel_auto_timer()
        self.drag = None
        self._win_dialog_shown = False
        self.game.new_game()
        if self.game.strategy.enabled and self.auto_var.get():
            self._schedule_auto()

    def undo(self) -> None:
        self.game.undo()

    def auto_step(self) -> None:
        if not self.game.strategy.enabled:
            return
        action = self.game.auto_step()
        if not action.changed:
            self.status_var.set("A estratégia não encontrou uma jogada automática.")

    def toggle_auto(self) -> None:
        enabled = bool(self.auto_var.get())
        self.game.strategy.enabled = enabled
        self._cancel_auto_timer()
        self.render()
        if enabled:
            self.status_var.set("Jogo automático ativado.")
            self._schedule_auto()
        else:
            self.status_var.set("Jogo automático desativado.")

    def _schedule_auto(self) -> None:
        if self.auto_var.get() and self.game.strategy.enabled and self.auto_after_id is None:
            self.auto_after_id = self.root.after(650, self._auto_tick)

    def _cancel_auto_timer(self) -> None:
        if self.auto_after_id is not None:
            try:
                self.root.after_cancel(self.auto_after_id)
            except tk.TclError:
                pass
            self.auto_after_id = None

    def _auto_tick(self) -> None:
        self.auto_after_id = None
        if not self.auto_var.get() or not self.game.strategy.enabled:
            return
        action = self.game.auto_step()
        if action.changed and not self.game.has_won:
            self._schedule_auto()
        elif not action.changed:
            self.status_var.set("A estratégia não encontrou mais jogadas. Você pode continuar manualmente.")

    def _on_game_event(self, event: dict[str, object]) -> None:
        self.render()
        if event.get("won") and not self._win_dialog_shown:
            self._win_dialog_shown = True
            self._cancel_auto_timer()
            self.status_var.set("Você venceu! Todas as 52 cartas estão nas fundações.")
            messagebox.showinfo("Solitaire", "Parabéns! Você venceu a partida!")


def main() -> None:
    root = tk.Tk()
    SolitaireUI(root)
    root.mainloop()


if __name__ == "__main__":
    main()
