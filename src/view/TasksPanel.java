package view;

import model.ITask;
import model.TaskRecord;
import model.TaskState;
import model.entity.Priority;
import viewmodel.TasksViewModel;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.font.TextAttribute;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import model.combinator.Filters;
import model.combinator.TaskFilter;
import model.sort.ByPriority;
import model.sort.TaskSortStrategy;
import model.decorator.PriorityDecorator; // <-- הוספתי: שימוש בדקורטור רק להצגה

public class TasksPanel extends JPanel {

    // ----- שני מודלים ושתי טבלאות -----
    private final DefaultTableModel modelActive = new DefaultTableModel(
            new Object[]{"ID","Title","Description","Priority","State"}, 0
    ) { @Override public boolean isCellEditable(int r, int c) { return false; } };

    private final DefaultTableModel modelCompleted = new DefaultTableModel(
            new Object[]{"ID","Title","Description","Priority","State"}, 0
    ) { @Override public boolean isCellEditable(int r, int c) { return false; } };

    private final JTable tableActive    = new JTable(modelActive);
    private final JTable tableCompleted = new JTable(modelCompleted);

    private final JTabbedPane tabs = new JTabbedPane();

    private TasksViewModel vm;
    private TaskSortStrategy sortStrategy = null;
    private List<ITask> currentView = java.util.Collections.emptyList(); // נשמור את הרשימה המסוננת-מסודרת האחרונה

    public TasksPanel() {
        setLayout(new BorderLayout());

        JPanel activeRoot = new JPanel(new BorderLayout());
        activeRoot.add(new JScrollPane(tableActive), BorderLayout.CENTER);

        JPanel completedRoot = new JPanel(new BorderLayout());
        completedRoot.add(new JScrollPane(tableCompleted), BorderLayout.CENTER);

        tabs.addTab("Active (ToDo + InProgress)", activeRoot);
        tabs.addTab("Completed", completedRoot);

        add(tabs, BorderLayout.CENTER);

        // ---- הוספה מינימלית: renderer לעמודת Title (עמודה 1) בשתי הטבלאות ----
        TitleCellRenderer titleRenderer = new TitleCellRenderer();
        tableActive.getColumnModel().getColumn(1).setCellRenderer(titleRenderer);
        tableCompleted.getColumnModel().getColumn(1).setCellRenderer(titleRenderer);
        // אופציונלי למניעת "קפיצות" גובה
        tableActive.setRowHeight(22);
        tableCompleted.setRowHeight(22);
    }

    // ---------- חיבור VM ----------
    public void setViewModel(TasksViewModel vm) {
        this.vm = vm;
        vm.addListener(tasks -> SwingUtilities.invokeLater(this::refreshFromVM));
        refreshFromVM();
    }

    private void refreshFromVM() {
        if (vm == null) return;
        currentView = applySort(vm.items());
        renderSplit(currentView); // מילוי שתי הטבלאות
    }

    private List<ITask> applySort(List<ITask> list) {
        if (sortStrategy == null) return list;
        return list.stream().sorted(sortStrategy.comparator()).toList();
    }

    // ---------- רינדור לשתי טבלאות ----------
    private void renderSplit(List<ITask> list) {
        modelActive.setRowCount(0);
        modelCompleted.setRowCount(0);

        for (ITask t : list) {
            Priority p = (t instanceof TaskRecord tr) ? tr.priority() : Priority.NONE;

            Object[] row = new Object[]{
                    t.getId(),
                    t.getTitle(),
                    t.getDescription(),
                    p.name(),
                    t.getState().name()
            };
            if (t.getState() == TaskState.COMPLETED) {
                modelCompleted.addRow(row);
            } else {
                modelActive.addRow(row);
            }
        }
    }

    // ---------- פילטר קיים (נשאר אותו API), עכשיו מפצל לשתי טבלאות ----------
    public void applyFilter(String query, String stateNameOrAll) {
        if (vm == null) return;

        var all = vm.items();
        TaskFilter textFilter = Filters.textContains(query);
        TaskFilter stateFilter = Filters.stateIs(stateNameOrAll);
        TaskFilter combinedFilter = textFilter.and(stateFilter);

        var filtered = all.stream()
                .filter(task -> combinedFilter.test(
                        task.getTitle(),
                        task.getDescription(),
                        task.getState().name()
                ))
                .collect(Collectors.toList());

        currentView = applySort(filtered);
        renderSplit(currentView);
    }

