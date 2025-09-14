package model.sort;

import model.ITask;
import java.util.Comparator;


/**
 * sort strategy for tasks.
 * Implementations return a {@link Comparator} used to sort lists of {@link ITask}.
 * Examples: ByPriority, ByState, ByTitle.
 */
public interface TaskSortStrategy {
    //Returns the comparator that defines the sort order.
    Comparator<ITask> comparator();
}
