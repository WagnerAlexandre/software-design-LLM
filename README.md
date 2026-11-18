# Solitaire (Klondike) em Python

Implementação de Solitaire com interface gráfica em Tkinter e motor separado da apresentação.

## Requisitos

- Python 3.10 ou superior.
- Tkinter, normalmente incluído na instalação do Python.
- No Linux, caso Tkinter não esteja instalado, instale o pacote `python3-tk` da sua distribuição.

## Executar

No terminal, dentro desta pasta:

```bash
python solitaire.py
```

## Como jogar

- **Monte:** clique para comprar uma carta. Quando o monte estiver vazio, clique novamente para reciclar o descarte.
- **Movimentar:** arraste uma carta visível ou uma sequência do tableau até uma coluna ou fundação.
- **Duplo clique:** tenta enviar a carta selecionada para a fundação correspondente.
- **Desfazer:** desfaz a última jogada, incluindo compras, reciclagem, movimentações e revelações.
- **Jogada automática:** executa uma jogada escolhida pela estratégia.
- **Jogo automático contínuo:** ativa/desativa a execução automática em sequência.
- **Nova partida:** embaralha e distribui um novo jogo.

A pontuação não é exibida; internamente, corresponde à quantidade de cartas nas fundações. A vitória ocorre quando ela chega a 52.

## Testes

Execute os testes do motor sem abrir a interface gráfica:

```bash
python -m unittest -v
```

## Simulações automáticas

O módulo também oferece `simulate_auto_games`, para executar partidas automáticas e obter estatísticas. Exemplo no interpretador Python:

```python
from solitaire import simulate_auto_games

resultado = simulate_auto_games(number_of_games=100, seed=42)
print(resultado)
```

## Organização interna

- `Card`, `Suit`, `Rank` e `CardRegistry`: identidade, ranks, naipes e serialização por ID.
- `Pile`: estrutura única usada por monte, descarte, fundações e tableau.
- `SolitaireGame`: regras centralizadas, consultas somente-leitura, validação, histórico, notificações e execução.
- `Action`, `TransferAction`, `FlipAction`, `CompositeAction`, `RecycleWasteAction` e `NoOpAction`: ações reversíveis.
- `AutoStrategy`: decide usando consultas do jogo; não modifica o estado. `SolitaireGame.execute_suggestion` revalida e executa a sugestão.
- `SolitaireUI`: interface Tkinter e eventos de mouse. A legalidade de movimentos é consultada em `SolitaireGame.can_move`.
- `test_solitaire.py`: testes automatizados do motor.
