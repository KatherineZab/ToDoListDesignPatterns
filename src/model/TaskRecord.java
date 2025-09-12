package model;

import model.entity.Priority;

public record TaskRecord(int id,
                         String title,
                         String description,
                         TaskState state,
                         Priority priority) implements ITask {
    @Override public int getId() { return id(); }
    @Override public String getTitle() { return title(); }
    @Override public String getDescription() { return description(); }
    @Override public TaskState getState() { return state(); }
}
