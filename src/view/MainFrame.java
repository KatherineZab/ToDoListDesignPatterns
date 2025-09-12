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

        JButton add  = new JButton("Add");
        JButton edit = new JButton("Edit");
        JButton del  = new JButton("Delete");
        JButton undo = new JButton("Undo");
        JButton redo = new JButton("Redo");
        JButton prio = new JButton("Priority");

        JLabel sortLbl = new JLabel("Sort:");
        JComboBox<String> sortBox = new JComboBox<>(new String[]{
                "None",
                "Priority (High→Low)"
        });

        p.add(sortLbl);
        p.add(sortBox);
        p.add(add); p.add(edit); p.add(del);
        p.add(undo); p.add(redo); p.add(prio);

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
                        (String) s.getSelectedItem()
                );
                cmd.execute(command);
            }
        });

        edit.addActionListener(e -> {
            int id = tasksPanel.selectedIdOrMinus1();
            if (id < 0) return;

            JTextField t = new JTextField(tasksPanel.currentTitle());
            JTextField d = new JTextField(tasksPanel.currentDesc());
            JComboBox<String> s = new JComboBox<>(new String[]{"TO_DO","IN_PROGRESS","COMPLETED"});
            s.setSelectedItem(tasksPanel.currentState());
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
        undo.addActionListener(e -> cmd.undo());
        redo.addActionListener(e -> cmd.redo());

        sortBox.addActionListener(e -> {
            String choice = (String) sortBox.getSelectedItem();
            if ("Priority (High→Low)".equals(choice)) {
                tasksPanel.sortByPriorityHighToLow();
            } else {
                tasksPanel.clearSort();
            }
        });

        return p;
    }
}
