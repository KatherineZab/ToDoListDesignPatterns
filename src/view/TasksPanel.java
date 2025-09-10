package view;

import model.ITask;
import model.TaskState;
import viewmodel.TasksViewModel;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class TasksPanel extends JPanel {

    private final DefaultTableModel model = new DefaultTableModel(
            new Object[]{"ID","Title","Description","State"}, 0
    ) {
        @Override public boolean isCellEditable(int row, int column) { return false; }
    };
    private final JTable table = new JTable(model);
    private TasksViewModel vm;

    // נשמור גם את כל-המשימות לאחר סינון (לצורך applyFilter)
    private List<ITask> currentView = java.util.Collections.emptyList();

    public TasksPanel() {
        setLayout(new BorderLayout());
        add(new JScrollPane(table), BorderLayout.CENTER);
    }

    // נקרא מתוך MainFrame.setViewModel(...)
    public void setViewModel(TasksViewModel vm) {
        this.vm = vm;
        // כל שינוי ב-VM → רענון טבלה
        vm.addListener(tasks -> SwingUtilities.invokeLater(this::refreshFromVM));
        refreshFromVM();
    }

    private void refreshFromVM() {
        if (vm == null) return;
        // ברירת מחדל: ללא פילטר — מציגים הכול
        currentView = vm.items();
        render(currentView);
    }

    private void render(List<ITask> list) {
        model.setRowCount(0);
        for (ITask t : list) {
            model.addRow(new Object[]{
                    t.getId(),
                    t.getTitle(),
                    t.getDescription(),
                    t.getState().name()   // מציגים "TO_DO"/"IN_PROGRESS"/"COMPLETED" כמו ב-Combo
            });
        }
    }

    // ===== מתודות קיימות שהפקודות שלכם קוראות =====

    // Add (פעם ראשונה) – מחזיר ID אמיתי מה-DB
    public int addRowReturningId(String title, String desc, String stateName) {
        try {
            int id = vm.addReturningId(title, desc, TaskState.valueOf(stateName));
            // ה-VM כבר עשה load() והפעלת listener תרנדר מחדש. נחזיר את ה-id לפקודה.
            return id;
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
    }

    // Add Redo – החזרה עם אותו ID
    public void addRowWithId(int id, String title, String desc, String stateName) {
        try {
            vm.addWithId(id, title, desc, TaskState.valueOf(stateName));
            // רענון קורה אוטומטית דרך listener
        } catch (Exception e) { e.printStackTrace(); }
    }

    public void removeRowById(int id) {
        try {
            vm.delete(id);
        } catch (Exception e) { e.printStackTrace(); }
    }

    public void setRowValuesById(int id, String newTitle, String newDesc, String newStateName) {
        try {
            vm.update(id, newTitle, newDesc, TaskState.valueOf(newStateName));
        } catch (Exception e) { e.printStackTrace(); }
    }

    // משמש DeleteTaskCommand לצילום לפני מחיקה
    public Object[] snapshotById(int id) {
        int idx = modelIndexById(id);
        if (idx < 0) return null;
        return new Object[]{
                model.getValueAt(idx, 0),
                model.getValueAt(idx, 1),
                model.getValueAt(idx, 2),
                model.getValueAt(idx, 3)
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
        // קודם DB, וה-VM ירנדר מחדש
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
        return Objects.toString(model.getValueAt(modelRow, 3), "TO_DO");
    }

    // ===== פילטר (נשאר בפאנל, מפשט חיבור ל-FiltersPanel הקיים) =====

    public void applyFilter(String query, String stateNameOrAll) {
        if (vm == null) return;
        var all = vm.items();
        var q = query == null ? "" : query.trim().toLowerCase();
        var filtered = all.stream()
                .filter(t -> q.isEmpty() || (t.getTitle()!=null && t.getTitle().toLowerCase().contains(q))
                        || (t.getDescription()!=null && t.getDescription().toLowerCase().contains(q)))
                .filter(t -> "ALL".equals(stateNameOrAll)
                        || t.getState().name().equals(stateNameOrAll))
                .collect(Collectors.toList());
        currentView = filtered;
        render(filtered);
    }
}
