package io.github.raesleg.engine.io;

/**
 * ICommand — Engine-level command interface (Command Pattern).
 * <p>
 * Encapsulates a reversible action as a first-class object.
 * Concrete implementations live in the game layer.
 * <p>
 * <b>ISP:</b> Only two methods — {@code execute()} and {@code undo()}.
 * <b>DIP:</b> {@link CommandHistory} depends on this abstraction, never on
 * concrete game commands.
 */
public interface ICommand {

    /** Performs the action. */
    void execute();

    /** Reverses the action performed by {@link #execute()}. */
    void undo();
}
