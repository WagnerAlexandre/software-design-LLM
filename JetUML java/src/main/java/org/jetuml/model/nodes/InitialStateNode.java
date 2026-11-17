package org.jetuml.model.nodes;

import org.jetuml.geom.Dimension2DInt;
import org.jetuml.geom.Point2DInt;
import org.jetuml.model.AbstractNode;
import org.jetuml.model.Node;

/**
 * Pseudoestado Inicial (circulo solido) em um Diagrama de Estados (RF19, RF22).
 */
public class InitialStateNode extends AbstractNode {
    public static final String ELEMENT_TYPE = "InitialStateNode";

    public InitialStateNode() {
        this(new Point2DInt(0, 0));
    }

    public InitialStateNode(Point2DInt position) {
        super(position, new Dimension2DInt(24, 24));
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
    public Node deepCopy() {
        return new InitialStateNode(getPosition());
    }
}
