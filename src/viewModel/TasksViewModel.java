package viewModel;

import dao.ITasksDAO;
import dao.TasksDAOException;
//import dao.TasksDAODerby;
import dao.ITasksDAOWithIds;
import model.ITask;
import model.TaskRecord;
import model.TaskState;
import model.entity.Priority;
import model.observable.TasksListener;
import model.observable.TasksRepository;
import model.sort.TaskSortStrategy;
import model.TaskFilter;
import model.Filters;

import java.util.*;
import java.util.stream.Collectors;


/**
 * ViewModel for tasks: sits between the DAO and the UI.
 * Responsibilities:
 * - Keeps an in-memory cache of tasks.
 * - Applies filtering and sorting in-memory for the current view.
 * - Notifies registered listeners (Observer) when data changes.
 * - Wraps DAO calls and refreshes the cache after each change.
 */
public class TasksViewModel {

    // Backing DAO (persistence).
    private final ITasksDAO dao;
    //Full cache of tasks from the DAO. */
    private final List<ITask> cache = new ArrayList<>();
    //Stores filtered results
    private List<ITask> filteredCache = null;
    //Current sort strategy (null = no sorting)
    private TaskSortStrategy sortStrategy = null;
    //Observer repository to notify views
    private final TasksRepository observers = new TasksRepository();

    //Create a view model bound to a DAO. Immediately loads data
    public TasksViewModel(ITasksDAO dao) {
        this.dao = Objects.requireNonNull(dao, "dao must not be null");
        try { load(); } catch (Exception ignored) {}
    }

    /* ---------------- Observer API (for Views) ---------------- */

    //Subscribe a UI listener to be notified on changes.
    public void addTasksListener(TasksListener l) { observers.addListener(l); }
    //Unsubscribe a UI listener.
    public void removeTasksListener(TasksListener l) { observers.removeListener(l); }
    //Notify all listeners with the current items() snapshot.
    private void fireChanged() { observers.notifyListeners(items()); }

    /* ---------------- Filtering (Combinator Pattern in ViewModel) ---------------- */
    //Apply textual+state filtering on the cached tasks.
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

    //Clear the active filter and show all tasks again.
    public void clearFilter() {
        this.filteredCache = null;
        fireChanged(); // Show all data again
    }

    /* ---------------- Sorting (Strategy Pattern) ---------------- */
    //Set the sort strategy (e.g., ByPriority, ByState)
    public void setSortStrategy(TaskSortStrategy strategy) {
        this.sortStrategy = strategy;
        fireChanged(); // Notify views to re-render in new order
    }
    /**
     * Apply the current sort strategy to a given list.
     * If no strategy is set, returns a copy of the source as-is.
     */
    private List<ITask> applySort(List<ITask> src) {
        if (sortStrategy == null) return new ArrayList<>(src);
        return src.stream().sorted(sortStrategy.comparator()).collect(Collectors.toList());
    }

    /* ---------------- Queries ---------------- */

    /**
     * Current items to display:
     * - If a filter is active, returns the filtered list; otherwise the full cache.
     * - Sorting is applied if a sort strategy is set.
     * The returned list is unmodifiable.
     */
    public List<ITask> items() {
        // Return filtered view if filter is active, otherwise full cache
        List<ITask> source = (filteredCache != null) ? filteredCache : cache;
        return Collections.unmodifiableList(applySort(source));
    }

    //Fetch a single task by id directly from the DAO (no cache).
    public ITask getById(int id) throws TasksDAOException {
        return dao.getTask(id);
    }

    // Reload the cache from the DAO and notify listeners.
    public void load() throws TasksDAOException {
        cache.clear();
        Collections.addAll(cache, dao.getTasks());
        // Clear filter when reloading data
        filteredCache = null;
        fireChanged();
    }

