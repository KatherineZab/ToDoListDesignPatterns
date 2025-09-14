package dao;

import model.ITask;

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

    /**
     * Persists a new task with an explicit identifier.
     * Useful for import flows, undo/redo snapshot replays, or replication.
     * This method is intended for special flows where the ID must remain fixed:
     * Undo/Redo:restoring a previously deleted task with
     * ID (so that references remain consistent)
     * bringing tasks from another source while
     * preserving their original IDs.
     */
    void addTaskWithId(int id, ITask task) throws TasksDAOException;
}
