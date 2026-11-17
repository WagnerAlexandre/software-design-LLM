package org.jetuml.model;

/**
 * Observador de mudancas estruturais ou visuais em um Diagram.
 */
@FunctionalInterface
public interface DiagramListener {
    void onDiagramChanged(Diagram diagram);
}
