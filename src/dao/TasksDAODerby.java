package dao;

import model.ITask;
import model.TaskRecord;
import model.TaskState;
import model.entity.Priority;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public final class TasksDAODerby implements ITasksDAO, ITasksDAOWithIds {
    /**
     * JVM-safe, lazy-loaded singleton holder for TasksDAODerby.
     */
    private static final class Holder {
        private static final TasksDAODerby INSTANCE = new TasksDAODerby();
    }
    /** Private constructor: use {@link #getInstance()} */
    private TasksDAODerby() {}

    /**
     * Returns the single shared DAO instance.
     */
    public static TasksDAODerby getInstance() {
        return Holder.INSTANCE;
    }

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

    @Override
    public void addTask(ITask task) throws TasksDAOException {
        final String sql = "INSERT INTO tasks (title,description,state,priority) VALUES (?,?,?,?)";
        try (Connection c = dao.Derby.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, task.getTitle());
            ps.setString(2, task.getDescription());
            ps.setString(3, task.getState().name());
            // אם זה TaskRecord נשתמש בפריוריטי שבו; אחרת NONE
            String pr = (task instanceof TaskRecord tr) ? tr.priority().name() : "NONE";
            ps.setString(4, pr);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new TasksDAOException("addTask failed", e);
        }
    }

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

    @Override
    public void deleteTasks() throws TasksDAOException {
        try (Connection c = dao.Derby.getConnection();
             Statement s = c.createStatement()) {
            s.executeUpdate("DELETE FROM tasks");
        } catch (SQLException e) {
            throw new TasksDAOException("deleteTasks failed", e);
        }
    }

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

    // ===== הרחבות קיימות אצלך (שימרתי חתימות) =====

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

    public void addTaskWithId(int id, ITask task) throws TasksDAOException {
        final String sql = "INSERT INTO tasks (id,title,description,state,priority) VALUES (?,?,?,?,?)";
        try (Connection c = dao.Derby.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.setString(2, task.getTitle());
            ps.setString(3, task.getDescription());
            ps.setString(4, task.getState().name());
            String pr = (task instanceof TaskRecord tr) ? tr.priority().name() : "NONE";
            ps.setString(5, pr);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new TasksDAOException("addTaskWithId failed id=" + id, e);
        }
    }

    // ===== עזרות פנימיות =====

    private static ITask map(ResultSet rs) throws SQLException {
        int id = rs.getInt("id");
        String title = rs.getString("title");
        String desc = rs.getString("description");
        TaskState state = TaskState.valueOf(rs.getString("state"));
        Priority priority = Priority.valueOf(rs.getString("priority"));
        return new TaskRecord(id, title, desc, state, priority);
        // נשמרת התאמה ל-ITask (ה-getters עובדים דרך ה-record)
    }

    private static String readExistingPriority(Connection c, int id) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("SELECT priority FROM tasks WHERE id=?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString(1) : "NONE";
            }
        }
    }
}
