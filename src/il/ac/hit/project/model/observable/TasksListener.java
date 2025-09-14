package il.ac.hit.project.model.observable;

import il.ac.hit.project.model.ITask;
import java.util.List;

/**
 * Observer interface for receiving notifications when the tasks list changes.
 * Part of the Observer pattern implementation - listeners implement this
 * to react to task data updates automatically.
 */
@FunctionalInterface
public interface TasksListener {

    /**
     * Called when the tasks list has changed in any way.
     * Receives an immutable snapshot of the current task state.
     * This method is always called on the Swing Event Dispatch Thread
     * to ensure safe UI updates.
     * @param snapshot read-only view of the current tasks list
     */
    void onTasksChanged(List<ITask> snapshot);
}