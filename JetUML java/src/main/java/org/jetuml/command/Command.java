package org.jetuml.command;

/**
 * Contrato do padrao Command para suporte completo a Undo/Redo sequencial (RF21).
 */
public interface Command {
    void execute();

    void undo();

    String getDescription();
}
