package model.observable;

import model.ITask;

import javax.swing.SwingUtilities;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Observable hub for tasks list changes.
 * Keeps listeners and notifies them on the Swing EDT with an immutable snapshot.
 */
public final class TasksRepository {
    private final CopyOnWriteArrayList<TasksListener> listeners = new CopyOnWriteArrayList<>();

    public void addListener(TasksListener l) {
        if (l != null) listeners.addIfAbsent(l);
    }

    public void removeListener(TasksListener l) {
        if (l != null) listeners.remove(l);
    }

    /** Notify all listeners with a defensive, unmodifiable snapshot on the EDT. */
    public void notifyListeners(List<ITask> current) {
        List<ITask> snapshot = Collections.unmodifiableList(new ArrayList<>(current));
        Runnable r = () -> {
            for (TasksListener l : listeners) {
                try { l.onTasksChanged(snapshot); } catch (Throwable ignored) {}
            }
        };
        if (SwingUtilities.isEventDispatchThread()) r.run();
        else SwingUtilities.invokeLater(r);
    }
}
