package model.command;

import model.TaskState;
import viewModel.TasksViewModel;

/** Command: Add a task via the ViewModel (no UI coupling). */
public final class AddTaskCommand implements Command {
    private final TasksViewModel vm;
    private final String title, desc, stateName;
    private Integer generatedId;

    public AddTaskCommand(TasksViewModel vm, String title, String desc, String stateName) {
        this.vm = vm;
        this.title = title;
        this.desc = desc;
        this.stateName = stateName;
    }

    @Override
    public void execute() {
        try {
            var state = TaskState.valueOf(stateName);
            generatedId = vm.addReturningId(title, desc, state);
        } catch (Exception e) {
            throw new RuntimeException("Add failed", e);
        }
    }

    @Override
    public void undo() {
        if (generatedId != null) {
            try { vm.delete(generatedId); }
            catch (Exception e) { throw new RuntimeException("Undo add failed", e); }
        }
    }
}
