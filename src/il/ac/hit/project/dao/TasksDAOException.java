package il.ac.hit.project.dao;

/**
 * Checked exception for persistence errors in the DAO layer.
 * Used by {@code ITasksDAO} (and its extensions) to signal failures when
 * talking to the database (e.g., JDBC/SQL errors).
 * Typical usage: wrap the original {@link java.sql.SQLException} as the cause
 * so callers can inspect/log the root problem.
 */
public class TasksDAOException extends Exception {

    //Creates a DAO exception with a message.
    public TasksDAOException(String message) { super(message); }
    /**
     * Creates a DAO exception with a message and an underlying cause.
     * @param message description of the failure
     * @param cause   the original exception (e.g., {@link java.sql.SQLException})
     */
    public TasksDAOException(String message, Throwable cause) { super(message, cause); }
}