    /* ---------------- Create ---------------- */
    /**
     * Add a new task and return the DB id if available.
     * If the DAO does not support returning ids, returns -1.
     */
    public int addReturningId(String title, String desc, TaskState state) throws TasksDAOException {
        var tr = new TaskRecord(0, title, desc, state, Priority.NONE);
        int id;
        if (dao instanceof ITasksDAOWithIds withIds) {
            id = withIds.addTaskReturningId(tr);   // real DB id
        } else {
            dao.addTask(tr);                       // persist without returning id
            id = -1;
        }
        load();                                    // refresh + notify observers
        return id;
    }

    /**
     * Add a new task with a specific priority and return the DB id if available.
     * If not supported by the DAO, returns -1.
     */
    public int addWithPriorityReturningId(String title, String desc, TaskState state, Priority priority) throws TasksDAOException {
        var tr = new TaskRecord(0, title, desc, state, priority);
        int id;
        if (dao instanceof ITasksDAOWithIds withIds) {
            id = withIds.addTaskReturningId(tr);   // real DB id
        } else {
            dao.addTask(tr);
            id = -1;
        }
        load();                                    // refresh + notify observers
        return id;
    }



    /* ---------------- Update / Delete ---------------- */
    /**
     * Update a task's title/description, and optionally its state.
     * If the state changes, uses {@link TaskRecord#withState(TaskState)} to validate the transition.
     * @throws TasksDAOException if the DAO update fails
     * @throws IllegalStateException if the state transition is not allowed
     */
    public void update(int id, String title, String desc, TaskState newState) throws TasksDAOException {
        var cur = dao.getTask(id);
        if (cur == null) return;

        // Extract priority even if current is not a TaskRecord
        var pr = (cur instanceof TaskRecord tr) ? tr.priority() : Priority.NONE;

        // Normalize to TaskRecord for consistent updates
        TaskRecord before = (cur instanceof TaskRecord tr)
                ? tr
                : new TaskRecord(cur.getId(), cur.getTitle(), cur.getDescription(), cur.getState(), pr);

        // Update title/description first
        TaskRecord after = new TaskRecord(before.id(), title, desc, before.state(), pr);

        // Change state only if requested and validate via withState()
        if (newState != before.state()) {
            after = after.withState(newState);
        }

        dao.updateTask(after);
        load();
    }

    //Delete a single task by id and refresh the cache.
    public void delete(int id) throws TasksDAOException {
        dao.deleteTask(id);
        load();
    }

    /**
     * INTERNAL (Undo/Redo only): apply a previously saved snapshot as-is,
     * without transition validation. Do not call from regular UI flows.
     */
    public void applyFromHistory(TaskRecord snapshot) {
        Objects.requireNonNull(snapshot, "snapshot");
        try {
            dao.updateTask(snapshot);
            load();
        } catch (TasksDAOException e) {
            throw new RuntimeException("History apply failed", e);
        }
    }
    // Delete all tasks and refresh the cache.
    public void deleteAll() throws dao.TasksDAOException {
        dao.deleteTasks();
        load();
    }


    /* ---------------- Priority ---------------- */
    //Set the priority of a task (other fields stay the same) and refresh the cache.
    public void setPriority(int id, Priority p) throws TasksDAOException {
        var current = dao.getTask(id);
        if (current == null) return;
        var tr = (current instanceof TaskRecord old)
                ? new TaskRecord(old.id(), old.title(), old.description(), old.state(), p)
                : new TaskRecord(current.getId(), current.getTitle(), current.getDescription(), current.getState(), p);
        dao.updateTask(tr);
        load();
    }

    /**
     * Return the allowed next states for a given task id.
     * If the task is not a TaskRecord, returns all states.
     * @throws TasksDAOException if the DAO lookup fails
     */
    public List<TaskState> allowedNextStatesOf(int taskId) throws TasksDAOException {
        var t = dao.getTask(taskId);
        if (t instanceof TaskRecord tr) return new ArrayList<>(tr.allowedNextStates());
        return Arrays.asList(TaskState.values());
    }

    /* ---------------- Reports (Visitor Pattern) ---------------- */

    /**
     * Build a human-readable combined report (counts by priority/state)
     * @throws TasksDAOException if loading items requires DAO and fails
     */
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

    // Export the current view (filtered/sorted items()) to CSV format.
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