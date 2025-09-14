package model.report;

import model.TaskRecord;
import model.TaskVisitor;

/**
 * Visitor that exports tasks to CSV using record pattern matching.
 */
public final class CSVExportVisitor implements TaskVisitor {

    // CSV buffer including the header row.
    private final StringBuilder sb = new StringBuilder("id,title,description,state,priority\n");

    /**
     * Adds one task as a CSV row.
     * - If {@code t} is null, it is skipped.
     * - Otherwise, fields are appended in the same order as the header.
     */
    @Override
    public void visit(TaskRecord t) {
        switch (t) {
            case null -> {
                return;
            }
            // record pattern: clean destructuring of the record's components
            case TaskRecord(var id, var title, var description, var state, var priority) -> {
                sb.append(id).append(",")
                        .append(escape(title)).append(",")
                        .append(escape(description)).append(",")
                        .append(state.name()).append(",")
                        .append(priority.name()).append("\n");
            }
        }
    }

    /**
     * Escapes a value for CSV:
     * - null becomes an empty string
     * - double quotes are doubled
     * - wrap in quotes if it contains a comma, newline, or quote
     */
    private static String escape(String s) {
        if (s == null) return "";
        String v = s.replace("\"", "\"\"");
        if (v.contains(",") || v.contains("\n") || v.contains("\"")) {
            return "\"" + v + "\"";
        }
        return v;
    }

    // Returns the final CSV string
    public String csv() {
        return sb.toString();
    }
}
