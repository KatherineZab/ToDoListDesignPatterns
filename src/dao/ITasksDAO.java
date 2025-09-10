package dao;

import model.ITask;

public interface ITasksDAO {
    ITask[] getTasks() throws TasksDAOException;
    ITask   getTask(int id) throws TasksDAOException;
    void    addTask(ITask task) throws TasksDAOException;
    void    updateTask(ITask task) throws TasksDAOException;
    void    deleteTasks() throws TasksDAOException;
    void    deleteTask(int id) throws TasksDAOException;

    // תוספות מינימליות למען Commands קיימים:
    int     addTaskReturningId(ITask task) throws TasksDAOException;
    void    addTaskWithId(int id, ITask task) throws TasksDAOException;
}
