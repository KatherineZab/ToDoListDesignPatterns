package view;

import viewModel.TasksViewModel;
import model.TaskState;
import model.TaskRecord;
import model.entity.Priority;

import javax.swing.*;
import java.awt.BorderLayout;
import java.awt.FlowLayout;

/**
 * Main application window (Swing).
 * Shows:
 *  - FiltersPanel at the top (search, state, sort).
 *  - TasksPanel in the center (table/list of tasks).
 *  - CRUD + actions bar at the bottom (add, edit, delete, undo/redo, priority, reports).
 * Binds UI actions to Commands and the {@link TasksViewModel}.
 */
public class MainFrame extends JFrame {
    //Command manager for undo/redo of user actions.
    private final model.command.CommandManager cmd = new model.command.CommandManager();
    //shows tasks
    private final TasksPanel tasksPanel = new TasksPanel();
    //Top bar: search/state/sort
    private final FiltersPanel filtersPanel = new FiltersPanel();

    //Bound ViewModel
    private TasksViewModel vm;

    //Builds the main frame UI and wires the filter/sort apply action.
    public MainFrame() {
        super("Tasks Management System");
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(8, 8));

        add(filtersPanel, BorderLayout.NORTH);
        add(tasksPanel, BorderLayout.CENTER);
        add(buildCrudBar(), BorderLayout.SOUTH);

        // Apply button: run both filtering and sorting based on FiltersPanel values.
        filtersPanel.setApplyAction(e -> {
            if (!ensureVmOrWarn()) return;

            // (1) Filtering: delegate to TasksPanel (which will talk to the ViewModel)
            tasksPanel.applyFilter(filtersPanel.getQuery(), filtersPanel.getState());

            // (2) Sorting: choose strategy by key and apply via TasksPanel
            String sortKey = filtersPanel.getSortKey();
            switch (sortKey) {
                case "PRIORITY" -> tasksPanel.sortByPriorityHighToLow();
                case "STATE"    -> tasksPanel.sortByStateToDoFirst();
                default         -> tasksPanel.clearSort();
            }
        });

