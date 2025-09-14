package model.command;

import model.ITask;
import model.TaskRecord;
import model.entity.Priority;
import viewModel.TasksViewModel;

/**
 * Command that changes a task's priority level.
 * Remembers the old priority so the change can be undone later.
 * Uses lazy initialization to capture the original priority only when needed.
 */
public final class ChangePriorityCommand implements Command {
    private final TasksViewModel vm;
    private final int id;
    private final Priority newPriority;

    private Priority oldPriority;   // Stored once for undo purposes
    private boolean initialized = false;

    /**
     * Creates a command to change a task's priority.
     * @param vm the ViewModel that handles priority changes
     * @param id which task to change (must be a valid task ID)
     * @param newPriority what priority to set (null becomes NONE)
     */
    public ChangePriorityCommand(TasksViewModel vm, int id, Priority newPriority) {
        this.vm = vm;
        this.id = id;
        this.newPriority = (newPriority == null) ? Priority.NONE : newPriority;
    }

    /**
     * Changes the task's priority to the new value.
     * On first run, saves the current priority for undo.
     * On redo, just applies the new priority again.
     * @throws RuntimeException if the task doesn't exist or priority change fails
     */
    @Override
    public void execute() {
        try {
            if (!initialized) {
                ITask cur = vm.getById(id);
                if (cur instanceof TaskRecord tr) {
                    oldPriority = tr.priority();
                } else {
                    oldPriority = Priority.NONE;
                }
                initialized = true;
            }
            vm.setPriority(id, newPriority);
        } catch (Exception e) {
            throw new RuntimeException("Change priority failed", e);
        }
    }

    /**
     * Restores the task's original priority.
     * Only works if execute() was called first to capture the old value.
     * @throws RuntimeException if restoring the priority fails
     */
    @Override
    public void undo() {
        if (!initialized || oldPriority == null) return;
        try {
            vm.setPriority(id, oldPriority);
        } catch (Exception e) {
            throw new RuntimeException("Undo change priority failed", e);
        }
    }
}