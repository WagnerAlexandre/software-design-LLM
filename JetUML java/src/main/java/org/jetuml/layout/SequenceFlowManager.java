package org.jetuml.layout;

import org.jetuml.geom.Point2DInt;
import org.jetuml.model.Node;
import org.jetuml.model.edges.CallEdge;
import org.jetuml.model.nodes.CallNode;
import org.jetuml.model.nodes.ImplicitParameterNode;

/**
 * Gerencia a construcao estrutural de chamadas, autochamadas e construtores <<create>>
 * no Diagrama de Sequencia conforme RF27, RF28 e RF29.
 */
public final class SequenceFlowManager {

    private SequenceFlowManager() {
    }

    /**
     * Prepara todas as entidades necessarias para criar uma chamada no Diagrama de Sequencia:
     * - Chamada inicial entre linhas de vida (cria caixas de ativacao em ambos, RF27);
     * - Chamadas adicionais a partir de uma caixa de ativacao (RF27);
     * - Autochamadas dentro de uma caixa de ativacao (RF28);
     * - Chamadas de construtor <<create>> ao soltar sobre o cabecalho do parametro implicito (RF29).
     */
    public static SequenceCallCreationPlan buildCallPlan(
            Node startNode,
            Node endNode,
            Point2DInt startPoint,
            Point2DInt endPoint
    ) {
        int callY = Math.max(80, startPoint != null ? startPoint.y() : 100);

        CallNode newCallerRoot = null;
        ImplicitParameterNode callerLifeline;
        CallNode callerNode;

        if (startNode instanceof ImplicitParameterNode startLife) {
            callerLifeline = startLife;
            callerNode = new CallNode(new Point2DInt(startLife.getLifelineCenterX() - CallNode.DEFAULT_WIDTH / 2, callY));
            newCallerRoot = callerNode;
        } else if (startNode instanceof CallNode existingCaller) {
            callerNode = existingCaller;
            callerLifeline = existingCaller.getImplicitParameter();
            callY = Math.max(existingCaller.getPosition().y() + 10, callY);
        } else {
            throw new IllegalArgumentException("Origem invalida para CallEdge.");
        }

        // Caso de Autochamada (RF28)
        if (startNode.equals(endNode) && startNode instanceof CallNode selfCaller) {
            int subY = Math.max(selfCaller.getPosition().y() + 12, callY);
            CallNode selfCallee = new CallNode(new Point2DInt(selfCaller.getPosition().x() + 8, subY));
            selfCallee.setImplicitParameter(selfCaller.getImplicitParameter());
            CallEdge edge = new CallEdge(selfCaller, selfCallee, false);
            return new SequenceCallCreationPlan(
                    null,
                    callerLifeline,
                    selfCaller,
                    selfCallee,
                    edge,
                    false,
                    0,
                    0
            );
        }

        ImplicitParameterNode calleeLifeline;
        boolean isCreateCall = false;
        if (endNode instanceof ImplicitParameterNode targetLife) {
            calleeLifeline = targetLife;
            if (endPoint != null && targetLife.getHeaderBounds().contains(endPoint)) {
                isCreateCall = true;
            }
        } else if (endNode instanceof CallNode targetCall) {
            calleeLifeline = targetCall.getImplicitParameter();
        } else {
            throw new IllegalArgumentException("Destino invalido para CallEdge.");
        }

        int prevOffset = calleeLifeline.getTopOffsetY();
        int newOffset = prevOffset;
        int calleeY = callY + 4;

        // RF29: Chamada de construtor <<create>>
        if (isCreateCall) {
            newOffset = Math.max(0, callY - calleeLifeline.getPosition().y() - ImplicitParameterNode.TOP_BOX_HEIGHT / 2);
            calleeY = calleeLifeline.getPosition().y() + newOffset + ImplicitParameterNode.TOP_BOX_HEIGHT;
        }

        CallNode calleeNode = new CallNode(
                new Point2DInt(calleeLifeline.getLifelineCenterX() - CallNode.DEFAULT_WIDTH / 2, calleeY)
        );
        calleeNode.setImplicitParameter(calleeLifeline);

        CallEdge callEdge = new CallEdge(callerNode, calleeNode, isCreateCall);

        return new SequenceCallCreationPlan(
                newCallerRoot,
                callerLifeline,
                callerNode,
                calleeNode,
                callEdge,
                isCreateCall,
                prevOffset,
                newOffset
        );
    }

    /**
     * @param newCallerRoot Preenchido apenas se for a primeira chamada raiz (RF27)
     */
    public record SequenceCallCreationPlan(CallNode newCallerRoot, ImplicitParameterNode callerLifeline,
                                           CallNode callerNode, CallNode calleeNode, CallEdge callEdge,
                                           boolean isCreateCall, int previousTargetTopOffsetY,
                                           int newTargetTopOffsetY) {
    }
}
