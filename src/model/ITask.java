package model;

public interface ITask {
    int getId();
    String getTitle();
    String getDescription();
    TaskState getState();
    void accept(TaskVisitor visitor);
}
