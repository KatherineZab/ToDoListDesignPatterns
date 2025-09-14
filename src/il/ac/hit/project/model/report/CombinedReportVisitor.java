package il.ac.hit.project.model.report;

import il.ac.hit.project.model.TaskRecord;
import il.ac.hit.project.model.TaskState;
import il.ac.hit.project.model.entity.Priority;

import java.util.EnumMap;

/**
 * Visitor that builds a combined report (counts by priority & state),
 * Uses a record pattern to unpack TaskRecord and switch expressions on the enums
 */
public final class CombinedReportVisitor implements TaskVisitor {

    //Counters per priority (HIGH, MEDIUM, LOW, NONE).
    private final EnumMap<Priority, Integer> priorityCounts = new EnumMap<>(Priority.class);
    //Counters per state (TO_DO, IN_PROGRESS, COMPLETED)
    private final EnumMap<TaskState, Integer> stateCounts   = new EnumMap<>(TaskState.class);
    //Total number of visited tasks
    private int total = 0;

    //Creates a new empty report with all counters set to 0.
    public CombinedReportVisitor() {
        for (var p : Priority.values())  priorityCounts.put(p, 0);
        for (var s : TaskState.values()) stateCounts.put(s, 0);
    }

    /**
     * Visits a single task and updates the counters.
     * - If {@code t} is {@code null}, this method does nothing.
     * - Otherwise, it increments the total and the specific priority/state buckets.
     * @param t the task to include in the report (may be null)
     */

    @Override
    public void visit(TaskRecord t) {
        // Handle null explicitly: skip if no task
        switch (t) {
            case null -> {
                return;
            }
            case TaskRecord(var id, var title, var description, var state, var priority) -> {
                total++;

                // Count by priority
                switch (priority) {
                    case HIGH   -> inc(priorityCounts, Priority.HIGH);
                    case MEDIUM -> inc(priorityCounts, Priority.MEDIUM);
                    case LOW    -> inc(priorityCounts, Priority.LOW);
                    case NONE   -> inc(priorityCounts, Priority.NONE);
                }
                // Count by state
                switch (state) {
                    case TO_DO       -> inc(stateCounts, TaskState.TO_DO);
                    case IN_PROGRESS -> inc(stateCounts, TaskState.IN_PROGRESS);
                    case COMPLETED   -> inc(stateCounts, TaskState.COMPLETED);
                }
            }
        }
    }

    // Helper: increment a counter in an EnumMap
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
