package model.sort;

import model.ITask;
import model.TaskState;

import java.util.Comparator;
import java.util.EnumMap;
import java.util.Map;

/**
 * Strategy: sort tasks by state.
 * Order: TO_DO first, then IN_PROGRESS, COMPLETED last.
 * Tiebreakers: title (case-insensitive), then id.
 */
public final class ByState implements TaskSortStrategy {

    private final Map<TaskState, Integer> rank = new EnumMap<>(TaskState.class);

    public ByState() {
        // TO_DO first (0), IN_PROGRESS (1), COMPLETED (2)
        rank.put(TaskState.TO_DO, 0);
        rank.put(TaskState.IN_PROGRESS, 1);
        rank.put(TaskState.COMPLETED, 2);
    }

    private int r(TaskState s) {
        return rank.getOrDefault(s, 99);
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }

    @Override
    public Comparator<ITask> comparator() {
        return Comparator
                .comparingInt((ITask t) -> r(t.getState()))
                .thenComparing(t -> safe(t.getTitle()), String.CASE_INSENSITIVE_ORDER)
                .thenComparingInt(ITask::getId);
    }
}
