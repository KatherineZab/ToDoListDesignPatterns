package model.report;

import model.TaskRecord;
import model.TaskState;
import model.entity.Priority; // אם Priority אצלך ב-model: import model.Priority

// שימי לב: מממשים את הממשק מתוך package model
public final class CombinedReportVisitor implements model.TaskVisitor {
    private final java.util.EnumMap<Priority, Integer> priorityCounts = new java.util.EnumMap<>(Priority.class);
    private final java.util.EnumMap<TaskState, Integer> stateCounts   = new java.util.EnumMap<>(TaskState.class);
    private int total = 0;

    public CombinedReportVisitor() {
        for (var p : Priority.values())   priorityCounts.put(p, 0);
        for (var s : TaskState.values())  stateCounts.put(s, 0);
    }

    @Override
    public void visit(TaskRecord t) {
        priorityCounts.compute(t.priority(), (k, v) -> v + 1);
        stateCounts.compute(t.state(), (k, v) -> v + 1);
        total++;
    }

    public String asText() {
        return """
               Tasks Report
               ------------
               Total: %d

               By Priority:
                 HIGH: %d
                 MEDIUM: %d
                 LOW: %d
                 NONE: %d

               By State:
                 TO_DO: %d
                 IN_PROGRESS: %d
                 COMPLETED: %d
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
