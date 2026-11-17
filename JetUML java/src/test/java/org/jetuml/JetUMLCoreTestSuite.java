package org.jetuml;

import org.jetuml.clipboard.DiagramClipboard;
import org.jetuml.command.AddNodeCommand;
import org.jetuml.command.CommandManager;
import org.jetuml.command.DeleteElementsCommand;
import org.jetuml.command.MoveNodesCommand;
import org.jetuml.command.SequenceCallCommand;
import org.jetuml.export.SvgDiagramExporter;
import org.jetuml.geom.Point2DInt;
import org.jetuml.layout.SequenceFlowManager;
import org.jetuml.model.Diagram;
import org.jetuml.model.DiagramType;
import org.jetuml.model.ToolType;
import org.jetuml.model.edges.ClassRelationshipEdge;
import org.jetuml.model.edges.TransitionEdge;
import org.jetuml.model.nodes.ClassNode;
import org.jetuml.model.nodes.FieldNode;
import org.jetuml.model.nodes.FinalStateNode;
import org.jetuml.model.nodes.ImplicitParameterNode;
import org.jetuml.model.nodes.InitialStateNode;
import org.jetuml.model.nodes.InterfaceNode;
import org.jetuml.model.nodes.ObjectNode;
import org.jetuml.model.nodes.PackageNode;
import org.jetuml.model.nodes.StateNode;
import org.jetuml.persistence.DiagramJsonSerializer;
import org.jetuml.validation.DiagramValidator;
import org.jetuml.validation.ValidationResult;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Suite de testes unitarios e de integracao cobrindo os requisitos funcionais RF02-RF38.
 */
class JetUMLCoreTestSuite {

    @Test
    void testClassDiagramValidationRulesRF18AndRF19() {
        Diagram diagram = new Diagram(DiagramType.CLASS);
        ClassNode classA = new ClassNode(new Point2DInt(100, 100));
        classA.setName("Cliente");
        ClassNode classB = new ClassNode(new Point2DInt(350, 100));
        classB.setName("Pedido");
        InterfaceNode iface = new InterfaceNode(new Point2DInt(350, 260));
        iface.setName("Pagavel");

        diagram.addRootNode(classA);
        diagram.addRootNode(classB);
        diagram.addRootNode(iface);

        // RF19: Auto-heranca e auto-dependencia proibidas
        assertFalse(DiagramValidator.validateEdgeCreation(diagram, ToolType.INHERITANCE, classA, classA, null, null).isValid());
        assertFalse(DiagramValidator.validateEdgeCreation(diagram, ToolType.DEPENDENCY, classA, classA, null, null).isValid());

        // Criar Heranca A -> B
        assertTrue(DiagramValidator.validateEdgeCreation(diagram, ToolType.INHERITANCE, classA, classB, null, null).isValid());
        ClassRelationshipEdge inh = new ClassRelationshipEdge(ClassRelationshipEdge.RelationshipKind.INHERITANCE, classA, classB);
        diagram.addEdge(inh);

        // RF18: Aresta duplicada do mesmo tipo proibida
        assertFalse(DiagramValidator.validateEdgeCreation(diagram, ToolType.INHERITANCE, classA, classB, null, null).isValid());

        // RF18: Ciclo direto de Heranca B -> A proibido
        assertFalse(DiagramValidator.validateEdgeCreation(diagram, ToolType.INHERITANCE, classB, classA, null, null).isValid());

        // RF18: Nao combinar Associacao e Agregacao entre as mesmas classes
        ClassRelationshipEdge assoc = new ClassRelationshipEdge(ClassRelationshipEdge.RelationshipKind.ASSOCIATION, classA, classB);
        diagram.addEdge(assoc);
        assertFalse(DiagramValidator.validateEdgeCreation(diagram, ToolType.AGGREGATION, classA, classB, null, null).isValid());
    }

