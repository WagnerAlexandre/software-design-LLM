package org.jetuml.command;

import java.util.ArrayList;
import java.util.List;

/**
 * Executa uma lista de comandos em ordem direta e os desfaz na ordem inversa (RF21).
 */
public class CompoundCommand implements Command {
    private final String description;
    private final List<Command> commands = new ArrayList<>();

    public CompoundCommand(String description) {
        this.description = description;
    }

    public void add(Command command) {
        if (command != null) {
            commands.add(command);
        }
    }

    public boolean isEmpty() {
        return commands.isEmpty();
    }

    @Override
    public void execute() {
        for (Command cmd : commands) {
            cmd.execute();
        }
    }

    @Override
    public void undo() {
        for (int i = commands.size() - 1; i >= 0; i--) {
            commands.get(i).undo();
        }
    }

    @Override
    public String getDescription() {
        return description;
    }
}
