# JetUML (Edição JavaFX)

> **Aviso de Pesquisa e Origem do Código:** Este projeto foi **integralmente gerado por um Modelo de Linguagem Grande (LLM — Large Language Model)** para **fins de pesquisa acadêmica e experimental** em Engenharia de Software, avaliação de geração de código assistida por IA e arquitetura de aplicações desktop em Java/JavaFX. O software é uma reimplementação limpa baseada em especificações de requisitos funcionais e não funcionais (RF01–RF44, RNF01–RNF03) inspiradas no editor [JetUML](https://github.com/prmr/JetUML).

Editor desktop leve e interativo para modelagem de diagramas UML, desenvolvido em **Java 21** e **JavaFX 21** com gerenciamento de dependências via **Apache Maven**.

O sistema foi projetado para oferecer criação ágil de diagramas com validação semântica preventiva, roteamento automático de conexões, histórico transacional completo (*Undo/Redo*), exportação vetorial e raster, além de persistência em formato JSON estruturado.

---

## Sumário

* [Contexto da Pesquisa](#contexto-da-pesquisa)
* [Diagramas Suportados](#diagramas-suportados)
* [Principais Funcionalidades](#principais-funcionalidades)
* [Arquitetura do Projeto](#arquitetura-do-projeto)
* [Padrões de Projeto Implementados](#padrões-de-projeto-implementados)
* [Pré-requisitos](#pré-requisitos)
* [Como Compilar, Testar e Executar](#como-compilar-testar-e-executar)
* [Atalhos de Teclado](#atalhos-de-teclado)

---

## Contexto da Pesquisa

Este repositório serve como artefato experimental para investigar a capacidade de LLMs na construção de sistemas gráficos interativos com regras de domínio complexas, abrangendo:

* Tradução sistemática de requisitos funcionais (RF01–RF44) e não funcionais (RNF01–RNF03) em uma arquitetura modular testável.
* Desacoplamento estrito entre o núcleo de domínio/geometria (testável em modo *headless* via JUnit 5) e a camada de apresentação JavaFX.
* Aplicação rigorosa de padrões de projeto clássicos (GoF) para manter extensibilidade, rastreabilidade de estado e integridade referencial dos grafos UML.

---

## Diagramas Suportados

A partir da **Tela de Boas-Vindas** ou pelo menu **Arquivo > Novo Diagrama**, o usuário pode criar e editar simultaneamente em abas independentes os 5 tipos de diagramas UML:

1. **Diagrama de Classes (*Class Diagram*):**
    * **Nós:** Classe (`ClassNode`), Interface (`InterfaceNode`), Pacote (`PackageNode`), Descrição de Pacote (`PackageDescriptionNode`) e Nota (`NoteNode`).
    * **Relacionamentos:** Dependência, Herança, Realização/Implementação, Associação, Agregação, Composição e Conector de Nota.
    * **Recursos específicos:** Formatação tipográfica inline (`/itálico/` para membros abstratos e `_sublinhado_` para membros estáticos), multiplicidades e rótulos nas extremidades e centro, aninhamento e desanexação de elementos em Pacotes via `Shift + Arrastar`, além de prevenção automática de ciclos de herança e duplicidade estrutural.

2. **Diagrama de Sequência (*Sequence Diagram*):**
    * **Nós:** Participante / Parâmetro Implícito (`ImplicitParameterNode`), Caixa de Ativação (`CallNode`) e Nota.
    * **Relacionamentos:** Chamada Síncrona (`CallEdge`), Construtor (`«create»` ao apontar para o cabeçalho de um participante), Auto-chamada recursiva, Retorno (`ReturnEdge`) e Conector de Nota.
    * **Recursos específicos:** Cálculo automático de fluxo temporal e alturas de ativação, suporte a chamadas incompletas com borda inferior aberta e rejeição silenciosa de chamadas raízes órfãs.

3. **Diagrama de Estados (*State Diagram*):**
    * **Nós:** Estado Inicial (`InitialStateNode`), Estado (`StateNode`), Estado Final (`FinalStateNode`) e Nota.
    * **Relacionamentos:** Transição (`TransitionEdge`) com rótulos de `evento [guarda] / ação` e Conector de Nota.
    * **Recursos específicos:** Bloqueio de transições chegando ao Estado Inicial ou saindo do Estado Final, limite de até 2 transições por direção entre dois estados e até 2 autotransições por nó com roteamento curvo/ortogonal sem sobreposição.

4. **Diagrama de Objetos (*Object Diagram*):**
    * **Nós:** Objeto (`ObjectNode`), Campo (`FieldNode`) e Nota.
    * **Relacionamentos:** Referência (`ReferenceEdge`), Colaboração (`CollaborationEdge`) e Conector de Nota.
    * **Recursos específicos:** Criação de campos vinculada a um objeto pai e validação de origem de referências na região de valor (*Value*) do campo.

5. **Diagrama de Casos de Uso (*Use Case Diagram*):**
    * **Nós:** Ator (`ActorNode`), Caso de Uso (`UseCaseNode`) e Nota.
    * **Relacionamentos:** Associação, Generalização, Dependência (`«include»` e `«extend»`) e Conector de Nota.

---

## Principais Funcionalidades

* **Tela de Boas-Vindas & Múltiplas Abas:** Criação rápida de diagramas, acesso a arquivos recentes e edição simultânea em abas com zoom independente (50% a 200%) e confirmação ao fechar documentos modificados (`*`).
* **Menu de Contexto e Barra Lateral Dinâmica:** Ferramentas adaptadas automaticamente ao tipo do diagrama ativo, acessíveis tanto pela barra lateral (com atalhos numéricos `1` a `9`) quanto pelo clique com o botão direito em qualquer ponto do canvas.
* **Edição Interativa Precisa:** Alinhamento automático à grade (*snap-to-grid*), criação de conexões com pré-visualização elástica (*rubber-banding*), seleção individual, aditiva (`Ctrl + Clique`), por laço retangular e movimentação fina pelas setas direcionais.
* **Histórico Transacional (*Undo/Redo*):** Todas as operações de criação, movimentação, reparentamento em pacotes, edição de propriedades, recorte e colagem são encapsuladas em comandos reversíveis.
* **Persistência e Exportação:**
    * Salvamento e carregamento completo em **JSON** (`.json` / `.jet`).
    * Exportação de imagens limpas (sem grade ou marcas de seleção) nos formatos **SVG**, **PNG**, **JPEG**, **GIF** e **BMP**.
    * Cópia direta da imagem renderizada do diagrama para a área de transferência do sistema (`Ctrl + B`).
* **Personalização Visual:** Alternância entre **Modo Claro** e **Modo Escuro**, exibição/ocultação da grade, redimensionamento da área de desenho, escolha de família e tamanho de fonte e configuração do tempo de exibição das notificações (0 a 10 segundos).

---

## Arquitetura do Projeto

O sistema adota uma **Arquitetura em Camadas Desacoplada** orientada ao modelo (*Domain-Centric*), garantindo que nenhuma classe de domínio, validação, layout ou comando dependa de componentes gráficos do JavaFX (`javafx.scene.*`). Essa separação permite executar toda a lógica central em ambientes sem servidor gráfico (*headless*) e facilita a manutenção evolutiva.

### Visão em Camadas e Fluxo de Execução

```
+-----------------------------------------------------------------------+
|  Camada de Apresentação (org.jetuml.ui)                               |
|  JetUMLMainWindow | WelcomePane | DiagramTabPane | DiagramCanvasView  |
+-----------------------------------+-----------------------------------+
                                    | Interações do Usuário (Mouse/Teclado)
                                    v
+-----------------------------------------------------------------------+
|  Camada de Controle & Transações (org.jetuml.command / clipboard)     |
|  CommandManager | AddNodeCommand | MoveNodesCommand | EdgeCommands    |
+-----------------------------------+-----------------------------------+
                                    | Valida e Mutaciona Estado
                                    v
+-----------------------------------------------------------------------+
|  Camada de Domínio, Validação & Layout (model / validation / layout)  |
|  Diagram | Node / Edge | DiagramValidator | EdgeRouter | SequenceFlow |
+-------------------+-----------------------------------+---------------+
                    | Notifica (Observer)               | Serializa / Exporta
                    v                                   v
+-----------------------------------+   +-------------------------------+
|  Camada de Renderização (rendering)|   |  Persistência & Exportação    |
|  DiagramRenderer (Canvas 2D)      |   |  JsonSerializer | SvgExporter |
+-----------------------------------+   +-------------------------------+
```

### Organização dos Pacotes

```
src/
├── main/java/org/jetuml/
│   ├── MainApp.java              # Ponto de entrada da aplicação JavaFX (Application)
│   ├── geom/                     # Primitivas geométricas inteiras imutáveis (Point2DInt, Rectangle2DInt, LineSegment2D)
│   ├── model/                    # Núcleo do grafo UML (Diagram, DiagramElement, Node, Edge, ParentNode, ChildNode, ElementFactory)
│   │   ├── nodes/                # Implementações concretas dos nós UML (ClassNode, PackageNode, CallNode, etc.)
│   │   └── edges/                # Implementações concretas das arestas e relacionamentos UML (ClassEdge, CallEdge, etc.)
│   ├── validation/               # Motor de regras semânticas e estruturais por diagrama (DiagramValidator, ValidationResult)
│   ├── layout/                   # Algoritmos de roteamento automático de arestas (EdgeRouter) e fluxo temporal (SequenceFlowManager)
│   ├── command/                  # Histórico transacional Undo/Redo (CommandManager e comandos concretos)
│   ├── rendering/                # Motor de desenho 2D e parser tipográfico inline (DiagramRenderer)
│   ├── export/                   # Exportação vetorial (SvgDiagramExporter) e raster/clipboard (DiagramImageExporter)
│   ├── persistence/              # Serialização e desserialização JSON preservando hierarquias e referências (DiagramJsonSerializer)
│   ├── clipboard/                # Área de transferência interna com clonagem profunda de subgrafos (DiagramClipboard)
│   ├── config/                   # Persistência de preferências do usuário e histórico de arquivos recentes (UserPreferences)
│   └── ui/                       # Componentes JavaFX (JetUMLMainWindow, WelcomePane, DiagramTabPane, DiagramCanvasView, diálogos)
└── test/java/org/jetuml/
    └── JetUMLCoreTestSuite.java  # Suíte automatizada de testes unitários e de integração (JUnit 5)
```

---

## Padrões de Projeto Implementados

A estrutura interna do software apoia-se em **6 padrões de projeto** aplicados de forma coordenada:

1. **Command (`org.jetuml.command`):**
    * **Motivação:** Suportar desfazer/refazer (*Undo/Redo*) multinível de forma determinística e isolada por aba (RF21).
    * **Implementação:** Toda mutação no grafo (`AddNodeCommand`, `AddEdgeCommand`, `DeleteElementsCommand`, `MoveNodesCommand`, `ReparentNodeCommand`, `SequenceCallCommand`, `EditPropertiesCommand`) encapsula o estado anterior e posterior, sendo orquestrada por uma instância dedicada de `CommandManager` por aba.

2. **Composite (`org.jetuml.model.ParentNode` / `ChildNode`):**
    * **Motivação:** Representar hierarquias recursivas de contenção entre elementos UML (RF15, RF23, RF27).
    * **Implementação:** Nós contêineres (`PackageNode`, `ObjectNode`, `ImplicitParameterNode`, `CallNode`) implementam `ParentNode` e gerenciam coleções ordenadas de `ChildNode` (`ClassNode`, `InterfaceNode`, `FieldNode`, `CallNode`), propagando deslocamentos (`translate`), cálculo de fronteiras (`getBounds`) e exclusões em cascata.

3. **Factory (`org.jetuml.model.ElementFactory`):**
    * **Motivação:** Centralizar a instanciação de nós e arestas a partir da ferramenta ativa (`ToolType`) selecionada na interface ou no menu de contexto.
    * **Implementação:** Desacopla `DiagramCanvasView` das classes concretas dos pacotes `nodes` e `edges`, aplicando coordenadas ajustadas à grade e configurações iniciais padrão de cada elemento UML.

4. **Observer (`org.jetuml.model.Diagram` / `DiagramListener`):**
    * **Motivação:** Manter a interface gráfica (canvas de desenho, título da aba com indicador `*` de alteração não salva e barra de status) sincronizada automaticamente com qualquer alteração no modelo.
    * **Implementação:** `Diagram` mantém uma lista de ouvintes (`DiagramListener`) notificados sempre que nós, arestas, propriedades ou seleções são modificados, disparando a re-renderização reativa sem acoplamento direto do modelo com a UI.

5. **Strategy / Política de Roteamento e Validação (`EdgeRouter` / `DiagramValidator`):**
    * **Motivação:** Isolar as regras topológicas e de roteamento específicas de cada um dos 5 tipos de diagramas UML (RF18, RF24, RF28, RF30, RF31).
    * **Implementação:** `DiagramValidator` aplica políticas distintas de validação conforme o `DiagramType` (ex.: detecção de ciclos em herança de classes, restrição de transições em estados inicial/final, validação de origem em campos de objetos), enquanto `EdgeRouter` seleciona trajetórias ortogonais, retas, curvas ou em laço (auto-chamadas/autotransições) evitando sobreposição entre múltiplas arestas.

6. **Singleton (`UserPreferences` e `DiagramClipboard`):**
    * **Motivação:** Compartilhar configurações globais da aplicação e permitir copiar/colar elementos compatíveis entre abas diferentes (RF22, RF38–RF42).
    * **Implementação:** `UserPreferences` centraliza tema (claro/escuro), grade, fonte e arquivos recentes; `DiagramClipboard` realiza clonagem profunda (*deep copy*) dos nós, hierarquias e arestas internas selecionadas para colagem segura em qualquer aba compatível.

---

## Pré-requisitos

* **Java Development Kit (JDK) 21** ou superior
* **Apache Maven 3.9+**

---

## Como Compilar, Testar e Executar

1. **Compilar o projeto e executar a suíte de testes JUnit 5:**
   ```bash
   mvn clean test
   ```

2. **Iniciar a aplicação gráfica JavaFX:**
   ```bash
   mvn javafx:run
   ```

   > **Nota para ambientes Linux (Wayland):** Caso o servidor gráfico apresente restrições com o backend padrão do GTK, execute com:
   > ```bash
   > GDK_BACKEND=x11 mvn javafx:run
   > ```

---

## Atalhos de Teclado

| Categoria | Atalho | Ação |
| :--- | :--- | :--- |
| **Arquivo** | `Ctrl + N` | Novo Diagrama de Classes |
| | `Ctrl + O` | Abrir Diagrama (`.json` / `.jet`) |
| | `Ctrl + S` | Salvar Diagrama |
| | `Ctrl + Shift + S` | Salvar Como... |
| | `Ctrl + E` | Exportar Imagem (`SVG`, `PNG`, `JPG`, `GIF`, `BMP`) |
| | `Ctrl + B` | Copiar Imagem do Diagrama para a Área de Transferência |
| | `Ctrl + W` | Fechar Aba Atual |
| | `Ctrl + 1` .. `Ctrl + 5` | Abrir Arquivos Recentes |
| **Edição** | `Ctrl + Z` / `Ctrl + Y` | Desfazer (*Undo*) / Refazer (*Redo*) |
| | `Ctrl + X` / `C` / `V` | Recortar / Copiar / Colar Elementos |
| | `Ctrl + A` | Selecionar Todos os Elementos |
| | `Delete` / `Backspace` | Excluir Elementos Selecionados |
| | `Duplo Clique` | Abrir Editor de Propriedades do Elemento |
| | `Setas Direcionais` | Mover Elementos Selecionados na Grade |
| **Ferramentas** | `1` .. `9` | Selecionar Ferramenta da Barra Lateral |
| | `Botão Direito` | Exibir Menu de Contexto com as Ferramentas do Diagrama |
| | `Shift + Arrastar` | Incluir ou Desanexar Elemento em um Pacote |
| | `Esc` | Retornar à Ferramenta Selecionar e Limpar Seleção |
| **Exibição** | `Ctrl + =` / `Ctrl + -` | Aumentar / Diminuir Zoom (50% a 200%) |
| | `Ctrl + 0` | Restaurar Zoom para 100% |
| | `Ctrl + G` | Exibir / Ocultar Grade |