package model.command;

import model.ITask;
import model.TaskRecord;
import model.entity.Priority;
import viewModel.TasksViewModel;

import java.util.ArrayList;
import java.util.List;

/** Command: delete ALL tasks via the ViewModel, with proper undo/redo (no ID reuse). */
public final class DeleteAllTasksCommand implements Command {
    private final TasksViewModel vm;
    private List<TaskRecord> snapshot; // backup for undo (without preserving IDs)

    public DeleteAllTasksCommand(TasksViewModel vm) {
        this.vm = vm;
    }

    @Override
    public void execute() {
        try {
            // מצלמים את כל המשימות רק בפעם הראשונה
            if (snapshot == null) {
                // לוודא שאין פילטר פעיל כדי לצלם את כולן
                vm.clearFilter();
                snapshot = new ArrayList<>();
                for (ITask t : vm.items()) {
                    Priority p = (t instanceof TaskRecord tr) ? tr.priority() : Priority.NONE;
                    snapshot.add(new TaskRecord(
                            t.getId(), t.getTitle(), t.getDescription(), t.getState(), p));
                }
            }
            vm.deleteAll(); // מוחק הכל; ה-VM יעשה load() ויעדכן את ה-UI
        } catch (Exception e) {
            throw new RuntimeException("Clear-all failed", e);
        }
    }

    @Override
    public void undo() {
        if (snapshot == null) return;
        try {
            // משחזרים מבלי לשמר IDs (ה-DB יקצה חדשים)
            for (TaskRecord tr : snapshot) {
                vm.addWithPriorityReturningId(tr.title(), tr.description(), tr.state(), tr.priority());
            }
        } catch (Exception e) {
            throw new RuntimeException("Undo clear-all failed", e);
        }
    }
}
