package org.jetuml.model.edges;

/**
 * Direcionalidade configuravel para arestas do Diagrama de Classes (RF20).
 */
public enum Directionality {
    UNSPECIFIED("Unspecified"),
    UNIDIRECTIONAL("Unidirectional"),
    BIDIRECTIONAL("Bidirectional");

    private final String label;

    Directionality(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
