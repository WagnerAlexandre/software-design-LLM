package org.jetuml.command;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * Gerencia as pilhas de Undo e Redo por diagrama (RF21).
 */
public class CommandManager {
    private final Deque<Command> undoStack = new ArrayDeque<>();
    private final Deque<Command> redoStack = new ArrayDeque<>();
    private final List<Runnable> listeners = new ArrayList<>();

    public void execute(Command command) {
        if (command == null) return;
        command.execute();
        undoStack.push(command);
        redoStack.clear();
        notifyListeners();
    }

    public boolean canUndo() {
        return !undoStack.isEmpty();
    }

    public boolean canRedo() {
        return !redoStack.isEmpty();
    }

    public void undo() {
        if (!canUndo()) return;
        Command command = undoStack.pop();
        command.undo();
        redoStack.push(command);
        notifyListeners();
    }

    public void redo() {
        if (!canRedo()) return;
        Command command = redoStack.pop();
        command.execute();
        undoStack.push(command);
        notifyListeners();
    }

    public void clear() {
        undoStack.clear();
        redoStack.clear();
        notifyListeners();
    }

    public void addListener(Runnable listener) {
        listeners.add(listener);
    }

    private void notifyListeners() {
        for (Runnable r : List.copyOf(listeners)) {
            r.run();
        }
    }
}
