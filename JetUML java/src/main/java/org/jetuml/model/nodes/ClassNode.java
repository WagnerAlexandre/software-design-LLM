package org.jetuml.model.nodes;

import org.jetuml.geom.Dimension2DInt;
import org.jetuml.geom.Point2DInt;
import org.jetuml.model.AbstractNode;
import org.jetuml.model.ChildNode;
import org.jetuml.model.Node;
import org.jetuml.model.ParentNode;

/**
 * Representa uma Classe UML com nome, atributos e metodos (RF15, RF22, RF24).
 */
public class ClassNode extends AbstractNode implements ChildNode {
    public static final String ELEMENT_TYPE = "ClassNode";

    private String name;
    private String attributes;
    private String methods;
    private ParentNode parent;

    public ClassNode() {
        this(new Point2DInt(0, 0));
    }

    public ClassNode(Point2DInt position) {
        super(position, new Dimension2DInt(130, 80));
        this.name = "Class";
        this.attributes = "";
        this.methods = "";
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
        recalculateSize();
    }

    public String getAttributes() {
        return attributes;
    }

    public void setAttributes(String attributes) {
        this.attributes = attributes != null ? attributes : "";
        recalculateSize();
    }

    public String getMethods() {
        return methods;
    }

    public void setMethods(String methods) {
        this.methods = methods != null ? methods : "";
        recalculateSize();
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
    public void translate(int dx, int dy) {
        super.translate(dx, dy);
        if (parent instanceof PackageNode pkg) {
            pkg.recalculateBoundsFromChildren();
        }
    }

    public void recalculateSize() {
        int maxLen = Math.max(12, name.length());
        String[] attrLines = attributes.isEmpty() ? new String[0] : attributes.split("\\R", -1);
        String[] methLines = methods.isEmpty() ? new String[0] : methods.split("\\R", -1);

        for (String line : attrLines) maxLen = Math.max(maxLen, line.length());
        for (String line : methLines) maxLen = Math.max(maxLen, line.length());

        int width = Math.max(120, maxLen * 7 + 28);
        int headerH = 28;
        int attrH = Math.max(22, attrLines.length * 17 + 10);
        int methH = Math.max(22, methLines.length * 17 + 10);
        setSize(new Dimension2DInt(width, headerH + attrH + methH));
        if (parent instanceof PackageNode pkg) {
            pkg.recalculateBoundsFromChildren();
        }
    }

    @Override
    public Node deepCopy() {
        ClassNode copy = new ClassNode(getPosition());
        copy.name = this.name;
        copy.attributes = this.attributes;
        copy.methods = this.methods;
        copy.setSize(getSize());
        return copy;
    }
}
