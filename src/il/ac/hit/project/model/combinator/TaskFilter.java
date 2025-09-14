package il.ac.hit.project.model.combinator;

/**
 * Functional interface for filtering tasks with combinator support.
 * Allows you to test tasks against criteria and combine filters using
 * logical operations like AND, OR, and NOT for complex filtering.
 */
@FunctionalInterface
public interface TaskFilter {

    /**
     * Tests whether a task matches this filter's criteria.
     * @param title the task's title text
     * @param desc the task's description text
     * @param state the task's current state as a string
     * @return true if the task passes this filter, false otherwise
     */
    boolean test(String title, String desc, String state);

    /**
     * Combines this filter with another using logical AND.
     * The resulting filter passes only if both filters pass.
     * @param other the filter to combine with this one
     * @return a new filter that requires both conditions to be true
     */
    default TaskFilter and(TaskFilter other) {
        return (t,d,s) -> this.test(t,d,s) && other.test(t,d,s);
    }

    /**
     * Combines this filter with another using logical OR.
     * The resulting filter passes if either filter passes.
     * @param other the filter to combine with this one
     * @return a new filter that requires only one condition to be true
     */
    default TaskFilter or(TaskFilter other) {
        return (t,d,s) -> this.test(t,d,s) || other.test(t,d,s);
    }

    /**
     * Creates the logical negation of this filter.
     * The resulting filter passes when this filter would fail.
     * @return a new filter that inverts this filter's logic
     */
    default TaskFilter not() {
        return (t,d,s) -> !this.test(t,d,s);
    }
}