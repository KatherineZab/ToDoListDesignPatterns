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

    public static synchronized TasksDAODerby getInstance() {
        if (INSTANCE == null) INSTANCE = new TasksDAODerby();
        return INSTANCE;
    }

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
                        TaskState.valueOf(rs.getString("state"))
                ));
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
                        TaskState.valueOf(rs.getString("state"))
                );
            }
        } catch (SQLException e) {
            throw new TasksDAOException("getTask failed (id=" + id + ")", e);
        }
    }

    @Override
    public void addTask(ITask t) throws TasksDAOException {
        final String sql = "INSERT INTO tasks(title,description,state) VALUES(?,?,?)";
        try (Connection c = dao.Derby.instance().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, t.getTitle());
            ps.setString(2, t.getDescription());
            ps.setString(3, t.getState().name());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new TasksDAOException("addTask failed", e);
        }
    }

    @Override
    public int addTaskReturningId(ITask t) throws TasksDAOException {
        final String sql = "INSERT INTO tasks(title,description,state) VALUES(?,?,?)";
        try (Connection c = dao.Derby.instance().getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, t.getTitle());
            ps.setString(2, t.getDescription());
            ps.setString(3, t.getState().name());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getInt(1);
            }
            throw new TasksDAOException("addTaskReturningId: no generated key");
        } catch (SQLException e) {
            throw new TasksDAOException("addTaskReturningId failed", e);
        }
    }

    public void addTaskWithId(int id, ITask t) throws TasksDAOException {
        final String deleteSql = "DELETE FROM tasks WHERE id=?";
        final String insertSql = """
        INSERT INTO tasks(id, title, description, state)
        OVERRIDING SYSTEM VALUE
        VALUES (?, ?, ?, ?)
    """;
        try (Connection c = dao.Derby.instance().getConnection()) {
            c.setAutoCommit(false);
            try (PreparedStatement del = c.prepareStatement(deleteSql);
                 PreparedStatement ins = c.prepareStatement(insertSql)) {

                del.setInt(1, id);
                del.executeUpdate();

                ins.setInt(1, id);
                ins.setString(2, t.getTitle());
                ins.setString(3, t.getDescription());
                ins.setString(4, t.getState().name());
                ins.executeUpdate();

                c.commit();
            } catch (SQLException e) {
                c.rollback();
                throw e;
            } finally {
                c.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new TasksDAOException("addTaskWithId failed (id=" + id + ")", e);
        }
    }


    @Override
    public void updateTask(ITask t) throws TasksDAOException {
        final String sql = "UPDATE tasks SET title=?,description=?,state=? WHERE id=?";
        try (Connection c = dao.Derby.instance().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, t.getTitle());
            ps.setString(2, t.getDescription());
            ps.setString(3, t.getState().name());
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
