package model.command;

import model.ITask;
import model.TaskRecord;
import model.entity.Priority;
import viewModel.TasksViewModel;

/** Command: Delete a task via the ViewModel (supports undo/redo correctly). */
public final class DeleteTaskCommand implements Command {
    private final TasksViewModel vm;
    private int id;                 // <-- must be mutable for redo to work
    private TaskRecord snapshot;    // for undo

    public DeleteTaskCommand(TasksViewModel vm, int id) {
        this.vm = vm;
        this.id = id;
    }

    @Override
    public void execute() {
        try {
            // capture current snapshot (from VM cache if possible)
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

    @Override
    public void undo() {
        if (snapshot == null) return;
        try {
            // re-add and capture the NEW id, so redo() deletes the right row
            int newId = vm.addReturningId(snapshot.title(), snapshot.description(), snapshot.state());
            vm.setPriority(newId, snapshot.priority());
            this.id = newId;   // <-- critical line for redo
        } catch (Exception e) {
            throw new RuntimeException("Undo delete failed", e);
        }
    }
}
