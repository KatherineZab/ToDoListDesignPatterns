package model.report;

import model.TaskRecord;
import model.TaskVisitor;

/**
 * Visitor that exports tasks to CSV using record pattern matching.
 */
public final class CSVExportVisitor implements TaskVisitor {

    private final StringBuilder sb = new StringBuilder("id,title,description,state,priority\n");

    @Override
    public void visit(TaskRecord t) {
        // exhaustive & explicit: handle null explicitly
        switch (t) {
            case null -> {
                // ignore null records safely
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

    private static String escape(String s) {
        if (s == null) return "";
        String v = s.replace("\"", "\"\"");
        if (v.contains(",") || v.contains("\n") || v.contains("\"")) {
            return "\"" + v + "\"";
        }
        return v;
    }

    /** Returns the final CSV string. */
    public String csv() {
        return sb.toString();
    }
}
