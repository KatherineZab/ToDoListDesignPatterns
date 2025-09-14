package il.ac.hit.project.view;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;

/**
 * UI panel that provides filtering and sorting controls for tasks.
 * Part of the View layer in MVVM - handles only UI presentation and
 * user input collection. Delegates all business logic to parent components
 * which coordinate with the ViewModel layer.
 */
public class FiltersPanel extends JPanel {

    private final JTextField query = new JTextField(22);

    private final JComboBox<String> state = new JComboBox<>(
            new String[]{"ALL", "TO_DO", "IN_PROGRESS", "COMPLETED"}
    );

    // Sort dropdown for ordering tasks
    private final JComboBox<String> sort = new JComboBox<>(
            new String[]{"— None —", "Priority (High→Low)", "State (ToDo→InProgress)"}
    );

    private final JButton apply = new JButton("Apply");
    private final JButton clear = new JButton("Clear");

    // Action listeners that parent components can set
    private ActionListener applyAction;
    private ActionListener clearAction;
    private ActionListener sortAction;

    /**
     * Creates the filters panel with all controls and sets up internal event handling.
     * The Apply button triggers the applyAction, sort changes trigger sortAction immediately,
     * and Clear resets all fields then triggers clearAction.
     */
    public FiltersPanel() {
        setLayout(new FlowLayout(FlowLayout.LEFT, 8, 6));

        add(new JLabel("Search:"));
        add(query);

        add(new JLabel("State:"));
        add(state);

        add(new JLabel("Sort:"));
        add(sort);

        add(apply);
        add(clear);

        // Apply button - delegates to external action
        apply.addActionListener(e -> {
            if (applyAction != null) applyAction.actionPerformed(e);
        });

        // Sort selection - triggers immediately for better UX
        sort.addActionListener(e -> {
            if (sortAction != null) sortAction.actionPerformed(e);
        });

        // Clear - resets all fields then calls external action
        clear.addActionListener(e -> {
            query.setText("");
            state.setSelectedIndex(0); // ALL
            sort.setSelectedIndex(0);  // — None —
            if (clearAction != null) clearAction.actionPerformed(e);
        });
    }

    /**
     * Gets the current search query text.
     * @return trimmed text from the search field
     */
    public String getQuery() {
        return query.getText().trim();
    }

    /**
     * Gets the selected state filter.
     * @return "ALL", "TO_DO", "IN_PROGRESS", or "COMPLETED"
     */
    public String getState() {
        Object x = state.getSelectedItem();
        return x == null ? "ALL" : x.toString();
    }

    /**
     * Gets the selected sort option as a key.
     * Converts user-friendly display text to simple keys for logic processing.
     * @return "NONE", "PRIORITY", or "STATE"
     */
    public String getSortKey() {
        Object x = sort.getSelectedItem();
        String s = (x == null) ? "— None —" : x.toString();
        return switch (s) {
            case "Priority (High→Low)"     -> "PRIORITY";
            case "State (ToDo→InProgress)" -> "STATE";
            default                        -> "NONE";
        };
    }

    /**
     * Resets the search query and state selector to default values.
     * Does not trigger any actions - just clears the UI fields.
     */
    public void reset() {
        query.setText("");
        state.setSelectedIndex(0); // "ALL"
    }

    /**
     * Sets the action to execute when Apply button is clicked.
     * @param l the action listener to call when user applies filters
     */
    public void setApplyAction(ActionListener l) {
        this.applyAction = l;
    }

//    /**
//     * Sets the action to execute when sort selection changes.
//     * This is triggered immediately when user changes sort dropdown.
//     * @param l the action listener to call when sort selection changes
//     */
//    public void setSortAction(ActionListener l) {
//        this.sortAction = l;
//    }

    /**
     * Sets the action to execute after Clear button resets all fields.
     * Called after the panel has already cleared its own UI elements.
     * @param l the action listener to call after clearing fields
     */
    public void setClearAction(ActionListener l) {
        this.clearAction = l;
    }
}