package viewModel;

import dao.ITasksDAO;
import dao.ITasksDAOWithIds;
import dao.TasksDAOException;
import model.ITask;
import model.TaskRecord;
import model.TaskState;
import model.entity.Priority;
import model.observable.TasksListener;
import model.sort.ByPriority;
import model.sort.ByState;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;
import java.util.concurrent.CountDownLatch;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Unit tests for TasksViewModel - tests business logic with mock DAO
 * This tests the ViewModel layer without actual database dependency
 */
class TasksViewModelTest {

    private MockTasksDAO mockDao;
    private TasksViewModel viewModel;
    private List<ITask> notifiedTasks;
    private boolean wasNotified;

    @BeforeEach
    void setUp() {
        mockDao = new MockTasksDAO();
        viewModel = new TasksViewModel(mockDao);

        // Setup test listener
        notifiedTasks = new ArrayList<>();
        wasNotified = false;

        TasksListener testListener = tasks -> {
            notifiedTasks.clear();
            notifiedTasks.addAll(tasks);
            wasNotified = true;
        };

        viewModel.addTasksListener(testListener);
    }

    @Test
    @DisplayName("Constructor should load initial data from DAO")
    void testConstructorLoadsData() throws TasksDAOException {
        // Add some tasks to mock DAO
        TaskRecord task1 = new TaskRecord(1, "Task 1", "Desc 1", TaskState.TO_DO, Priority.HIGH);
        TaskRecord task2 = new TaskRecord(2, "Task 2", "Desc 2", TaskState.IN_PROGRESS, Priority.LOW);
        mockDao.addTestTask(task1);
        mockDao.addTestTask(task2);

        // Create new ViewModel (should load data)
        TasksViewModel newVm = new TasksViewModel(mockDao);

        assertEquals(2, newVm.items().size());
    }

    @Test
    @DisplayName("applyFilter should filter tasks using Combinator pattern")
    void testApplyFilter() throws TasksDAOException {
        // Setup test data
        TaskRecord task1 = new TaskRecord(1, "Bug fix urgent", "Fix critical bug",
                TaskState.TO_DO, Priority.HIGH);
        TaskRecord task2 = new TaskRecord(2, "Feature request", "Add new feature",
                TaskState.IN_PROGRESS, Priority.MEDIUM);
        TaskRecord task3 = new TaskRecord(3, "Bug in UI", "Minor UI bug",
                TaskState.COMPLETED, Priority.LOW);

        mockDao.addTestTask(task1);
        mockDao.addTestTask(task2);
        mockDao.addTestTask(task3);
        viewModel.load();

        // Filter by text "bug" - should match task1 and task3
        viewModel.applyFilter("bug", "ALL");

        assertEquals(2, viewModel.items().size());
        assertTrue(viewModel.items().stream().anyMatch(t -> t.getId() == 1));
        assertTrue(viewModel.items().stream().anyMatch(t -> t.getId() == 3));

        // Filter by state IN_PROGRESS - should match only task2
        viewModel.applyFilter("", "IN_PROGRESS");

        assertEquals(1, viewModel.items().size());
        assertEquals(2, viewModel.items().get(0).getId());
    }

    @Test
    @DisplayName("clearFilter should restore all tasks")
    void testClearFilter() throws TasksDAOException {
        // Setup test data
        TaskRecord task1 = new TaskRecord(1, "Task 1", "Desc 1", TaskState.TO_DO, Priority.HIGH);
        TaskRecord task2 = new TaskRecord(2, "Task 2", "Desc 2", TaskState.IN_PROGRESS, Priority.LOW);
        mockDao.addTestTask(task1);
        mockDao.addTestTask(task2);
        viewModel.load();

        // Apply filter
        viewModel.applyFilter("Task 1", "ALL");
        assertEquals(1, viewModel.items().size());

        // Clear filter
        viewModel.clearFilter();
        assertEquals(2, viewModel.items().size());
    }

    @Test
    @DisplayName("setSortStrategy should sort tasks using Strategy pattern")
    void testSetSortStrategy() throws TasksDAOException {
        // Setup test data in non-priority order
        TaskRecord lowTask = new TaskRecord(1, "Low priority", "Desc", TaskState.TO_DO, Priority.LOW);
        TaskRecord highTask = new TaskRecord(2, "High priority", "Desc", TaskState.TO_DO, Priority.HIGH);
        TaskRecord medTask = new TaskRecord(3, "Medium priority", "Desc", TaskState.TO_DO, Priority.MEDIUM);

        mockDao.addTestTask(lowTask);
        mockDao.addTestTask(highTask);
        mockDao.addTestTask(medTask);
        viewModel.load();

        // Sort by priority (HIGH -> MEDIUM -> LOW)
        viewModel.setSortStrategy(new ByPriority());

        List<ITask> sorted = viewModel.items();
        assertEquals(3, sorted.size());

        // Extract priorities for comparison
        Priority[] priorities = sorted.stream()
                .map(t -> (t instanceof TaskRecord tr) ? tr.priority() : Priority.NONE)
                .toArray(Priority[]::new);

        assertEquals(Priority.HIGH, priorities[0]);
        assertEquals(Priority.MEDIUM, priorities[1]);
        assertEquals(Priority.LOW, priorities[2]);
    }


