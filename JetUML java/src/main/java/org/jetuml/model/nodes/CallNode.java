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
 * Representa uma caixa de ativacao (CallNode) na linha de vida de um Diagrama de Sequencia
 * (RF22, RF27, RF28, RF29, RF30, RF31).
 */
public class CallNode extends AbstractNode implements ParentNode, ChildNode {
    public static final String ELEMENT_TYPE = "CallNode";
    public static final int DEFAULT_WIDTH = 16;
    public static final int DEFAULT_HEIGHT = 36;
    private final List<ChildNode> callees = new ArrayList<>();
    private ImplicitParameterNode implicitParameter;
    private ParentNode parent;
    private boolean openBottom;

    public CallNode() {
        this(new Point2DInt(0, 0));
    }

    public CallNode(Point2DInt position) {
        super(position, new Dimension2DInt(DEFAULT_WIDTH, DEFAULT_HEIGHT));
        this.openBottom = false;
    }

    @Override
    public String getElementType() {
        return ELEMENT_TYPE;
    }

    public ImplicitParameterNode getImplicitParameter() {
        return implicitParameter;
    }

    public void setImplicitParameter(ImplicitParameterNode implicitParameter) {
        this.implicitParameter = implicitParameter;
    }

    public boolean isOpenBottom() {
        return openBottom;
    }

    public void setOpenBottom(boolean openBottom) {
        this.openBottom = openBottom;
    }

    public boolean isRootCall() {
        return !(parent instanceof CallNode);
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
        return Collections.unmodifiableList(callees);
    }

    @Override
    public boolean canAcceptChild(Node candidate) {
        return candidate instanceof CallNode && candidate != this;
    }

    @Override
    public void addChild(ChildNode child) {
        if (child instanceof CallNode call && !callees.contains(call)) {
            callees.add(call);
            call.setParent(this);
            recalculateHeight();
        }
    }

    @Override
    public void insertChild(int index, ChildNode child) {
        if (child instanceof CallNode call && !callees.contains(call)) {
            int safeIdx = Math.max(0, Math.min(index, callees.size()));
            callees.add(safeIdx, call);
            call.setParent(this);
            recalculateHeight();
        }
    }

    @Override
    public void removeChild(ChildNode child) {
        if (callees.remove(child)) {
            if (child.getParent() == this) {
                child.setParent(null);
            }
            recalculateHeight();
        }
    }

    public void recalculateHeight() {
        int bottom = getPosition().y() + DEFAULT_HEIGHT;
        for (ChildNode child : callees) {
            if (child instanceof CallNode sub) {
                sub.recalculateHeight();
                bottom = Math.max(bottom, sub.getBounds().getMaxY() + 14);
            }
        }
        setSize(new Dimension2DInt(DEFAULT_WIDTH, Math.max(DEFAULT_HEIGHT, bottom - getPosition().y())));
        if (parent instanceof CallNode parentCall) {
            parentCall.recalculateHeight();
        }
        if (implicitParameter != null) {
            implicitParameter.layoutChildren();
        }
    }

    @Override
    public Node deepCopy() {
        CallNode copy = new CallNode(getPosition());
        copy.openBottom = this.openBottom;
        copy.setSize(getSize());
        return copy;
    }
}
