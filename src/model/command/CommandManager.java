package model.command;

import java.util.ArrayDeque;
import java.util.Deque;

public final class CommandManager {
    private final Deque<Command> undoStack = new ArrayDeque<>();
    private final Deque<Command> redoStack = new ArrayDeque<>();

    public void execute(Command c) {
        c.execute();
        undoStack.push(c);
        redoStack.clear();           // clear redo after a new action
    }

    public void undo() {
        if (undoStack.isEmpty()) return;
        Command c = undoStack.pop();
        c.undo();
        redoStack.push(c);
    }

    public void redo() {
        if (redoStack.isEmpty()) return;
        Command c = redoStack.pop();
        c.execute();                 // redo = re-apply the command
        undoStack.push(c);
    }
}
