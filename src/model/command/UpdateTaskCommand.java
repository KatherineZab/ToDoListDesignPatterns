package model.command;

import view.TasksPanel;

public class UpdateTaskCommand implements Command {
    private final TasksPanel panel;
    private final int id;
    private final String newTitle, newDesc, newState;

    private String oldTitle, oldDesc, oldState;

    public UpdateTaskCommand(TasksPanel panel, int id, String newTitle, String newDesc, String newState) {
        this.panel = panel;
        this.id = id;
        this.newTitle = newTitle;
        this.newDesc = newDesc;
        this.newState = newState;
    }

    @Override public void execute() {
        Object[] snap = panel.snapshotById(id);
        if (snap == null) return;
        oldTitle = (String) snap[1];
        oldDesc  = (String) snap[2];
        oldState = (String) snap[3];

        panel.setRowValuesById(id, newTitle, newDesc, newState);
    }

    @Override public void undo() {
        if (oldTitle != null) {
            panel.setRowValuesById(id, oldTitle, oldDesc, oldState);
        }
    }
}
