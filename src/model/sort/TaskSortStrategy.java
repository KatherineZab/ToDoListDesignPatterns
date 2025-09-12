package model.sort;

import model.ITask;
import java.util.Comparator;

/** Strategy: מחזיר Comparator לוגיקת מיון ניתנת להחלפה */
public interface TaskSortStrategy {
    Comparator<ITask> comparator();
}
