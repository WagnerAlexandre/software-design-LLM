package org.jetuml.validation;

import org.jetuml.geom.Point2DInt;
import org.jetuml.model.*;
import org.jetuml.model.edges.*;
import org.jetuml.model.nodes.*;

import java.util.List;

/**
 * Motor central de validacao das regras estruturais dos 5 diagramas UML
 * conforme RF14, RF15, RF18, RF19, RF25, RF26, RF27, RF28, RF29 e RF30.
 */
public final class DiagramValidator {

    private DiagramValidator() {
    }

    /**
     * Valida a criacao de um no em um diagrama e eventual conteiner pai (RF15, RF25).
     */
    public static ValidationResult validateNodeCreation(Diagram diagram, ToolType tool, Node parentCandidate) {
        if (diagram == null || tool == null || !tool.isNodeTool()) {
            return ValidationResult.error("Ferramenta de no invalida.");
        }
        if (tool == ToolType.FIELD) {
            if (!(parentCandidate instanceof ObjectNode)) {
                return ValidationResult.error("Um Campo (Field) deve obrigatoriamente ser criado dentro de um Objeto.");
            }
            return ValidationResult.ok();
        }
        if (parentCandidate != null && !(parentCandidate instanceof PackageNode)) {
            return ValidationResult.error("Este elemento nao pode conter nos filhos deste tipo.");
        }
        if (parentCandidate instanceof PackageNode pkg) {
            boolean allowedInPackage = tool == ToolType.CLASS
                    || tool == ToolType.INTERFACE
                    || tool == ToolType.PACKAGE
                    || tool == ToolType.PACKAGE_DESCRIPTION;
            if (!allowedInPackage) {
                return ValidationResult.error("Apenas Classes, Interfaces, Pacotes ou Descricoes podem ser inseridos em um Pacote.");
            }
        }
        return ValidationResult.ok();
    }

    /**
     * Valida a criacao de uma aresta entre startNode e endNode (RF14, RF18, RF19, RF26, RF30).
     */
    public static ValidationResult validateEdgeCreation(
            Diagram diagram,
            ToolType tool,
            Node startNode,
            Node endNode,
            Point2DInt startPoint,
            Point2DInt endPoint
    ) {
        if (diagram == null || tool == null || !tool.isEdgeTool() || startNode == null) {
            return ValidationResult.error("Conexao invalida.");
        }

        // Regra RF14: NoteConnector pode ligar Note <-> Elemento ou Note -> ponto fixo no canvas
        if (tool == ToolType.NOTE_CONNECTOR) {
            return validateNoteConnector(startNode, endNode);
        }

        if (endNode == null) {
            return ValidationResult.error("Selecione um elemento de destino valido.");
        }

        return switch (diagram.getType()) {
            case CLASS -> validateClassEdge(diagram, tool, startNode, endNode);
            case SEQUENCE -> validateSequenceEdge(diagram, tool, startNode, endNode, startPoint, endPoint);
            case STATE -> validateStateEdge(diagram, tool, startNode, endNode);
            case OBJECT -> validateObjectEdge(diagram, tool, startNode, endNode, startPoint);
            case USE_CASE -> validateUseCaseEdge(diagram, tool, startNode, endNode);
        };
    }

    private static ValidationResult validateNoteConnector(Node startNode, Node endNode) {
        if (startNode instanceof NoteNode) {
            if (endNode instanceof NoteNode) {
                return ValidationResult.error("Nao e permitido conectar duas Notas diretamente.");
            }
            return ValidationResult.ok();
        }
        if (endNode instanceof NoteNode) {
            if (startNode instanceof PointNode) {
                return ValidationResult.error("Origem invalida para conector de Nota.");
            }
            return ValidationResult.ok();
        }
        return ValidationResult.error("Note Connector deve conectar uma Nota a um elemento ou ponto do diagrama.");
    }

