package viewModel;

import dao.ITasksDAO;
import dao.TasksDAOException;
import model.ITask;
import model.TaskRecord;
import model.TaskState;
import model.entity.Priority;
import model.observable.TasksListener;
import model.observable.TasksRepository;
import model.sort.TaskSortStrategy;        // <-- Strategy lives in VM now

import java.util.*;

public class TasksViewModel {

    private final ITasksDAO dao;

    /** Raw cache, exactly as stored in the DB (no view concerns here). */
    private final List<ITask> cache = new ArrayList<>();

    /** Strategy: current sorting policy selected by the user (may be null). */
    private TaskSortStrategy sortStrategy = null;

    /** Observer hub (separate files, EDT-safe). */
    private final TasksRepository observers = new TasksRepository();

    public TasksViewModel(ITasksDAO dao) {
        this.dao = Objects.requireNonNull(dao, "dao must not be null");
        try { load(); } catch (Exception ignored) {}
    }

    /* ---------------- Observer API (for Views) ---------------- */

    public void addTasksListener(TasksListener l) { observers.addListener(l); }
    public void removeTasksListener(TasksListener l) { observers.removeListener(l); }

    /** Notifies with a **sorted** immutable snapshot. */
    private void fireChanged() { observers.notifyListeners(items()); }

    /* ---------------- Sorting (Strategy) ---------------- */

    /** Set/clear the current sorting strategy. */
    public void setSortStrategy(TaskSortStrategy strategy) {
        this.sortStrategy = strategy;
        fireChanged(); // let views re-render in the new order
    }

    private List<ITask> applySort(List<ITask> src) {
        if (sortStrategy == null) return List.copyOf(src);
        return src.stream().sorted(sortStrategy.comparator()).toList();
    }

    /* ---------------- Queries ---------------- */

    /** Immutable, **sorted** view of the current tasks according to the strategy. */
    public List<ITask> items() { return Collections.unmodifiableList(applySort(cache)); }

    public ITask getById(int id) throws TasksDAOException { return dao.getTask(id); }

    public void load() throws TasksDAOException {
        cache.clear();
        Collections.addAll(cache, dao.getTasks());
        fireChanged(); // sorted snapshot goes to observers
    }

    /* ---------------- Create ---------------- */

    public int addReturningId(String title, String desc, TaskState state) throws TasksDAOException {
        var tr = new TaskRecord(0, title, desc, state, Priority.NONE);
        Set<Integer> before = currentIds();
        dao.addTask(tr);
        load();
        return findNewIdAfterAdd(before, title, desc);
    }

    /** Best-effort add with id (DB may ignore id and assign a new one). */
    public void addWithId(int id, String title, String desc, TaskState state) throws TasksDAOException {
        var tr = new TaskRecord(id, title, desc, state, Priority.NONE);
        dao.addTask(tr);
        load();
    }

    public int addWithPriorityReturningId(String title, String desc,
                                          TaskState state, Priority priority) throws TasksDAOException {
        var tr = new TaskRecord(0, title, desc, state, priority);
        Set<Integer> before = currentIds();
        dao.addTask(tr);
        load();
        return findNewIdAfterAdd(before, title, desc);
    }

    /* ---------------- Update / Delete ---------------- */

    public void update(int id, String title, String desc, TaskState newState) throws TasksDAOException {
        var current = dao.getTask(id);
        if (current == null) return;

        TaskState oldState = current.getState();
        var pr = (current instanceof TaskRecord r) ? r.priority() : Priority.NONE;

        // Enforce transition rules only when the state actually changes
        if (oldState != newState && current instanceof TaskRecord r) {
            if (!r.state().canTransitionTo(newState)) {
                throw new IllegalStateException(oldState + " → " + newState + " not allowed");
            }
        }

        dao.updateTask(new TaskRecord(id, title, desc, newState, pr));
        load(); // refresh & notify observers with sorted snapshot
    }

    public void delete(int id) throws TasksDAOException {
        dao.deleteTask(id);
        load();
    }

    /* ---------------- Priority ---------------- */

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

    /* ---------------- Reports (Visitor kept in VM) ---------------- */

    /** Alias kept for MainFrame — returns the same as buildCombinedReport(). */
    public String generateCombinedReport() throws TasksDAOException { return buildCombinedReport(); }

    /** Human-friendly combined report using Visitor + pattern matching. */
    public String buildCombinedReport() throws TasksDAOException {
        var visitor = new model.report.CombinedReportVisitor();
        for (var t : items()) { // use the **sorted** view
            if (t instanceof TaskRecord tr) visitor.visit(tr);
            else visitor.visit(new TaskRecord(
                    t.getId(), t.getTitle(), t.getDescription(), t.getState(), Priority.NONE));
        }
        return visitor.asText();
    }

    /** CSV export via Visitor + pattern matching (respects current sort). */
    public String exportCSV() throws TasksDAOException {
        var visitor = new model.report.CSVExportVisitor();
        for (var t : items()) { // use the **sorted** view
            if (t instanceof TaskRecord tr) visitor.visit(tr);
            else visitor.visit(new TaskRecord(
                    t.getId(), t.getTitle(), t.getDescription(), t.getState(), Priority.NONE));
        }
        return visitor.csv();
    }

    /* ---------------- Helpers ---------------- */

    private Set<Integer> currentIds() {
        Set<Integer> s = new HashSet<>();
        for (var t : cache) s.add(t.getId());
        return s;
    }

    private int findNewIdAfterAdd(Set<Integer> before, String title, String desc) {
        for (var t : cache) {
            if (!before.contains(t.getId())
                    && Objects.equals(t.getTitle(), title)
                    && Objects.equals(t.getDescription(), desc)) {
                return t.getId();
            }
        }
        int max = -1;
        for (var t : cache) max = Math.max(max, t.getId());
        return max;
    }
}
