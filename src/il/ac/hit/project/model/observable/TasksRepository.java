package il.ac.hit.project.model.observable;

import il.ac.hit.project.model.ITask;

import javax.swing.SwingUtilities;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Central hub for task change notifications using the Observer pattern.
 * Maintains a list of listeners and ensures they're notified safely on the
 * Swing EDT with immutable data snapshots. Thread-safe for concurrent access.
 */
public final class TasksRepository {

    /** Thread-safe listener collection that supports concurrent modifications */
    private final CopyOnWriteArrayList<TasksListener> listeners = new CopyOnWriteArrayList<>();

    /**
     * Registers a listener to receive task change notifications.
     * Duplicate listeners are automatically prevented.
     * @param l the listener to add; null values are ignored
     */
    public void addListener(TasksListener l) {
        if (l != null) listeners.addIfAbsent(l);
    }

    /**
     * Unregisters a listener from receiving notifications.
     * @param l the listener to remove; null values are ignored
     */
    public void removeListener(TasksListener l) {
        if (l != null) listeners.remove(l);
    }

    /**
     * Notifies all registered listeners with a snapshot of current tasks.
     * Creates a defensive copy to prevent external modifications and ensures
     * all notifications happen on the Swing EDT for thread safety.
     * Swallows individual listener exceptions to prevent one bad listener
     * from affecting others.
     * @param current the current tasks list to broadcast
     */
    public void notifyListeners(List<ITask> current) {
        // Create immutable defensive copy
        List<ITask> snapshot = Collections.unmodifiableList(new ArrayList<>(current));

        Runnable notifyTask = () -> {
            for (TasksListener l : listeners) {
                try {
                    l.onTasksChanged(snapshot);
                } catch (Throwable ignored) {
                    // Isolate listener failures - don't let one bad listener break others
                }
            }
        };

        // Ensure EDT execution for safe Swing updates
        if (SwingUtilities.isEventDispatchThread()) {
            notifyTask.run();
        } else {
            SwingUtilities.invokeLater(notifyTask);
        }
    }
}