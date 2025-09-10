package model.combinator;

@FunctionalInterface
public interface TaskFilter {
    boolean test(String title, String desc, String state);

    default TaskFilter and(TaskFilter other) { return (t,d,s) -> this.test(t,d,s) && other.test(t,d,s); }
    default TaskFilter or (TaskFilter other) { return (t,d,s) -> this.test(t,d,s) ||  other.test(t,d,s); }
    default TaskFilter not()                 { return (t,d,s) -> !this.test(t,d,s); }
}
