package dao;

import java.sql.*;

/**
 * Utility for working with embedded Apache Derby.
 * Provides a static {@link #getConnection()} and ensures schema on first load.
 */
public final class Derby {
    private static final String URL = "jdbc:derby:taskdb;create=true";

    private Derby() { /* no instances */ }

    static {
        // Initialize schema once when class loads
        try (Connection c = DriverManager.getConnection(URL)) {
            ensureSchema(c);
            ensurePriorityColumn(c);
        } catch (SQLException e) {
            throw new RuntimeException("Derby init failed", e);
        }
    }

    /** Obtain a fresh connection (caller should close it). */
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL);
    }

    /** Create TASKS table if not exists. */
    private static void ensureSchema(Connection c) throws SQLException {
        DatabaseMetaData md = c.getMetaData();
        try (ResultSet rs = md.getTables(null, null, "TASKS", null)) {
            if (rs.next()) return; // exists
        }
        try (Statement s = c.createStatement()) {
            s.executeUpdate(
                    "CREATE TABLE tasks (" +
                            "  id INTEGER NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1)," +
                            "  title VARCHAR(255) NOT NULL," +
                            "  description VARCHAR(4000)," +
                            "  state VARCHAR(32) NOT NULL," +
                            "  priority VARCHAR(10) DEFAULT 'NONE' NOT NULL," +
                            "  PRIMARY KEY (id)" +
                            ")"
            );
        }
    }

    /** Add PRIORITY column if missing (DEFAULT 'NONE' NOT NULL). */
    private static void ensurePriorityColumn(Connection c) throws SQLException {
        DatabaseMetaData md = c.getMetaData();
        try (ResultSet rs = md.getColumns(null, null, "TASKS", "PRIORITY")) {
            if (rs.next()) return; // exists
        }
        try (Statement s = c.createStatement()) {
            s.executeUpdate("ALTER TABLE tasks ADD COLUMN priority VARCHAR(10) DEFAULT 'NONE' NOT NULL");
        }
    }
}
