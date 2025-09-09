package model.db;

import java.sql.*;

public final class DerbyInit {

    private static final String URL = "jdbc:derby:./data/tasksdb;create=true";

    private DerbyInit() { }

    public static Connection open() throws SQLException {
        return DriverManager.getConnection(URL);
    }

    public static void ensureSchema() throws SQLException {
        try (Connection con = open()) {
            if (!tableExists(con, "TASKS")) {
                createTasksTable(con);
                createRecommendedIndexes(con);
            }
        }
    }

    private static boolean tableExists(Connection con, String table) throws SQLException {
        try (ResultSet rs = con.getMetaData().getTables(null, null, table, null)) {
            return rs.next();
        }
    }

    private static void createTasksTable(Connection con) throws SQLException {
        String ddl = """
            CREATE TABLE tasks (
                id INTEGER PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
                title VARCHAR(255) NOT NULL,
                description CLOB,
                state VARCHAR(50) NOT NULL DEFAULT 'TO_DO',
                created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                updated_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                due_date DATE,
                priority VARCHAR(20) DEFAULT 'MEDIUM'
            )
            """;
        try (Statement st = con.createStatement()) {
            st.executeUpdate(ddl);
        } catch (SQLException e) {
            if (!"X0Y32".equals(e.getSQLState())) throw e;
        }
    }

    private static void createRecommendedIndexes(Connection con) throws SQLException {
        try (Statement st = con.createStatement()) {
            st.executeUpdate("CREATE INDEX idx_tasks_state ON tasks(state)");
        } catch (SQLException e) {
            if (!"X0Y32".equals(e.getSQLState())) throw e;
        }
        try (Statement st = con.createStatement()) {
            st.executeUpdate("CREATE INDEX idx_tasks_due ON tasks(due_date)");
        } catch (SQLException e) {
            if (!"X0Y32".equals(e.getSQLState())) throw e;
        }
        try (Statement st = con.createStatement()) {
            st.executeUpdate("CREATE INDEX idx_tasks_priority ON tasks(priority)");
        } catch (SQLException e) {
            if (!"X0Y32".equals(e.getSQLState())) throw e;
        }
    }
}
