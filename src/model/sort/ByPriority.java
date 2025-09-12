package model.sort;

import model.ITask;
import model.TaskRecord;
import model.entity.Priority; // אם Priority תחת model: import model.Priority;

import java.util.Comparator;

/** ממיין לפי Priority: HIGH→LOW→NONE, ואז לפי Title (A→Z) */
public final class ByPriority implements TaskSortStrategy {

    @Override
    public Comparator<ITask> comparator() {
        return Comparator
                .comparing((ITask t) -> rank(priorityOf(t)))   // 1..4
                .reversed()                                    // 4..1 (גבוה→נמוך)
                .thenComparing(ITask::getTitle, String.CASE_INSENSITIVE_ORDER);
    }

    private static Priority priorityOf(ITask t) {
        return (t instanceof TaskRecord tr) ? tr.priority() : Priority.NONE;
    }

    private static int rank(Priority p) {
        if (p == null) return 1;
        return switch (p) {
            case HIGH   -> 4;
            case MEDIUM -> 3;
            case LOW    -> 2;
            default     -> 1; // NONE
        };
    }
}
