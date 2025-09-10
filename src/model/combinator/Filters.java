package model.combinator;

public final class Filters {
    private Filters() {}

    public static TaskFilter any() { return (t,d,s) -> true; }

    public static TaskFilter textContains(String q) {
        String x = q == null ? "" : q.trim().toLowerCase();
        if (x.isEmpty()) return any();
        return (t,d,s) -> t.toLowerCase().contains(x) || d.toLowerCase().contains(x);
    }

    public static TaskFilter stateIs(String st) {
        if (st == null || "ANY".equals(st)) return any();
        return (t,d,s) -> s.equals(st);
    }
}
