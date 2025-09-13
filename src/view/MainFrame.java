package view;

import viewModel.TasksViewModel;
import model.TaskState;

import javax.swing.*;
import java.awt.BorderLayout;
import java.awt.FlowLayout;

public class MainFrame extends JFrame {
    private final model.command.CommandManager cmd = new model.command.CommandManager();
    private final TasksPanel tasksPanel = new TasksPanel();
    private final FiltersPanel filtersPanel = new FiltersPanel();

    private TasksViewModel vm;

    public MainFrame() {
        super("Tasks Management System");
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(8, 8));

        add(filtersPanel, BorderLayout.NORTH);
        add(tasksPanel, BorderLayout.CENTER);
        add(buildCrudBar(), BorderLayout.SOUTH);

        // Filter action - this is acceptable as it's a UI coordination
        filtersPanel.setApplyAction(e ->
                tasksPanel.applyFilter(filtersPanel.getQuery(), filtersPanel.getState())
        );

        setSize(900, 600);
        setLocationRelativeTo(null);
    }

    public void setViewModel(TasksViewModel vm) {
        this.vm = vm;
        this.tasksPanel.setViewModel(vm);
    }

    private boolean ensureVmOrWarn() {
        if (vm != null) return true;
        JOptionPane.showMessageDialog(this, "ViewModel not set", "Error", JOptionPane.ERROR_MESSAGE);
        return false;
    }

    private JComponent buildCrudBar() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT));

        JButton add       = new JButton("Add");
        JButton edit      = new JButton("Edit");
        JButton del       = new JButton("Delete");
        JButton undo      = new JButton("Undo");
        JButton redo      = new JButton("Redo");
        JButton prio      = new JButton("Priority");
        JButton sortPrio  = new JButton("Sort: Priority");
        JButton sortClear = new JButton("Sort: Clear");
        JButton reportBtn = new JButton("Report");

        p.add(reportBtn);
        p.add(sortPrio);
        p.add(sortClear);
        p.add(add);
        p.add(edit);
        p.add(del);
        p.add(undo);
        p.add(redo);
        p.add(prio);

        // ADD - Proper MVVM: View -> Command -> ViewModel
        add.addActionListener(e -> {
            if (!ensureVmOrWarn()) return;

            JTextField titleField = new JTextField(20);
            JTextField descField = new JTextField(20);
            JComboBox<String> stateCombo = new JComboBox<>(new String[]{"TO_DO", "IN_PROGRESS", "COMPLETED"});

            Object[] form = {
                    "Title:", titleField,
                    "Description:", descField,
                    "State:", stateCombo
            };

            int result = JOptionPane.showConfirmDialog(this, form, "Add Task", JOptionPane.OK_CANCEL_OPTION);
            if (result == JOptionPane.OK_OPTION) {
                // CORRECT MVVM: Direct Command -> ViewModel communication
                var command = new model.command.AddTaskCommand(
                        vm,  // Goes directly to ViewModel
                        titleField.getText().trim(),
                        descField.getText().trim(),
                        (String) stateCombo.getSelectedItem()
                );
                cmd.execute(command);
            }
        });

        // EDIT - Proper MVVM: Get selection through ViewModel
        edit.addActionListener(e -> {
            if (!ensureVmOrWarn()) return;

            // CORRECT MVVM: Ask ViewModel for selected task data
            var selectedTask = getSelectedTaskFromViewModel();
            if (selectedTask == null) {
                JOptionPane.showMessageDialog(this, "Please select a task to edit", "No Selection", JOptionPane.WARNING_MESSAGE);
                return;
            }

            JTextField titleField = new JTextField(selectedTask.title(), 20);
            JTextField descField = new JTextField(selectedTask.description(), 20);

            // Get allowed states from ViewModel (proper State pattern usage)
            java.util.LinkedHashSet<String> allowedStates = new java.util.LinkedHashSet<>();
            allowedStates.add(selectedTask.state().name()); // Current state always allowed

            try {
                for (var state : vm.allowedNextStatesOf(selectedTask.id())) {
                    allowedStates.add(state.name());
                }
            } catch (Exception ex) {
                // Fallback to all states if error
                for (TaskState state : TaskState.values()) {
                    allowedStates.add(state.name());
                }
            }

            JComboBox<String> stateCombo = new JComboBox<>(allowedStates.toArray(new String[0]));
            stateCombo.setSelectedItem(selectedTask.state().name());

            Object[] form = {
                    "Title:", titleField,
                    "Description:", descField,
                    "State:", stateCombo
            };

            int result = JOptionPane.showConfirmDialog(this, form, "Edit Task", JOptionPane.OK_CANCEL_OPTION);
            if (result == JOptionPane.OK_OPTION) {
                // CORRECT MVVM: Command -> ViewModel
                var command = new model.command.UpdateTaskCommand(
                        vm,  // Goes directly to ViewModel
                        selectedTask.id(),
                        titleField.getText().trim(),
                        descField.getText().trim(),
                        (String) stateCombo.getSelectedItem()
                );
                cmd.execute(command);
            }
        });

        // DELETE - Proper MVVM: Get selection through ViewModel
        del.addActionListener(e -> {
            if (!ensureVmOrWarn()) return;

            var selectedTask = getSelectedTaskFromViewModel();
            if (selectedTask == null) {
                JOptionPane.showMessageDialog(this, "Please select a task to delete", "No Selection", JOptionPane.WARNING_MESSAGE);
                return;
            }

            int confirm = JOptionPane.showConfirmDialog(
                    this,
                    "Are you sure you want to delete: " + selectedTask.title() + "?",
                    "Confirm Delete",
                    JOptionPane.YES_NO_OPTION
            );

            if (confirm == JOptionPane.YES_OPTION) {
                // CORRECT MVVM: Command -> ViewModel
                var command = new model.command.DeleteTaskCommand(vm, selectedTask.id());
                cmd.execute(command);
            }
        });

        // Priority editing - this could also be moved to ViewModel for better MVVM
        prio.addActionListener(e -> {
            if (!ensureVmOrWarn()) return;

            var selectedTask = getSelectedTaskFromViewModel();
            if (selectedTask == null) {
                JOptionPane.showMessageDialog(this, "Please select a task", "No Selection", JOptionPane.WARNING_MESSAGE);
                return;
            }

            String[] priorities = {"NONE", "LOW", "MEDIUM", "HIGH"};
            String currentPriority = (selectedTask instanceof model.TaskRecord tr) ?
                    tr.priority().name() : "NONE";

            String chosen = (String) JOptionPane.showInputDialog(
                    this, "Select priority:", "Set Priority",
                    JOptionPane.PLAIN_MESSAGE, null, priorities, currentPriority
            );

            if (chosen != null) {
                try {
                    vm.setPriority(selectedTask.id(), model.entity.Priority.valueOf(chosen));
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(this, "Failed to update priority: " + ex.getMessage(),
                            "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        // Command pattern operations
        undo.addActionListener(e -> cmd.undo());
        redo.addActionListener(e -> cmd.redo());

        // Strategy pattern for sorting
        sortPrio.addActionListener(e -> tasksPanel.sortByPriorityHighToLow());
        sortClear.addActionListener(e -> tasksPanel.clearSort());

        // Visitor pattern for reports
        reportBtn.addActionListener(e -> {
            if (!ensureVmOrWarn()) return;

            var summaryVisitor = new model.report.CombinedReportVisitor();
            for (var task : vm.items()) {
                if (task instanceof model.TaskRecord tr) {
                    summaryVisitor.visit(tr);
                }
            }

            String reportText = summaryVisitor.asText();

            JTextArea textArea = new JTextArea(reportText, 18, 50);
            textArea.setEditable(false);
            JScrollPane scrollPane = new JScrollPane(textArea);

            Object[] options = {"Export CSV...", "Close"};
            int choice = JOptionPane.showOptionDialog(
                    this, scrollPane, "Tasks Report",
                    JOptionPane.YES_NO_OPTION, JOptionPane.INFORMATION_MESSAGE,
                    null, options, options[1]
            );

            if (choice == 0) {
                exportCsvReport();
            }
        });

        return p;
    }

    /**
     * CORRECT MVVM: Get selected task through ViewModel, not direct View access
     * This method abstracts the View selection logic and returns business data
     */
    private model.TaskRecord getSelectedTaskFromViewModel() {
        // This is the proper way: let the View handle its own selection,
        // but expose only business data to the controller layer
        int selectedId = tasksPanel.selectedIdOrMinus1();
        if (selectedId < 0) return null;

        try {
            var task = vm.getById(selectedId);
            return (task instanceof model.TaskRecord tr) ? tr : null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * CSV Export using Visitor pattern
     */
    private void exportCsvReport() {
        var csvVisitor = new model.report.CSVExportVisitor();
        for (var task : vm.items()) {
            if (task instanceof model.TaskRecord tr) {
                csvVisitor.visit(tr);
            }
        }
        String csvContent = csvVisitor.csv();

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
                        "Failed to export CSV: " + ex.getMessage(),
                        "Export Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}