    // ------------------------------------------------------------------------
    // Regras do Diagrama de Classes (RF18, RF19)
    // ------------------------------------------------------------------------
    private static ValidationResult validateClassEdge(Diagram diagram, ToolType tool, Node start, Node end) {
        if (!isClassDiagramConnectable(start) || !isClassDiagramConnectable(end)) {
            return ValidationResult.error("Arestas de classe exigem elementos validos de origem e destino.");
        }

        // Pacotes so podem participar de Dependency
        if ((start instanceof PackageNode || end instanceof PackageNode) && tool != ToolType.DEPENDENCY) {
            return ValidationResult.error("Pacotes suportam apenas arestas de Dependencia.");
        }

        ClassRelationshipEdge.RelationshipKind newKind = mapClassToolToKind(tool);
        if (newKind == null) {
            return ValidationResult.error("Tipo de relacao invalido para Diagrama de Classes.");
        }

        boolean selfEdge = start.equals(end);

        // RF19: Bloquear auto-heranca e auto-dependencia em Diagramas de Classes
        if (selfEdge) {
            if (newKind == ClassRelationshipEdge.RelationshipKind.INHERITANCE
                    || newKind == ClassRelationshipEdge.RelationshipKind.REALIZATION) {
                return ValidationResult.error("Nao e permitido criar auto-heranca ou auto-realizacao.");
            }
            if (newKind == ClassRelationshipEdge.RelationshipKind.DEPENDENCY) {
                return ValidationResult.error("Nao e permitido criar auto-dependencia em Diagramas de Classes.");
            }
        }

        // Realizacao exige Interface como destino (ou origem/destino compativel)
        if (newKind == ClassRelationshipEdge.RelationshipKind.REALIZATION && !(end instanceof InterfaceNode)) {
            return ValidationResult.error("A aresta de Realizacao deve apontar para uma Interface.");
        }
        if (newKind == ClassRelationshipEdge.RelationshipKind.INHERITANCE
                && ((start instanceof InterfaceNode) != (end instanceof InterfaceNode))) {
            return ValidationResult.error("Heranca deve conectar duas Classes ou duas Interfaces.");
        }

        for (Edge existing : diagram.getEdges()) {
            if (!(existing instanceof ClassRelationshipEdge rel)) continue;

            boolean sameDirection = rel.getStartNode().equals(start) && rel.getEndNode().equals(end);
            boolean reverseDirection = !selfEdge && rel.getStartNode().equals(end) && rel.getEndNode().equals(start);

            // RF18: Nao criar mais de 1 aresta do mesmo tipo entre os mesmos dois nos
            if (sameDirection && rel.getKind() == newKind) {
                return ValidationResult.error("Ja existe uma aresta de " + newKind.getDisplayName() + " entre estes elementos.");
            }

            // RF18: Nao criar ciclos diretos (vai-e-volta) com Heranca, Dependencia, Agregacao ou Associacao
            if (reverseDirection) {
                if (isCycleRestrictedKind(newKind) && rel.getKind() == newKind) {
                    return ValidationResult.error("Ciclo direto (vai-e-volta) nao permitido para " + newKind.getDisplayName() + ".");
                }
                if (newKind.isGeneralizationFamily() && rel.getKind().isGeneralizationFamily()) {
                    return ValidationResult.error("Ciclo direto de heranca/realizacao nao e permitido.");
                }
            }

            // RF18: Nao combinar Associacao e Agregacao simultaneamente entre as mesmas duas classes
            if (sameDirection || reverseDirection) {
                boolean existingAssocOrAgg = rel.getKind() == ClassRelationshipEdge.RelationshipKind.ASSOCIATION
                        || rel.getKind().isAggregationOrComposition();
                boolean newAssocOrAgg = newKind == ClassRelationshipEdge.RelationshipKind.ASSOCIATION
                        || newKind.isAggregationOrComposition();

                if (existingAssocOrAgg && newAssocOrAgg && rel.getKind() != newKind) {
                    return ValidationResult.error("Nao e permitido combinar Associacao e Agregacao/Composicao entre as mesmas classes.");
                }
            }
        }

        return ValidationResult.ok();
    }

    private static boolean isClassDiagramConnectable(Node node) {
        return node instanceof ClassNode
                || node instanceof InterfaceNode
                || node instanceof PackageNode
                || node instanceof PackageDescriptionNode;
    }

    private static boolean isCycleRestrictedKind(ClassRelationshipEdge.RelationshipKind kind) {
        return kind == ClassRelationshipEdge.RelationshipKind.INHERITANCE
                || kind == ClassRelationshipEdge.RelationshipKind.REALIZATION
                || kind == ClassRelationshipEdge.RelationshipKind.DEPENDENCY
                || kind == ClassRelationshipEdge.RelationshipKind.AGGREGATION
                || kind == ClassRelationshipEdge.RelationshipKind.COMPOSITION
                || kind == ClassRelationshipEdge.RelationshipKind.ASSOCIATION;
    }

