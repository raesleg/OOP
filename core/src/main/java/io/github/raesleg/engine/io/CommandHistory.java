package io.github.raesleg.engine.io;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * CommandHistory — Engine-level undo stack (Command Pattern).
 * <p>
 * Records executed {@link ICommand} instances and supports undoing the
 * most recent command. Entirely context-free — lives in the engine
 * with zero game knowledge.
 * <p>
 * <b>DIP:</b> Depends only on {@link ICommand} (abstraction).
 * <b>SRP:</b> Sole responsibility is maintaining the command history stack.
 */
public class CommandHistory {

    private final Deque<ICommand> history = new ArrayDeque<>();

    /**
     * Executes the given command and pushes it onto the history stack.
     *
     * @param command the command to execute and record
     */
    public void executeAndRecord(ICommand command) {
        if (command == null)
            return;
        command.execute();
        history.push(command);
    }

    /**
     * Undoes the most recently executed command.
     * Does nothing if the history is empty.
     */
    public void undoLast() {
        if (!history.isEmpty()) {
            history.pop().undo();
        }
    }

    /** Returns the number of commands in the history. */
    public int size() {
        return history.size();
    }

    /** Clears the entire history without undoing anything. */
    public void clear() {
        history.clear();
    }
}
