package view;

import model.ITask;
import model.TaskRecord;
import model.TaskState;
import model.entity.Priority;
import model.observable.TasksListener;
import model.decorator.PriorityDecorator;
import model.sort.ByState;
import model.sort.ByPriority;

import viewModel.TasksViewModel;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.font.TextAttribute;
import java.util.*;
import java.util.List;
import java.util.Objects;

public class TasksPanel extends JPanel {

    /* ===== שינוי מרכזי: טבלה אחת במקום שתי טבלאות + לשוניות ===== */
    private final DefaultTableModel model = new DefaultTableModel(
            new Object[]{"ID","Title","Description","Priority","State"}, 0
    ) { @Override public boolean isCellEditable(int r, int c) { return false; } };

    private final JTable table = new JTable(model);

    /* ===== נשמר: חיבור ל-VM, Observer, currentView ===== */
    private TasksViewModel vm;
    private List<ITask> currentView = java.util.Collections.emptyList();

    // Subscribe via a dedicated TasksListener (observer lives outside the view)
    private final TasksListener uiListener = this::refreshFromSnapshot;

    public TasksPanel() {
        setLayout(new BorderLayout());

        /* ===== שינוי: במקום tabs עם שתי טבלאות, רק JScrollPane אחד ===== */
        add(new JScrollPane(table), BorderLayout.CENTER);

        // Title renderer (Decorator usage)
        TitleCellRenderer titleRenderer = new TitleCellRenderer();
        table.getColumnModel().getColumn(1).setCellRenderer(titleRenderer);

        // State renderer (badge)
        StateCellRenderer stateRenderer = new StateCellRenderer();
        table.getColumnModel().getColumn(4).setCellRenderer(stateRenderer);

        table.setRowHeight(22);
    }

    /* ---------------- MVVM wiring ---------------- */

    public void setViewModel(TasksViewModel vm) {
        // unsubscribe from previous VM
        if (this.vm != null) {
            this.vm.removeTasksListener(uiListener);
        }
        this.vm = vm;
        if (this.vm != null) {
            this.vm.addTasksListener(uiListener);
            // initial paint from VM (already sorted by the VM's strategy)
            refreshFromVM();
        } else {
            // clear UI if no VM
            renderSplit(Collections.emptyList());
        }
    }

    @Override
    public void addNotify() {
        super.addNotify();
        if (vm != null) vm.addTasksListener(uiListener);
    }

    @Override
    public void removeNotify() {
        if (vm != null) vm.removeTasksListener(uiListener);
        super.removeNotify();
    }

    /* ---------------- Refresh paths ---------------- */

    private void refreshFromVM() {
        if (vm == null) return;
        refreshFromSnapshot(vm.items());
    }

    private void refreshFromSnapshot(List<ITask> snapshot) {
        // VM provides a *sorted* snapshot; view renders as-is
        currentView = snapshot;
        renderSplit(currentView);
    }

    /* ---------------- Rendering (עוד שומר שם מתודה) ---------------- */

    /** במקום פיצול לשתי טבלאות, ממלאים טבלה אחת. */
    private void renderSplit(List<ITask> list) {
        model.setRowCount(0);

        for (ITask t : list) {
            Priority p = (t instanceof TaskRecord tr) ? tr.priority() : Priority.NONE;
            Object[] row = new Object[]{
                    t.getId(),
                    t.getTitle(),
                    t.getDescription(),
                    p.name(),                 // משאירים כמחרוזת כדי לא לשבור רנדרר קיים
                    t.getState().name()       // idem
            };
            model.addRow(row);
        }
    }

    /* ---------------- Filtering (Combinator) ---------------- */

    public void applyFilter(String query, String stateNameOrAll) {
        if (vm != null) {
            vm.applyFilter(query, stateNameOrAll); // Delegate to ViewModel
            // ViewModel will notify us via Observer pattern
        }
    }

    public void clearFilter() {
        if (vm != null) {
            vm.clearFilter();
        }
    }

    /* ---------------- Selection helpers (שומרים חתימות) ---------------- */

    public int selectedIdOrMinus1() {
        JTable tbl = selectedTable();
        int viewRow = tbl.getSelectedRow();
        if (viewRow < 0) return -1;
        int modelRow = tbl.convertRowIndexToModel(viewRow);
        DefaultTableModel m = modelOf(tbl);
        Object val = m.getValueAt(modelRow, 0);
        return (val instanceof Integer) ? (Integer) val : Integer.parseInt(val.toString());
    }

    /** תמיד מחזיר את הטבלה היחידה (כדי לא לשנות קריאות קיימות). */
    private JTable selectedTable() {
        return table;
    }

    /** תמיד מחזיר את המודל היחיד (כדי לא לשנות קריאות קיימות). */
    private DefaultTableModel modelOf(JTable t) {
        return model;
    }

    /* ---------------- Strategy (sorting) — delegate to VM ---------------- */

    public void sortByPriorityHighToLow() {
        if (vm == null) return;
        vm.setSortStrategy(new ByPriority());
        // VM will notify and refresh us via uiListener
    }

    public void sortByStateToDoFirst() {
        if (vm == null) return;
        vm.setSortStrategy(new ByState()); // default order in ByState is TO_DO → IN_PROGRESS
    }

    public void clearSort() {
        if (vm == null) return;
        vm.setSortStrategy(null);
        // VM will notify and refresh us via uiListener
    }

    /* ---------------- Optional: Priority editor (UI trigger) ---------------- */

    public void setPriorityForSelected() {
        int id = selectedIdOrMinus1();
        if (id < 0 || vm == null) return;

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

    /* ---------------- Renderers ---------------- */

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

            // Decorator pattern: Enhance title (wrapper that can add logic later)
            ITask rowTask = new TaskRecord(-1, title, desc, st, pr);
            String decoratedTitle = new PriorityDecorator(rowTask, pr).getTitle();

            if (!isSelected) {
                lbl.setForeground(colorFor(pr));
            }

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

    private static final class StateCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(
                JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            JLabel lbl = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

            TaskState st;
            try {
                // בעמודה שמרנו name(), לכן צריך להמיר חזרה ל-enum
                st = TaskState.valueOf(Objects.toString(value, "TO_DO"));
            } catch (Exception e) {
                st = TaskState.TO_DO;
            }

            lbl.setText(st.badge()); // shows friendly text
            return lbl;
        }
    }
}
