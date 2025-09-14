package il.ac.hit.project.model.command;

/**
 * Basic contract for commands that can be executed and undone.
 * This is the foundation of the Command pattern - any operation that
 * needs undo/redo capability should implement this interface.
 */
public interface Command {

    /**
     * Performs the operation this command represents.
     * Should be safe to call multiple times (for redo functionality).
     * @throws RuntimeException if the operation fails
     */
    void execute();

    /**
     * Reverses what execute() did.
     * Should restore the system to the state before execute() was called.
     * @throws RuntimeException if undoing fails
     */
    void undo();
}