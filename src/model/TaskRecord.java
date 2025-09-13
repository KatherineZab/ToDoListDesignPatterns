package model;

import java.util.Set;
import model.entity.Priority;

public record TaskRecord(
        int id,
        String title,
        String description,
        TaskState state,
        Priority priority
) implements ITask {

    @Override public int getId() { return id; }
    @Override public String getTitle() { return title; }
    @Override public String getDescription() { return description; }
    @Override public TaskState getState() { return state; }

    public Set<TaskState> allowedNextStates() { return state.nextStates(); }

    public TaskRecord withState(TaskState next) {
        if (!state.canTransitionTo(next))
            throw new IllegalStateException(state + " → " + next + " not allowed");
        return new TaskRecord(id, title, description, next, priority);
    }
}
