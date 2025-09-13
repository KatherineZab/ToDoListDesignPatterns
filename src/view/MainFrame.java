package view;

import viewModel.TasksViewModel;
import model.TaskState;
import model.TaskRecord;

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

        // Priority (UI convenience)
        prio.addActionListener(e -> {
            if (!ensureVmOrWarn()) return;
            tasksPanel.setPriorityForSelected();
        });

        // Undo/Redo
        undo.addActionListener(e -> cmd.undo());
        redo.addActionListener(e -> cmd.redo());

        // Sort buttons now delegate to VM (via TasksPanel tiny patch below)
        sortPrio.addActionListener(e -> tasksPanel.sortByPriorityHighToLow());
        sortClear.addActionListener(e -> tasksPanel.clearSort());

        // Reports (Visitor inside VM)
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
