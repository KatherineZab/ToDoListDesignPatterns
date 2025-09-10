package viewmodel;

import dao.ITasksDAO;
import dao.TasksDAOException;
import model.ITask;
import model.TaskRecord;
import model.TaskState;

import java.util.*;
import java.util.function.Consumer;

public class TasksViewModel {

    private final ITasksDAO dao;
    private final List<ITask> cache = new ArrayList<>();
    private final List<Consumer<List<ITask>>> listeners = new ArrayList<>();

    public TasksViewModel(ITasksDAO dao) { this.dao = dao; }

    // Observer API
    public void addListener(Consumer<List<ITask>> l) { listeners.add(l); }
    private void notifyListeners() {
        var snap = List.copyOf(cache);
        for (var l: listeners) l.accept(snap);
    }

    public List<ITask> items() { return List.copyOf(cache); }

    public void load() throws TasksDAOException {
        cache.clear();
        cache.addAll(Arrays.asList(dao.getTasks()));
        notifyListeners();
    }

    public int addReturningId(String title, String desc, TaskState state) throws TasksDAOException {
        int id = dao.addTaskReturningId(new TaskRecord(0, title, desc, state));
        load();
        return id;
    }

    public void addWithId(int id, String title, String desc, TaskState state) throws TasksDAOException {
        dao.addTaskWithId(id, new TaskRecord(id, title, desc, state));
        load();
    }

    public void update(int id, String title, String desc, TaskState state) throws TasksDAOException {
        dao.updateTask(new TaskRecord(id, title, desc, state));
        load();
    }

    public void delete(int id) throws TasksDAOException {
        dao.deleteTask(id);
        load();
    }
}
