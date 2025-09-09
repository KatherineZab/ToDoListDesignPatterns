
/* teachers code*/

package model.entity;

public interface ITask {
    int getId();
    String getTitle();
    String getDescription();
    TaskState getState();
}
