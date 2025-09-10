package view;

import model.entity.ITask;
import java.util.List;

public interface IView {
    void showTasks(List<? extends ITask> tasks);
    void showMessage(String message);
    void setBusy(boolean busy);
}
