package model.command;

import view.TasksPanel;

public class AddTaskCommand implements Command {
    private final TasksPanel panel;
    private final String title, desc, state;
    private Integer generatedId = null;

    public AddTaskCommand(TasksPanel panel, String title, String desc, String state) {
        this.panel = panel;
        this.title = title;
        this.desc = desc;
        this.state = state;
    }

    @Override
    public void execute() {
        if (generatedId == null) {
            generatedId = panel.addRowReturningId(title, desc, state);
        } else {
            // For redo: just add again and get a new ID
            generatedId = panel.addRowReturningId(title, desc, state);
        }
    }

    @Override
    public void undo() {
        if (generatedId != null) {
            panel.removeRowById(generatedId);
        }
    }
}