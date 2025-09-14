package il.ac.hit.project.view;

import il.ac.hit.project.model.ITask;
import il.ac.hit.project.model.TaskRecord;
import il.ac.hit.project.model.TaskState;
import il.ac.hit.project.model.entity.Priority;
import il.ac.hit.project.model.observable.TasksListener;
import il.ac.hit.project.model.decorator.PriorityDecorator;
import il.ac.hit.project.model.sort.ByState;
import il.ac.hit.project.model.sort.ByPriority;

import il.ac.hit.project.viewModel.TasksViewModel;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.font.TextAttribute;
import java.util.*;
import java.util.List;
import java.util.Objects;

/**
 * Main UI component for displaying tasks in a table format.
 * Implements the View layer in MVVM - handles data presentation and
 * delegates all business operations to the ViewModel. Uses Observer pattern
 * to automatically update when task data changes.
 */
public class TasksPanel extends JPanel {

    /**
     * Table model with read-only cells for displaying task data.
     * Columns: ID, Title, Description, Priority, State
     */
    private final DefaultTableModel model = new DefaultTableModel(
            new Object[]{"ID","Title","Description","Priority","State"}, 0
    ) { @Override public boolean isCellEditable(int r, int c) { return false; } };

    private final JTable table = new JTable(model);

    /** The ViewModel that provides task data and handles business logic */
    private TasksViewModel vm;

    /** Current snapshot of tasks being displayed */
    private List<ITask> currentView = java.util.Collections.emptyList();

    /** Observer that gets notified when ViewModel data changes */
    private final TasksListener uiListener = this::refreshFromSnapshot;

    /**
     * Creates the tasks panel with table setup and custom renderers.
     * Sets up title renderer (uses Decorator pattern) and state renderer (shows badges).
     */
    public TasksPanel() {
        setLayout(new BorderLayout());
        add(new JScrollPane(table), BorderLayout.CENTER);

        // Custom renderers for enhanced display
        TitleCellRenderer titleRenderer = new TitleCellRenderer();
        table.getColumnModel().getColumn(1).setCellRenderer(titleRenderer);

        StateCellRenderer stateRenderer = new StateCellRenderer();
        table.getColumnModel().getColumn(4).setCellRenderer(stateRenderer);

        table.setRowHeight(22);
    }

    /**
     * Connects this view to a ViewModel following MVVM pattern.
     * Subscribes to ViewModel notifications and performs initial data load.
     * Properly unsubscribes from previous ViewModel to prevent memory leaks.
     * @param vm the ViewModel to connect to; null clears the view
     */
    public void setViewModel(TasksViewModel vm) {
        // Clean up previous ViewModel connection
        if (this.vm != null) {
            this.vm.removeTasksListener(uiListener);
        }

        this.vm = vm;

        if (this.vm != null) {
            this.vm.addTasksListener(uiListener);
            refreshFromVM(); // Initial data load
        } else {
            renderSplit(Collections.emptyList()); // Clear display
        }
    }

    /**
     * Ensures observer subscription when component becomes visible.
     * Part of proper cleanup lifecycle in Swing applications.
     */
    @Override
    public void addNotify() {
        super.addNotify();
        if (vm != null) vm.addTasksListener(uiListener);
    }

    /**
     * Ensures observer unsubscription when component is removed.
     * Prevents memory leaks by cleaning up observer references.
     */
    @Override
    public void removeNotify() {
        if (vm != null) vm.removeTasksListener(uiListener);
        super.removeNotify();
    }

    /**
     * Refreshes display from current ViewModel data.
     * Called internally when we need to reload from ViewModel.
     */
    private void refreshFromVM() {
        if (vm == null) return;
        refreshFromSnapshot(vm.items());
    }

    /**
     * Updates the display with a new task snapshot.
     * Called by Observer pattern when ViewModel notifies of changes.
     * The ViewModel provides pre-sorted data, so we render as-is.
     * @param snapshot the current list of tasks to display
     */
    private void refreshFromSnapshot(List<ITask> snapshot) {
        currentView = snapshot;
        renderSplit(currentView);
    }

    /**
     * Renders the task list into the table model.
     * Extracts all necessary data and converts to table row format.
     * @param list the tasks to display in the table
     */
    private void renderSplit(List<ITask> list) {
        model.setRowCount(0);

        for (ITask t : list) {
            Priority p = (t instanceof TaskRecord tr) ? tr.priority() : Priority.NONE;
            Object[] row = new Object[]{
                    t.getId(),
                    t.getTitle(),
                    t.getDescription(),
                    p.name(),           // Store as string for renderer compatibility
                    t.getState().name() // Store as string for renderer compatibility
            };
            model.addRow(row);
        }
    }

