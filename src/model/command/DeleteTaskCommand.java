package model.command;

import view.TasksPanel;

public class DeleteTaskCommand implements Command {
    private final TasksPanel panel;
    private int currentId;

    private Object[] deletedRow; // {id,title,desc,priority,state}

    public DeleteTaskCommand(TasksPanel panel, int id) {
        this.panel = panel;
        this.currentId = id;
    }

    @Override
    public void execute() {
        deletedRow = panel.snapshotById(currentId);
        if (deletedRow != null) {
            panel.removeRowById(currentId);
        }
    }

    @Override
    public void undo() {
        if (deletedRow != null) {
            String t  = (String) deletedRow[1];  // title
            String d  = (String) deletedRow[2];  // description
            String p  = (String) deletedRow[3];  // priority
            String st = (String) deletedRow[4];  // state

            // Restore with priority
            currentId = panel.addRowWithPriorityReturningId(t, d, st, p);
        }
    }
}