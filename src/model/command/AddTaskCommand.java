// src/model/command/AddTaskCommand.java
package model.command;

import view.TasksPanel;

public class AddTaskCommand implements Command {
    private final TasksPanel panel;
    private final String title, desc, state;
    private Integer generatedId = null; // היה int=-1 -> מחליפים ל-Integer

    public AddTaskCommand(TasksPanel panel, String title, String desc, String state) {
        this.panel = panel;
        this.title = title;
        this.desc = desc;
        this.state = state;
    }

    @Override public void execute() {
        if (generatedId == null) {
            // הפעלה ראשונה: מייצרים ID חדש ושומרים
            generatedId = panel.addRowReturningId(title, desc, state);
        } else {
            // Redo: משתמשים באותו ID שנשמר
            panel.addRowWithId(generatedId, title, desc, state);
        }
    }

    @Override public void undo() {
        if (generatedId != null) {
            panel.removeRowById(generatedId);
        }
    }
}