    @Test
    @DisplayName("Observer notifies listeners when data changes")
    void testObserverNotification() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        wasNotified = false;
        notifiedTasks.clear();

        viewModel.addTasksListener(tasks -> {
            notifiedTasks.clear();
            notifiedTasks.addAll(tasks);
            wasNotified = true;
            latch.countDown();   // אות שקיבלנו עדכון
        });

        viewModel.addReturningId("New Task", "Description", TaskState.TO_DO);

        // ממתינים לאסינכרון (EDT) במקום Thread.sleep
        assertTrue(latch.await(1, TimeUnit.SECONDS), "Listener wasn't notified in time");

        assertTrue(wasNotified);
        assertEquals(1, notifiedTasks.size());
        assertEquals("New Task", notifiedTasks.get(0).getTitle());
    }

    @Test
    @DisplayName("addReturningId should create task and return ID")
    void testAddReturningId() throws TasksDAOException {
        int id = viewModel.addReturningId("Test Task", "Test Description", TaskState.TO_DO);

        assertTrue(id > 0);
        assertEquals(1, viewModel.items().size());
        assertEquals("Test Task", viewModel.items().get(0).getTitle());
    }

    @Test
    @DisplayName("update should modify existing task")
    void testUpdate() throws TasksDAOException {
        // Add initial task
        TaskRecord original = new TaskRecord(1, "Original", "Original desc",
                TaskState.TO_DO, Priority.NONE);
        mockDao.addTestTask(original);
        viewModel.load();

        // Update task
        viewModel.update(1, "Updated", "Updated desc", TaskState.IN_PROGRESS);

        ITask updated = viewModel.getById(1);
        assertEquals("Updated", updated.getTitle());
        assertEquals("Updated desc", updated.getDescription());
        assertEquals(TaskState.IN_PROGRESS, updated.getState());
    }

    @Test
    @DisplayName("update should validate state transitions")
    void testUpdateStateTransitionValidation() throws TasksDAOException {
        TaskRecord original = new TaskRecord(1, "Task", "Desc", TaskState.TO_DO, Priority.NONE);
        mockDao.addTestTask(original);
        viewModel.load();

        // Invalid: TO_DO -> COMPLETED (should throw)
        assertThrows(IllegalStateException.class,
                () -> viewModel.update(1, "Task", "Desc", TaskState.COMPLETED));
        assertEquals(TaskState.TO_DO, viewModel.getById(1).getState()); // stayed the same

        // Valid: TO_DO -> IN_PROGRESS
        assertDoesNotThrow(() -> viewModel.update(1, "Task", "Desc", TaskState.IN_PROGRESS));
        assertEquals(TaskState.IN_PROGRESS, viewModel.getById(1).getState());
    }

    @Test
    @DisplayName("delete should remove task")
    void testDelete() throws TasksDAOException {
        // Add task
        TaskRecord task = new TaskRecord(1, "Task", "Desc", TaskState.TO_DO, Priority.NONE);
        mockDao.addTestTask(task);
        viewModel.load();
        assertEquals(1, viewModel.items().size());

        // Delete task
        viewModel.delete(1);
        assertEquals(0, viewModel.items().size());
    }

    @Test
    @DisplayName("setPriority should update task priority")
    void testSetPriority() throws TasksDAOException {
        // Add task
        TaskRecord task = new TaskRecord(1, "Task", "Desc", TaskState.TO_DO, Priority.NONE);
        mockDao.addTestTask(task);
        viewModel.load();

        // Set priority
        viewModel.setPriority(1, Priority.HIGH);

        ITask updated = viewModel.getById(1);
        assertTrue(updated instanceof TaskRecord);
        assertEquals(Priority.HIGH, ((TaskRecord) updated).priority());
    }

    @Test
    @DisplayName("generateCombinedReport should use Visitor pattern")
    void testGenerateCombinedReport() throws TasksDAOException {
        // Add test data
        TaskRecord task1 = new TaskRecord(1, "Task 1", "Desc 1", TaskState.TO_DO, Priority.HIGH);
        TaskRecord task2 = new TaskRecord(2, "Task 2", "Desc 2", TaskState.COMPLETED, Priority.LOW);
        mockDao.addTestTask(task1);
        mockDao.addTestTask(task2);
        viewModel.load();

        String report = viewModel.generateCombinedReport();

        assertNotNull(report);
        assertTrue(report.contains("Total tasks: 2"));
        assertTrue(report.contains("HIGH:   1"));
        assertTrue(report.contains("LOW:    1"));
        assertTrue(report.contains("TO_DO:       1"));
        assertTrue(report.contains("COMPLETED:   1"));
    }

    @Test
    @DisplayName("exportCSV should use Visitor pattern")
    void testExportCSV() throws TasksDAOException {
        // Add test data
        TaskRecord task = new TaskRecord(1, "Test Task", "Test Description",
                TaskState.TO_DO, Priority.MEDIUM);
        mockDao.addTestTask(task);
        viewModel.load();

        String csv = viewModel.exportCSV();

        assertNotNull(csv);
        assertTrue(csv.contains("id,title,description,state,priority"));
        assertTrue(csv.contains("1,Test Task,Test Description,TO_DO,MEDIUM"));
    }

    @Test
    @DisplayName("Combined filtering and sorting should work together")
    void testFilteringAndSortingCombined() throws TasksDAOException {
        // Add test data
        TaskRecord task1 = new TaskRecord(1, "Bug high", "Desc", TaskState.TO_DO, Priority.HIGH);
        TaskRecord task2 = new TaskRecord(2, "Bug low", "Desc", TaskState.TO_DO, Priority.LOW);
        TaskRecord task3 = new TaskRecord(3, "Feature", "Desc", TaskState.TO_DO, Priority.MEDIUM);

        mockDao.addTestTask(task1);
        mockDao.addTestTask(task2);
        mockDao.addTestTask(task3);
        viewModel.load();

        // Filter by "bug" (should get task1 and task2)
        viewModel.applyFilter("bug", "ALL");
        assertEquals(2, viewModel.items().size());

        // Sort by priority (HIGH first)
        viewModel.setSortStrategy(new ByPriority());

        List<ITask> result = viewModel.items();
        assertEquals(2, result.size());

        // Should be sorted HIGH -> LOW within filtered results
        TaskRecord first = (TaskRecord) result.get(0);
        TaskRecord second = (TaskRecord) result.get(1);

        assertEquals(Priority.HIGH, first.priority());
        assertEquals(Priority.LOW, second.priority());
    }

    /**
     * Mock implementation of ITasksDAO for testing
     * Simulates database operations in memory
     */
    // Mock DAO used only for tests
    private static class MockTasksDAO implements ITasksDAO, ITasksDAOWithIds {
        private final List<TaskRecord> tasks = new ArrayList<>();
        private int nextId = 1;

        // helper for tests
        void addTestTask(TaskRecord task) {
            tasks.add(task);
            nextId = Math.max(nextId, task.id() + 1);
        }

        @Override
        public ITask[] getTasks() {
            return tasks.toArray(new ITask[0]);
        }

        @Override
        public ITask getTask(int id) {
            return tasks.stream().filter(t -> t.id() == id).findFirst().orElse(null);
        }

        @Override
        public void addTask(ITask task) {
            // assign synthetic id when caller doesn't need the id back
            int id = nextId++;
            TaskRecord rec = (task instanceof TaskRecord tr)
                    ? new TaskRecord(id, tr.title(), tr.description(), tr.state(), tr.priority())
                    : new TaskRecord(id, task.getTitle(), task.getDescription(), task.getState(), Priority.NONE);
            tasks.add(rec);
        }

        @Override
        public void updateTask(ITask task) {
            for (int i = 0; i < tasks.size(); i++) {
                if (tasks.get(i).id() == task.getId()) {
                    TaskRecord rec = (task instanceof TaskRecord tr)
                            ? tr
                            : new TaskRecord(task.getId(), task.getTitle(), task.getDescription(), task.getState(), Priority.NONE);
                    tasks.set(i, rec);
                    return;
                }
            }
        }

        @Override
        public void deleteTasks() {
            tasks.clear();
            nextId = 1;
        }

        @Override
        public void deleteTask(int id) {
            tasks.removeIf(t -> t.id() == id);
        }

        // ---------- ITasksDAOWithIds ----------

        @Override
        public int addTaskReturningId(ITask task) /* throws TasksDAOException */ {
            int id = nextId++;
            TaskRecord rec = (task instanceof TaskRecord tr)
                    ? new TaskRecord(id, tr.title(), tr.description(), tr.state(), tr.priority())
                    : new TaskRecord(id, task.getTitle(), task.getDescription(), task.getState(), Priority.NONE);
            tasks.add(rec);
            return id;
        }

        @Override
        public void addTaskWithId(int id, ITask task) /* throws TasksDAOException */ {
            // insert with explicit id (used when caller already knows the id)
            TaskRecord rec = (task instanceof TaskRecord tr)
                    ? new TaskRecord(id, tr.title(), tr.description(), tr.state(), tr.priority())
                    : new TaskRecord(id, task.getTitle(), task.getDescription(), task.getState(), Priority.NONE);
            tasks.add(rec);
            nextId = Math.max(nextId, id + 1);
        }
    }

}