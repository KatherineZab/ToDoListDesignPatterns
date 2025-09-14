package model.sort;

import model.ITask;
import model.TaskRecord;
import model.entity.Priority;

import java.util.Comparator;

/**
 * Sorts tasks by priority
 * Order by priority (highest first): HIGH → MEDIUM → LOW → NONE.
 * Then break ties by title A→Z (case-insensitive).
 * Uses an explicit numeric rank (see {@link #rank(Priority)}) and does NOT rely
 * on enum ordinals.
 */
public final class ByPriority implements TaskSortStrategy {

    /**
     * Returns a comparator that:
     * 1) compares by mapped rank (HIGH=4 … NONE=1), higher first,
     * 2) then compares titles A→Z, case-insensitive.
     */
    @Override
    public Comparator<ITask> comparator() {
        // map priority to numeric rank
        return Comparator
                .comparing((ITask t) -> rank(priorityOf(t)))
                .reversed()
                .thenComparing(ITask::getTitle, String.CASE_INSENSITIVE_ORDER);
    }

    //Extracts the priority from a TaskRecord; otherwise returns NONE.
    private static Priority priorityOf(ITask t) {
        return (t instanceof TaskRecord tr) ? tr.priority() : Priority.NONE;
    }

    /**
     * Maps a priority to a numeric rank used for sorting.
     * Higher number = higher priority. Null is treated like NONE.
     * HIGH=4, MEDIUM=3, LOW=2, NONE=1.
     */
    private static int rank(Priority p) {
        if (p == null) return 1;
        return switch (p) {
            case HIGH   -> 4;
            case MEDIUM -> 3;
            case LOW    -> 2;
            default     -> 1;
        };
    }
}
