package org.jetuml.command;

import org.jetuml.layout.EdgeRouter;
import org.jetuml.layout.SequenceFlowManager.SequenceCallCreationPlan;
import org.jetuml.model.Diagram;

/**
 * Executa e desfaz de forma atomica a criacao de chamadas, autochamadas e construtores <<create>>
 * no Diagrama de Sequencia (RF21, RF27, RF28, RF29).
 */
public class SequenceCallCommand implements Command {
    private final Diagram diagram;
    private final SequenceCallCreationPlan plan;

    public SequenceCallCommand(Diagram diagram, SequenceCallCreationPlan plan) {
        this.diagram = diagram;
        this.plan = plan;
    }

    @Override
    public void execute() {
        if (plan.newCallerRoot() != null) {
            plan.callerLifeline().addChild(plan.newCallerRoot());
        }
        if (plan.isCreateCall() && plan.calleeNode().getImplicitParameter() != null) {
            plan.calleeNode().getImplicitParameter().setTopOffsetY(plan.newTargetTopOffsetY());
        }
        plan.callerNode().addChild(plan.calleeNode());
        if (plan.calleeNode().getImplicitParameter() != null) {
            plan.calleeNode().getImplicitParameter().layoutChildren();
        }
        diagram.addEdge(plan.callEdge());
        EdgeRouter.routeAll(diagram);
        diagram.selectSingle(plan.callEdge());
        diagram.setDirty(true);
    }

    @Override
    public void undo() {
        diagram.removeEdge(plan.callEdge());
        plan.callerNode().removeChild(plan.calleeNode());
        if (plan.isCreateCall() && plan.calleeNode().getImplicitParameter() != null) {
            plan.calleeNode().getImplicitParameter().setTopOffsetY(plan.previousTargetTopOffsetY());
        }
        if (plan.newCallerRoot() != null) {
            plan.callerLifeline().removeChild(plan.newCallerRoot());
        }
        if (plan.calleeNode().getImplicitParameter() != null) {
            plan.calleeNode().getImplicitParameter().layoutChildren();
        }
        if (plan.callerLifeline() != null) {
            plan.callerLifeline().layoutChildren();
        }
        EdgeRouter.routeAll(diagram);
        diagram.clearSelection();
        diagram.setDirty(true);
    }

    @Override
    public String getDescription() {
        return "Add Sequence Call";
    }
}
