package model.report;

import model.TaskRecord;

// מממשים את הממשק מתוך package model
public final class CSVExportVisitor implements model.TaskVisitor {
    private final StringBuilder sb = new StringBuilder("id,title,description,state,priority\n");

    @Override
    public void visit(TaskRecord t) {
        sb.append(t.id()).append(",")
                .append(escape(t.title())).append(",")
                .append(escape(t.description())).append(",")
                .append(t.state().name()).append(",")
                .append(t.priority().name()).append("\n");
    }

    public String csv() { return sb.toString(); }

    private static String escape(String s) {
        if (s == null) return "";
        String x = s.replace("\"", "\"\"");
        if (x.contains(",") || x.contains("\n")) return "\"" + x + "\"";
        return x;
    }
}
