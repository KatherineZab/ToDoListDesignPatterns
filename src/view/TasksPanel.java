package view;

import model.ITask;
import model.TaskRecord;
import model.TaskState;
import model.entity.Priority;
import model.observable.TasksListener;
import model.combinator.Filters;
import model.combinator.TaskFilter;
import model.sort.ByPriority;                 // we only *select* a strategy; VM applies it
import model.decorator.PriorityDecorator;

import viewModel.TasksViewModel;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.font.TextAttribute;
import java.util.*;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class TasksPanel extends JPanel {

    // ----- models & tables -----
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
    private List<ITask> currentView = java.util.Collections.emptyList();

    // Subscribe via a dedicated TasksListener (observer lives outside the view)
    private final TasksListener uiListener = this::refreshFromSnapshot;

    public TasksPanel() {
        setLayout(new BorderLayout());

        JPanel activeRoot = new JPanel(new BorderLayout());
        activeRoot.add(new JScrollPane(tableActive), BorderLayout.CENTER);

        JPanel completedRoot = new JPanel(new BorderLayout());
        completedRoot.add(new JScrollPane(tableCompleted), BorderLayout.CENTER);

        tabs.addTab("Active (ToDo + InProgress)", activeRoot);
        tabs.addTab("Completed", completedRoot);

        add(tabs, BorderLayout.CENTER);

        // Title renderer for both tables
        TitleCellRenderer titleRenderer = new TitleCellRenderer();
        tableActive.getColumnModel().getColumn(1).setCellRenderer(titleRenderer);
        tableCompleted.getColumnModel().getColumn(1).setCellRenderer(titleRenderer);

        tableActive.setRowHeight(22);
        tableCompleted.setRowHeight(22);
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

    /* ---------------- Rendering into two tables ---------------- */

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
            if (t.getState() == TaskState.COMPLETED) modelCompleted.addRow(row);
            else modelActive.addRow(row);
        }
    }

    /* ---------------- Filtering (UI-only; Combinator) ---------------- */

    public void applyFilter(String query, String stateNameOrAll) {
        if (vm == null) return;

        var all = vm.items(); // already sorted by VM; filtering is a view concern
        TaskFilter textFilter  = Filters.textContains(query);
        TaskFilter stateFilter = Filters.stateIs(stateNameOrAll);
        TaskFilter combined    = textFilter.and(stateFilter);

        var filtered = all.stream()
                .filter(task -> combined.test(
                        task.getTitle(),
                        task.getDescription(),
                        task.getState().name()
                ))
                .collect(Collectors.toList());

        currentView = filtered;
        renderSplit(currentView);
    }

    /* ---------------- Selection helpers (View-only) ---------------- */

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
        return (tabs.getSelectedIndex() == 1) ? tableCompleted : tableActive;
    }

    private DefaultTableModel modelOf(JTable t) {
        return (t == tableActive) ? modelActive : modelCompleted;
    }

    /* ---------------- Strategy (sorting) — delegate to VM ---------------- */

    public void sortByPriorityHighToLow() {
        if (vm == null) return;
        vm.setSortStrategy(new ByPriority());
        // VM will notify and refresh us via uiListener
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

    /* ---------------- Renderer for Title column (Decorator) ---------------- */

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
}
