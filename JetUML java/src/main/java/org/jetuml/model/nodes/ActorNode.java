package org.jetuml.model.nodes;

import org.jetuml.geom.Dimension2DInt;
import org.jetuml.geom.Point2DInt;
import org.jetuml.model.AbstractNode;
import org.jetuml.model.Node;

/**
 * Representa um Ator em um Diagrama de Casos de Uso (RF22).
 */
public class ActorNode extends AbstractNode {
    public static final String ELEMENT_TYPE = "ActorNode";

    private String name;

    public ActorNode() {
        this(new Point2DInt(0, 0));
    }

    public ActorNode(Point2DInt position) {
        super(position, new Dimension2DInt(70, 90));
        this.name = "Actor";
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
        int width = Math.max(70, this.name.length() * 8 + 20);
        setSize(new Dimension2DInt(width, 90));
    }

    @Override
    public Node deepCopy() {
        ActorNode copy = new ActorNode(getPosition());
        copy.name = this.name;
        copy.setSize(getSize());
        return copy;
    }
}
