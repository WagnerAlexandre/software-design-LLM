package org.jetuml.model.nodes;

import org.jetuml.geom.Dimension2DInt;
import org.jetuml.geom.Point2DInt;
import org.jetuml.model.AbstractNode;
import org.jetuml.model.Node;

/**
 * No de Nota UML para comentarios livres em qualquer diagrama (RF13, RF22).
 */
public class NoteNode extends AbstractNode {
    public static final String ELEMENT_TYPE = "NoteNode";
    private String text;

    public NoteNode() {
        this(new Point2DInt(0, 0));
    }

    public NoteNode(Point2DInt position) {
        super(position, new Dimension2DInt(120, 60));
        this.text = "Note";
    }

    @Override
    public String getElementType() {
        return ELEMENT_TYPE;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text != null ? text : "";
        recalculateSize();
    }

    public void recalculateSize() {
        String[] lines = text.split("\\R", -1);
        int maxChars = 12;
        for (String line : lines) {
            maxChars = Math.max(maxChars, line.length());
        }
        int width = Math.max(110, Math.min(360, maxChars * 7 + 28));
        int height = Math.max(50, lines.length * 18 + 24);
        setSize(new Dimension2DInt(width, height));
    }

    @Override
    public Node deepCopy() {
        NoteNode copy = new NoteNode(getPosition());
        copy.setText(this.text);
        copy.setSize(getSize());
        return copy;
    }
}
