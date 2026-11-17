package org.jetuml.model;

/**
 * Define todas as ferramentas disponiveis nas barras laterais dos 5 tipos de diagramas UML,
 * incluindo rotulos curtos e descricoes estendidas (RF07, RF10).
 */
public enum ToolType {
    // Ferramenta universal de selecao e manipulacao
    SELECT("Select", Category.SELECTION,
            "Seleciona, move e redimensiona elementos ou abre laco de selecao (Arrastar)."),

    // Nos do Diagrama de Classes
    CLASS("Class", Category.NODE,
            "Cria uma Classe UML com compartimentos para nome, atributos e metodos."),
    INTERFACE("Interface", Category.NODE,
            "Cria uma Interface UML com estereotipo <<interface>> e lista de operacoes."),
    PACKAGE("Package", Category.NODE,
            "Cria um Pacote UML capaz de agrupar classes, interfaces e subpacotes."),
    PACKAGE_DESCRIPTION("Package Description", Category.NODE,
            "Cria um no de Descricao de Pacote com nome e conteudo textual detalhado."),

    // Nos do Diagrama de Sequencia
    IMPLICIT_PARAMETER("Implicit Parameter", Category.NODE,
            "Cria uma linha de vida (objeto/participante) em um Diagrama de Sequencia."),

    // Nos do Diagrama de Estados
    STATE("State", Category.NODE,
            "Cria um Estado UML representando uma situacao ou condicao durante a vida de um objeto."),
    INITIAL_STATE("Initial State", Category.NODE,
            "Cria o pseudoestado Inicial indicando o ponto de partida da maquina de estados."),
    FINAL_STATE("Final State", Category.NODE,
            "Cria o estado Final indicando o encerramento da maquina de estados."),

    // Nos do Diagrama de Objetos
    OBJECT("Object", Category.NODE,
            "Cria uma instancia de Objeto UML (nome:Classe) com suporte a campos internos."),
    FIELD("Field", Category.NODE,
            "Cria um Campo (nome = valor) dentro de um Objeto existente."),

    // Nos do Diagrama de Casos de Uso
    ACTOR("Actor", Category.NODE,
            "Cria um Ator representando um papel externo que interage com o sistema."),
    USE_CASE("Use Case", Category.NODE,
            "Cria um Caso de Uso descrevendo uma funcionalidade observavel do sistema."),

    // Nota universal
    NOTE("Note", Category.NODE,
            "Cria uma Nota UML para comentarios e observacoes livres no diagrama."),

    // Arestas do Diagrama de Classes
    DEPENDENCY("Dependency", Category.EDGE,
            "Cria uma aresta de Dependencia tracejada indicando uso temporario entre elementos."),
    INHERITANCE("Inheritance", Category.EDGE,
            "Cria uma relacao de Heranca/Generalizacao com seta triangular fechada vazia."),
    REALIZATION("Realization", Category.EDGE,
            "Cria uma relacao de Realizacao/Implementacao tracejada com seta triangular vazia."),
    ASSOCIATION("Association", Category.EDGE,
            "Cria uma Associacao estrutural entre classes, com multiplicidades e direcionalidade."),
    AGGREGATION("Aggregation", Category.EDGE,
            "Cria uma Agregacao (todo-parte compartilhada) com losango vazio na extremidade."),
    COMPOSITION("Composition", Category.EDGE,
            "Cria uma Composicao (todo-parte exclusiva) com losango preenchido na extremidade."),

    // Arestas do Diagrama de Sequencia
    CALL("Call", Category.EDGE,
            "Cria uma mensagem de Chamada sincrona ou construtor <<create>> entre linhas de vida."),
    RETURN("Return", Category.EDGE,
            "Cria uma mensagem de Retorno tracejada de uma caixa de ativacao para o chamador."),

    // Arestas do Diagrama de Estados
    TRANSITION("Transition", Category.EDGE,
            "Cria uma Transicao direcionada entre estados ou autotransicao no mesmo estado."),

    // Arestas do Diagrama de Objetos
    REFERENCE("Reference", Category.EDGE,
            "Conecta o valor de um Campo a outro Objeto referenciado no diagrama."),
    COLLABORATION("Collaboration", Category.EDGE,
            "Cria um vinculo de Colaboracao entre dois Objetos."),

    // Arestas do Diagrama de Casos de Uso
    USE_CASE_ASSOCIATION("Association", Category.EDGE,
            "Conecta um Ator a um Caso de Uso ou relaciona elementos de Casos de Uso."),
    INCLUDE("Include", Category.EDGE,
            "Cria uma relacao <<include>> indicando inclusao obrigatoria de comportamento."),
    EXTEND("Extend", Category.EDGE,
            "Cria uma relacao <<extend>> indicando extensao condicional de comportamento."),
    GENERALIZATION("Generalization", Category.EDGE,
            "Cria uma Generalizacao entre Atores ou entre Casos de Uso."),

    // Conector universal de notas
    NOTE_CONNECTOR("Note Connector", Category.EDGE,
            "Liga um elemento a uma Nota ou ancora uma Nota a um ponto fixo do diagrama."),

    // Acao imediata da barra lateral
    TO_CLIPBOARD("To Clipboard", Category.ACTION,
            "Copia uma imagem limpa do diagrama (sem grade e ajustada ao conteudo) para a area de transferencia.");

    private final String displayName;
    private final Category category;
    private final String verboseDescription;
    ToolType(String displayName, Category category, String verboseDescription) {
        this.displayName = displayName;
        this.category = category;
        this.verboseDescription = verboseDescription;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Category getCategory() {
        return category;
    }

    public String getVerboseDescription() {
        return verboseDescription;
    }

    public boolean isNodeTool() {
        return category == Category.NODE;
    }

    public boolean isEdgeTool() {
        return category == Category.EDGE;
    }

    public enum Category {
        SELECTION,
        NODE,
        EDGE,
        ACTION
    }
}
