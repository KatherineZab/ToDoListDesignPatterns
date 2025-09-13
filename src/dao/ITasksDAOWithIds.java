package dao;

import model.ITask;

/**
 * NEW: extra interface for optional ID-oriented operations.
 * This allows us to keep ITasksDAO untouched as required.
 */
public interface ITasksDAOWithIds {
    int  addTaskReturningId(ITask task) throws TasksDAOException; // NEW
    void addTaskWithId(int id, ITask task) throws TasksDAOException; // NEW
}
