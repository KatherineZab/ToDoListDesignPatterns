package viewModel;

import dao.ITasksDAO;
import dao.TasksDAOException;
import dao.TasksDAODerby;
import model.ITask;
import model.TaskRecord;
import model.TaskState;
import model.entity.Priority;
import model.observable.TasksListener;
import model.observable.TasksRepository;
import model.sort.TaskSortStrategy;
import model.combinator.TaskFilter;
import model.combinator.Filters;

import java.util.*;
import java.util.stream.Collectors;

public class TasksViewModel {

    private final ITasksDAO dao;
    private final List<ITask> cache = new ArrayList<>();
    private List<ITask> filteredCache = null; // Stores filtered results
    private TaskSortStrategy sortStrategy = null;
    private final TasksRepository observers = new TasksRepository();

    public TasksViewModel(ITasksDAO dao) {
        this.dao = Objects.requireNonNull(dao, "dao must not be null");
        try { load(); } catch (Exception ignored) {}
    }

    /* ---------------- Observer API (for Views) ---------------- */

    public void addTasksListener(TasksListener l) { observers.addListener(l); }
    public void removeTasksListener(TasksListener l) { observers.removeListener(l); }

    private void fireChanged() { observers.notifyListeners(items()); }

    /* ---------------- Filtering (Combinator Pattern in ViewModel) ---------------- */

    public void applyFilter(String query, String stateNameOrAll) {
        // Business logic filtering in ViewModel using Combinator pattern
        TaskFilter textFilter = Filters.textContains(query);
        TaskFilter stateFilter = Filters.stateIs(stateNameOrAll);
        TaskFilter combinedFilter = textFilter.and(stateFilter);

        // Apply filter to cache
        var filtered = cache.stream()
                .filter(task -> combinedFilter.test(
                        task.getTitle(),
                        task.getDescription(),
                        task.getState().name()
                ))
                .collect(Collectors.toList());

        // Store filtered results
        this.filteredCache = filtered;
        fireChanged(); // Notify views with filtered, sorted data
    }

    public void clearFilter() {
        this.filteredCache = null;
        fireChanged(); // Show all data again
    }

    /* ---------------- Sorting (Strategy Pattern) ---------------- */

    public void setSortStrategy(TaskSortStrategy strategy) {
        this.sortStrategy = strategy;
        fireChanged(); // Notify views to re-render in new order
    }

    private List<ITask> applySort(List<ITask> src) {
        if (sortStrategy == null) return new ArrayList<>(src);
        return src.stream().sorted(sortStrategy.comparator()).collect(Collectors.toList());
    }

    /* ---------------- Queries ---------------- */

    public List<ITask> items() {
        // Return filtered view if filter is active, otherwise full cache
        List<ITask> source = (filteredCache != null) ? filteredCache : cache;
        return Collections.unmodifiableList(applySort(source));
    }

    public ITask getById(int id) throws TasksDAOException {
        return dao.getTask(id);
    }

    public void load() throws TasksDAOException {
        cache.clear();
        Collections.addAll(cache, dao.getTasks());
        // Clear filter when reloading data
        filteredCache = null;
        fireChanged();
    }

    /* ---------------- Create ---------------- */

    public int addReturningId(String title, String desc, TaskState state) throws TasksDAOException {
        var tr = new TaskRecord(0, title, desc, state, Priority.NONE);
        if (dao instanceof TasksDAODerby derbyDao) {
            int id = derbyDao.addTaskReturningId(tr);
            load();
            return id;
        } else {
            dao.addTask(tr);
            load();
            return -1;
        }
    }

    public void addWithId(int id, String title, String desc, TaskState state) throws TasksDAOException {
        var tr = new TaskRecord(id, title, desc, state, Priority.NONE);
        if (dao instanceof TasksDAODerby derbyDao) {
            derbyDao.addTaskWithId(id, tr);
            load();
        } else {
            throw new UnsupportedOperationException("addTaskWithId is not supported by this DAO");
        }
    }

    public int addWithPriorityReturningId(String title, String desc, TaskState state, Priority priority) throws TasksDAOException {
        var tr = new TaskRecord(0, title, desc, state, priority);
        if (dao instanceof TasksDAODerby derbyDao) {
            int id = derbyDao.addTaskReturningId(tr);
            load();
            return id;
        } else {
            dao.addTask(tr);
            load();
            return -1;
        }
    }

    /* ---------------- Update / Delete ---------------- */

    public void update(int id, String title, String desc, TaskState newState) throws TasksDAOException {
        var current = dao.getTask(id);
        if (current == null) return;

        TaskState oldState = current.getState();
        var pr = (current instanceof TaskRecord r) ? r.priority() : Priority.NONE;

        if (oldState != newState && current instanceof TaskRecord r) {
            if (!r.state().canTransitionTo(newState)) {
                throw new IllegalStateException(oldState + " → " + newState + " not allowed");
            }
        }

        dao.updateTask(new TaskRecord(id, title, desc, newState, pr));
        load();
    }

    public void delete(int id) throws TasksDAOException {
        dao.deleteTask(id);
        load();
    }
    /**
     * INTERNAL (Undo/Redo only): apply a previously saved snapshot WITHOUT transition validation.
     * Do not call this from regular UI flows.
     */
    public void applyFromHistory(TaskRecord snapshot) {
        Objects.requireNonNull(snapshot, "snapshot");
        try {
            dao.updateTask(snapshot);  // בלי בדיקות canTransitionTo
            load();                    // מרענן ומודיע ל-View (או refreshView() אצלך)
        } catch (TasksDAOException e) {
            throw new RuntimeException("History apply failed", e);
        }
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

    /* ---------------- Reports (Visitor Pattern) ---------------- */

    public String generateCombinedReport() throws TasksDAOException {
        var visitor = new model.report.CombinedReportVisitor();
        for (ITask t : items()) { // Uses current filtered/sorted view
            if (t instanceof TaskRecord tr) {
                visitor.visit(tr);
            } else {
                var tr = new TaskRecord(
                        t.getId(),
                        t.getTitle(),
                        t.getDescription(),
                        t.getState(),
                        Priority.NONE
                );
                visitor.visit(tr);
            }
        }
        return visitor.asText();
    }

    public String exportCSV() throws TasksDAOException {
        var visitor = new model.report.CSVExportVisitor();
        for (ITask t : items()) { // Uses current filtered/sorted view
            if (t instanceof TaskRecord tr) {
                visitor.visit(tr);
            } else {
                var tr = new TaskRecord(
                        t.getId(),
                        t.getTitle(),
                        t.getDescription(),
                        t.getState(),
                        Priority.NONE
                );
                visitor.visit(tr);
            }
        }
        return visitor.csv();
    }
}