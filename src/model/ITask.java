package model;

/**
 * Minimal read-only view of a task.
 * Implemented by {@code TaskRecord} and by decorators
 * (PriorityDecorator) used for display. Changes to tasks are done
 * through DAO, not via this interface.
 * @see TaskRecord
 * @see model.decorator.AbstractTaskDecorator
 */
public interface ITask {

    //The database identifier of the task.
    int getId();
    //The task title
    String getTitle();
    //The task description
    String getDescription();
    //The current workflow state of the task.
    TaskState getState();
}