    private static ClassRelationshipEdge.RelationshipKind mapClassToolToKind(ToolType tool) {
        return switch (tool) {
            case DEPENDENCY -> ClassRelationshipEdge.RelationshipKind.DEPENDENCY;
            case INHERITANCE -> ClassRelationshipEdge.RelationshipKind.INHERITANCE;
            case REALIZATION -> ClassRelationshipEdge.RelationshipKind.REALIZATION;
            case ASSOCIATION -> ClassRelationshipEdge.RelationshipKind.ASSOCIATION;
            case AGGREGATION -> ClassRelationshipEdge.RelationshipKind.AGGREGATION;
            case COMPOSITION -> ClassRelationshipEdge.RelationshipKind.COMPOSITION;
            default -> null;
        };
    }

    // ------------------------------------------------------------------------
    // Regras do Diagrama de Sequencia (RF27, RF28, RF29, RF30)
    // ------------------------------------------------------------------------
    private static ValidationResult validateSequenceEdge(
            Diagram diagram,
            ToolType tool,
            Node start,
            Node end,
            Point2DInt startPoint,
            Point2DInt endPoint
    ) {
        if (tool == ToolType.CALL) {
            // Origem pode ser ImplicitParameterNode (primeira chamada) ou CallNode existente (RF27, RF28)
            if (!(start instanceof ImplicitParameterNode) && !(start instanceof CallNode)) {
                return ValidationResult.silentReject();
            }
            // Destino pode ser ImplicitParameterNode (cabecalho = <<create>> RF29, ou linha de vida) ou CallNode
            if (!(end instanceof ImplicitParameterNode) && !(end instanceof CallNode)) {
                return ValidationResult.silentReject();
            }

            // RF30: Impedir mais de um bloco de chamada raiz orfao (sem chamador) no mesmo diagrama
            if (start instanceof ImplicitParameterNode) {
                boolean hasExistingRootCall = diagram.getAllNodes().stream()
                        .filter(n -> n instanceof CallNode)
                        .map(n -> (CallNode) n)
                        .anyMatch(CallNode::isRootCall);
                if (hasExistingRootCall) {
                    return ValidationResult.silentReject();
                }
            }

            // RF30: Se o destino ja e uma CallNode, impedir que receba chamadas de multiplos chamadores
            if (end instanceof CallNode targetCall) {
                if (!start.equals(targetCall)) {
                    return ValidationResult.silentReject();
                }
            }

            // Se destino e cabecalho de ImplicitParameterNode (<<create>> RF29),
            // o objeto nao pode ja ter chamadas previas ou ser o mesmo participante
            if (end instanceof ImplicitParameterNode targetLife && endPoint != null
                    && targetLife.getHeaderBounds().contains(endPoint)) {
                ImplicitParameterNode callerLife = (start instanceof CallNode c)
                        ? c.getImplicitParameter()
                        : (ImplicitParameterNode) start;
                if (targetLife.equals(callerLife) || !targetLife.getChildren().isEmpty()) {
                    return ValidationResult.silentReject();
                }
            }

            return ValidationResult.ok();
        }

        if (tool == ToolType.RETURN) {
            // RF30: Conectar arestas de Retorno apenas entre Caixas de Ativacao (CallNodes)
            if (!(start instanceof CallNode callee) || !(end instanceof CallNode caller)) {
                return ValidationResult.silentReject();
            }
            // O retorno deve ir exatamente do callee para o seu chamador direto no fluxo de controle
            if (callee.getParent() != caller) {
                return ValidationResult.silentReject();
            }
            // RF18: Nao duplicar retorno
            for (Edge existing : diagram.getEdges()) {
                if (existing instanceof ReturnEdge
                        && existing.getStartNode().equals(callee)
                        && existing.getEndNode().equals(caller)) {
                    return ValidationResult.silentReject();
                }
            }
            return ValidationResult.ok();
        }

        return ValidationResult.silentReject();
    }

