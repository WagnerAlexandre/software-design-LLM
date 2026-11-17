package org.jetuml.model;

import org.jetuml.geom.Dimension2DInt;
import org.jetuml.geom.Point2DInt;

/**
 * Contrato para todos os nos de um diagrama UML.
 */
public interface Node extends DiagramElement {
    Point2DInt getPosition();

    void setPosition(Point2DInt position);

    void translate(int dx, int dy);

    Dimension2DInt getSize();

    void setSize(Dimension2DInt size);

    Point2DInt getConnectionPoint(Point2DInt targetPoint);

    Node deepCopy();

    default ToolType getToolType() {
        if (this instanceof org.jetuml.model.nodes.ClassNode) return ToolType.CLASS;
        if (this instanceof org.jetuml.model.nodes.InterfaceNode) return ToolType.INTERFACE;
        if (this instanceof org.jetuml.model.nodes.PackageNode) return ToolType.PACKAGE;
        if (this instanceof org.jetuml.model.nodes.PackageDescriptionNode) return ToolType.PACKAGE_DESCRIPTION;
        if (this instanceof org.jetuml.model.nodes.FieldNode) return ToolType.FIELD;
        if (this instanceof org.jetuml.model.nodes.CallNode) return ToolType.CALL;
        return ToolType.SELECT;
    }
}
