package model;

/**
 * Factory class for creating common task filters.
 * Provides pre-built filters for text searching and state matching
 * that can be combined using the TaskFilter combinator methods.
 */
public final class Filters {

    /** Prevent instantiation - this is a utility class with only static methods */
    private Filters() {}

    /**
     * Creates a filter that accepts all tasks.
     * Useful as a base case or when you want to clear all filtering.
     * @return a filter that always returns true
     */
    public static TaskFilter any() {
        return (t,d,s) -> true;
    }

    /**
     * Creates a filter that searches for text in task titles and descriptions.
     * The search is case-insensitive and matches partial text.
     * Empty or null queries return a filter that accepts everything.
     * @param q the text to search for; null and empty strings are treated as "match all"
     * @return a filter that checks if title or description contains the query text
     */
    public static TaskFilter textContains(String q) {
        String x = q == null ? "" : q.trim().toLowerCase();
        if (x.isEmpty()) return any();
        return (t,d,s) -> t.toLowerCase().contains(x) || d.toLowerCase().contains(x);
    }

    /**
     * Creates a filter that matches tasks in a specific state.
     * Special values "ALL" and null return a filter that accepts all states.
     * @param st the state name to match (e.g., "TO_DO", "COMPLETED"); "ALL" or null means any state
     * @return a filter that checks if the task state matches exactly
     */
    public static TaskFilter stateIs(String st) {
        if (st == null || "ALL".equals(st)) return any();
        return (t,d,s) -> s.equals(st);
    }
}