    @Test
    void testStateDiagramTransitionLimitsRF19() {
        Diagram diagram = new Diagram(DiagramType.STATE);
        InitialStateNode init = new InitialStateNode(new Point2DInt(60, 100));
        StateNode s1 = new StateNode(new Point2DInt(180, 90));
        StateNode s2 = new StateNode(new Point2DInt(380, 90));
        FinalStateNode fin = new FinalStateNode(new Point2DInt(560, 100));

        diagram.addRootNode(init);
        diagram.addRootNode(s1);
        diagram.addRootNode(s2);
        diagram.addRootNode(fin);

        // RF19: Bloquear transicao chegando no Estado Inicial ou saindo do Estado Final
        assertFalse(DiagramValidator.validateEdgeCreation(diagram, ToolType.TRANSITION, s1, init, null, null).isValid());
        assertFalse(DiagramValidator.validateEdgeCreation(diagram, ToolType.TRANSITION, fin, s1, null, null).isValid());

        // RF19: Ate 2 transicoes em cada direcao entre 2 nos
        assertTrue(DiagramValidator.validateEdgeCreation(diagram, ToolType.TRANSITION, s1, s2, null, null).isValid());
        diagram.addEdge(new TransitionEdge(s1, s2));
        assertTrue(DiagramValidator.validateEdgeCreation(diagram, ToolType.TRANSITION, s1, s2, null, null).isValid());
        diagram.addEdge(new TransitionEdge(s1, s2));
        assertFalse(DiagramValidator.validateEdgeCreation(diagram, ToolType.TRANSITION, s1, s2, null, null).isValid());

        // RF19: Ate 2 autotransicoes em cada no
        assertTrue(DiagramValidator.validateEdgeCreation(diagram, ToolType.TRANSITION, s1, s1, null, null).isValid());
        diagram.addEdge(new TransitionEdge(s1, s1));
        assertTrue(DiagramValidator.validateEdgeCreation(diagram, ToolType.TRANSITION, s1, s1, null, null).isValid());
        diagram.addEdge(new TransitionEdge(s1, s1));
        assertFalse(DiagramValidator.validateEdgeCreation(diagram, ToolType.TRANSITION, s1, s1, null, null).isValid());
    }

    @Test
    void testObjectDiagramFieldCreationAndReferenceRF25AndRF26() {
        Diagram diagram = new Diagram(DiagramType.OBJECT);
        ObjectNode obj1 = new ObjectNode(new Point2DInt(100, 100));
        ObjectNode obj2 = new ObjectNode(new Point2DInt(340, 100));
        diagram.addRootNode(obj1);
        diagram.addRootNode(obj2);

        // RF25: Field exige um ObjectNode pai
        assertFalse(DiagramValidator.validateNodeCreation(diagram, ToolType.FIELD, null).isValid());
        assertTrue(DiagramValidator.validateNodeCreation(diagram, ToolType.FIELD, obj1).isValid());

        FieldNode field = new FieldNode(new Point2DInt(110, 140));
        obj1.addChild(field);

        // RF26: Referencia deve iniciar na regiao Value do campo
        Point2DInt insideValue = field.getValueCenterPoint();
        Point2DInt outsideValue = new Point2DInt(field.getBounds().x() + 4, field.getBounds().getCenter().y());

        assertTrue(DiagramValidator.validateEdgeCreation(diagram, ToolType.REFERENCE, field, obj2, insideValue, null).isValid());
        assertFalse(DiagramValidator.validateEdgeCreation(diagram, ToolType.REFERENCE, field, obj2, outsideValue, null).isValid());
    }