    // ===== API used by commands (נשאר זהה חתימתית) =====

    public int addRowReturningId(String title, String desc, String stateName) {
        try {
            return vm.addReturningId(title, desc, TaskState.valueOf(stateName));
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
    }

    public void addRowWithId(int id, String title, String desc, String stateName) {
        try { vm.addWithId(id, title, desc, TaskState.valueOf(stateName)); }
        catch (Exception e) { e.printStackTrace(); }
    }

    public void removeRowById(int id) {
        try { vm.delete(id); } catch (Exception e) { e.printStackTrace(); }
    }

    public void setRowValuesById(int id, String newTitle, String newDesc, String newStateName) {
        try { vm.update(id, newTitle, newDesc, TaskState.valueOf(newStateName)); }
        catch (Exception e) { e.printStackTrace(); }
    }

    public Object[] snapshotById(int id) {
        int idx = modelIndexById(modelActive, id);
        DefaultTableModel m = modelActive;
        if (idx < 0) {
            idx = modelIndexById(modelCompleted, id);
            m = modelCompleted;
        }
        if (idx < 0) return null;
        return new Object[]{
                m.getValueAt(idx, 0), // id
                m.getValueAt(idx, 1), // title (RAW)
                m.getValueAt(idx, 2), // description
                m.getValueAt(idx, 3), // priority
                m.getValueAt(idx, 4)  // state
        };
    }

    public int addRowWithPriorityReturningId(String title, String desc, String stateName, String priorityName) {
        try {
            int id = vm.addWithPriorityReturningId(title, desc, TaskState.valueOf(stateName), Priority.valueOf(priorityName));
            return id;
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
    }

    // חיפוש לפי ID במודל מסוים
    private int modelIndexById(DefaultTableModel m, int id) {
        for (int i = 0; i < m.getRowCount(); i++) {
            Object val = m.getValueAt(i, 0);
            int rowId = (val instanceof Integer) ? (Integer) val : Integer.parseInt(val.toString());
            if (rowId == id) return i;
        }
        return -1;
    }

    // נשאיר חתימה ישנה (אם יש שימוש חיצוני), נחזיר אינדקס מה-Active ואם לא נמצא מה-Completed, אחרת -1
    public int modelIndexById(int id) {
        int idx = modelIndexById(modelActive, id);
        if (idx >= 0) return idx;
        return modelIndexById(modelCompleted, id);
    }

    public void addRowWithIdAt(int index, int id, String t, String d, String stName) {
        // Keeping same signature for compatibility
        addRowWithId(id, t, d, stName);
    }

    // ===== עזר ל-MainFrame (נשאר אותו API, כעת קורא מהטבלה המסומנת) =====

    public int selectedIdOrMinus1() {
        JTable tbl = selectedTable();
        int viewRow = tbl.getSelectedRow();
        if (viewRow < 0) return -1;
        int modelRow = tbl.convertRowIndexToModel(viewRow);
        DefaultTableModel m = modelOf(tbl);
        Object val = m.getValueAt(modelRow, 0);
        return (val instanceof Integer) ? (Integer) val : Integer.parseInt(val.toString());
    }

    public String currentTitle() {
        JTable tbl = selectedTable();
        int viewRow = tbl.getSelectedRow();
        if (viewRow < 0) return "";
        int modelRow = tbl.convertRowIndexToModel(viewRow);
        return Objects.toString(modelOf(tbl).getValueAt(modelRow, 1), "");
    }

    public String currentDesc() {
        JTable tbl = selectedTable();
        int viewRow = tbl.getSelectedRow();
        if (viewRow < 0) return "";
        int modelRow = tbl.convertRowIndexToModel(viewRow);
        return Objects.toString(modelOf(tbl).getValueAt(modelRow, 2), "");
    }

    public String currentState() {
        JTable tbl = selectedTable();
        int viewRow = tbl.getSelectedRow();
        if (viewRow < 0) return "TO_DO";
        int modelRow = tbl.convertRowIndexToModel(viewRow);
        return Objects.toString(modelOf(tbl).getValueAt(modelRow, 4), "TO_DO");
    }

    private JTable selectedTable() {
        if (tableActive.getSelectedRow() >= 0) return tableActive;
        if (tableCompleted.getSelectedRow() >= 0) return tableCompleted;
        // אם אין בחירה, נשתמש בטאב הפעיל
        return (tabs.getSelectedIndex() == 1) ? tableCompleted : tableActive;
    }

    private DefaultTableModel modelOf(JTable t) {
        return (t == tableActive) ? modelActive : modelCompleted;
    }

    // ===== Strategy (סידור) =====
    public void sortByPriorityHighToLow() {
        sortStrategy = new ByPriority();
        if (currentView != null) renderSplit(applySort(currentView));
    }

    public void clearSort() {
        sortStrategy = null;
        if (currentView != null) renderSplit(currentView);
    }

    // ===== Priority editor =====
    public void setPriorityForSelected() {
        int id = selectedIdOrMinus1();
        if (id < 0) return;

        String[] opts = {"NONE","LOW","MEDIUM","HIGH"};
        String chosen = (String) JOptionPane.showInputDialog(
                this, "Select priority:", "Priority",
                JOptionPane.PLAIN_MESSAGE, null, opts, "NONE"
        );
        if (chosen == null) return;

        try {
            vm.setPriority(id, Priority.valueOf(chosen));
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Failed to update priority",
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // =======================
    //  Renderer לעמודת Title
    // =======================
    /**
     * - משתמש ב-PriorityDecorator כדי להעשיר את הכותרת (prefix/סימון),
     * - צובע לפי Priority,
     * - מוסיף קו-חוצה אם Completed,
     * תוך שמירה על מודל טקסט נקי (RAW).
     */
    private static final class TitleCellRenderer extends DefaultTableCellRenderer {
        private static final Font BASE_FONT = new JLabel().getFont();

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus,
                                                       int row, int column) {
            JLabel lbl = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

            int modelRow = table.convertRowIndexToModel(row);
            DefaultTableModel m = (DefaultTableModel) table.getModel();

            String title = Objects.toString(m.getValueAt(modelRow, 1), "");
            String desc  = Objects.toString(m.getValueAt(modelRow, 2), "");
            Priority pr  = safePriority(Objects.toString(m.getValueAt(modelRow, 3), "NONE"));
            TaskState st = safeState(Objects.toString(m.getValueAt(modelRow, 4), "TO_DO"));

            // בניית TaskRecord קצר כדי להשתמש בדקורטור (בלי לגעת במודל)
            ITask rowTask = new TaskRecord(-1, title, desc, st, pr);
            String decoratedTitle = new PriorityDecorator(rowTask, pr).getTitle();

            // צבע לפי Priority (לא לדרוס בזמן בחירה)
            if (!isSelected) {
                lbl.setForeground(colorFor(pr));
            }

            // קו-חוצה אם Completed
            if (st == TaskState.COMPLETED) {
                Map<TextAttribute, Object> attrs = new HashMap<>(BASE_FONT.getAttributes());
                attrs.put(TextAttribute.STRIKETHROUGH, TextAttribute.STRIKETHROUGH_ON);
                lbl.setFont(BASE_FONT.deriveFont(attrs));
            } else {
                lbl.setFont(BASE_FONT);
            }

            lbl.setText(decoratedTitle);
            lbl.setToolTipText(title);

            return lbl;
        }


        private static Color colorFor(Priority p) {
            return switch (p) {
                case HIGH   -> new Color(0xC0, 0x00, 0x00);
                case MEDIUM -> new Color(0xB3, 0x6B, 0x00);
                case LOW    -> new Color(0x66, 0x66, 0x66);
                default     -> Color.BLACK;
            };
        }

        private static Priority safePriority(String n) {
            try { return Priority.valueOf(n); } catch (Exception e) { return Priority.NONE; }
        }

        private static TaskState safeState(String n) {
            try { return TaskState.valueOf(n); } catch (Exception e) { return TaskState.TO_DO; }
        }
    }
}
