package viewmodel;

import dao.ITasksDAO;
import dao.TasksDAOException;
import model.ITask;
import model.TaskRecord;
import model.TaskState;
import model.entity.Priority;

import java.util.*;
import java.util.function.Consumer;

public class TasksViewModel {

    private final ITasksDAO dao;
    private final List<ITask> cache = new ArrayList<>();
    private final List<Consumer<List<ITask>>> listeners = new ArrayList<>();

    public TasksViewModel(ITasksDAO dao) {
        this.dao = dao;
        try { load(); } catch (Exception ignored) {}
    }

    public void addListener(Consumer<List<ITask>> l) { listeners.add(l); }
    private void notifyListeners() {
        var ro = Collections.unmodifiableList(cache);
        for (var l : listeners) l.accept(ro);
    }

    public List<ITask> items() { return cache; }

    public void load() throws TasksDAOException {
        cache.clear();
        cache.addAll(Arrays.asList(dao.getTasks()));
        notifyListeners();
    }

    /* ---------- Add ---------- */

    public int addReturningId(String title, String desc, TaskState state) throws TasksDAOException {
        var tr = new TaskRecord(0, title, desc, state, Priority.NONE);
        // מתודת הרחבה קיימת אצלך ב-DAO:
        int id = ((dao.TasksDAODerby) dao).addTaskReturningId(tr);
        load();
        return id;
    }

    public void addWithId(int id, String title, String desc, TaskState state) throws TasksDAOException {
        var tr = new TaskRecord(id, title, desc, state, Priority.NONE);
        ((dao.TasksDAODerby) dao).addTaskWithId(id, tr);
        load();
    }

    public int addWithPriorityReturningId(String title, String desc, TaskState state, Priority priority) throws TasksDAOException {
        var tr = new TaskRecord(0, title, desc, state, priority);
        int id = ((dao.TasksDAODerby) dao).addTaskReturningId(tr);
        load();
        return id;
    }

    /* ---------- Update/Delete ---------- */

    public void update(int id, String title, String desc, TaskState state) throws TasksDAOException {
        // שומרים על priority קיים
        var current = dao.getTask(id);
        Priority p = (current instanceof TaskRecord tr) ? tr.priority() : Priority.NONE;
        dao.updateTask(new TaskRecord(id, title, desc, state, p));
        load();
    }

    public void delete(int id) throws TasksDAOException {
        dao.deleteTask(id);
        load();
    }

    /* ---------- Priority API ל-View ---------- */

    public void setPriority(int id, Priority p) throws TasksDAOException {
        var current = dao.getTask(id);
        if (current == null) return;
        var tr = (current instanceof TaskRecord old)
                ? new TaskRecord(old.id(), old.title(), old.description(), old.state(), p)
                : new TaskRecord(current.getId(), current.getTitle(), current.getDescription(), current.getState(), p);
        dao.updateTask(tr);
        load();
    }
}