        setSize(900, 600);
        setLocationRelativeTo(null);
    }

    //Connects a ViewModel to this frame and forwards it to child views.
    public void setViewModel(TasksViewModel vm) {
        this.vm = vm;
        this.tasksPanel.setViewModel(vm);
    }

    //Ensures a ViewModel is set; shows an error dialog if not.
    private boolean ensureVmOrWarn() {
        if (vm != null) return true;
        JOptionPane.showMessageDialog(this, "ViewModel not set", "Error", JOptionPane.ERROR_MESSAGE);
        return false;
    }

    /**
     * Bottom action bar: Add/Edit/Delete, Undo/Redo, Priority, Report, Delete All.
     * Wires each button to the proper Command or ViewModel call.
     */
    private JComponent buildCrudBar() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT));

        JButton add       = new JButton("Add");
        JButton edit      = new JButton("Edit");
        JButton del       = new JButton("Delete");
        JButton undo      = new JButton("Undo");
        JButton redo      = new JButton("Redo");
        JButton prio      = new JButton("Priority");
        JButton reportBtn = new JButton("Report");
        JButton deleteAll  = new JButton("Delete All");

        p.add(reportBtn);
        p.add(add);
        p.add(edit);
        p.add(del);
        p.add(undo);
        p.add(redo);
        p.add(prio);
        p.add(deleteAll);

        // ADD — View -> Command -> ViewModel
        add.addActionListener(e -> {
            if (!ensureVmOrWarn()) return;

            JTextField titleField = new JTextField(20);
            JTextField descField  = new JTextField(20);
            JComboBox<String> stateCombo = new JComboBox<>(new String[]{"TO_DO","IN_PROGRESS","COMPLETED"});

            Object[] form = {"Title:", titleField, "Description:", descField, "State:", stateCombo};
            int ok = JOptionPane.showConfirmDialog(this, form, "Add Task", JOptionPane.OK_CANCEL_OPTION);
            if (ok == JOptionPane.OK_OPTION) {
                var command = new model.command.AddTaskCommand(
                        vm,
                        titleField.getText().trim(),
                        descField.getText().trim(),
                        (String) stateCombo.getSelectedItem()
                );
                cmd.execute(command);
            }
        });

        //  Clear ALL — destructive action with Undo/Redo via Command
        deleteAll.addActionListener(e -> {
            if (!ensureVmOrWarn()) return;
            int ok = JOptionPane.showConfirmDialog(
                    this,
                    "Delete ALL tasks? (You can undo afterwards)",
                    "Confirm Clear All",
                    JOptionPane.YES_NO_OPTION
            );
            if (ok == JOptionPane.YES_OPTION) {
                cmd.execute(new model.command.DeleteAllTasksCommand(vm));
            }
        });

        // EDIT — View -> Command -> ViewModel
        edit.addActionListener(e -> {
            if (!ensureVmOrWarn()) return;

            int selectedId = tasksPanel.selectedIdOrMinus1();
            if (selectedId < 0) {
                JOptionPane.showMessageDialog(this, "Please select a task to edit", "No Selection", JOptionPane.WARNING_MESSAGE);
                return;
            }

            TaskRecord selectedTask;
            try {
                var task = vm.getById(selectedId);
                selectedTask = (task instanceof TaskRecord tr) ? tr : null;
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Could not load task: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            if (selectedTask == null) {
                JOptionPane.showMessageDialog(this, "Task not found", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            JTextField titleField = new JTextField(selectedTask.title(), 20);
            JTextField descField  = new JTextField(selectedTask.description(), 20);

            //Allowed states: current + next allowed
            java.util.LinkedHashSet<String> allowedStates = new java.util.LinkedHashSet<>();
            allowedStates.add(selectedTask.state().name());
            try {
                for (var state : vm.allowedNextStatesOf(selectedTask.id())) {
                    allowedStates.add(state.name());
                }
            } catch (Exception ex) {
                for (TaskState state : TaskState.values()) {
                    allowedStates.add(state.name());
                }
            }

            JComboBox<String> stateCombo = new JComboBox<>(allowedStates.toArray(new String[0]));
            stateCombo.setSelectedItem(selectedTask.state().name());

            Object[] form = {"Title:", titleField, "Description:", descField, "State:", stateCombo};
            int ok = JOptionPane.showConfirmDialog(this, form, "Edit Task", JOptionPane.OK_CANCEL_OPTION);
            if (ok == JOptionPane.OK_OPTION) {
                var command = new model.command.UpdateTaskCommand(
                        vm,
                        selectedTask.id(),
                        titleField.getText().trim(),
                        descField.getText().trim(),
                        (String) stateCombo.getSelectedItem()
                );
                cmd.execute(command);
            }
        });

        // DELETE — View -> Command -> ViewModel
        del.addActionListener(e -> {
            if (!ensureVmOrWarn()) return;

            int selectedId = tasksPanel.selectedIdOrMinus1();
            if (selectedId < 0) {
                JOptionPane.showMessageDialog(this, "Please select a task to delete", "No Selection", JOptionPane.WARNING_MESSAGE);
                return;
            }

            TaskRecord selectedTask;
            try {
                var task = vm.getById(selectedId);
                selectedTask = (task instanceof TaskRecord tr) ? tr : null;
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Could not load task: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            if (selectedTask == null) {
                JOptionPane.showMessageDialog(this, "Task not found", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            int confirm = JOptionPane.showConfirmDialog(
                    this,
                    "Are you sure you want to delete: " + selectedTask.title() + "?",
                    "Confirm Delete",
                    JOptionPane.YES_NO_OPTION
            );
            if (confirm == JOptionPane.YES_OPTION) {
                var command = new model.command.DeleteTaskCommand(vm, selectedId);
                cmd.execute(command);
            }
        });

        // PRIORITY — change priority via Command (supports undo/redo)
        prio.addActionListener(e -> {
            if (!ensureVmOrWarn()) return;

            int id = tasksPanel.selectedIdOrMinus1();
            if (id < 0) {
                JOptionPane.showMessageDialog(this, "Please select a task first", "No Selection", JOptionPane.WARNING_MESSAGE);
                return;
            }

            String[] opts = {"NONE","LOW","MEDIUM","HIGH"};
            String chosen = (String) JOptionPane.showInputDialog(
                    this, "Select priority:", "Priority",
                    JOptionPane.PLAIN_MESSAGE, null, opts, "NONE"
            );
            if (chosen == null) return; // המשתמש ביטל

            try {
                cmd.execute(new model.command.ChangePriorityCommand(vm, id, Priority.valueOf(chosen)));
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Failed to update priority: " + ex.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        });


        // Undo/Redo
        undo.addActionListener(e -> cmd.undo());
        redo.addActionListener(e -> cmd.redo());

        // Sort buttons
        filtersPanel.setClearAction(e -> {
            if (!ensureVmOrWarn()) return;
            vm.clearFilter();          // clear filter state in VM
            tasksPanel.clearSort();    // clear sort (calls vm.setSortStrategy(null))
            filtersPanel.reset();      // reset filter panel controls
        });

        // REPORTS — generate a text report, optionally export CSV
        reportBtn.addActionListener(e -> {
            if (!ensureVmOrWarn()) return;

            try {
                String reportText = vm.generateCombinedReport();

                JTextArea area = new JTextArea(reportText, 18, 50);
                area.setEditable(false);
                JScrollPane scroll = new JScrollPane(area);

                Object[] options = {"Export CSV...", "Close"};
                int choice = JOptionPane.showOptionDialog(
                        this, scroll, "Tasks Report",
                        JOptionPane.YES_NO_OPTION, JOptionPane.INFORMATION_MESSAGE,
                        null, options, options[1]
                );

                if (choice == 0) {
                    String csvContent = vm.exportCSV();

                    JFileChooser fileChooser = new JFileChooser();
                    fileChooser.setSelectedFile(new java.io.File("tasks_report.csv"));

                    if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
                        try {
                            java.nio.file.Files.writeString(
                                    fileChooser.getSelectedFile().toPath(),
                                    csvContent,
                                    java.nio.charset.StandardCharsets.UTF_8
                            );
                            JOptionPane.showMessageDialog(this,
                                    "CSV exported successfully to:\n" + fileChooser.getSelectedFile().getAbsolutePath(),
                                    "Export Complete", JOptionPane.INFORMATION_MESSAGE);
                        } catch (Exception ex) {
                            JOptionPane.showMessageDialog(this,
                                    "Failed to save CSV: " + ex.getMessage(),
                                    "Export Error", JOptionPane.ERROR_MESSAGE);
                        }
                    }
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this,
                        "Report generation failed: " + ex.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        return p;
    }
}
