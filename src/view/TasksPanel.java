package view;

import model.ITask;
import model.TaskRecord;
import model.TaskState;
import model.entity.Priority;
import model.observable.TasksListener;
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

public class TasksPanel extends JPanel {

    private final DefaultTableModel modelActive = new DefaultTableModel(
            new Object[]{"ID","Title","Description","Priority","State"}, 0
    ) { @Override public boolean isCellEditable(int r, int c) { return false; } };

    private final DefaultTableModel modelCompleted = new DefaultTableModel(
            new Object[]{"ID","Title","Description","Priority","State"}, 0
    ) { @Override public boolean isCellEditable(int r, int c) { return false; } };

    private final JTable tableActive = new JTable(modelActive);
    private final JTable tableCompleted = new JTable(modelCompleted);
    private final JTabbedPane tabs = new JTabbedPane();

    private TasksViewModel vm;

    // Observer pattern: Listen to ViewModel changes
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

        // Decorator pattern: Custom renderer for title column
        TitleCellRenderer titleRenderer = new TitleCellRenderer();
        tableActive.getColumnModel().getColumn(1).setCellRenderer(titleRenderer);
        tableCompleted.getColumnModel().getColumn(1).setCellRenderer(titleRenderer);

        tableActive.setRowHeight(22);
        tableCompleted.setRowHeight(22);
    }

    /* ---------------- MVVM Wiring ---------------- */

    public void setViewModel(TasksViewModel vm) {
        if (this.vm != null) {
            this.vm.removeTasksListener(uiListener);
        }
        this.vm = vm;
        if (this.vm != null) {
            this.vm.addTasksListener(uiListener);
            refreshFromVM();
        } else {
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

    /* ---------------- UI Updates (Observer Pattern) ---------------- */

    private void refreshFromVM() {
        if (vm == null) return;
        refreshFromSnapshot(vm.items());
    }

    private void refreshFromSnapshot(List<ITask> snapshot) {
        // Pure UI: Just render what ViewModel provides (already filtered/sorted)
        renderSplit(snapshot);
    }

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

    /* ---------------- Pure UI: Trigger ViewModel Operations ---------------- */

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

    public void sortByPriorityHighToLow() {
        if (vm != null) {
            vm.setSortStrategy(new model.sort.ByPriority());
        }
    }

    public void clearSort() {
        if (vm != null) {
            vm.setSortStrategy(null);
        }
    }

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

    /* ---------------- Selection Helpers (Pure UI) ---------------- */

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

    /* ---------------- Decorator Pattern: Title Cell Renderer ---------------- */

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
            String desc = Objects.toString(m.getValueAt(modelRow, 2), "");
            Priority pr = safePriority(Objects.toString(m.getValueAt(modelRow, 3), "NONE"));
            TaskState st = safeState(Objects.toString(m.getValueAt(modelRow, 4), "TO_DO"));

            // Decorator pattern: Enhance title with priority indicators
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
                case HIGH -> new Color(0xC0, 0x00, 0x00);
                case MEDIUM -> new Color(0xB3, 0x6B, 0x00);
                case LOW -> new Color(0x66, 0x66, 0x66);
                default -> Color.BLACK;
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