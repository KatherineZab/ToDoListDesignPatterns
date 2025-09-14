package model.command;

import model.ITask;
import model.TaskRecord;
import model.entity.Priority;
import viewModel.TasksViewModel;

/** Command: שינוי Priority עם Undo/Redo תקין דרך ה-ViewModel. */
public final class ChangePriorityCommand implements Command {
    private final TasksViewModel vm;
    private final int id;
    private final Priority newPriority;

    private Priority oldPriority;   // נשמר פעם אחת לצורך undo
    private boolean initialized = false;

    public ChangePriorityCommand(TasksViewModel vm, int id, Priority newPriority) {
        this.vm = vm;
        this.id = id;
        this.newPriority = (newPriority == null) ? Priority.NONE : newPriority;
    }

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
