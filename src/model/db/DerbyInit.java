package model.db;

import model.entity.Task;

import java.sql.*;

public final class DerbyInit {

    private static final String URL= "jdbc:derby:./data/tasksdb;create=true";

    private DerbyInit(){};

    public static Connection openConnection() throws SQLException {
        return DriverManager.getConnection(URL);
    }

    public static void ensureSchema() throws SQLException {
        try (Connection con = open()){
            if (!tableExist(con, "TASKS")) {
            createTasksTable(con)
            }
        }
    }
}
