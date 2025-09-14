package model.command;

import model.ITask;
import model.TaskRecord;
import model.entity.Priority;
import viewModel.TasksViewModel;

import java.util.ArrayList;
import java.util.List;

/**
 * Command that removes all tasks from the system with full undo support.
 * Takes a complete snapshot on first execution so tasks can be restored
 * if needed. Restored tasks get new IDs but keep all their original data.
 */
public final class DeleteAllTasksCommand implements Command {
    private final TasksViewModel vm;
    private List<TaskRecord> snapshot; // backup for undo (without preserving IDs)

    /**
     * Creates a command to delete all tasks in the system.
     * @param vm the ViewModel that handles the actual deletion operations
     */
    public DeleteAllTasksCommand(TasksViewModel vm) {
        this.vm = vm;
    }

    /**
     * Deletes all tasks after creating a backup snapshot.
     * On first run, captures every task for undo purposes.
     * Clears filters first to ensure we backup everything, not just visible tasks.
     * @throws RuntimeException if deletion fails
     */
    @Override
    public void execute() {
        try {
            // snapshoting just the first time
            if (snapshot == null) {
                vm.clearFilter();
                snapshot = new ArrayList<>();
                for (ITask t : vm.items()) {
                    Priority p = (t instanceof TaskRecord tr) ? tr.priority() : Priority.NONE;
                    snapshot.add(new TaskRecord(
                            t.getId(), t.getTitle(), t.getDescription(), t.getState(), p));
                }
            }
            vm.deleteAll(); // deletes everything; VM will reload and update UI
        } catch (Exception e) {
            throw new RuntimeException("Clear-all failed", e);
        }
    }

    /**
     * Restores all tasks from the backup snapshot.
     * Recreates each task with new IDs but original content.
     * Does nothing if no snapshot exists.
     * @throws RuntimeException if restoration fails
     */
    @Override
    public void undo() {
        if (snapshot == null) return;
        try {
            // recreate with new IDs
            for (TaskRecord tr : snapshot) {
                vm.addWithPriorityReturningId(tr.title(), tr.description(), tr.state(), tr.priority());
            }
        } catch (Exception e) {
            throw new RuntimeException("Undo clear-all failed", e);
        }
    }
}