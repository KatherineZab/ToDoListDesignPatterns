package model;

/**
 * Visitor pattern for processing tasks without changing {@link TaskRecord}.
 * keeps reporting/export logic separate from the data model,
 * and lets you add new operations by creating new visitors.
 */
public interface TaskVisitor {
    //Process a single task.
    void visit(TaskRecord task);
}