    // ------------------------------------------------------------------------
    // Regras do Diagrama de Estados (RF18, RF19)
    // ------------------------------------------------------------------------
    private static ValidationResult validateStateEdge(Diagram diagram, ToolType tool, Node start, Node end) {
        if (tool != ToolType.TRANSITION) {
            return ValidationResult.error("Ferramenta invalida para Diagrama de Estados.");
        }
        if (!isStateNode(start) || !isStateNode(end)) {
            return ValidationResult.error("Transicoes devem conectar Estados validos.");
        }

        // RF19: Bloquear transicoes partindo de um Estado Final ou chegando em um Estado Inicial
        if (start instanceof FinalStateNode) {
            return ValidationResult.error("Nao e permitido criar transicoes partindo de um Estado Final.");
        }
        if (end instanceof InitialStateNode) {
            return ValidationResult.error("Nao e permitido criar transicoes chegando em um Estado Inicial.");
        }

        boolean selfTransition = start.equals(end);
        if (selfTransition && (start instanceof InitialStateNode || start instanceof FinalStateNode)) {
            return ValidationResult.error("Estados Inicial e Final nao aceitam autotransicoes.");
        }

        int sameDirectionCount = 0;
        for (Edge existing : diagram.getEdges()) {
            if (existing instanceof TransitionEdge
                    && existing.getStartNode().equals(start)
                    && existing.getEndNode().equals(end)) {
                sameDirectionCount++;
            }
        }

        // RF19: Cada no aceita ate 2 autotransicoes; entre 2 nos sao aceitas ate 2 transicoes em cada direcao
        if (sameDirectionCount >= 2) {
            if (selfTransition) {
                return ValidationResult.error("Cada estado aceita no maximo 2 autotransicoes.");
            }
            return ValidationResult.error("Sao aceitas no maximo 2 transicoes em cada direcao entre dois estados.");
        }

        return ValidationResult.ok();
    }

    private static boolean isStateNode(Node node) {
        return node instanceof StateNode
                || node instanceof InitialStateNode
                || node instanceof FinalStateNode;
    }

    // ------------------------------------------------------------------------
    // Regras do Diagrama de Objetos (RF18, RF19, RF26)
    // ------------------------------------------------------------------------
    private static ValidationResult validateObjectEdge(
            Diagram diagram,
            ToolType tool,
            Node start,
            Node end,
            Point2DInt startPoint
    ) {
        if (tool == ToolType.REFERENCE) {
            // RF26: Arrastar da parte Value de um FieldNode ate o ObjectNode de destino
            if (!(start instanceof FieldNode field)) {
                return ValidationResult.error("A referencia deve partir da parte Value de um Campo (Field).");
            }
            if (startPoint != null && !field.getValueBounds().contains(startPoint)) {
                return ValidationResult.error("Inicie o arraste da referencia a partir da regiao Value do campo.");
            }
            if (!(end instanceof ObjectNode)) {
                return ValidationResult.error("A referencia deve apontar para um Objeto.");
            }
            for (Edge existing : diagram.getEdges()) {
                if (existing instanceof ReferenceEdge
                        && existing.getStartNode().equals(field)
                        && existing.getEndNode().equals(end)) {
                    return ValidationResult.error("Ja existe esta referencia entre o campo e o objeto.");
                }
            }
            return ValidationResult.ok();
        }

        if (tool == ToolType.COLLABORATION) {
            if (!(start instanceof ObjectNode) || !(end instanceof ObjectNode)) {
                return ValidationResult.error("Colaboracao deve conectar dois Objetos.");
            }
            // RF19: Bloquear auto-colaboracao para Objetos
            if (start.equals(end)) {
                return ValidationResult.error("Nao e permitido criar auto-colaboracao para Objetos.");
            }
            for (Edge existing : diagram.getEdges()) {
                if (existing instanceof CollaborationEdge) {
                    // RF18: Nao criar mais de 1 aresta do mesmo tipo entre os mesmos dois nos
                    if (existing.getStartNode().equals(start) && existing.getEndNode().equals(end)) {
                        return ValidationResult.error("Ja existe uma colaboracao entre estes dois Objetos.");
                    }
                    // RF18: Nao criar ciclos de colaboracao entre dois Objetos
                    if (existing.getStartNode().equals(end) && existing.getEndNode().equals(start)) {
                        return ValidationResult.error("Ciclo de colaboracao entre dois Objetos nao e permitido.");
                    }
                }
            }
            return ValidationResult.ok();
        }

        return ValidationResult.error("Aresta invalida para Diagrama de Objetos.");
    }

