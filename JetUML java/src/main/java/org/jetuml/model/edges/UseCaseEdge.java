package org.jetuml.model.edges;

import org.jetuml.model.AbstractEdge;
import org.jetuml.model.Edge;
import org.jetuml.model.Node;

/**
 * Aresta do Diagrama de Casos de Uso (Association, Include, Extend, Generalization)
 * com suporte a alteracao do Dependency Type entre None, Include e Extend (RF18, RF19, RF20).
 */
public class UseCaseEdge extends AbstractEdge {
    public static final String ELEMENT_TYPE = "UseCaseEdge";
    private UseCaseKind kind;
    private DependencyType dependencyType;

    public UseCaseEdge(UseCaseKind kind, Node startNode, Node endNode) {
        super(startNode, endNode);
        this.kind = kind;
        this.dependencyType = switch (kind) {
            case INCLUDE -> DependencyType.INCLUDE;
            case EXTEND -> DependencyType.EXTEND;
            default -> DependencyType.NONE;
        };
    }

    public boolean isDependency() {
        return dependencyType != null && dependencyType != DependencyType.NONE;
    }

    @Override
    public String getElementType() {
        return ELEMENT_TYPE;
    }

    public UseCaseKind getKind() {
        return kind;
    }

    public DependencyType getDependencyType() {
        return dependencyType;
    }

    public void setDependencyType(DependencyType dependencyType) {
        if (dependencyType == null) return;
        this.dependencyType = dependencyType;
        if (this.kind != UseCaseKind.GENERALIZATION) {
            this.kind = switch (dependencyType) {
                case INCLUDE -> UseCaseKind.INCLUDE;
                case EXTEND -> UseCaseKind.EXTEND;
                case NONE -> UseCaseKind.ASSOCIATION;
            };
        }
    }

    @Override
    public boolean hasEditableProperties() {
        return kind != UseCaseKind.GENERALIZATION;
    }

    @Override
    public Edge deepCopy(Node mappedStartNode, Node mappedEndNode) {
        UseCaseEdge copy = new UseCaseEdge(this.kind, mappedStartNode, mappedEndNode);
        copy.dependencyType = this.dependencyType;
        return copy;
    }

    public enum DependencyType {
        NONE("None"),
        INCLUDE("Include"),
        EXTEND("Extend");

        private final String label;

        DependencyType(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }
    }

    public enum UseCaseKind {
        ASSOCIATION,
        INCLUDE,
        EXTEND,
        GENERALIZATION
    }
}
