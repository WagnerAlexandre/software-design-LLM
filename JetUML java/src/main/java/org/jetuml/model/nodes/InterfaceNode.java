package org.jetuml.model.nodes;

import org.jetuml.geom.Dimension2DInt;
import org.jetuml.geom.Point2DInt;
import org.jetuml.model.AbstractNode;
import org.jetuml.model.ChildNode;
import org.jetuml.model.Node;
import org.jetuml.model.ParentNode;

/**
 * Representa uma Interface UML com estereotipo <<interface>>, nome e metodos (RF15, RF22, RF24).
 */
public class InterfaceNode extends AbstractNode implements ChildNode {
    public static final String ELEMENT_TYPE = "InterfaceNode";

    private String name;
    private String methods;
    private ParentNode parent;

    public InterfaceNode() {
        this(new Point2DInt(0, 0));
    }

    public InterfaceNode(Point2DInt position) {
        super(position, new Dimension2DInt(130, 70));
        this.name = "Interface";
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
        int maxLen = Math.max(14, name.length());
        String[] methLines = methods.isEmpty() ? new String[0] : methods.split("\\R", -1);
        for (String line : methLines) {
            maxLen = Math.max(maxLen, line.length());
        }
        int width = Math.max(125, maxLen * 7 + 28);
        int headerH = 38;
        int methH = Math.max(24, methLines.length * 17 + 10);
        setSize(new Dimension2DInt(width, headerH + methH));
        if (parent instanceof PackageNode pkg) {
            pkg.recalculateBoundsFromChildren();
        }
    }

    @Override
    public Node deepCopy() {
        InterfaceNode copy = new InterfaceNode(getPosition());
        copy.name = this.name;
        copy.methods = this.methods;
        copy.setSize(getSize());
        return copy;
    }
}
