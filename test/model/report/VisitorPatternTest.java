package model.report;

import model.TaskRecord;
import model.TaskState;
import model.entity.Priority;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Visitor pattern tests for:
 * - CombinedReportVisitor (text summary)
 * - CSVExportVisitor (CSV export with proper escaping)
 */
@DisplayName("Visitor Pattern – CombinedReportVisitor & CSVExportVisitor")
class VisitorPatternTest {

    private TaskRecord highPriorityTask;
    private TaskRecord mediumPriorityTask;
    private TaskRecord lowPriorityTask;
    private TaskRecord noPriorityTask;
    private TaskRecord completedTask;

    @BeforeEach
    void setUp() {
        highPriorityTask   = new TaskRecord(1, "Critical Bug",   "Fix ASAP",
                TaskState.IN_PROGRESS, Priority.HIGH);

        mediumPriorityTask = new TaskRecord(2, "Feature Request","Add new feature",
                TaskState.TO_DO, Priority.MEDIUM);

        lowPriorityTask    = new TaskRecord(3, "Minor Update",   "Update docs",
                TaskState.TO_DO, Priority.LOW);

        noPriorityTask     = new TaskRecord(4, "Regular Task",   "Normal work",
                TaskState.IN_PROGRESS, Priority.NONE);

        completedTask      = new TaskRecord(5, "Done Task",      "Finished work",
                TaskState.COMPLETED, Priority.MEDIUM);
    }

    // ---------- Helpers ----------

    private void assertCountLine(String report, String label, int expected) {
        // Match lines like: "HIGH:   1" or "IN_PROGRESS: 2" with flexible spaces.
        Pattern p = Pattern.compile("(?m)^\\s*" + Pattern.quote(label) + ":\\s*" + expected + "\\s*$");
        assertTrue(p.matcher(report).find(),
                () -> "Expected line '" + label + ": " + expected + "' in report:\n" + report);
    }

    private String[] csvLines(String csv) {
        return csv.replace("\r\n", "\n").split("\n");
    }

    // ---------- CombinedReportVisitor ----------

    @Test
    @DisplayName("CombinedReportVisitor counts by priority")
    void combinedReport_countsByPriority() {
        CombinedReportVisitor visitor = new CombinedReportVisitor();

        visitor.visit(highPriorityTask);   // HIGH
        visitor.visit(mediumPriorityTask); // MEDIUM
        visitor.visit(lowPriorityTask);    // LOW
        visitor.visit(noPriorityTask);     // NONE
        visitor.visit(completedTask);      // MEDIUM

        String report = visitor.asText();

        assertTrue(report.contains("Total tasks: 5"));
        assertCountLine(report, "HIGH",     1);
        assertCountLine(report, "MEDIUM",   2);
        assertCountLine(report, "LOW",      1);
        assertCountLine(report, "NONE",     1);
    }

    @Test
    @DisplayName("CombinedReportVisitor counts by state")
    void combinedReport_countsByState() {
        CombinedReportVisitor visitor = new CombinedReportVisitor();

        visitor.visit(highPriorityTask);   // IN_PROGRESS
        visitor.visit(mediumPriorityTask); // TO_DO
        visitor.visit(lowPriorityTask);    // TO_DO
        visitor.visit(noPriorityTask);     // IN_PROGRESS
        visitor.visit(completedTask);      // COMPLETED

        String report = visitor.asText();

        assertCountLine(report, "TO_DO",       2);
        assertCountLine(report, "IN_PROGRESS", 2);
        assertCountLine(report, "COMPLETED",   1);
    }

    @Test
    @DisplayName("CombinedReportVisitor handles empty visitor")
    void combinedReport_empty() {
        CombinedReportVisitor visitor = new CombinedReportVisitor();

        String report = visitor.asText();

        assertTrue(report.contains("Total tasks: 0"));
        assertCountLine(report, "HIGH",       0);
        assertCountLine(report, "MEDIUM",     0);
        assertCountLine(report, "LOW",        0);
        assertCountLine(report, "NONE",       0);
        assertCountLine(report, "TO_DO",       0);
        assertCountLine(report, "IN_PROGRESS", 0);
        assertCountLine(report, "COMPLETED",   0);
    }

    /**
     * Note: If your CombinedReportVisitor does NOT accept nulls (i.e., it throws or NPEs),
     * remove this test OR change it to assertThrows. The version below assumes it simply ignores nulls.
     */
    @Test
    @DisplayName("CombinedReportVisitor gracefully ignores null tasks (if supported)")
    void combinedReport_ignoresNulls() {
        CombinedReportVisitor visitor = new CombinedReportVisitor();
        visitor.visit(highPriorityTask);
        visitor.visit(null); // should be ignored by implementation (if you implemented it this way)
        visitor.visit(mediumPriorityTask);

        String report = visitor.asText();

        assertTrue(report.contains("Total tasks: 2"));
        assertCountLine(report, "HIGH",   1);
        assertCountLine(report, "MEDIUM", 1);
    }

    // ---------- CSVExportVisitor ----------

    @Test
    @DisplayName("CSVExportVisitor produces header and rows")
    void csv_basicFormat() {
        CSVExportVisitor visitor = new CSVExportVisitor();

        visitor.visit(highPriorityTask);
        visitor.visit(mediumPriorityTask);

        String csv = visitor.csv();
        String[] lines = csvLines(csv);

        // header
        assertEquals("id,title,description,state,priority", lines[0]);

        // first row
        assertEquals("1,Critical Bug,Fix ASAP,IN_PROGRESS,HIGH", lines[1]);

        // second row
        assertEquals("2,Feature Request,Add new feature,TO_DO,MEDIUM", lines[2]);

        // header + 2 rows = 3 lines
        assertEquals(3, lines.length);
    }

    @Test
    @DisplayName("CSVExportVisitor escapes commas and quotes per CSV rules")
    void csv_escaping() {
        TaskRecord withCommas = new TaskRecord(
                10, "Task, with commas", "Description, also with commas",
                TaskState.TO_DO, Priority.HIGH
        );
        TaskRecord withQuotes = new TaskRecord(
                11, "Task \"with quotes\"", "Description \"with quotes\"",
                TaskState.IN_PROGRESS, Priority.LOW
        );

        CSVExportVisitor visitor = new CSVExportVisitor();
        visitor.visit(withCommas);
        visitor.visit(withQuotes);

        String[] lines = csvLines(visitor.csv());

        // Header exists
        assertEquals("id,title,description,state,priority", lines[0]);

        // RFC4180-style escaping: fields containing comma or quote must be quoted,
        // and inner double quotes must be doubled.
        // Line for 'withCommas' should contain quoted title/description.
        assertTrue(lines[1].contains("\"Task, with commas\""));
        assertTrue(lines[1].contains("\"Description, also with commas\""));

        // For 'withQuotes': quotes must be doubled inside quoted fields -> "".
        assertTrue(lines[2].contains("\"Task \"\"with quotes\"\"\""));
        assertTrue(lines[2].contains("\"Description \"\"with quotes\"\"\""));
    }

    @Test
    @DisplayName("CSVExportVisitor on empty visitor returns only the header")
    void csv_empty() {
        CSVExportVisitor visitor = new CSVExportVisitor();
        String[] lines = csvLines(visitor.csv());

        assertEquals(1, lines.length);
        assertEquals("id,title,description,state,priority", lines[0]);
    }
}
