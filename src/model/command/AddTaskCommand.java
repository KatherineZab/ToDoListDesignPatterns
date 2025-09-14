package model.command;

import model.TaskState;
import viewModel.TasksViewModel;

/**
 * Command that handles adding a new task to the system.
 * Wraps the add operation so it can be undone later if needed.
 * Works through the ViewModel to keep business logic separate from UI.
 */
public final class AddTaskCommand implements Command {
    private final TasksViewModel vm;
    private final String title, desc, stateName;
    private Integer generatedId;

    /**
     * Creates a command to add a new task with the given details.
     * @param vm the ViewModel that will handle the actual task creation
     * @param title what the task is called
     * @param desc additional details about the task
     * @param stateName the starting state like "TO_DO" or "IN_PROGRESS"
     * @throws IllegalArgumentException if vm is null or parameters are invalid
     */
    public AddTaskCommand(TasksViewModel vm, String title, String desc, String stateName) {
        this.vm = vm;
        this.title = title;
        this.desc = desc;
        this.stateName = stateName;
    }

    /**
     * Actually creates the task in the system.
     * Remembers the task ID so we can delete it later if undoing.
     * @throws RuntimeException if creating the task fails
     */
    @Override
    public void execute() {
        try {
            var state = TaskState.valueOf(stateName);
            generatedId = vm.addReturningId(title, desc, state);
        } catch (Exception e) {
            throw new RuntimeException("Add failed", e);
        }
    }

    /**
     * Removes the task that was created during execute().
     * Only works if a task was actually created successfully.
     * This is how the "undo" functionality works.
     * @throws RuntimeException if deleting the task fails
     */
    @Override
    public void undo() {
        if (generatedId != null) {
            try { vm.delete(generatedId); }
            catch (Exception e) { throw new RuntimeException("Undo add failed", e); }
        }
    }
}