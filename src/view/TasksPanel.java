package view;

import model.combinator.TaskFilter;
import model.combinator.Filters;
import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;

public class TasksPanel extends JPanel {
    private final JTable table;
    private final DefaultTableModel model;
    private final TableRowSorter<DefaultTableModel> sorter;
    private int nextId = 1;

    public TasksPanel() {
        setLayout(new BorderLayout(8, 8));

        model = new DefaultTableModel(new Object[]{"ID", "Title", "Description", "State"}, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }

            @Override
            public Class<?> getColumnClass(int c) {
                return c == 0 ? Integer.class : String.class;
            }
        };

        table = new JTable(model);
        table.setRowHeight(24);
        sorter = new TableRowSorter<>(model);
        table.setRowSorter(sorter);
        add(new JScrollPane(table), BorderLayout.CENTER);

        addRow("Demo 1", "hello", "TO_DO");
        addRow("Demo 2", "world", "IN_PROGRESS");
        addRow("Demo 3", "finish ui", "COMPLETED");
    }

    public int addRowReturningId(String title, String desc, String state) {
        int id = nextId++;
        model.addRow(new Object[]{id, title, desc, state});
        return id;
    }

    public void addRowWithId(int id, String title, String desc, String state) {
        nextId = Math.max(nextId, id + 1);
        model.addRow(new Object[]{id, title, desc, state});
    }

    public void removeRowById(int id) {
        for (int i = 0; i < model.getRowCount(); i++) {
            Object v = model.getValueAt(i, 0);
            if (v instanceof Integer && (Integer) v == id) {
                model.removeRow(i);
                return;
            }
        }
    }

    public int selectedIdOrMinus1() {
        int r = table.getSelectedRow();
        return r < 0 ? -1 : (int) model.getValueAt(table.convertRowIndexToModel(r), 0);
    }

    public void setRowValuesById(int id, String title, String desc, String state) {
        for (int i = 0; i < model.getRowCount(); i++) {
            if ((int) model.getValueAt(i, 0) == id) {
                model.setValueAt(title, i, 1);
                model.setValueAt(desc, i, 2);
                model.setValueAt(state, i, 3);
                return;
            }
        }
    }

    public Object[] snapshotById(int id) {
        for (int i = 0; i < model.getRowCount(); i++) {
            if ((int) model.getValueAt(i, 0) == id) {
                return new Object[]{
                        model.getValueAt(i, 0),
                        model.getValueAt(i, 1),
                        model.getValueAt(i, 2),
                        model.getValueAt(i, 3)
                };
            }
        }
        return null;
    }

    public int modelIndexById(int id) {
        for (int i = 0; i < model.getRowCount(); i++) {
            Object v = model.getValueAt(i, 0);
            if (v instanceof Integer && (Integer) v == id) return i;
        }
        return -1;
    }

    public void addRowWithIdAt(int modelIndex, int id, String title, String desc, String state) {
        nextId = Math.max(nextId, id + 1);
        model.insertRow(Math.max(0, Math.min(modelIndex, model.getRowCount())),
                new Object[]{id, title, desc, state});
    }

    public void addRow(String title, String desc, String state) {
        model.addRow(new Object[]{nextId++, title, desc, state});
    }

    public void editSelectedRow(String newTitle, String newDesc, String newState) {
        int r = table.getSelectedRow();
        if (r < 0) return;
        int mr = table.convertRowIndexToModel(r);
        model.setValueAt(newTitle, mr, 1);
        model.setValueAt(newDesc, mr, 2);
        model.setValueAt(newState, mr, 3);
    }

    public void deleteSelectedRow() {
        int r = table.getSelectedRow();
        if (r >= 0) model.removeRow(table.convertRowIndexToModel(r));
    }

    public String currentTitle() {
        int r = table.getSelectedRow();
        return r >= 0 ? String.valueOf(model.getValueAt(table.convertRowIndexToModel(r), 1)) : "";
    }

    public String currentDesc() {
        int r = table.getSelectedRow();
        return r >= 0 ? String.valueOf(model.getValueAt(table.convertRowIndexToModel(r), 2)) : "";
    }

    public String currentState() {
        int r = table.getSelectedRow();
        return r >= 0 ? String.valueOf(model.getValueAt(table.convertRowIndexToModel(r), 3)) : "TO_DO";
    }

    // entry-point used by the UI
    public void applyFilter(String text, String state) {
        var f = Filters.textContains(text).and(Filters.stateIs(state));
        applyFilter(f);
    }

    // the only place that talks to JTable's RowFilter
    public void applyFilter(TaskFilter f) {
        sorter.setRowFilter(new RowFilter<>() {
            @Override public boolean include(Entry<? extends DefaultTableModel, ? extends Integer> e) {
                String title = String.valueOf(e.getValue(1));
                String desc  = String.valueOf(e.getValue(2));
                String state = String.valueOf(e.getValue(3));
                return f.test(title, desc, state);   // <- single choke point
            }
        });
    }

}