package org.jetuml.model.nodes;

import org.jetuml.geom.Dimension2DInt;
import org.jetuml.geom.Point2DInt;
import org.jetuml.geom.Rectangle2DInt;
import org.jetuml.model.AbstractNode;
import org.jetuml.model.ChildNode;
import org.jetuml.model.Node;
import org.jetuml.model.ParentNode;

/**
 * Representa um Campo (nome = valor) dentro de um ObjectNode no Diagrama de Objetos (RF22, RF25, RF26).
 * Nao pode existir como raiz solta no diagrama.
 */
public class FieldNode extends AbstractNode implements ChildNode {
    public static final String ELEMENT_TYPE = "FieldNode";
    public static final int DEFAULT_HEIGHT = 22;

    private String name;
    private String value;
    private ParentNode parent;

    public FieldNode() {
        this(new Point2DInt(0, 0));
    }

    public FieldNode(Point2DInt position) {
        super(position, new Dimension2DInt(110, DEFAULT_HEIGHT));
        this.name = "field";
        this.value = "value";
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
        if (parent instanceof ObjectNode obj) {
            obj.layoutChildren();
        }
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value != null ? value : "";
        if (parent instanceof ObjectNode obj) {
            obj.layoutChildren();
        }
    }

    public int computePreferredWidth() {
        String text = name + " = " + value;
        return Math.max(90, text.length() * 7 + 16);
    }

    /**
     * Retorna a regiao correspondente a parte Value do campo (RF26).
     */
    public Rectangle2DInt getValueBounds() {
        Rectangle2DInt b = getBounds();
        int splitX = b.x() + (int) Math.round(b.width() * 0.45);
        return new Rectangle2DInt(splitX, b.y(), b.getMaxX() - splitX, b.height());
    }

    public Point2DInt getValueCenterPoint() {
        return getValueBounds().getCenter();
    }

    @Override
    public ParentNode getParent() {
        return parent;
    }

    @Override
    public void setParent(ParentNode parent) {
        this.parent = parent;
    }

    @Override
    public Node deepCopy() {
        FieldNode copy = new FieldNode(getPosition());
        copy.name = this.name;
        copy.value = this.value;
        copy.setSize(getSize());
        return copy;
    }
}
