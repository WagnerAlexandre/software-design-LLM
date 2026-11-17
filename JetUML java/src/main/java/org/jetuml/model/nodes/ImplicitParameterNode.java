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
 * Representa uma linha de vida (Implicit Parameter) no Diagrama de Sequencia (RF22, RF27, RF29).
 */
public class ImplicitParameterNode extends AbstractNode implements ParentNode {
    public static final String ELEMENT_TYPE = "ImplicitParameterNode";
    public static final int TOP_BOX_HEIGHT = 40;
    public static final int DEFAULT_LIFELINE_HEIGHT = 280;
    private final List<ChildNode> children = new ArrayList<>();
    private String name;
    private int topOffsetY = 0;

    public ImplicitParameterNode() {
        this(new Point2DInt(0, 0));
    }

    public ImplicitParameterNode(Point2DInt position) {
        super(position, new Dimension2DInt(110, DEFAULT_LIFELINE_HEIGHT));
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
        int width = Math.max(100, this.name.length() * 8 + 28);
        setSize(new Dimension2DInt(width, getSize().height()));
    }

    /**
     * Deslocamento vertical da caixa cabecalho quando o objeto e criado via chamada <<create>> (RF29).
     */
    public int getTopOffsetY() {
        return topOffsetY;
    }

    public void setTopOffsetY(int topOffsetY) {
        this.topOffsetY = Math.max(0, topOffsetY);
    }

    public int getLifelineCenterX() {
        return getPosition().x() + getSize().width() / 2;
    }

    public Rectangle2DInt getHeaderBounds() {
        return new Rectangle2DInt(
                getPosition().x(),
                getPosition().y() + topOffsetY,
                getSize().width(),
                TOP_BOX_HEIGHT
        );
    }

    @Override
    public boolean contains(Point2DInt point) {
        Rectangle2DInt header = getHeaderBounds();
        if (header.contains(point)) {
            return true;
        }
        int centerX = getLifelineCenterX();
        int topY = header.getMaxY();
        int bottomY = getBounds().getMaxY();
        return Math.abs(point.x() - centerX) <= 12
                && point.y() >= topY
                && point.y() <= bottomY;
    }

    @Override
    public List<ChildNode> getChildren() {
        return Collections.unmodifiableList(children);
    }

    @Override
    public boolean canAcceptChild(Node candidate) {
        return candidate instanceof CallNode;
    }

    @Override
    public void addChild(ChildNode child) {
        if (child instanceof CallNode call && !children.contains(call)) {
            children.add(call);
            call.setParent(this);
            call.setImplicitParameter(this);
            layoutChildren();
        }
    }

    @Override
    public void insertChild(int index, ChildNode child) {
        if (child instanceof CallNode call && !children.contains(call)) {
            int safeIdx = Math.max(0, Math.min(index, children.size()));
            children.add(safeIdx, call);
            call.setParent(this);
            call.setImplicitParameter(this);
            layoutChildren();
        }
    }

    @Override
    public void removeChild(ChildNode child) {
        if (children.remove(child)) {
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

    /**
     * Reposiciona todas as caixas de ativacao ao longo do eixo central da linha de vida.
     */
    public void layoutChildren() {
        int maxY = getPosition().y() + DEFAULT_LIFELINE_HEIGHT;
        for (ChildNode child : children) {
            if (child instanceof CallNode call) {
                layoutCallRecursive(call, 0);
                maxY = Math.max(maxY, getBottomMostY(call) + 40);
            }
        }
        setSize(new Dimension2DInt(getSize().width(), Math.max(DEFAULT_LIFELINE_HEIGHT, maxY - getPosition().y())));
    }

    private void layoutCallRecursive(CallNode call, int nestingDepth) {
        ImplicitParameterNode owner = call.getImplicitParameter() != null ? call.getImplicitParameter() : this;
        int x = owner.getLifelineCenterX() - CallNode.DEFAULT_WIDTH / 2 + nestingDepth * 8;
        call.setPosition(new Point2DInt(x, call.getPosition().y()));
        for (ChildNode sub : call.getChildren()) {
            if (sub instanceof CallNode subCall) {
                int nextDepth = (subCall.getImplicitParameter() == owner) ? nestingDepth + 1 : 0;
                layoutCallRecursive(subCall, nextDepth);
            }
        }
    }

    private int getBottomMostY(CallNode call) {
        int bottom = call.getBounds().getMaxY();
        for (ChildNode sub : call.getChildren()) {
            if (sub instanceof CallNode subCall) {
                bottom = Math.max(bottom, getBottomMostY(subCall));
            }
        }
        return bottom;
    }

    @Override
    public Node deepCopy() {
        ImplicitParameterNode copy = new ImplicitParameterNode(getPosition());
        copy.name = this.name;
        copy.topOffsetY = this.topOffsetY;
        copy.setSize(getSize());
        return copy;
    }
}
