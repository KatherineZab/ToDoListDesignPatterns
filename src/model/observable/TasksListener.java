package model.observable;

import model.ITask;
import java.util.List;

/** Observer for changes in the tasks list. */
@FunctionalInterface
public interface TasksListener {
    /** Called with an immutable snapshot of the current tasks. Runs on the EDT. */
    void onTasksChanged(List<ITask> snapshot);
}
