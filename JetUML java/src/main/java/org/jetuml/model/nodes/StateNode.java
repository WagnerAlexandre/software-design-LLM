package org.jetuml.model.nodes;

import org.jetuml.geom.Dimension2DInt;
import org.jetuml.geom.Point2DInt;
import org.jetuml.model.AbstractNode;
import org.jetuml.model.Node;

/**
 * Representa um Estado UML em um Diagrama de Estados (RF22).
 */
public class StateNode extends AbstractNode {
    public static final String ELEMENT_TYPE = "StateNode";

    private String name;

    public StateNode() {
        this(new Point2DInt(0, 0));
    }

    public StateNode(Point2DInt position) {
        super(position, new Dimension2DInt(100, 50));
        this.name = "State";
    }

    @Override
    public String getElementType() {
        return ELEMENT_TYPE;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name != null ? name : "";
        int width = Math.max(90, this.name.length() * 8 + 30);
        setSize(new Dimension2DInt(width, 50));
    }

    @Override
    public Node deepCopy() {
        StateNode copy = new StateNode(getPosition());
        copy.name = this.name;
        copy.setSize(getSize());
        return copy;
    }
}
