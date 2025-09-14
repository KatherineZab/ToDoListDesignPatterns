package il.ac.hit.project.dao;

import il.ac.hit.project.model.ITask;

/**
 * Data Access Object (DAO) contract for {@link ITask} persistence.
 *   Throw {@link TasksDAOException} on any persistence failure.
 *   Return empty arrays instead of {@code null}.
 *  Make sure the inputs are valid, and if not, stop right away by throwing IllegalArgumentException.
 */
public interface ITasksDAO {

    /**
     * Retrieves all tasks in an implementation-defined order.
     * @return a non-null array (possibly empty) of tasks
     * @throws TasksDAOException if retrieving tasks fails
     */
    ITask[] getTasks() throws TasksDAOException;
    /**
     * Retrieves a single task by its identifier.
     * id a task identifier
     * @return the task if found
     * @throws IllegalArgumentException if {@code id <= 0}
     * @throws TasksDAOException        if the lookup fails
     */
    ITask   getTask(int id) throws TasksDAOException;
    /**
     * Persists a new task.
     * @param task the task to add; must not be {@code null}
     * @throws IllegalArgumentException if {@code task == null}
     * @throws TasksDAOException        if persisting fails
     */
    void    addTask(ITask task) throws TasksDAOException;
    /**
     * Updates an existing task (matched by its id).
     * @param task the task to update; must not be {@code null} and must have a valid id
     * @throws IllegalArgumentException if {@code task == null} or {@code task.getId() <= 0}
     * @throws TasksDAOException        if updating fails
     */
    void    updateTask(ITask task) throws TasksDAOException;
    /**
     * Deletes all tasks.
     * @throws TasksDAOException if deletion fails
     */
    void    deleteTasks() throws TasksDAOException;
/**
 * Deletes a single task by id.
 * @throws IllegalArgumentException if {@code id <= 0}
 * @throws TasksDAOException        if deletion fails
 */
    void    deleteTask(int id) throws TasksDAOException;
}
