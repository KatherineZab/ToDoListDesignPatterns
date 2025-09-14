package il.ac.hit.project.model.sort;

import il.ac.hit.project.model.ITask;
import il.ac.hit.project.model.TaskState;

import java.util.Comparator;
import java.util.EnumMap;
import java.util.Map;

/**
 * Strategy: sort tasks by state.
 * Order: TO_DO first, then IN_PROGRESS, COMPLETED last.
 * title (case-insensitive), then id.
 *  Uses an explicit rank map (EnumMap) instead of enum ordinals,
 *  so changes in the enum declaration order won't break sorting.
 */
public final class ByState implements TaskSortStrategy {

    //Maps each state to its rank
    private final Map<TaskState, Integer> rank = new EnumMap<>(TaskState.class);

    public ByState() {
        // TO_DO first (0), IN_PROGRESS (1), COMPLETED (2)
        rank.put(TaskState.TO_DO, 0);
        rank.put(TaskState.IN_PROGRESS, 1);
        rank.put(TaskState.COMPLETED, 2);
    }

    //Get the numeric rank for a state; unknown/null states go to the end.
    private int r(TaskState s) {
        return rank.getOrDefault(s, 99);
    }

    //Null-safe title for comparison (null titles sort as empty string).
    private static String safe(String s) {
        return s == null ? "" : s;
    }

    /**
     * Comparator:
     *  - by state rank (TO_DO → IN_PROGRESS → COMPLETED),
     *  - then by title (case-insensitive),
     *  - then by id.
     */
    @Override
    public Comparator<ITask> comparator() {
        return Comparator
                .comparingInt((ITask t) -> r(t.getState()))
                .thenComparing(t -> safe(t.getTitle()), String.CASE_INSENSITIVE_ORDER)
                .thenComparingInt(ITask::getId);
    }
}
