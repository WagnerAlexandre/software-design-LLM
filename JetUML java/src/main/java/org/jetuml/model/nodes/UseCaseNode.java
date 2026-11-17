package org.jetuml.model.nodes;

import org.jetuml.geom.Dimension2DInt;
import org.jetuml.geom.Point2DInt;
import org.jetuml.model.AbstractNode;
import org.jetuml.model.Node;

/**
 * Representa um Caso de Uso (elipse) em um Diagrama de Casos de Uso (RF22).
 */
public class UseCaseNode extends AbstractNode {
    public static final String ELEMENT_TYPE = "UseCaseNode";

    private String name;

    public UseCaseNode() {
        this(new Point2DInt(0, 0));
    }

    public UseCaseNode(Point2DInt position) {
        super(position, new Dimension2DInt(130, 60));
        this.name = "Use Case";
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
        int width = Math.max(120, this.name.length() * 8 + 36);
        setSize(new Dimension2DInt(width, 60));
    }

    @Override
    public Node deepCopy() {
        UseCaseNode copy = new UseCaseNode(getPosition());
        copy.name = this.name;
        copy.setSize(getSize());
        return copy;
    }
}
