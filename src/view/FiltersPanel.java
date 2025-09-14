package view;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;

public class FiltersPanel extends JPanel {

    private final JTextField query = new JTextField(22);

    private final JComboBox<String> state = new JComboBox<>(
            new String[]{"ALL", "TO_DO", "IN_PROGRESS", "COMPLETED"}
    );

    // חדש: קומבו למיון
    private final JComboBox<String> sort = new JComboBox<>(
            new String[]{"— None —", "Priority (High→Low)", "State (ToDo→InProgress)"}
    );

    private final JButton apply = new JButton("Apply");
    private final JButton clear = new JButton("Clear");

    // מאזינים שה-MainFrame יכול להגדיר
    private ActionListener applyAction;
    private ActionListener clearAction;
    private ActionListener sortAction;

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

        // לחיצה על Apply
        apply.addActionListener(e -> {
            if (applyAction != null) applyAction.actionPerformed(e);
        });

        // שינוי בחירת מיון → להפעיל מיד (UX נוח)
        sort.addActionListener(e -> {
            if (sortAction != null) sortAction.actionPerformed(e);
        });

        // Clear: איפוס שדות וגם קריאה ללוגיקה החיצונית
        clear.addActionListener(e -> {
            query.setText("");
            state.setSelectedIndex(0); // ALL
            sort.setSelectedIndex(0);  // — None —
            if (clearAction != null) clearAction.actionPerformed(e);
        });
    }

    /* ===== Getters קיימים ===== */

    public String getQuery() { return query.getText().trim(); }

    /** מחזיר "ALL" / "TO_DO" / "IN_PROGRESS" / "COMPLETED" */
    public String getState() {
        Object x = state.getSelectedItem();
        return x == null ? "ALL" : x.toString();
    }

    /* ===== חדש: מפתח מיון ===== */
    /** מחזיר "NONE" / "PRIORITY" / "STATE" */
    public String getSortKey() {
        Object x = sort.getSelectedItem();
        String s = (x == null) ? "— None —" : x.toString();
        return switch (s) {
            case "Priority (High→Low)"     -> "PRIORITY";
            case "State (ToDo→InProgress)" -> "STATE";
            default                        -> "NONE";
        };
    }
    public void reset() {
        query.setText("");
        state.setSelectedIndex(0); // "ALL"
    }

    /* ===== Setters למאזינים (שומרים API קיים + מוסיפים חדשים) ===== */

    /** מאפשר ל-MainFrame לחבר פעולה ל-Apply */
    public void setApplyAction(ActionListener l) {
        this.applyAction = l;
    }

    /** חדש: מאזין לשינוי מיון (נקרא בכל שינוי קומבו) */
    public void setSortAction(ActionListener l) {
        this.sortAction = l;
    }

    /** חדש: מאזין ל-Clear (אחרי שאיפסנו את השדות) */
    public void setClearAction(ActionListener l) {
        this.clearAction = l;
    }
}
