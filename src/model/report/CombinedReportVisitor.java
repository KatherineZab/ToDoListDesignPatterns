package model.report;

import model.TaskRecord;
import model.TaskVisitor;
import model.TaskState;
import model.entity.Priority;

import java.util.EnumMap;

/**
 * Visitor that builds a combined report (counts by priority & state),
 * using record pattern + switch pattern on enums.
 */
public final class CombinedReportVisitor implements TaskVisitor {

    private final EnumMap<Priority, Integer> priorityCounts = new EnumMap<>(Priority.class);
    private final EnumMap<TaskState, Integer> stateCounts   = new EnumMap<>(TaskState.class);
    private int total = 0;

    public CombinedReportVisitor() {
        for (var p : Priority.values())  priorityCounts.put(p, 0);
        for (var s : TaskState.values()) stateCounts.put(s, 0);
    }

    @Override
    public void visit(TaskRecord t) {
        // exhaustive & explicit: handle null explicitly
        switch (t) {
            case null -> {
                return;
            }
            case TaskRecord(var id, var title, var description, var state, var priority) -> {
                total++;

                // switch pattern on enums (clear and explicit)
                switch (priority) {
                    case HIGH   -> inc(priorityCounts, Priority.HIGH);
                    case MEDIUM -> inc(priorityCounts, Priority.MEDIUM);
                    case LOW    -> inc(priorityCounts, Priority.LOW);
                    case NONE   -> inc(priorityCounts, Priority.NONE);
                }
                switch (state) {
                    case TO_DO       -> inc(stateCounts, TaskState.TO_DO);
                    case IN_PROGRESS -> inc(stateCounts, TaskState.IN_PROGRESS);
                    case COMPLETED   -> inc(stateCounts, TaskState.COMPLETED);
                }
            }
        }
    }

    private static <E extends Enum<E>> void inc(EnumMap<E, Integer> map, E key) {
        map.put(key, map.get(key) + 1);
    }

    /** A human-readable summary for UI or logs. */
    public String asText() {
        return """
               Total tasks: %d

               By priority:
                 HIGH:   %d
                 MEDIUM: %d
                 LOW:    %d
                 NONE:   %d

               By state:
                 TO_DO:       %d
                 IN_PROGRESS: %d
                 COMPLETED:   %d
               """.formatted(
                total,
                priorityCounts.get(Priority.HIGH),
                priorityCounts.get(Priority.MEDIUM),
                priorityCounts.get(Priority.LOW),
                priorityCounts.get(Priority.NONE),
                stateCounts.get(TaskState.TO_DO),
                stateCounts.get(TaskState.IN_PROGRESS),
                stateCounts.get(TaskState.COMPLETED)
        );
    }
}
