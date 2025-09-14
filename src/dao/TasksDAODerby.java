package dao;

import model.ITask;
import model.TaskRecord;
import model.TaskState;
import model.entity.Priority;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Derby-backed implementation of {@link ITasksDAO} and {@link ITasksDAOWithIds}.
 *  CRUD over the embedded Apache Derby database (table: {@code tasks}).
 *  Mapping JDBC rows to domain {@link ITask} instances (using {@link TaskRecord}).
 *  Optionally returning/accepting explicit ids via {@link ITasksDAOWithIds}.
 * Design & Patterns
 * DAO: isolates persistence from the rest of the application.
 * Singleton: exposed via {@link #getInstance()} using a holder class.
 * Exception Wrapping: JDBC errors are wrapped in {@link TasksDAOException}
 */
public final class TasksDAODerby implements ITasksDAO, ITasksDAOWithIds {
    /**
     * JVM-safe, lazy-loaded singleton holder for TasksDAODerby.
     */
    private static final class Holder {
        private static final TasksDAODerby INSTANCE = new TasksDAODerby();
    }

    private TasksDAODerby() {}

    //Returns the single shared DAO instance.
    public static TasksDAODerby getInstance() {
        return Holder.INSTANCE;
    }

    /**
     * Retrieves all tasks ordered by {@code id}
     * @return a non-null array (possibly empty)
     * @throws TasksDAOException if the query fails
     */
    @Override
    public ITask[] getTasks() throws TasksDAOException {
        List<ITask> list = new ArrayList<>();
        final String sql = "SELECT id,title,description,state,priority FROM tasks ORDER BY id";
        try (Connection c = dao.Derby.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(map(rs));
            return list.toArray(ITask[]::new);
        } catch (SQLException e) {
            throw new TasksDAOException("getTasks failed", e);
        }
    }

    /**
     * Retrieves a single task by its {@code id}.
     * @param id the task identifier
     * @return the task if found; otherwise {@code null}
     * @throws TasksDAOException if the query fails
     */
    @Override
    public ITask getTask(int id) throws TasksDAOException {
        final String sql = "SELECT id,title,description,state,priority FROM tasks WHERE id=?";
        try (Connection c = dao.Derby.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                return map(rs);
            }
        } catch (SQLException e) {
            throw new TasksDAOException("getTask failed id=" + id, e);
        }
    }

    /**
     * Inserts a new task
     * @param task the task to persist
     * @throws TasksDAOException if the insert fails
     */
    @Override
    public void addTask(ITask task) throws TasksDAOException {
        final String sql = "INSERT INTO tasks (title,description,state,priority) VALUES (?,?,?,?)";
        try (Connection c = dao.Derby.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, task.getTitle());
            ps.setString(2, task.getDescription());
            ps.setString(3, task.getState().name());

            String pr = (task instanceof TaskRecord tr) ? tr.priority().name() : "NONE";
            ps.setString(4, pr);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new TasksDAOException("addTask failed", e);
        }
    }

    /**
     * Updates an existing task matched by {@code id}.
     * If {@code task} is not a {@link TaskRecord}, the existing priority is read from the DB and preserved.
     * @param task the task to update
     * @throws TasksDAOException if the update fails
     */
    @Override
    public void updateTask(ITask task) throws TasksDAOException {
        final String sql = "UPDATE tasks SET title=?,description=?,state=?,priority=? WHERE id=?";
        try (Connection c = dao.Derby.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, task.getTitle());
            ps.setString(2, task.getDescription());
            ps.setString(3, task.getState().name());
            String pr = (task instanceof TaskRecord tr) ? tr.priority().name() : readExistingPriority(c, task.getId());
            ps.setString(4, pr);
            ps.setInt(5, task.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new TasksDAOException("updateTask failed id=" + task.getId(), e);
        }
    }

    //Deletes all tasks.
    @Override
    public void deleteTasks() throws TasksDAOException {
        try (Connection c = dao.Derby.getConnection();
             Statement s = c.createStatement()) {
            s.executeUpdate("DELETE FROM tasks");
        } catch (SQLException e) {
            throw new TasksDAOException("deleteTasks failed", e);
        }
    }

    //Deletes a single task by id
    @Override
    public void deleteTask(int id) throws TasksDAOException {
        try (Connection c = dao.Derby.getConnection();
             PreparedStatement ps = c.prepareStatement("DELETE FROM tasks WHERE id=?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new TasksDAOException("deleteTask failed id=" + id, e);
        }
    }

    /**
     * Inserts a new task and returns the database-generated identifier.
     * @param task the task to persist
     * @return the generated id if returned by the DB; otherwise {@code -1}
     * @throws TasksDAOException if the insert fails
     */
    public int addTaskReturningId(ITask task) throws TasksDAOException {
        final String sql = "INSERT INTO tasks (title,description,state,priority) VALUES (?,?,?,?)";
        try (Connection c = dao.Derby.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, task.getTitle());
            ps.setString(2, task.getDescription());
            ps.setString(3, task.getState().name());
            String pr = (task instanceof TaskRecord tr) ? tr.priority().name() : "NONE";
            ps.setString(4, pr);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                return keys.next() ? keys.getInt(1) : -1;
            }
        } catch (SQLException e) {
            throw new TasksDAOException("addTaskReturningId failed", e);
        }
    }

    /**
     * Maps the current {@link ResultSet} row into a {@link TaskRecord}.
     * @param rs an open result set positioned on a row
     * @return a mapped {@link ITask}
     * @throws SQLException if column access or conversion fails
     */
    private static ITask map(ResultSet rs) throws SQLException {
        int id = rs.getInt("id");
        String title = rs.getString("title");
        String desc = rs.getString("description");
        TaskState state = TaskState.valueOf(rs.getString("state"));
        Priority priority = Priority.valueOf(rs.getString("priority"));
        return new TaskRecord(id, title, desc, state, priority);

    }


    /**
     * Reads the current priority for a given {@code id}.
     * @param c  an open connection
     * @param id the task id
     * @return the stored priority name, or {@code "NONE"} if not found
     * @throws SQLException if the query fails
     */
    private static String readExistingPriority(Connection c, int id) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("SELECT priority FROM tasks WHERE id=?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString(1) : "NONE";
            }
        }
    }
}
