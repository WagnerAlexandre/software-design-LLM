package org.jetuml.model.nodes;

import org.jetuml.geom.Dimension2DInt;
import org.jetuml.geom.Point2DInt;
import org.jetuml.model.AbstractNode;
import org.jetuml.model.ChildNode;
import org.jetuml.model.Node;
import org.jetuml.model.ParentNode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Representa um Objeto UML no Diagrama de Objetos (RF22, RF25).
 * Contem uma lista ordenada de FieldNodes internos.
 */
public class ObjectNode extends AbstractNode implements ParentNode {
    public static final String ELEMENT_TYPE = "ObjectNode";
    public static final int HEADER_HEIGHT = 34;
    private static final int PADDING = 8;
    private final List<ChildNode> fields = new ArrayList<>();
    private String name;

    public ObjectNode() {
        this(new Point2DInt(0, 0));
    }

    public ObjectNode(Point2DInt position) {
        super(position, new Dimension2DInt(130, 60));
        this.name = ":Object";
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
        layoutChildren();
    }

    @Override
    public List<ChildNode> getChildren() {
        return Collections.unmodifiableList(fields);
    }

    @Override
    public boolean canAcceptChild(Node candidate) {
        return candidate instanceof FieldNode;
    }

    @Override
    public void addChild(ChildNode child) {
        if (child instanceof FieldNode field && !fields.contains(field)) {
            if (field.getParent() != null && field.getParent() != this) {
                field.getParent().removeChild(field);
            }
            fields.add(field);
            field.setParent(this);
            layoutChildren();
        }
    }

    @Override
    public void insertChild(int index, ChildNode child) {
        if (child instanceof FieldNode field && !fields.contains(field)) {
            int safeIdx = Math.max(0, Math.min(index, fields.size()));
            fields.add(safeIdx, field);
            field.setParent(this);
            layoutChildren();
        }
    }

    @Override
    public void removeChild(ChildNode child) {
        if (fields.remove(child)) {
            if (child.getParent() == this) {
                child.setParent(null);
            }
            layoutChildren();
        }
    }

    @Override
    public void translate(int dx, int dy) {
        super.translate(dx, dy);
        layoutChildren();
    }

    public void layoutChildren() {
        int maxWidth = Math.max(120, name.length() * 8 + 30);
        for (ChildNode child : fields) {
            if (child instanceof FieldNode field) {
                maxWidth = Math.max(maxWidth, field.computePreferredWidth() + PADDING * 2);
            }
        }
        int currentY = getPosition().y() + HEADER_HEIGHT + 4;
        for (ChildNode child : fields) {
            if (child instanceof FieldNode field) {
                field.setPosition(new Point2DInt(getPosition().x() + PADDING, currentY));
                field.setSize(new Dimension2DInt(maxWidth - PADDING * 2, FieldNode.DEFAULT_HEIGHT));
                currentY += FieldNode.DEFAULT_HEIGHT + 4;
            }
        }
        int totalHeight = fields.isEmpty() ? 60 : (currentY - getPosition().y() + 4);
        setSize(new Dimension2DInt(maxWidth, totalHeight));
    }

    @Override
    public Node deepCopy() {
        ObjectNode copy = new ObjectNode(getPosition());
        copy.name = this.name;
        copy.setSize(getSize());
        return copy;
    }
}
