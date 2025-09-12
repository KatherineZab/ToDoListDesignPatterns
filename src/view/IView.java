package view;

import model.ITask;
import java.util.List;

public interface IView {
    void showTasks(List<? extends ITask> tasks);
    void showMessage(String message);
    void setBusy(boolean busy);
}
