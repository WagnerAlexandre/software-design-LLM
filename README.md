# Impacto na Qualidade de Software com o Uso de LLMs no Desenvolvimento de Software

Estudo empírico e comparativo desenvolvido na disciplina **EXA871 — Arquitetura e Design de Software** (Engenharia de Computação — Universidade Estadual de Feira de Santana - UEFS)
O objetivo deste repositório é investigar o impacto do uso de Grandes Modelos de Linguagem (**LLMs**), como **ChatGPT** e **Gemini**, na qualidade estrutural e arquitetural de sistemas de software. Para isso, foram geradas quatro implementações funcionais de dois sistemas de referência (**Solitaire** e **JetUML**) em duas linguagens (**Python** e **Java**), posteriormente confrontadas com suas respectivas implementações **padrão-ouro** em Java sob a ótica dos princípios de design de software de Martin P. Robillard (*Introduction to Software Design with Java*, 2019).

---

> [!IMPORTANT]
> **Onde encontrar os códigos-fonte?**
> Os códigos das implementações **não estão na branch `main`** (que centraliza a documentação e o relatório do estudo). Cada implementação está hospedada em sua respectiva **branch** neste repositório. Utilize o seletor de branches no topo da página do GitHub ou os links e comandos abaixo para acessar cada projeto.

## 🗂️ Estrutura de Branches do Repositório

| Sistema | Linguagem | Branch | Acesso Direto | Descrição |
| :--- | :--- | :--- | :--- | :--- |
| **Documentação** | — | `main` *(default)* | [Ver branch](https://github.com/WagnerAlexandre/software-design-LLM/tree/main) | Documentação geral e relatório técnico do estudo. |
| **Solitaire** | Python | `Solitaire-Python` | [Ver branch](https://github.com/WagnerAlexandre/software-design-LLM/tree/Solitaire-Python) | Implementação gerada via **ChatGPT** com arquitetura em camadas (estilo MVC sem *controller* explícito) e padrões *Observer* e *Command*. |
| **Solitaire** | Java | `Solitaire-Java` | [Ver branch](https://github.com/WagnerAlexandre/software-design-LLM/tree/Solitaire-Java) | Implementação gerada via **Gemini Pro** (20 classes em pacote único `solitaire`, interface Swing e hierarquia polimórfica `CardPile`). |
| **JetUML** | Python | `JetUMLPython` | [Ver branch](https://github.com/WagnerAlexandre/software-design-LLM/tree/JetUMLPython) | Implementação monolítica (~3.000 linhas em `jetuml.py`) utilizando `tkinter`, *snapshots* para *Undo/Redo* e pipeline `RenderCommand`. |
| **JetUML** | Java | `jetUML-java` | [Ver branch](https://github.com/WagnerAlexandre/software-design-LLM/tree/jetUML-java) | Implementação gerada via **Gemini 4 Hydrogen Thinking** em 5 lotes incrementais (*bottom-up*), estruturada em camadas com Maven, JavaFX e testes JUnit. |

### Como clonar e alternar entre as branches localmente

```bash
# Clonar o repositório
git clone [https://github.com/](https://github.com/)<seu-usuario>/software-design-LLM.git
cd software-design-LLM

# Acessar a branch desejada (atenção às letras maiúsculas e minúsculas)
git checkout Solitaire-Python
git checkout Solitaire-Java
git checkout JetUMLPython
git checkout jetUML-java
```

---

## 🔍 Visão Geral das Análises e Resultados

A avaliação comparativa considerou **32 Requisitos Funcionais (RFs)** e **5 Requisitos Não Funcionais (RNFs)** para o *Solitaire*, e **42 Requisitos Funcionais (RFs)** e **3 Requisitos Não Funcionais (RNFs)** para o *JetUML*, observando critérios como encapsulamento, ocultamento de informação, coesão, acoplamento, imutabilidade, *code smells* e aplicação de padrões de projeto (GoF):

* **Solitaire em Python (`Solitaire-Python`):** Atendeu à quase totalidade dos requisitos funcionais (com ressalvas parciais em consultas somente leitura e prevenção de ciclos), mas comprometeu o encapsulamento e a separação entre regras e apresentação ao expor coleções mutáveis (`Pile.cards`) e utilizar uma *flag* booleana em vez de injeção polimórfica de estratégias.
* **Solitaire em Java (`Solitaire-Java`):** Cumpriu os requisitos funcionais e superou o padrão-ouro em pontos específicos do domínio (como a validação polimórfica nas subclasses de `CardPile` sem `instanceof` e o desacoplamento entre `GameAction` e o histórico). Por outro lado, concentrou todas as classes em um único pacote plano e gerou uma interface Swing monolítica (`SolitaireUI`).
* **JetUML em Python (`JetUMLPython`):** Priorizou a execução imediata em arquivo único sem dependências externas (`tkinter`), mas incorreu em *God Class* (`DiagramTab`), *Primitive Obsession* (nós e arestas discriminados por *strings* e dicionários), *Switch Statements* na validação procedural e perda de formatação de texto na exportação raster.
* **JetUML em Java (`jetUML-java`):** Desenvolvido via estratégia incremental em 5 lotes com retroalimentação de erros de compilação, isolou adequadamente o núcleo de domínio da camada JavaFX (permitindo testes *headless*) e aplicou tipos geométricos imutáveis e padrão *Composite*. Apresentou limitações nos fluxos complexos de Diagrama de Sequência (RF27–RF31), uso recorrente de `instanceof` e concentração de responsabilidades em `JetUMLMainWindow`.

---

## 💬 Histórico de Interações com as LLMs (Prompts)

Conforme diretriz metodológica do projeto, todas as conversas e *prompts* utilizados para geração dos códigos estão registrados publicamente:

* **Solitaire em Python (ChatGPT):** [Acessar histórico](https://chatgpt.com/share/6aff63a0-6ef0-83e9-9385-33ca31cc02bc)
* **Solitaire em Java (Gemini):** [Acessar documento](https://docs.google.com/document/d/1SrZ6VNEZ8k9GldH1g0nWj0R7EYCdOVedlSqVO97qb_Y/edit?usp=sharing)
* **JetUML em Python:** [Acessar documento](https://docs.google.com/document/d/1rcBKZ2TqaWyO0KxFF3IJvwaJKS33Zke1nE9xOHC1V_s/edit?usp=sharing)
* **JetUML em Java (Gemini):** [Acessar histórico](https://gemini.google.com/share/d/1cBe2JX7qPhWHJwpPAbXUAGobHco4W-_p?usp=sharing)

---

## 📚 Referências e Códigos Padrão-Ouro

* **Livro-base:** ROBILLARD, Martin P. *Introduction to Software Design with Java*. Cham: Springer, 2019.
* **Repositório Padrão-Ouro — Solitaire:** [github.com/prmr/Solitaire](https://github.com/prmr/Solitaire)
* **Repositório Padrão-Ouro — JetUML:** [github.com/prmr/JetUML](https://github.com/prmr/JetUML)

---

## 👥 Autores

* **Jeferson Almeida**
* **Lucas Lima** 
* **Wagner Alexandre**
