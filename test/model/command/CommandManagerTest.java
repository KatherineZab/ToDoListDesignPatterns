package model.command;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the Command pattern infrastructure.
 * Focus: CommandManager's execute / undo / redo behavior (stacks and ordering).
 */
@DisplayName("CommandManager – Execute / Undo / Redo Tests")
class CommandManagerTest {

    private CommandManager manager;
    private TestCommand command1;
    private TestCommand command2;

    @BeforeEach
    void setUp() {
        manager = new CommandManager();
        command1 = new TestCommand("Action1");
        command2 = new TestCommand("Action2");
    }

    @Test
    @DisplayName("execute() should call command.execute() and put it on the undo stack")
    void executeCallsCommandAndPushesUndo() {
        manager.execute(command1);
        assertTrue(command1.wasExecuted());
        assertFalse(command1.wasUndone());
    }

    @Test
    @DisplayName("undo() should call undo() on the last executed command (LIFO)")
    void undoCallsUndoOnLastExecuted() {
        manager.execute(command1);
        manager.undo();

        assertTrue(command1.wasExecuted());
        assertTrue(command1.wasUndone());
    }

    @Test
    @DisplayName("undo() on empty stack should be a no-op (no exception)")
    void undoOnEmptyIsNoOp() {
        assertDoesNotThrow(() -> manager.undo());
    }

    @Test
    @DisplayName("redo() should re-execute the most recently undone command")
    void redoReExecutesUndoneCommand() {
        manager.execute(command1);
        manager.undo();

        // reset flags to verify redo calls execute() again
        command1.reset();

        manager.redo();

        assertTrue(command1.wasExecuted());
        assertFalse(command1.wasUndone());
    }

    @Test
    @DisplayName("redo() on empty redo stack should be a no-op (no exception)")
    void redoOnEmptyIsNoOp() {
        assertDoesNotThrow(() -> manager.redo());
    }

    @Test
    @DisplayName("Executing a new command after undo should clear the redo stack")
    void executeClearsRedoStack() {
        manager.execute(command1);
        manager.undo();           // command1 now sits in redo stack
        manager.execute(command2); // new execute should clear redo stack

        // redo should do nothing (command1 should NOT be redone)
        command1.reset();
        manager.redo();

        assertFalse(command1.wasExecuted());
    }

    @Test
    @DisplayName("Multiple undos should follow LIFO order")
    void multipleUndosAreLifo() {
        manager.execute(command1);
        manager.execute(command2);

        manager.undo(); // affects command2 first
        assertTrue(command2.wasUndone());
        assertFalse(command1.wasUndone());

        manager.undo(); // now affects command1
        assertTrue(command1.wasUndone());
    }

    @Test
    @DisplayName("Multiple redos should re-execute in the reverse order of undos")
    void multipleRedos() {
        manager.execute(command1);
        manager.execute(command2);
        manager.undo(); // undo command2
        manager.undo(); // undo command1

        // Reset to check re-execution through redo
        command1.reset();
        command2.reset();

        manager.redo(); // re-exec command1 first
        assertTrue(command1.wasExecuted());
        assertFalse(command2.wasExecuted());

        manager.redo(); // re-exec command2 next
        assertTrue(command1.wasExecuted());
        assertTrue(command2.wasExecuted());
    }

    @Test
    @DisplayName("Complex sequence: execute -> undo -> redo -> execute new -> undo twice")
    void complexSequence() {
        manager.execute(command1);
        assertTrue(command1.wasExecuted());

        manager.undo();
        assertTrue(command1.wasUndone());

        command1.reset();
        manager.redo();
        assertTrue(command1.wasExecuted());

        manager.execute(command2);     // clears redo stack
        assertTrue(command2.wasExecuted());

        manager.undo();                // undo command2
        assertTrue(command2.wasUndone());

        manager.undo();                // undo command1
        assertTrue(command1.wasUndone());
    }

    /**
     * Simple test double for Command.
     * Tracks whether execute()/undo() were called.
     */
    private static class TestCommand implements Command {
        private final String name;
        private boolean executed = false;
        private boolean undone = false;

        TestCommand(String name) {
            this.name = name;
        }

        @Override
        public void execute() {
            executed = true;
        }

        @Override
        public void undo() {
            undone = true;
        }

        boolean wasExecuted() { return executed; }
        boolean wasUndone()   { return undone;   }

        void reset() {
            executed = false;
            undone = false;
        }

        @Override
        public String toString() {
            return "TestCommand{" + name + "}";
        }
    }
}
