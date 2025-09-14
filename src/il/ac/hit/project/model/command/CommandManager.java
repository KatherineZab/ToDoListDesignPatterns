package il.ac.hit.project.model.command;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Manages command execution with full undo/redo support.
 * keeps track of the order the undo redo
 * Maintains two stacks: one for commands that can be undone,
 * and one for commands that can be redone.
 */
public final class CommandManager {
    private final Deque<Command> undoStack = new ArrayDeque<>();
    private final Deque<Command> redoStack = new ArrayDeque<>();

    /**
     * Executes a command and makes it available for undo.
     * Also clears any pending redo operations since executing a new
     * command means you can't redo the old "undone" commands anymore.
     * @param c the command to execute; must not be null
     */
    public void execute(Command c) {
        c.execute();
        undoStack.push(c);
        redoStack.clear();           // clear redo after a new action
    }

    /**
     * Undoes the most recently executed command.
     * Moves the command to the redo stack so it can be redone later.
     * Does nothing if there's nothing to undo.
     */
    public void undo() {
        if (undoStack.isEmpty()) return;
        Command c = undoStack.pop();
        c.undo();
        redoStack.push(c);
    }

    /**
     * Redoes the most recently undone command.
     * Re-executes the command and moves it back to the undo stack.
     * Does nothing if there's nothing to redo.
     */
    public void redo() {
        if (redoStack.isEmpty()) return;
        Command c = redoStack.pop();
        c.execute();                 // redo = re-apply the command
        undoStack.push(c);
    }
}