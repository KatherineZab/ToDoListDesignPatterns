package app;

import dao.*;
import model.*;

public class Main {
    public static void main(String[] args) throws Exception {
        ITasksDAO dao = TasksDAODerby.getInstance();

        try { dao.deleteTasks(); } catch (TasksDAOException ignored) {}

        //dao.addTask(new TaskRecord(0, "Buy milk", "2% milk", TaskState.TODO));
        dao.addTask(new TaskRecord(0, "Write report", "Visitor pattern", TaskState.IN_PROGRESS));
        dao.addTask(new TaskRecord(0, "Pay bills", "Electricity", TaskState.COMPLETED));

        System.out.println("All tasks:");
        for (var t : dao.getTasks()) {
            System.out.printf("#%d | %s | %s | %s%n",
                    t.getId(), t.getTitle(), t.getDescription(), t.getState());
        }

        var t2 = dao.getTask(2);
        if (t2 != null) {
            dao.updateTask(new TaskRecord(t2.getId(), t2.getTitle() + " (final)",
                    t2.getDescription(), TaskState.COMPLETED));
        }

        dao.deleteTask(1);
        System.out.println("Count after delete: " + dao.getTasks().length);
    }
}
