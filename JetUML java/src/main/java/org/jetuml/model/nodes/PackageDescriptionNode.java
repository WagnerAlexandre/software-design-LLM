package org.jetuml.model.nodes;

import org.jetuml.geom.Dimension2DInt;
import org.jetuml.geom.Point2DInt;
import org.jetuml.model.AbstractNode;
import org.jetuml.model.ChildNode;
import org.jetuml.model.Node;
import org.jetuml.model.ParentNode;

/**
 * No de Descricao de Pacote com nome na aba superior e conteudo textual no corpo (RF15, RF22).
 */
public class PackageDescriptionNode extends AbstractNode implements ChildNode {
    public static final String ELEMENT_TYPE = "PackageDescriptionNode";

    private String name;
    private String contents;
    private ParentNode parent;

    public PackageDescriptionNode() {
        this(new Point2DInt(0, 0));
    }

    public PackageDescriptionNode(Point2DInt position) {
        super(position, new Dimension2DInt(140, 80));
        this.name = "Package";
        this.contents = "";
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

    public String getContents() {
        return contents;
    }

    public void setContents(String contents) {
        this.contents = contents != null ? contents : "";
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
        int maxLen = Math.max(12, name.length() + 4);
        String[] lines = contents.isEmpty() ? new String[0] : contents.split("\\R", -1);
        for (String line : lines) {
            maxLen = Math.max(maxLen, line.length());
        }
        int width = Math.max(130, maxLen * 7 + 26);
        int height = Math.max(70, 24 + Math.max(1, lines.length) * 18 + 16);
        setSize(new Dimension2DInt(width, height));
        if (parent instanceof PackageNode pkg) {
            pkg.recalculateBoundsFromChildren();
        }
    }

    @Override
    public Node deepCopy() {
        PackageDescriptionNode copy = new PackageDescriptionNode(getPosition());
        copy.name = this.name;
        copy.contents = this.contents;
        copy.setSize(getSize());
        return copy;
    }
}
