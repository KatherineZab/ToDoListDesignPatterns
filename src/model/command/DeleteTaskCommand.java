// src/model/command/DeleteTaskCommand.java
package model.command;

import view.TasksPanel;

public class DeleteTaskCommand implements Command {
    private final TasksPanel panel;
    private final int id;

    private Object[] deletedRow; // {id,title,desc,state}
    private int originalModelIndex = -1;

    public DeleteTaskCommand(TasksPanel panel, int id) {
        this.panel = panel;
        this.id = id;
    }

    @Override public void execute() {
        // שומרים צילום + מיקום במודל
        deletedRow = panel.snapshotById(id);
        originalModelIndex = panel.modelIndexById(id);
        if (deletedRow != null) {
            panel.removeRowById(id);
        }
    }

    @Override public void undo() {
        if (deletedRow != null) {
            int did   = (int)    deletedRow[0];
            String t  = (String) deletedRow[1];
            String d  = (String) deletedRow[2];
            String st = (String) deletedRow[3];
            // החזרה לאותו אינדקס
            panel.addRowWithIdAt(originalModelIndex, did, t, d, st);
        }
    }
}
