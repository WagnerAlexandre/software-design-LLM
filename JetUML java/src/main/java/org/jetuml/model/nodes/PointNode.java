package org.jetuml.model.nodes;

import org.jetuml.geom.Dimension2DInt;
import org.jetuml.geom.Point2DInt;
import org.jetuml.model.AbstractNode;
import org.jetuml.model.Node;

/**
 * No ponto invisivel usado para ancorar a extremidade livre de um NoteConnector
 * em uma coordenada fixa do diagrama (RF14).
 */
public class PointNode extends AbstractNode {
    public static final String ELEMENT_TYPE = "PointNode";

    public PointNode() {
        this(new Point2DInt(0, 0));
    }

    public PointNode(Point2DInt position) {
        super(position, new Dimension2DInt(6, 6));
    }

    @Override
    public String getElementType() {
        return ELEMENT_TYPE;
    }

    @Override
    public boolean hasEditableProperties() {
        return false;
    }

    @Override
    public Point2DInt getConnectionPoint(Point2DInt targetPoint) {
        return getBounds().getCenter();
    }

    @Override
    public Node deepCopy() {
        return new PointNode(getPosition());
    }
}
