package view;

import model.ITask;
import model.TaskState;
import model.TaskRecord;
import model.entity.Priority; // אם Priority אצלך ב-model ולא ב-entity, החליפי ל: import model.Priority;
import viewmodel.TasksViewModel;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class TasksPanel extends JPanel {

    // הוספנו עמודת "Priority" (סדר: ID, Title, Description, Priority, State)
    private final DefaultTableModel model = new DefaultTableModel(
            new Object[]{"ID","Title","Description","Priority","State"}, 0
    ) {
        @Override public boolean isCellEditable(int row, int column) { return false; }
    };

    private final JTable table = new JTable(model);
    private TasksViewModel vm;

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
        currentView = vm.items();
        render(currentView);
    }

    private void render(List<ITask> list) {
        model.setRowCount(0);
        for (ITask t : list) {
            Priority p = (t instanceof TaskRecord tr) ? tr.priority() : Priority.NONE;
            model.addRow(new Object[]{
                    t.getId(),
                    t.getTitle(),               // לא מוסיפים תגית בכותרת יותר
                    t.getDescription(),
                    p.name(),                   // עמודת Priority חדשה
                    t.getState().name()
            });
        }
    }

    // ===== מתודות קיימות שהפקודות שלך קוראות =====

    public int addRowReturningId(String title, String desc, String stateName) {
        try {
            int id = vm.addReturningId(title, desc, TaskState.valueOf(stateName));
            return id; // ה-VM ירענן דרך listener
        } catch (Exception e) { e.printStackTrace(); return -1; }
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

    // משמש DeleteTaskCommand לצילום לפני מחיקה — שומרים *בדיוק* אותם 4 שדות כמו קודם
    public Object[] snapshotById(int id) {
        int idx = modelIndexById(id);
        if (idx < 0) return null;
        return new Object[]{
                model.getValueAt(idx, 0), // id
                model.getValueAt(idx, 1), // title
                model.getValueAt(idx, 2), // description
                model.getValueAt(idx, 4)  // state  (שימי לב: היה 3, עכשיו 4 כי נוספה עמודת Priority)
        };
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
        // לא משנים חתימה — נשאר תואם לפקודות הקיימות
        addRowWithId(id, t, d, stName);
    }

    // ===== עזר ל-MainFrame (כפתורי Edit/Delete) =====

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
        return Objects.toString(model.getValueAt(modelRow, 4), "TO_DO"); // היה 3 → כעת 4
    }

    // ===== פילטר (נשאר כמו שהיה) =====

    public void applyFilter(String query, String stateNameOrAll) {
        if (vm == null) return;
        var all = vm.items();
        var q = query == null ? "" : query.trim().toLowerCase();
        var filtered = all.stream()
                .filter(t -> q.isEmpty()
                        || (t.getTitle()!=null && t.getTitle().toLowerCase().contains(q))
                        || (t.getDescription()!=null && t.getDescription().toLowerCase().contains(q)))
                .filter(t -> "ALL".equals(stateNameOrAll)
                        || t.getState().name().equals(stateNameOrAll))
                .collect(Collectors.toList());
        currentView = filtered;
        render(filtered);
    }

    // ===== Priority: עדכון ב-DB דרך ה-VM =====

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
            JOptionPane.showMessageDialog(this, "Failed to update priority","Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
