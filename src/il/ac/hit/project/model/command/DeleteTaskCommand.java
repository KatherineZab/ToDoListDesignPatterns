package il.ac.hit.project.model.command;

import il.ac.hit.project.model.ITask;
import il.ac.hit.project.model.TaskRecord;
import il.ac.hit.project.model.entity.Priority;
import il.ac.hit.project.viewmodel.TasksViewModel;

/**
 * Command that deletes a single task with proper undo/redo support.
 * The tricky part is handling ID changes - when we restore a deleted task,
 * it gets a new ID, so we need to track that for future redo operations.
 */
public final class DeleteTaskCommand implements Command {
    private final TasksViewModel vm;
    private int id;                 //  gets updated when task is restored with new ID
    private TaskRecord snapshot;    // complete backup of the deleted task

    /**
     * Creates a command to delete a specific task.
     * @param vm the ViewModel that handles task operations
     * @param id the ID of the task to delete
     */
    public DeleteTaskCommand(TasksViewModel vm, int id) {
        this.vm = vm;
        this.id = id;
    }

    /**
     * Deletes the task after capturing a complete snapshot for undo.
     * Finds the task in the ViewModel's cache and creates a backup copy
     * with all details including priority information.
     * Does nothing if the task doesn't exist.
     * @throws RuntimeException if deletion fails
     */
    @Override
    public void execute() {
        try {
            // Find and snapshot the task before deleting it
            ITask t = vm.items().stream().filter(x -> x.getId() == id).findFirst().orElse(null);
            if (t == null) return;

            if (t instanceof TaskRecord tr) {
                snapshot = tr;
            } else {
                snapshot = new TaskRecord(t.getId(), t.getTitle(), t.getDescription(),
                        t.getState(), Priority.NONE);
            }
            vm.delete(id);
        } catch (Exception e) {
            throw new RuntimeException("Delete failed", e);
        }
    }

    /**
     * Restores the deleted task by recreating it from the snapshot.
     * The restored task gets a new ID from the database, so we update
     * our tracking ID to point to the new task. This ensures that if
     * the user redoes this command, we'll delete the right task.
     * @throws RuntimeException if restoration fails
     */
    @Override
    public void undo() {
        if (snapshot == null) return;
        try {
            // Recreate the task - it will get a new ID
            int newId = vm.addReturningId(snapshot.title(), snapshot.description(), snapshot.state());
            vm.setPriority(newId, snapshot.priority());
            this.id = newId;   // Critical: update our ID reference for future operations
        } catch (Exception e) {
            throw new RuntimeException("Undo delete failed", e);
        }
    }
}