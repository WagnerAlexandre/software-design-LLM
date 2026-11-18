# Rastreabilidade dos requisitos

| Requisito | Implementação principal |
|---|---|
| RF01 | `SolitaireGame.new_game()` distribui 28 cartas no tableau e 24 no monte; `draw_or_recycle()` recicla o descarte quando o monte fica vazio. |
| RF02–RF03 | `has_won` e `score`; a interface não exibe a pontuação. |
| RF04–RF07 | `Card`, `Suit`, `Rank`, `CardRegistry` e embaralhamento em `new_game()`. |
| RF08–RF10 | `Pile`, consultas `is_empty`/`top`, `resolve_location()` e `find_accessible_card()`. |
| RF11–RF13 | Compra e reciclagem em `draw_or_recycle()`; `can_move()` não permite usar o descarte como destino. |
| RF14–RF15 | Quatro pilhas em `foundations`; `_accepts_foundation()` e validação de movimento unitário. |
| RF16–RF20 | Distribuição e consultas do tableau; `_accepts_tableau()`, movimentação de sufixos e revelação com `FlipAction`. |
| RF21 | `SolitaireGame.can_move()` centraliza a legalidade; interface e estratégia apenas consultam o motor. |
| RF22–RF25 | Ações reversíveis, `CompositeAction`, `NoOpAction`, `history`, `undo()` e `undo_all()`. |
| RF26 | Observadores registrados por `subscribe()` recebem eventos após mudança ou desfazer. |
| RF27 | Consultas retornam tuplas ou valores e não alteram as pilhas. |
| RF28–RF31 | `AutoStrategy.choose()` decide sem alterar o jogo; `execute_suggestion()` revalida e executa pelo motor. A opção desativada retorna `NoOpAction`. |
| RF32 | `SolitaireUI` implementa arrastar/soltar sequências e consulta `can_move()` para indicar destino válido. |
| RNF01 | A visibilidade pertence à própria entidade `Card` e é alterada por ações reversíveis. |
| RNF02 | Ações simples/compostas/nulas compartilham o contrato `Action`; estratégia desativada produz ação nula. |
| RNF03–RNF04 | Regras no motor; interface e estratégia não implementam regras próprias de empilhamento. |
| RNF05 | `CardRegistry` serializa IDs e ordem e reconstitui as mesmas instâncias por meio do registro. |

## Observação sobre a estratégia

A estratégia respeita a prioridade RF29. Na prioridade 5, tenta primeiro revelar uma carta oculta e depois esvaziar uma coluna. Não move uma coluna inteira para outra coluna vazia, evitando a troca de lugar sem progresso descrita em RF30. A estratégia não recicla o descarte automaticamente, pois a sequência de prioridades especificada só prevê compras enquanto ainda há cartas no monte; a reciclagem permanece disponível ao jogador pelo clique no monte vazio.
