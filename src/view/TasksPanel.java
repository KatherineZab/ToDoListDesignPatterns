package view;


import model.ITask;
import model.TaskState;
import model.TaskRecord;
import model.entity.Priority; // if Priority is elsewhere, adjust the import
import viewmodel.TasksViewModel;



import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import model.combinator.TaskFilter;
import model.combinator.Filters;

import model.sort.TaskSortStrategy;
import model.sort.ByPriority;

public class TasksPanel extends JPanel {

    // Columns: ID, Title, Description, Priority, State
    private final DefaultTableModel model = new DefaultTableModel(
            new Object[]{"ID","Title","Description","Priority","State"}, 0
    ) {
        @Override public boolean isCellEditable(int row, int column) { return false; }
    };

    private final JTable table = new JTable(model);
    private TasksViewModel vm;
    private TaskSortStrategy sortStrategy = null;

    private List<ITask> currentView = java.util.Collections.emptyList();

    public TasksPanel() {
        setLayout(new BorderLayout());
        add(new JScrollPane(table), BorderLayout.CENTER);
    }

    public void setViewModel(TasksViewModel vm) {
        this.vm = vm;
        vm.addListener(tasks -> SwingUtilities.invokeLater(this::refreshFromVM));
        refreshFromVM();
    }

    private void refreshFromVM() {
        if (vm == null) return;
        currentView = applySort(vm.items());
        render(currentView);
    }

    private List<ITask> applySort(List<ITask> list) {
        if (sortStrategy == null) return list;
        return list.stream().sorted(sortStrategy.comparator()).toList();
    }

    private void render(List<ITask> list) {
        model.setRowCount(0);
        for (ITask t : list) {
            Priority p = (t instanceof TaskRecord tr) ? tr.priority() : Priority.NONE;
            model.addRow(new Object[]{
                    t.getId(),
                    t.getTitle(),
                    t.getDescription(),
                    p.name(),
                    t.getState().name()
            });
        }
    }

    public void applyFilter(String query, String stateNameOrAll) {
        if (vm == null) return;

        var all = vm.items();

        // Use your Combinator pattern
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
        render(currentView);
    }


    // ===== API used by commands =====

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
        int idx = modelIndexById(id);
        if (idx < 0) return null;
        return new Object[]{
                model.getValueAt(idx, 0), // id
                model.getValueAt(idx, 1), // title
                model.getValueAt(idx, 2), // description
                model.getValueAt(idx, 3), // priority
                model.getValueAt(idx, 4)  // state
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

    public int modelIndexById(int id) {
        for (int i = 0; i < model.getRowCount(); i++) {
            Object val = model.getValueAt(i, 0);
            int rowId = (val instanceof Integer) ? (Integer) val : Integer.parseInt(val.toString());
            if (rowId == id) return i;
        }
        return -1;
    }

    public void addRowWithIdAt(int index, int id, String t, String d, String stName) {
        // Keeping same signature for compatibility
        addRowWithId(id, t, d, stName);
    }

    // ===== helpers for MainFrame =====

    public int selectedIdOrMinus1() {
        int viewRow = table.getSelectedRow();
        if (viewRow < 0) return -1;
        int modelRow = table.convertRowIndexToModel(viewRow);
        Object val = model.getValueAt(modelRow, 0);
        return (val instanceof Integer) ? (Integer) val : Integer.parseInt(val.toString());
    }

    public String currentTitle() {
        int viewRow = table.getSelectedRow();
        if (viewRow < 0) return "";
        int modelRow = table.convertRowIndexToModel(viewRow);
        return Objects.toString(model.getValueAt(modelRow, 1), "");
    }

    public String currentDesc() {
        int viewRow = table.getSelectedRow();
        if (viewRow < 0) return "";
        int modelRow = table.convertRowIndexToModel(viewRow);
        return Objects.toString(model.getValueAt(modelRow, 2), "");
    }

    public String currentState() {
        int viewRow = table.getSelectedRow();
        if (viewRow < 0) return "TO_DO";
        int modelRow = table.convertRowIndexToModel(viewRow);
        return Objects.toString(model.getValueAt(modelRow, 4), "TO_DO");
    }

    // ===== sorting strategy hooks =====

    public void sortByPriorityHighToLow() {
        sortStrategy = new ByPriority();
        if (currentView != null) render(applySort(currentView));
    }

    public void clearSort() {
        sortStrategy = null;
        if (currentView != null) render(currentView);
    }

    // ===== priority editor =====

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
}
