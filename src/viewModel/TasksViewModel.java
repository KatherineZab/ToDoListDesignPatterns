package viewModel;

import dao.ITasksDAO;
import dao.TasksDAOException;
import model.ITask;
import model.TaskRecord;
import model.TaskState;
import model.entity.Priority;
import model.observable.TasksListener;
import model.observable.TasksRepository;

import java.util.*;
import java.util.function.Consumer;

public class TasksViewModel {

    private final ITasksDAO dao;
    private final List<ITask> cache = new ArrayList<>();

    // Observer hub lives in its own class/file
    private final TasksRepository observers = new TasksRepository();

    public TasksViewModel(ITasksDAO dao) {
        this.dao = Objects.requireNonNull(dao, "dao must not be null");
        try { load(); } catch (Exception ignored) {}
    }

    /* ---------------- Observer API (for Views) ---------------- */

    public void addTasksListener(TasksListener l) { observers.addListener(l); }
    public void removeTasksListener(TasksListener l) { observers.removeListener(l); }

    private void fireChanged() { observers.notifyListeners(cache); }

    /* ---------------- Queries ---------------- */

    /** Immutable view of current tasks cache. */
    public List<ITask> items() { return Collections.unmodifiableList(cache); }

    public ITask getById(int id) throws TasksDAOException {
        return dao.getTask(id);
    }

    public void load() throws TasksDAOException {
        cache.clear();
        Collections.addAll(cache, dao.getTasks());
        fireChanged(); // notifies on EDT
    }

    /* ---------- Add ---------- */

    public int addReturningId(String title, String desc, TaskState state) throws TasksDAOException {
        var tr = new TaskRecord(0, title, desc, state, Priority.NONE);
        if (dao instanceof ITasksDAOWithIds idDao) { // NEW: interface pattern matching
            int id = idDao.addTaskReturningId(tr);
            load();
            return id;
        } else {
            // Fallback: keep data consistent; caller will see there's no ID path
            dao.addTask(tr);
            load();
            // You can return -1 or throw; your UI already catches exceptions and uses -1.
            return -1;
        }
    }

    public void addWithId(int id, String title, String desc, TaskState state) throws TasksDAOException {
        var tr = new TaskRecord(id, title, desc, state, Priority.NONE);
        if (dao instanceof ITasksDAOWithIds idDao) { // NEW
            idDao.addTaskWithId(id, tr);
            load();
        } else {
            // If the underlying DAO doesn't support this, fail loudly or silently – your choice.
            throw new UnsupportedOperationException("addTaskWithId is not supported by this DAO");
        }
    }

    public int addWithPriorityReturningId(String title, String desc, TaskState state, Priority priority) throws TasksDAOException {
        var tr = new TaskRecord(0, title, desc, state, priority);
        if (dao instanceof ITasksDAOWithIds idDao) { // NEW
            int id = idDao.addTaskReturningId(tr);
            load();
            return id;
        } else {
            // Fallback: insert without returning id
            dao.addTask(tr);
            load();
            return -1;
        }
    }

    /* ---------- Update/Delete ---------- */

    public void update(int id, String title, String desc, TaskState newState) throws TasksDAOException {
        var current = dao.getTask(id);
        if (current == null) return;

        TaskState oldState = current.getState();
        var pr = (current instanceof TaskRecord r) ? r.priority() : Priority.NONE;

        // Enforce transition rules only when state actually changes
        if (oldState != newState && current instanceof TaskRecord r) {
            if (!r.state().canTransitionTo(newState)) {
                throw new IllegalStateException(oldState + " → " + newState + " not allowed");
            }
            dao.updateTask(new TaskRecord(id, title, desc, newState, p));
        } else {
            // גיבוי: אם אי פעם יגיע סוג אחר
            dao.updateTask(new TaskRecord(id, title, desc, newState, p));
        }

        dao.updateTask(new TaskRecord(id, title, desc, newState, pr));
        load(); // refresh & notify (EDT)
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

    public List<TaskState> allowedNextStatesOf(int taskId) throws TasksDAOException {
        var t = dao.getTask(taskId);
        if (t instanceof TaskRecord tr) return new ArrayList<>(tr.allowedNextStates());
        return Arrays.asList(TaskState.values());
    }

    /* ---------------- Helpers ---------------- */

    private Set<Integer> currentIds() {
        Set<Integer> s = new HashSet<>();
        for (var t : cache) s.add(t.getId());
        return s;
    }

    /* ---------- Reports & Export (Visitor) ---------- */

    /**
     * Builds a human-readable combined report using the Visitor + pattern matching.
     */
    public String buildCombinedReport() throws TasksDAOException {
        var visitor = new model.report.CombinedReportVisitor();
        for (model.ITask t : dao.getTasks()) {
            if (t instanceof model.TaskRecord tr) {
                visitor.visit(tr); // record pattern matching יופעל בתוך ה-visitor
            } else {
                // fallback: אם אי פעם יגיע ITask שאינו record, נעטוף ל-TaskRecord עם priority NONE
                var tr = new model.TaskRecord(
                        t.getId(),
                        t.getTitle(),
                        t.getDescription(),
                        t.getState(),
                        model.entity.Priority.NONE
                );
                visitor.visit(tr);
            }
        }
        return visitor.asText();
    }

    /**
     * Exports all tasks to CSV string using the Visitor + record pattern matching.
     */
    public String exportCSV() throws TasksDAOException {
        var visitor = new model.report.CSVExportVisitor();
        for (model.ITask t : dao.getTasks()) {
            if (t instanceof model.TaskRecord tr) {
                visitor.visit(tr);
            } else {
                var tr = new model.TaskRecord(
                        t.getId(),
                        t.getTitle(),
                        t.getDescription(),
                        t.getState(),
                        model.entity.Priority.NONE
                );
                visitor.visit(tr);
            }
        }
        return visitor.csv();
    }


}
