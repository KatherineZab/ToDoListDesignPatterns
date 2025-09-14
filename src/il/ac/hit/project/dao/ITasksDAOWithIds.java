package il.ac.hit.project.dao;

import il.ac.hit.project.model.ITask;

/**
 * NEW: extra interface for optional ID-oriented operations.
 * This allows us to keep ITasksDAO untouched as required.
 */
public interface ITasksDAOWithIds {
    /**
     * Persists a new task and returns the database-generated identifier if available.
     * @return the generated identifier, or {@code -1} if the store does not return a key
     * @throws TasksDAOException    if persistence fails
     *
     * This method is intended for normal task creation flows (adding a
     * new task from the UI) where the task does not yet have an ID.
     * The database decides on the identifier (auto-increment / auto-generated key),
     * and the returned value tells the caller what ID was assigned.
     */
    int  addTaskReturningId(ITask task) throws TasksDAOException;
}
