package model.decorator;

import model.ITask;
import model.entity.Priority;

/**
 * PriorityDecorator — Decorator ל-ITask.
 * אינו מחזיר HTML ואינו מוסיף טקסט (ללא prefix).
 * ההעשרה הוויזואלית (צבע/קו-חוצה) מתבצעת ברמת ה-View (renderer של הטבלה),
 * בעוד שהדקורטור משמש כנקודת הרחבה מבלי לשנות את מחלקות המודל/DAO.
 */
public final class PriorityDecorator extends AbstractTaskDecorator {
    private final Priority priority;

    public PriorityDecorator(ITask inner, Priority priority) {
        super(inner);
        this.priority = (priority == null) ? Priority.NONE : priority;
    }

    /** מחזיר את הכותרת המקורית; ה-View מעצב (צבע/קו-חוצה) לפי priority/state. */
    @Override
    public String getTitle() {
        return inner.getTitle();
    }

    /** אופציונלי: מאפשר לשכבת התצוגה לשאול מה ה-priority שהוזרק לדקורטור. */
    public Priority getPriority() {
        return priority;
    }
}