    @Test
    void testSequenceDiagramCallsAndSilentRejectionRF27ToRF30() {
        Diagram diagram = new Diagram(DiagramType.SEQUENCE);
        CommandManager cmd = new CommandManager();

        ImplicitParameterNode user = new ImplicitParameterNode(new Point2DInt(80, 40));
        user.setName("usuario : Usuario");
        ImplicitParameterNode service = new ImplicitParameterNode(new Point2DInt(280, 40));
        service.setName("servico : PedidoService");
        ImplicitParameterNode orphan = new ImplicitParameterNode(new Point2DInt(480, 40));
        orphan.setName("outro : Outro");

        diagram.addRootNode(user);
        diagram.addRootNode(service);
        diagram.addRootNode(orphan);

        // RF27: Primeira chamada entre linhas de vida cria caixas de ativacao
        Point2DInt startPt = new Point2DInt(user.getLifelineCenterX(), 120);
        Point2DInt endPt = new Point2DInt(service.getLifelineCenterX(), 120);
        assertTrue(DiagramValidator.validateEdgeCreation(diagram, ToolType.CALL, user, service, startPt, endPt).isValid());

        var plan = SequenceFlowManager.buildCallPlan(user, service, startPt, endPt);
        cmd.execute(new SequenceCallCommand(diagram, plan));

        assertEquals(1, user.getChildren().size());
        assertEquals(1, service.getChildren().size());

        // RF30: Segunda chamada raiz orfa e rejeitada silenciosamente
        Point2DInt orphanPt = new Point2DInt(orphan.getLifelineCenterX(), 160);
        ValidationResult secondRoot = DiagramValidator.validateEdgeCreation(diagram, ToolType.CALL, orphan, service, orphanPt, endPt);
        assertFalse(secondRoot.isValid());
        assertTrue(secondRoot.isSilent());

        // RF21: Desfazer remove chamada e caixas de ativacao
        cmd.undo();
        assertTrue(user.getChildren().isEmpty());
        assertTrue(service.getChildren().isEmpty());
    }

    @Test
    void testCommandUndoRedoAndPackageReparentingRF15AndRF21() {
        Diagram diagram = new Diagram(DiagramType.CLASS);
        CommandManager cmd = new CommandManager();

        PackageNode pkg = new PackageNode(new Point2DInt(80, 80));
        ClassNode cls = new ClassNode(new Point2DInt(110, 120));

        cmd.execute(new AddNodeCommand(diagram, pkg, null));
        cmd.execute(new AddNodeCommand(diagram, cls, pkg));
        assertEquals(1, pkg.getChildren().size());

        cmd.execute(new MoveNodesCommand(diagram, List.of(pkg), 40, 20));
        assertEquals(120, pkg.getPosition().x());
        assertEquals(150, cls.getPosition().x());

        cmd.undo();
        assertEquals(80, pkg.getPosition().x());
        assertEquals(110, cls.getPosition().x());

        cmd.execute(new DeleteElementsCommand(diagram, java.util.Set.of(pkg)));
        assertTrue(diagram.getRootNodes().isEmpty());

        cmd.undo();
        assertEquals(1, diagram.getRootNodes().size());
        assertEquals(1, pkg.getChildren().size());
    }

    @Test
    void testJsonPersistenceAndSvgExportRF02AndRF04() throws IOException {
        Diagram diagram = new Diagram(DiagramType.CLASS);
        ClassNode c1 = new ClassNode(new Point2DInt(100, 100));
        c1.setName("/EntidadeBase/");
        c1.setAttributes("- _contador_ : int");
        c1.setMethods("+ /validar/() : boolean");

        ClassNode c2 = new ClassNode(new Point2DInt(340, 100));
        c2.setName("Produto");

        diagram.addRootNode(c1);
        diagram.addRootNode(c2);
        diagram.addEdge(new ClassRelationshipEdge(ClassRelationshipEdge.RelationshipKind.INHERITANCE, c2, c1));

        String json = DiagramJsonSerializer.toJson(diagram);
        Diagram restored = DiagramJsonSerializer.fromJson(json);

        assertEquals(DiagramType.CLASS, restored.getType());
        assertEquals(2, restored.getRootNodes().size());
        assertEquals(1, restored.getEdges().size());

        String svg = SvgDiagramExporter.generateSvg(restored);
        assertTrue(svg.contains("<svg"));
        assertTrue(svg.contains("Produto"));
    }

    @Test
    void testClipboardBlocksSequencePasteRF34() {
        Diagram classDiag = new Diagram(DiagramType.CLASS);
        ClassNode c1 = new ClassNode(new Point2DInt(100, 100));
        classDiag.addRootNode(c1);
        classDiag.selectSingle(c1);

        DiagramClipboard.getInstance().copy(classDiag);

        Diagram seqDiag = new Diagram(DiagramType.SEQUENCE);
        ValidationResult pasteInSeq = DiagramClipboard.getInstance().validatePaste(seqDiag);
        assertFalse(pasteInSeq.isValid());
    }
}
