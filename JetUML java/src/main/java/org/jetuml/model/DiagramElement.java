package org.jetuml.model;

import org.jetuml.geom.Point2DInt;
import org.jetuml.geom.Rectangle2DInt;

/**
 * Contrato base para qualquer elemento visual de um diagrama UML (no ou aresta).
 */
public interface DiagramElement {
    String getId();

    void setId(String id);

    String getElementType();

    Rectangle2DInt getBounds();

    boolean contains(Point2DInt point);

    boolean hasEditableProperties();
}
