package dao;

import model.ITask;
import model.TaskRecord;
import model.TaskState;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public final class TasksDAODerby implements ITasksDAO {
    private static TasksDAODerby INSTANCE;
    private TasksDAODerby() {}

    // Singleton ל-DAO (לפי הדרישות)
    public static synchronized TasksDAODerby getInstance() {
        if (INSTANCE == null) INSTANCE = new TasksDAODerby();
        return INSTANCE;
    }

    private static TaskState stateFrom(String s) { return TaskState.valueOf(s); }
    private static String stateTo(TaskState s) { return s.name(); }

    @Override
    public ITask[] getTasks() throws TasksDAOException {
        final String sql = "SELECT id,title,description,state FROM tasks ORDER BY id";
        try (Connection c = dao.Derby.instance().getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            List<ITask> list = new ArrayList<>();
            while (rs.next()) {
                list.add(new TaskRecord(
                        rs.getInt("id"),
                        rs.getString("title"),
                        rs.getString("description"),
                        stateFrom(rs.getString("state"))));
            }
            return list.toArray(ITask[]::new);
        } catch (SQLException e) {
            throw new TasksDAOException("getTasks failed", e);
        }
    }

    @Override
    public ITask getTask(int id) throws TasksDAOException {
        final String sql = "SELECT id,title,description,state FROM tasks WHERE id=?";
        try (Connection c = dao.Derby.instance().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                return new TaskRecord(
                        rs.getInt("id"),
                        rs.getString("title"),
                        rs.getString("description"),
                        stateFrom(rs.getString("state")));
            }
        } catch (SQLException e) {
            throw new TasksDAOException("getTask failed (id=" + id + ")", e);
        }
    }

    @Override
    public void addTask(ITask t) throws TasksDAOException {
        final String sql = "INSERT INTO tasks(title,description,state) VALUES(?,?,?)";
        try (Connection c = dao.Derby.instance().getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, t.getTitle());
            ps.setString(2, t.getDescription());
            ps.setString(3, stateTo(t.getState()));
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    int newId = keys.getInt(1);
                    // אפשר לשמור/להדפיס newId אם צריך
                }
            }
        } catch (SQLException e) {
            throw new TasksDAOException("addTask failed", e);
        }
    }

    @Override
    public void updateTask(ITask t) throws TasksDAOException {
        final String sql = "UPDATE tasks SET title=?,description=?,state=? WHERE id=?";
        try (Connection c = dao.Derby.instance().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, t.getTitle());
            ps.setString(2, t.getDescription());
            ps.setString(3, stateTo(t.getState()));
            ps.setInt(4, t.getId());
            int updated = ps.executeUpdate();
            if (updated == 0) throw new TasksDAOException("updateTask: id not found: " + t.getId());
        } catch (SQLException e) {
            throw new TasksDAOException("updateTask failed (id=" + t.getId() + ")", e);
        }
    }

    @Override
    public void deleteTasks() throws TasksDAOException {
        try (Connection c = dao.Derby.instance().getConnection();
             PreparedStatement ps = c.prepareStatement("DELETE FROM tasks")) {
            ps.executeUpdate();
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
}

