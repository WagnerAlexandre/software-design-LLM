package org.jetuml.model;

import java.util.List;

/**
 * Enumera os 5 tipos de diagramas UML suportados (RF01) e define
 * a ordem exata das ferramentas na barra lateral (RF07) e seus atalhos (RF08).
 */
public enum DiagramType {
    CLASS("Class Diagram", "Diagrama de Classes", List.of(
            ToolType.SELECT,
            ToolType.CLASS,
            ToolType.INTERFACE,
            ToolType.PACKAGE,
            ToolType.PACKAGE_DESCRIPTION,
            ToolType.NOTE,
            ToolType.DEPENDENCY,
            ToolType.INHERITANCE,
            ToolType.REALIZATION,
            ToolType.ASSOCIATION,
            ToolType.AGGREGATION,
            ToolType.COMPOSITION,
            ToolType.NOTE_CONNECTOR,
            ToolType.TO_CLIPBOARD
    )),
    SEQUENCE("Sequence Diagram", "Diagrama de Sequencia", List.of(
            ToolType.SELECT,
            ToolType.IMPLICIT_PARAMETER,
            ToolType.NOTE,
            ToolType.CALL,
            ToolType.RETURN,
            ToolType.NOTE_CONNECTOR,
            ToolType.TO_CLIPBOARD
    )),
    STATE("State Diagram", "Diagrama de Estados", List.of(
            ToolType.SELECT,
            ToolType.STATE,
            ToolType.INITIAL_STATE,
            ToolType.FINAL_STATE,
            ToolType.NOTE,
            ToolType.TRANSITION,
            ToolType.NOTE_CONNECTOR,
            ToolType.TO_CLIPBOARD
    )),
    OBJECT("Object Diagram", "Diagrama de Objetos", List.of(
            ToolType.SELECT,
            ToolType.OBJECT,
            ToolType.FIELD,
            ToolType.NOTE,
            ToolType.REFERENCE,
            ToolType.COLLABORATION,
            ToolType.NOTE_CONNECTOR,
            ToolType.TO_CLIPBOARD
    )),
    USE_CASE("Use Case Diagram", "Diagrama de Casos de Uso", List.of(
            ToolType.SELECT,
            ToolType.ACTOR,
            ToolType.USE_CASE,
            ToolType.NOTE,
            ToolType.USE_CASE_ASSOCIATION,
            ToolType.INCLUDE,
            ToolType.EXTEND,
            ToolType.GENERALIZATION,
            ToolType.NOTE_CONNECTOR,
            ToolType.TO_CLIPBOARD
    ));

    private final String displayName;
    private final String portugueseName;
    private final List<ToolType> tools;

    DiagramType(String displayName, String portugueseName, List<ToolType> tools) {
        this.displayName = displayName;
        this.portugueseName = portugueseName;
        this.tools = tools;
    }

    /**
     * Retorna o rotulo do atalho de teclado conforme a posicao (1-9, 0, A, B, ...) definido no RF08.
     */
    public static String getShortcutLabelForIndex(int index) {
        if (index < 0) return "";
        if (index < 9) return String.valueOf(index + 1);
        if (index == 9) return "0";
        char letter = (char) ('A' + (index - 10));
        return String.valueOf(letter);
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getPortugueseName() {
        return portugueseName;
    }

    public List<ToolType> getTools() {
        return tools;
    }

    public java.util.List<ToolType> getAvailableTools() {
        return switch (this) {
            case CLASS -> java.util.List.of(
                    ToolType.SELECT,
                    ToolType.CLASS,
                    ToolType.INTERFACE,
                    ToolType.PACKAGE,
                    ToolType.PACKAGE_DESCRIPTION,
                    ToolType.DEPENDENCY,
                    ToolType.INHERITANCE,
                    ToolType.REALIZATION,
                    ToolType.ASSOCIATION,
                    ToolType.AGGREGATION,
                    ToolType.COMPOSITION,
                    ToolType.NOTE,
                    ToolType.NOTE_CONNECTOR
            );
            case SEQUENCE -> java.util.List.of(
                    ToolType.SELECT,
                    ToolType.IMPLICIT_PARAMETER,
                    ToolType.CALL,
                    ToolType.RETURN,
                    ToolType.NOTE,
                    ToolType.NOTE_CONNECTOR
            );
            case STATE -> java.util.List.of(
                    ToolType.SELECT,
                    ToolType.INITIAL_STATE,
                    ToolType.STATE,
                    ToolType.FINAL_STATE,
                    ToolType.TRANSITION,
                    ToolType.NOTE,
                    ToolType.NOTE_CONNECTOR
            );
            case OBJECT -> java.util.List.of(
                    ToolType.SELECT,
                    ToolType.OBJECT,
                    ToolType.FIELD,
                    ToolType.REFERENCE,
                    ToolType.COLLABORATION,
                    ToolType.NOTE,
                    ToolType.NOTE_CONNECTOR
            );
            case USE_CASE -> java.util.List.of(
                    ToolType.SELECT,
                    ToolType.ACTOR,
                    ToolType.USE_CASE,
                    ToolType.ASSOCIATION,
                    ToolType.GENERALIZATION,
                    ToolType.DEPENDENCY,
                    ToolType.NOTE,
                    ToolType.NOTE_CONNECTOR
            );
        };
    }
}
