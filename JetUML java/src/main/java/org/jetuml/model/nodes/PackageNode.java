package org.jetuml.model.nodes;

import org.jetuml.geom.Dimension2DInt;
import org.jetuml.geom.Point2DInt;
import org.jetuml.geom.Rectangle2DInt;
import org.jetuml.model.AbstractNode;
import org.jetuml.model.ChildNode;
import org.jetuml.model.Node;
import org.jetuml.model.ParentNode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * No de Pacote UML capaz de conter nos filhos (Classes, Interfaces, Descricoes e Subpacotes)
 * conforme RF15 e RF22.
 */
public class PackageNode extends AbstractNode implements ParentNode, ChildNode {
    public static final String ELEMENT_TYPE = "PackageNode";
    public static final int TAB_HEIGHT = 22;
    private static final int PADDING = 16;
    private final List<ChildNode> children = new ArrayList<>();
    private String name;
    private ParentNode parent;

    public PackageNode() {
        this(new Point2DInt(0, 0));
    }

    public PackageNode(Point2DInt position) {
        super(position, new Dimension2DInt(180, 120));
        this.name = "Package";
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
        recalculateBoundsFromChildren();
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
    public List<ChildNode> getChildren() {
        return Collections.unmodifiableList(children);
    }

    @Override
    public boolean canAcceptChild(Node candidate) {
        if (candidate == null || candidate == this) return false;
        if (isAncestor(candidate)) return false;
        return candidate instanceof ClassNode
                || candidate instanceof InterfaceNode
                || candidate instanceof PackageNode
                || candidate instanceof PackageDescriptionNode;
    }

    private boolean isAncestor(Node node) {
        ParentNode current = this.parent;
        while (current != null) {
            if (current == node) return true;
            if (current instanceof ChildNode childParent) {
                current = childParent.getParent();
            } else {
                break;
            }
        }
        return false;
    }

    @Override
    public void addChild(ChildNode child) {
        if (child == null || !canAcceptChild(child) || children.contains(child)) return;
        if (child.getParent() != null && child.getParent() != this) {
            child.getParent().removeChild(child);
        }
        children.add(child);
        child.setParent(this);
        recalculateBoundsFromChildren();
    }

    @Override
    public void insertChild(int index, ChildNode child) {
        if (child == null || !canAcceptChild(child) || children.contains(child)) return;
        int safeIdx = Math.max(0, Math.min(index, children.size()));
        children.add(safeIdx, child);
        child.setParent(this);
        recalculateBoundsFromChildren();
    }

    @Override
    public void removeChild(ChildNode child) {
        if (children.remove(child)) {
            if (child.getParent() == this) {
                child.setParent(null);
            }
            recalculateBoundsFromChildren();
        }
    }

    @Override
    public void translate(int dx, int dy) {
        super.translate(dx, dy);
        for (ChildNode child : children) {
            translateChildWithoutParentLoop(child, dx, dy);
        }
        if (parent instanceof PackageNode pkg) {
            pkg.recalculateBoundsFromChildren();
        }
    }

    private void translateChildWithoutParentLoop(ChildNode child, int dx, int dy) {
        child.setPosition(child.getPosition().translate(dx, dy));
        if (child instanceof PackageNode subPkg) {
            for (ChildNode grandChild : subPkg.getChildren()) {
                translateChildWithoutParentLoop(grandChild, dx, dy);
            }
        }
    }

    /**
     * Ajusta automaticamente os limites do pacote para englobar todos os seus filhos (RF15).
     */
    public void recalculateBoundsFromChildren() {
        int minTabWidth = Math.max(160, name.length() * 8 + 44);
        if (children.isEmpty()) {
            Dimension2DInt current = getSize();
            setSize(new Dimension2DInt(Math.max(minTabWidth, current.width()), Math.max(100, current.height())));
        } else {
            Rectangle2DInt union = null;
            for (ChildNode child : children) {
                Rectangle2DInt b = child.getBounds();
                union = (union == null) ? b : union.union(b);
            }
            int newX = Math.min(getPosition().x(), union.x() - PADDING);
            int newY = Math.min(getPosition().y(), union.y() - (TAB_HEIGHT + PADDING));
            int newRight = Math.max(newX + minTabWidth, union.getMaxX() + PADDING);
            int newBottom = Math.max(newY + 100, union.getMaxY() + PADDING);

            setPosition(new Point2DInt(newX, newY));
            setSize(new Dimension2DInt(newRight - newX, newBottom - newY));
        }

        if (parent instanceof PackageNode pkg) {
            pkg.recalculateBoundsFromChildren();
        }
    }

    @Override
    public Node deepCopy() {
        PackageNode copy = new PackageNode(getPosition());
        copy.name = this.name;
        copy.setSize(getSize());
        return copy;
    }
}
