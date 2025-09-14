package model.state;

import model.TaskRecord;
import model.TaskState;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the State pattern implementation:
 * - TaskState transitions and helpers
 * - TaskRecord state transitions (with validation)
 */
@DisplayName("TaskState & TaskRecord – State Pattern Tests")
class TaskStateTest {

    @Test
    @DisplayName("TO_DO allows transition only to IN_PROGRESS")
    void toDoTransitions() {
        TaskState toDo = TaskState.TO_DO;

        assertTrue(toDo.canTransitionTo(TaskState.IN_PROGRESS));
        assertFalse(toDo.canTransitionTo(TaskState.TO_DO));
        assertFalse(toDo.canTransitionTo(TaskState.COMPLETED));
    }

    @Test
    @DisplayName("IN_PROGRESS allows transition only to COMPLETED")
    void inProgressTransitions() {
        TaskState inProgress = TaskState.IN_PROGRESS;

        assertTrue(inProgress.canTransitionTo(TaskState.COMPLETED));
        assertFalse(inProgress.canTransitionTo(TaskState.TO_DO));
        assertFalse(inProgress.canTransitionTo(TaskState.IN_PROGRESS));
    }

    @Test
    @DisplayName("COMPLETED allows REOPEN to TO_DO")
    void completedTransitions() {
        TaskState completed = TaskState.COMPLETED;

        assertTrue(completed.canTransitionTo(TaskState.TO_DO));    // reopen
        assertFalse(completed.canTransitionTo(TaskState.IN_PROGRESS));
        assertFalse(completed.canTransitionTo(TaskState.COMPLETED));
    }

    @Test
    @DisplayName("nextStates() returns the correct allowed states")
    void nextStatesContract() {
        assertEquals(1, TaskState.TO_DO.nextStates().size());
        assertTrue(TaskState.TO_DO.nextStates().contains(TaskState.IN_PROGRESS));

        assertEquals(1, TaskState.IN_PROGRESS.nextStates().size());
        assertTrue(TaskState.IN_PROGRESS.nextStates().contains(TaskState.COMPLETED));

        assertEquals(1, TaskState.COMPLETED.nextStates().size());
        assertTrue(TaskState.COMPLETED.nextStates().contains(TaskState.TO_DO));
    }

    @Test
    @DisplayName("isTerminal() is false for all states (since COMPLETED can reopen)")
    void isTerminalContract() {
        assertFalse(TaskState.TO_DO.isTerminal());
        assertFalse(TaskState.IN_PROGRESS.isTerminal());
        assertFalse(TaskState.COMPLETED.isTerminal());
    }

    @Test
    @DisplayName("badge() returns user-friendly labels")
    void badgeText() {
        assertEquals("To Do", TaskState.TO_DO.badge());
        assertEquals("In Progress", TaskState.IN_PROGRESS.badge());
        assertEquals("Completed", TaskState.COMPLETED.badge());
    }

    @Test
    @DisplayName("TaskRecord.withState validates transitions")
    void taskRecordTransitionValidation() {
        TaskRecord task = new TaskRecord(
                1, "Test task", "Description",
                TaskState.TO_DO, model.entity.Priority.NONE
        );

        // Valid: TO_DO -> IN_PROGRESS
        TaskRecord inProgress = task.withState(TaskState.IN_PROGRESS);
        assertEquals(TaskState.IN_PROGRESS, inProgress.state());
        assertEquals(task.id(), inProgress.id());
        assertEquals(task.title(), inProgress.title());
        assertEquals(task.description(), inProgress.description());
        assertEquals(task.priority(), inProgress.priority());

        // Invalid: TO_DO -> COMPLETED
        assertThrows(IllegalStateException.class,
                () -> task.withState(TaskState.COMPLETED));
    }

    @Test
    @DisplayName("Full lifecycle: TO_DO -> IN_PROGRESS -> COMPLETED -> TO_DO")
    void fullLifecycle() {
        TaskRecord task = new TaskRecord(
                1, "Test", "Desc", TaskState.TO_DO, model.entity.Priority.NONE
        );

        TaskRecord step1 = task.withState(TaskState.IN_PROGRESS);
        assertEquals(TaskState.IN_PROGRESS, step1.state());

        TaskRecord step2 = step1.withState(TaskState.COMPLETED);
        assertEquals(TaskState.COMPLETED, step2.state());

        TaskRecord step3 = step2.withState(TaskState.TO_DO); // reopen
        assertEquals(TaskState.TO_DO, step3.state());
    }

    @Test
    @DisplayName("allowedNextStates() matches the transition rules")
    void allowedNextStatesContract() {
        TaskRecord todoTask = new TaskRecord(
                1, "Test", "Desc", TaskState.TO_DO, model.entity.Priority.NONE
        );
        TaskRecord inProgressTask = new TaskRecord(
                1, "Test", "Desc", TaskState.IN_PROGRESS, model.entity.Priority.NONE
        );
        TaskRecord completedTask = new TaskRecord(
                1, "Test", "Desc", TaskState.COMPLETED, model.entity.Priority.NONE
        );

        assertEquals(1, todoTask.allowedNextStates().size());
        assertTrue(todoTask.allowedNextStates().contains(TaskState.IN_PROGRESS));

        assertEquals(1, inProgressTask.allowedNextStates().size());
        assertTrue(inProgressTask.allowedNextStates().contains(TaskState.COMPLETED));

        assertEquals(1, completedTask.allowedNextStates().size());
        assertTrue(completedTask.allowedNextStates().contains(TaskState.TO_DO));
    }

    @Test
    @DisplayName("Invalid skip transitions are rejected with an exception")
    void invalidSkipTransitions() {
        TaskRecord task = new TaskRecord(
                1, "Test", "Desc", TaskState.TO_DO, model.entity.Priority.NONE
        );

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> task.withState(TaskState.COMPLETED));

        String msg = ex.getMessage() == null ? "" : ex.getMessage();
        assertTrue(msg.toLowerCase().contains("not allowed"));
        assertTrue(msg.contains("TO_DO"));
        assertTrue(msg.contains("COMPLETED"));
    }
}