    // ------------------------------------------------------------------------
    // Regras do Diagrama de Casos de Uso (RF18, RF19)
    // ------------------------------------------------------------------------
    private static ValidationResult validateUseCaseEdge(Diagram diagram, ToolType tool, Node start, Node end) {
        if (!isUseCaseDiagramNode(start) || !isUseCaseDiagramNode(end)) {
            return ValidationResult.error("Conexao invalida no Diagrama de Casos de Uso.");
        }

        // RF19: Bloquear auto-associacao, auto-heranca ou auto-dependencia para Atores ou Casos de Uso
        if (start.equals(end)) {
            return ValidationResult.error("Autoarestas nao sao permitidas para Atores ou Casos de Uso.");
        }

        if (tool == ToolType.GENERALIZATION && start.getClass() != end.getClass()) {
            return ValidationResult.error("Generalizacao deve ligar dois Atores ou dois Casos de Uso.");
        }

        if ((tool == ToolType.INCLUDE || tool == ToolType.EXTEND)
                && (!(start instanceof UseCaseNode) || !(end instanceof UseCaseNode))) {
            return ValidationResult.error("Relacoes <<include>> e <<extend>> devem conectar dois Casos de Uso.");
        }

        // RF18: Nao criar mais de 1 aresta do mesmo tipo entre os mesmos dois nos
        for (Edge existing : diagram.getEdges()) {
            if (existing instanceof UseCaseEdge ucEdge
                    && ucEdge.getStartNode().equals(start)
                    && ucEdge.getEndNode().equals(end)) {
                boolean sameTool = switch (tool) {
                    case USE_CASE_ASSOCIATION -> ucEdge.getKind() == UseCaseEdge.UseCaseKind.ASSOCIATION;
                    case INCLUDE -> ucEdge.getKind() == UseCaseEdge.UseCaseKind.INCLUDE;
                    case EXTEND -> ucEdge.getKind() == UseCaseEdge.UseCaseKind.EXTEND;
                    case GENERALIZATION -> ucEdge.getKind() == UseCaseEdge.UseCaseKind.GENERALIZATION;
                    default -> false;
                };
                if (sameTool) {
                    return ValidationResult.error("Ja existe uma relacao deste tipo entre estes elementos.");
                }
            }
        }

        return ValidationResult.ok();
    }

    private static boolean isUseCaseDiagramNode(Node node) {
        return node instanceof ActorNode || node instanceof UseCaseNode;
    }

    /**
     * Valida se uma lista de elementos pode ser colada no diagrama de destino (RF34).
     */
    public static ValidationResult validatePasteCompatibility(Diagram targetDiagram, List<Node> nodesToPaste) {
        if (targetDiagram == null) {
            return ValidationResult.error("Nenhum diagrama ativo para colar.");
        }
        // RF34: Colar em diagramas de sequencia nao e permitido
        if (targetDiagram.getType() == DiagramType.SEQUENCE) {
            return ValidationResult.error("Colar elementos em Diagramas de Sequencia nao e permitido.");
        }
        if (nodesToPaste == null || nodesToPaste.isEmpty()) {
            return ValidationResult.error("Area de transferencia vazia.");
        }

        for (Node node : nodesToPaste) {
            if (!isNodeCompatibleWithDiagram(targetDiagram.getType(), node)) {
                return ValidationResult.error(
                        "Elemento '" + node.getElementType() + "' e incompativel com " + targetDiagram.getType().getDisplayName() + "."
                );
            }
        }
        return ValidationResult.ok();
    }

    public static boolean isNodeCompatibleWithDiagram(DiagramType type, Node node) {
        if (node instanceof NoteNode || node instanceof PointNode) {
            return true;
        }
        return switch (type) {
            case CLASS -> node instanceof ClassNode
                    || node instanceof InterfaceNode
                    || node instanceof PackageNode
                    || node instanceof PackageDescriptionNode;
            case SEQUENCE -> node instanceof ImplicitParameterNode || node instanceof CallNode;
            case STATE -> node instanceof StateNode
                    || node instanceof InitialStateNode
                    || node instanceof FinalStateNode;
            case OBJECT -> node instanceof ObjectNode || node instanceof FieldNode;
            case USE_CASE -> node instanceof ActorNode || node instanceof UseCaseNode;
        };
    }
}
