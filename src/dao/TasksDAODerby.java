package dao;

import model.ITask;
import model.TaskRecord;
import model.TaskState;
import model.entity.Priority;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public final class TasksDAODerby implements ITasksDAO {
    private static TasksDAODerby INSTANCE;
    private TasksDAODerby() {}

    public static synchronized TasksDAODerby getInstance() {
        if (INSTANCE == null) INSTANCE = new TasksDAODerby();
        return INSTANCE;
    }

    @Override
    public ITask[] getTasks() throws TasksDAOException {
        List<ITask> list = new ArrayList<>();
        final String sql = "SELECT id,title,description,state,priority FROM tasks ORDER BY id";
        try (Connection c = dao.Derby.instance().getConnection();
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
        try (Connection c = dao.Derby.instance().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        } catch (SQLException e) {
            throw new TasksDAOException("getTask failed (id=" + id + ")", e);
        }
    }

    @Override
    public void addTask(ITask t) throws TasksDAOException {
        final String sql = "INSERT INTO tasks (title,description,state,priority) VALUES (?,?,?,?)";
        try (Connection c = dao.Derby.instance().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, t.getTitle());
            ps.setString(2, t.getDescription());
            ps.setString(3, t.getState().name());
            ps.setString(4, (t instanceof TaskRecord tr) ? tr.priority().name() : "NONE");
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new TasksDAOException("addTask failed", e);
        }
    }

    @Override
    public void updateTask(ITask t) throws TasksDAOException {
        final String sql = "UPDATE tasks SET title=?, description=?, state=?, priority=? WHERE id=?";
        try (Connection c = dao.Derby.instance().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, t.getTitle());
            ps.setString(2, t.getDescription());
            ps.setString(3, t.getState().name());
            ps.setString(4, (t instanceof TaskRecord tr) ? tr.priority().name() : readExistingPriority(c, t.getId()));
            ps.setInt(5, t.getId());
            int updated = ps.executeUpdate();
            if (updated == 0) throw new TasksDAOException("updateTask: id not found: " + t.getId());
        } catch (SQLException e) {
            throw new TasksDAOException("updateTask failed (id=" + t.getId() + ")", e);
        }
    }

    @Override
    public void deleteTasks() throws TasksDAOException {
        try (Connection c = dao.Derby.instance().getConnection();
             Statement s = c.createStatement()) {
            s.executeUpdate("DELETE FROM tasks");
        } catch (SQLException e) {
            throw new TasksDAOException("deleteTasks failed", e);
        }
    }

    @Override
    public void deleteTask(int id) throws TasksDAOException {
        try (Connection c = dao.Derby.instance().getConnection();
             PreparedStatement ps = c.prepareStatement("DELETE FROM tasks WHERE id=?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new TasksDAOException("deleteTask failed (id=" + id + ")", e);
        }
    }

    /* ---------- הרחבות שכבר בשימוש אצלך ב-ViewModel (שומרות על Undo/Redo) ---------- */

    public int addTaskReturningId(ITask t) throws TasksDAOException {
        final String sql = "INSERT INTO tasks (title,description,state,priority) VALUES (?,?,?,?)";
        try (Connection c = dao.Derby.instance().getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, t.getTitle());
            ps.setString(2, t.getDescription());
            ps.setString(3, t.getState().name());
            ps.setString(4, (t instanceof TaskRecord tr) ? tr.priority().name() : "NONE");
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getInt(1);
                throw new SQLException("No generated key");
            }
        } catch (SQLException e) {
            throw new TasksDAOException("addTaskReturningId failed", e);
        }
    }

    public void addTaskWithId(int id, ITask t) throws TasksDAOException {
        String sql = "INSERT INTO tasks (id,title,description,state,priority) VALUES (?,?,?,?,?)";
        try (Connection c = dao.Derby.instance().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.setString(2, t.getTitle());
            ps.setString(3, t.getDescription());
            ps.setString(4, t.getState().name());
            ps.setString(5, (t instanceof TaskRecord tr) ? tr.priority().name() : "NONE");
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new TasksDAOException("addTaskWithId failed (id=" + id + ")", e);
        }
    }

    private int getCurrentIdentityValue(Connection c) throws SQLException {
        try (Statement stmt = c.createStatement();
             ResultSet rs = stmt.executeQuery("VALUES IDENTITY_VAL_LOCAL()")) {
            return rs.next() ? rs.getInt(1) : 1;
        }
    }

    private int getMaxId(Connection c) throws SQLException {
        try (Statement stmt = c.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COALESCE(MAX(id), 0) FROM tasks")) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    /* --------------------------------- Helpers --------------------------------- */

    private static ITask map(ResultSet rs) throws SQLException {
        int id         = rs.getInt("id");
        String title   = rs.getString("title");
        String desc    = rs.getString("description");
        String st      = rs.getString("state");
        String pr      = rs.getString("priority");
        TaskState state = TaskState.valueOf(st);
        Priority priority = switch (pr) {
            case "HIGH"   -> Priority.HIGH;
            case "MEDIUM" -> Priority.MEDIUM;
            case "LOW"    -> Priority.LOW;
            default       -> Priority.NONE;
        };
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