    /**
     * Applies text and state filters through the ViewModel.
     * Delegates to ViewModel which will notify us of results via Observer pattern.
     * @param query text to search for in title/description
     * @param stateNameOrAll specific state filter or "ALL" for no state filtering
     */
    public void applyFilter(String query, String stateNameOrAll) {
        if (vm != null) {
            vm.applyFilter(query, stateNameOrAll);
        }
    }

    /**
     * Clears all active filters through the ViewModel.
     * ViewModel will notify us of the updated results.
     */
    public void clearFilter() {
        if (vm != null) {
            vm.clearFilter();
        }
    }

    /**
     * Gets the ID of the currently selected task.
     * Handles view-to-model row conversion for proper selection tracking.
     * @return the selected task ID, or -1 if no selection
     */
    public int selectedIdOrMinus1() {
        int viewRow = table.getSelectedRow();
        if (viewRow < 0) return -1;
        int modelRow = table.convertRowIndexToModel(viewRow);
        Object val = model.getValueAt(modelRow, 0);
        return (val instanceof Integer) ? (Integer) val : Integer.parseInt(val.toString());
    }

    /**
     * Applies priority-based sorting through the ViewModel.
     * Uses Strategy pattern - delegates to ViewModel which applies ByPriority strategy.
     */
    public void sortByPriorityHighToLow() {
        if (vm == null) return;
        vm.setSortStrategy(new ByPriority());
    }

    /**
     * Applies state-based sorting through the ViewModel.
     * Uses Strategy pattern - delegates to ViewModel which applies ByState strategy.
     */
    public void sortByStateToDoFirst() {
        if (vm == null) return;
        vm.setSortStrategy(new ByState());
    }

    /**
     * Clears sorting and returns to default order through the ViewModel.
     * Uses Strategy pattern - removes any active sorting strategy.
     */
    public void clearSort() {
        if (vm == null) return;
        vm.setSortStrategy(null);
    }

    /**
     * Shows priority selection dialog for the currently selected task.
     * Provides direct UI for priority editing without going through commands.
     * Updates priority through ViewModel which will notify observers.
     */
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

    /**
     * Custom renderer for task title column that applies visual enhancements.
     * Uses Decorator pattern to enhance titles and applies priority-based coloring.
     * Shows strikethrough text for completed tasks.
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

            // Extract task data from table model
            String title = Objects.toString(m.getValueAt(modelRow, 1), "");
            String desc  = Objects.toString(m.getValueAt(modelRow, 2), "");
            Priority pr  = safePriority(Objects.toString(m.getValueAt(modelRow, 3), "NONE"));
            TaskState st = safeState(Objects.toString(m.getValueAt(modelRow, 4), "TO_DO"));

            // Apply Decorator pattern for title enhancement
            ITask rowTask = new TaskRecord(-1, title, desc, st, pr);
            String decoratedTitle = new PriorityDecorator(rowTask, pr).getTitle();

            // Apply priority-based coloring (unless selected)
            if (!isSelected) {
                lbl.setForeground(colorFor(pr));
            }

            // Apply strikethrough for completed tasks
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

        /** Maps priority levels to display colors */
        private static Color colorFor(Priority p) {
            return switch (p) {
                case HIGH   -> new Color(0xC0, 0x00, 0x00); // Dark red
                case MEDIUM -> new Color(0xB3, 0x6B, 0x00); // Orange
                case LOW    -> new Color(0x66, 0x66, 0x66); // Gray
                default     -> Color.BLACK;
            };
        }

        /** Safely parses priority string, defaulting to NONE on error */
        private static Priority safePriority(String n) {
            try { return Priority.valueOf(n); } catch (Exception e) { return Priority.NONE; }
        }

        /** Safely parses task state string, defaulting to TO_DO on error */
        private static TaskState safeState(String n) {
            try { return TaskState.valueOf(n); } catch (Exception e) { return TaskState.TO_DO; }
        }
    }

    /**
     * Custom renderer for task state column that shows user-friendly badges.
     * Converts internal state names to display-friendly text.
     */
    private static final class StateCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(
                JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            JLabel lbl = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

            TaskState st;
            try {
                st = TaskState.valueOf(Objects.toString(value, "TO_DO"));
            } catch (Exception e) {
                st = TaskState.TO_DO;
            }

            lbl.setText(st.badge()); // Display user-friendly text
            return lbl;
        }
    }
}