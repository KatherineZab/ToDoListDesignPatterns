package view;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;

public class FiltersPanel extends JPanel {
    private final JTextField query = new JTextField(22);
    private final JComboBox<String> state =
            new JComboBox<>(new String[]{"ALL", "TO_DO", "IN_PROGRESS", "COMPLETED"});
    private final JButton apply = new JButton("Apply");

    public FiltersPanel() {
        setLayout(new FlowLayout(FlowLayout.LEFT));
        add(new JLabel("Search:"));
        add(query);
        add(new JLabel("State:"));
        add(state);
        add(apply);
    }

    public String getQuery() { return query.getText().trim(); }
    public String getState() { return (String) state.getSelectedItem(); }

    /** מאפשר ל-MainFrame לחבר פעולה ל-Apply בלי תלות חיצונית */
    public void setApplyAction(ActionListener l) {
        for (ActionListener old : apply.getActionListeners()) apply.removeActionListener(old);
        apply.addActionListener(l);
    }
}
