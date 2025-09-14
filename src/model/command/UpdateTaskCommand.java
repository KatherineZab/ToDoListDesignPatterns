package model.command;

import model.ITask;
import model.TaskRecord;
import model.TaskState;
import model.entity.Priority;
import viewModel.TasksViewModel;

/**
 * Command that updates a task's title, description, and state with full undo support.
 * Uses a two-snapshot approach: captures the original state for undo and
 * builds the target state for consistent redo operations.
 */
public final class UpdateTaskCommand implements Command {
    private final TasksViewModel vm;
    private final int id;
    private final String newTitle;
    private final String newDesc;
    private final String newStateName;

    // Two-snapshot strategy for reliable undo/redo
    private TaskRecord before;   // original state before any changes
    private TaskRecord after;    // target state we want to achieve

    /**
     * Creates a command to update a task's basic information.
     * @param vm the ViewModel that handles task updates
     * @param id which task to update
     * @param newTitle the new title text
     * @param newDesc the new description text
     * @param newStateName the new state as a string (e.g., "IN_PROGRESS")
     */
    public UpdateTaskCommand(TasksViewModel vm, int id, String newTitle, String newDesc, String newStateName) {
        this.vm = vm;
        this.id = id;
        this.newTitle = newTitle;
        this.newDesc = newDesc;
        this.newStateName = newStateName;
    }

    /**
     * Updates the task with the new values after capturing snapshots.
     * On first execution, creates both before and after snapshots for
     * reliable undo/redo. The priority level is preserved from the original task.
     * On subsequent executions (redo), applies the pre-built after snapshot.
     * @throws RuntimeException if the task doesn't exist or update fails
     */
    @Override
    public void execute() {
        try {
            // Create snapshots only once, on first execution
            if (before == null || after == null) {
                ITask cur = vm.items().stream().filter(x -> x.getId() == id).findFirst().orElse(null);
                if (cur == null) return;

                Priority p = (cur instanceof TaskRecord r) ? r.priority() : Priority.NONE;

                // Capture exact current state
                if (cur instanceof TaskRecord r) {
                    before = r;
                } else {
                    before = new TaskRecord(cur.getId(), cur.getTitle(), cur.getDescription(), cur.getState(), p);
                }

                // Build target state with new values but same priority
                TaskState target = TaskState.valueOf(newStateName);
                after = new TaskRecord(id, newTitle, newDesc, target, p);
            }

            // Apply the target state
            vm.update(id, after.title(), after.description(), after.state());
        } catch (Exception e) {
            throw new RuntimeException("Update failed", e);
        }
    }

    /**
     * Restores the task to its original state before the update.
     * Uses the ViewModel's applyFromHistory method to restore the complete
     * original state including priority information.
     * @throws RuntimeException if restoration fails
     */
    @Override
    public void undo() {
        if (before == null) return;
        try {
            vm.applyFromHistory(before);   // Full restoration without validation
        } catch (RuntimeException e) {
            throw new RuntimeException("Undo update failed", e);
        }
    }
}