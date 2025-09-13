package viewmodel;

import dao.ITasksDAO;
import dao.TasksDAOException;
import model.ITask;
import model.TaskRecord;
import model.TaskState;
import model.entity.Priority;
import dao.ITasksDAOWithIds;
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
        // שמירת priority קיים
        var p = (current instanceof TaskRecord tr) ? tr.priority() : Priority.NONE;

        // בדיקת חוקיות מעבר לפי ה-STATE enum
        if (current instanceof TaskRecord tr) {
            if (!tr.state().canTransitionTo(newState)) {
                throw new IllegalStateException(tr.state() + " → " + newState + " not allowed");
            }
            dao.updateTask(new TaskRecord(id, title, desc, newState, p));
        } else {
            // גיבוי: אם אי פעם יגיע סוג אחר
            dao.updateTask(new TaskRecord(id, title, desc, newState, p));
        }
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

    public java.util.List<model.TaskState> allowedNextStatesOf(int taskId) throws dao.TasksDAOException {
        var t = dao.getTask(taskId);
        if (t instanceof model.TaskRecord tr) {
            return new java.util.ArrayList<>(tr.allowedNextStates());
        }
        // fallback אם אי פעם זה לא TaskRecord:
        return java.util.Arrays.asList(model.TaskState.values());
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
