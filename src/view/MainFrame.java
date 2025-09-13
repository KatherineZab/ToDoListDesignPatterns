package view;

import viewmodel.TasksViewModel;

import javax.swing.*;
import java.awt.*;

public class MainFrame extends JFrame {
    private final model.command.CommandManager cmd = new model.command.CommandManager();
    private final TasksPanel tasksPanel = new TasksPanel();
    private final FiltersPanel filtersPanel = new FiltersPanel();

    private TasksViewModel vm;

    public MainFrame() {
        super("Tasks – UI Skeleton");
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

        // סדר הכפתורים בפאנל
        p.add(reportBtn);
        p.add(sortPrio);
        p.add(sortClear);
        p.add(add);
        p.add(edit);
        p.add(del);
        p.add(undo);
        p.add(redo);
        p.add(prio);

        add.addActionListener(e -> {
            JTextField t = new JTextField();
            JTextField d = new JTextField();
            JComboBox<String> s = new JComboBox<>(new String[]{"TO_DO","IN_PROGRESS","COMPLETED"});
            Object[] form = {"Title:", t, "Description:", d, "State:", s};
            int ok = JOptionPane.showConfirmDialog(this, form, "Add Task", JOptionPane.OK_CANCEL_OPTION);
            if (ok == JOptionPane.OK_OPTION) {
                var command = new model.command.AddTaskCommand(
                        tasksPanel,
                        t.getText().trim(),
                        d.getText().trim(),
                        (String) s.getSelectedItem() // enum name: "TO_DO"...
                );
                cmd.execute(command);
            }
        });

        edit.addActionListener(e -> {
            int id = tasksPanel.selectedIdOrMinus1();
            if (id < 0) return;

            JTextField t = new JTextField(tasksPanel.currentTitle());
            JTextField d = new JTextField(tasksPanel.currentDesc());

            // מצב נוכחי
            String currentStateName = tasksPanel.currentState();

            // בניית רשימת מצבים מותרת מה-VM (עם גיבוי אם vm==null/שגיאה)
            java.util.LinkedHashSet<String> options = new java.util.LinkedHashSet<>();
            options.add(currentStateName); // תמיד נאפשר "להשאיר כפי שהוא"
            try {
                for (var st : vm.allowedNextStatesOf(id)) options.add(st.name());
            } catch (Exception ex) {
                // גיבוי – לא נכשל אם vm לא מוכן
                options.add("IN_PROGRESS");
                options.add("COMPLETED");
            }

            JComboBox<String> s = new JComboBox<>(options.toArray(new String[0]));
            s.setSelectedItem(currentStateName);

            Object[] form = {"Title:", t, "Description:", d, "State:", s};
            int ok = JOptionPane.showConfirmDialog(this, form, "Edit Task", JOptionPane.OK_CANCEL_OPTION);
            if (ok == JOptionPane.OK_OPTION) {
                var command = new model.command.UpdateTaskCommand(
                        tasksPanel, id,
                        t.getText().trim(),
                        d.getText().trim(),
                        (String) s.getSelectedItem()
                );
                cmd.execute(command);
            }
        });


        del.addActionListener(e -> {
            int id = tasksPanel.selectedIdOrMinus1();
            if (id >= 0) {
                var command = new model.command.DeleteTaskCommand(tasksPanel, id);
                cmd.execute(command);
            }
        });

        prio.addActionListener(e -> tasksPanel.setPriorityForSelected());

        // Undo/Redo
        undo.addActionListener(e -> cmd.undo());
        redo.addActionListener(e -> cmd.redo());

        // Strategy: מיון לפי Priority והסרת מיון
        sortPrio.addActionListener(e -> tasksPanel.sortByPriorityHighToLow());
        sortClear.addActionListener(e -> tasksPanel.clearSort());

        // === REPORT: Visitor + Record + Pattern Matching ===
        reportBtn.addActionListener(e -> {
            if (vm == null) {
                JOptionPane.showMessageDialog(this, "ViewModel not set", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // מבקר סיכומים (מימוש ב-package report)
            var summaryVisitor = new model.report.CombinedReportVisitor();

            // Record + Pattern Matching (לפי דרישת המרצה)
            for (var t : vm.items()) {
                if (t instanceof model.TaskRecord tr) {
                    summaryVisitor.visit(tr);
                }
            }

            String text = summaryVisitor.asText();

            // תצוגה יפה + אופציית ייצוא ל-CSV
            JTextArea area = new JTextArea(text, 18, 50);
            area.setEditable(false);
            JScrollPane scroll = new JScrollPane(area);

            Object[] options = {"Export CSV...", "Close"};
            int choice = JOptionPane.showOptionDialog(
                    this, scroll, "Tasks Report",
                    JOptionPane.YES_NO_OPTION, JOptionPane.INFORMATION_MESSAGE,
                    null, options, options[1]
            );

            if (choice == 0) {
                var csvVisitor = new model.report.CSVExportVisitor();
                for (var t : vm.items()) {
                    if (t instanceof model.TaskRecord tr) {
                        csvVisitor.visit(tr);
                    }
                }
                String csv = csvVisitor.csv();

                javax.swing.JFileChooser fc = new javax.swing.JFileChooser();
                fc.setSelectedFile(new java.io.File("tasks_report.csv"));
                if (fc.showSaveDialog(this) == javax.swing.JFileChooser.APPROVE_OPTION) {
                    try {
                        java.nio.file.Files.writeString(
                                fc.getSelectedFile().toPath(),
                                csv,
                                java.nio.charset.StandardCharsets.UTF_8
                        );
                        JOptionPane.showMessageDialog(this,
                                "CSV saved to:\n" + fc.getSelectedFile().getAbsolutePath(),
                                "Export CSV", JOptionPane.INFORMATION_MESSAGE);
                    } catch (Exception ex) {
                        JOptionPane.showMessageDialog(this,
                                "Failed to save CSV: " + ex.getMessage(),
                                "Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
            }
        });
        // === END REPORT ===

        return p;
    }
}
