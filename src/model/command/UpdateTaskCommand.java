package model.command;

import model.ITask;
import model.TaskRecord;
import model.TaskState;
import model.entity.Priority;
import viewModel.TasksViewModel;

/** Command: Update a task via the ViewModel with solid undo/redo. */
public final class UpdateTaskCommand implements Command {
    private final TasksViewModel vm;
    private final int id;
    private final String newTitle;
    private final String newDesc;
    private final String newStateName;

    // Snapshots for reliable undo/redo
    private TaskRecord before;   // original state
    private TaskRecord after;    // target state (what we want after update)

    public UpdateTaskCommand(TasksViewModel vm, int id, String newTitle, String newDesc, String newStateName) {
        this.vm = vm;
        this.id = id;
        this.newTitle = newTitle;
        this.newDesc = newDesc;
        this.newStateName = newStateName;
    }

    @Override
    public void execute() {
        try {
            // Initialize snapshots only once
            if (before == null || after == null) {
                ITask cur = vm.items().stream().filter(x -> x.getId() == id).findFirst().orElse(null);
                if (cur == null) return;

                Priority p = (cur instanceof TaskRecord r) ? r.priority() : Priority.NONE;
                // before = exact current
                if (cur instanceof TaskRecord r) {
                    before = r;
                } else {
                    before = new TaskRecord(cur.getId(), cur.getTitle(), cur.getDescription(), cur.getState(), p);
                }
                // after = desired values (keep same priority)
                TaskState target = TaskState.valueOf(newStateName);
                after = new TaskRecord(id, newTitle, newDesc, target, p);
            }

            // Apply the 'after' snapshot
            vm.update(id, after.title(), after.description(), after.state());
            // If you later add UI for priority change, call vm.setPriority(id, after.priority());
        } catch (Exception e) {
            throw new RuntimeException("Update failed", e);
        }
    }

    @Override
    public void undo() {
        if (before == null) return;
        try {
            vm.update(before.id(), before.title(), before.description(), before.state());
            // Restore priority if needed
            vm.setPriority(before.id(), before.priority());
        } catch (Exception e) {
            throw new RuntimeException("Undo update failed", e);
        }
    }
}
