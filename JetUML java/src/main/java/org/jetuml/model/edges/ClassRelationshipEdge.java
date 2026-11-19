package org.jetuml.model.edges;

import org.jetuml.model.AbstractEdge;
import org.jetuml.model.Edge;
import org.jetuml.model.Node;

/**
 * Representa as arestas de relacionamento do Diagrama de Classes:
 * Dependency, Inheritance, Realization, Association, Aggregation e Composition (RF18, RF19, RF20).
 */
public class ClassRelationshipEdge extends AbstractEdge {
    public static final String ELEMENT_TYPE = "ClassRelationshipEdge";
    private RelationshipKind kind;
    private Directionality directionality;
    private String startLabel = "";
    private String middleLabel = "";
    private String endLabel = "";

    public ClassRelationshipEdge(RelationshipKind kind, Node startNode, Node endNode) {
        super(startNode, endNode);
        this.kind = kind;
        this.directionality = kind.isAssociationFamily()
                ? Directionality.UNSPECIFIED
                : Directionality.UNIDIRECTIONAL;
    }

    @Override
    public String getElementType() {
        return ELEMENT_TYPE;
    }

    public RelationshipKind getKind() {
        return kind;
    }

    /**
     * Permite alternar entre Aggregation e Composition ou entre Inheritance e Realization
     * diretamente no painel de propriedades sem recriar a aresta (RF20).
     */
    public void setKind(RelationshipKind kind) {
        if (kind != null) {
            this.kind = kind;
        }
    }

    public Directionality getDirectionality() {
        return directionality;
    }

    public void setDirectionality(Directionality directionality) {
        if (directionality != null) {
            this.directionality = directionality;
        }
    }

    public String getStartLabel() {
        return startLabel;
    }

    public void setStartLabel(String startLabel) {
        this.startLabel = startLabel != null ? startLabel : "";
    }

    public String getMiddleLabel() {
        return middleLabel;
    }

    public void setMiddleLabel(String middleLabel) {
        this.middleLabel = middleLabel != null ? middleLabel : "";
    }

    public String getEndLabel() {
        return endLabel;
    }

    public void setEndLabel(String endLabel) {
        this.endLabel = endLabel != null ? endLabel : "";
    }

    @Override
    public Edge deepCopy(Node mappedStartNode, Node mappedEndNode) {
        ClassRelationshipEdge copy = new ClassRelationshipEdge(this.kind, mappedStartNode, mappedEndNode);
        copy.directionality = this.directionality;
        copy.startLabel = this.startLabel;
        copy.middleLabel = this.middleLabel;
        copy.endLabel = this.endLabel;
        return copy;
    }

    public enum RelationshipKind {
        DEPENDENCY("Dependency"),
        INHERITANCE("Inheritance"),
        REALIZATION("Realization"),
        ASSOCIATION("Association"),
        AGGREGATION("Aggregation"),
        COMPOSITION("Composition");

        private final String displayName;

        RelationshipKind(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }

        public boolean isAssociationFamily() {
            return this == ASSOCIATION || this == AGGREGATION || this == COMPOSITION;
        }

        public boolean isAggregationOrComposition() {
            return this == AGGREGATION || this == COMPOSITION;
        }

        public boolean isGeneralizationFamily() {
            return this == INHERITANCE || this == REALIZATION;
        }
    }
}
