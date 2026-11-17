# Matriz de rastreabilidade — Anexo II

Legenda: **P** = parcialmente implementado; **N** = não implementado ou ainda sem teste de aceitação completo. Nenhum
requisito é marcado como integralmente concluído sem teste específico.

| Requisito | Estado | Observação                                                                                                          |
|-----------|-------:|---------------------------------------------------------------------------------------------------------------------|
| RF01      |      P | Cinco tipos e abas; tela de boas-vindas e File > New.                                                               |
| RF02      |      P | JSON, Open, Save, Save As, Close e Recent Files em memória.                                                         |
| RF03      |      P | Duplicação com novos IDs e aba.                                                                                     |
| RF04      |      P | PNG/JPEG; SVG e outros formatos pendentes.                                                                          |
| RF05      |      P | Cópia de imagem sem grade, ajustada ao conteúdo; via menu/atalho/barra.                                             |
| RF06      |      P | Exit e Ctrl+Q.                                                                                                      |
| RF07      |      P | Barras específicas por diagrama; ferramenta To Clipboard. Alguns elementos têm interação genérica.                  |
| RF08      |      N | Mapeamento completo 1–9, 0, A… ainda pendente.                                                                      |
| RF09      |      P | Menu de contexto apresenta ferramentas.                                                                             |
| RF10      |      P | Tooltips; opções existem, mas Show Tool Hint/Verbose ainda não alteram plenamente a apresentação.                   |
| RF11      |      P | Criação básica de nós por clique.                                                                                   |
| RF12      |      P | Auto Edit Node para nós.                                                                                            |
| RF13      |      P | Notas.                                                                                                              |
| RF14      |      N | Note Connector ainda não suporta âncora móvel/fixa conforme especificação.                                          |
| RF15      |      P | Field em Object; Package parentId básico. Shift attach/detach e clique dentro do pacote pendentes.                  |
| RF16      |      P | Criação de aresta por dois cliques; arraste rubber-band pendente.                                                   |
| RF17      |      P | Arestas redesenhadas com movimento de nós; roteamento ortogonal/automático avançado pendente.                       |
| RF18      |      P | Algumas regras estruturais; matriz completa de compatibilidade por tipo pendente.                                   |
| RF19      |      P | Parte das regras de autoaresta e limites de transição; casos de Actor/Use Case e transições refinadas pendentes.    |
| RF20      |      P | Estrutura de propriedades de aresta no modelo; painel completo de propriedades de arestas pendente.                 |
| RF21      |      P | CommandManager e Undo/Redo para criação de nós/arestas; todas as operações ainda não são comandos.                  |
| RF22      |      P | Edição básica por duplo clique; painel completo, Ctrl+Enter, Tab e Esc pendentes.                                   |
| RF23      |      N | Inserção de « » com Ctrl+Q pendente.                                                                                |
| RF24      |      N | Renderização de /itálico/ e _sublinhado_ pendente.                                                                  |
| RF25      |      P | Field exige pai Object; editor de nome/valor básico.                                                                |
| RF26      |      N | Referência a partir da região Value ainda pendente.                                                                 |
| RF27      |      N | Criação de chamadas/ativações conforme fluxo de sequência pendente.                                                 |
| RF28      |      N | Autochamadas pendentes.                                                                                             |
| RF29      |      N | Chamadas de construtor e «create» pendentes.                                                                        |
| RF30      |      P | Validação inicial de endpoints Activation; validação completa de fluxo de controle pendente.                        |
| RF31      |      P | Campo openBottom no modelo e editor genérico inicial; fluxo completo pendente.                                      |
| RF32      |      P | Seleção simples e movimentação de nó; Ctrl+clique, seleção múltipla e laço pendentes.                               |
| RF33      |      N | Select All completo (nós e arestas) pendente.                                                                       |
| RF34      |      N | Cut/Copy/Paste de elementos e compatibilidade entre diagramas pendentes.                                            |
| RF35      |      P | Exclusão simples do nó selecionado; seleção múltipla e comando reversível pendentes.                                |
| RF36      |      P | Grade liga/desliga; snap aplicado à criação/movimento.                                                              |
| RF37      |      P | Zoom limitado a dois níveis em cada direção.                                                                        |
| RF38      |      P | Diálogo e limites de dimensão; aplicação imediata, persistência de preferência e cálculo relativo à tela pendentes. |
| RF39      |      N | Fonte e tamanho configuráveis pendentes.                                                                            |
| RF40      |      P | Modo escuro básico.                                                                                                 |
| RF41      |      P | Notificações temporárias com cores de erro/sucesso.                                                                 |
| RF42      |      P | Duração 0–10, padrão 5; preferência Restore Default pendente.                                                       |
| RNF01     |      P | Canvas inicial fixo; dimensionamento proporcional à tela e desempenho em diagramas grandes precisam de validação.   |
| RNF02     |      P | JSON legível/documentado; versionamento e schema formal pendentes.                                                  |
| RNF03     |      P | Operações principais via menu, barra e mouse em parte; cobertura de atalhos ainda incompleta.                       |

## Próximos incrementos prioritários

1. Tornar todas as mutações comandos reversíveis (incluindo mover, editar e excluir).
2. Implementar seleção múltipla/laço, clipboard de elementos e atalhos de ferramentas.
3. Completar validação semântica por tipo de diagrama.
4. Implementar as regras específicas de Sequence e os tipos de elementos Activation/CallNode.
5. Completar propriedades de nós/arestas e formatação UML.
6. Exportação SVG, dimensionamento e configurações persistentes.
7. Criar testes automatizados de aceitação para RF01–RF42 e RNF01–RNF03.
