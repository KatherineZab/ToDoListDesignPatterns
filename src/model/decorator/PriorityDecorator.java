package model.decorator;

import model.ITask;
import model.entity.Priority;

/** דקורטור שמוסיף תגית Priority בתחילת הכותרת (ללא שינוי DB) */
public final class PriorityDecorator extends AbstractTaskDecorator {
    private final Priority priority;

    public PriorityDecorator(ITask inner, Priority priority) {
        super(inner);
        this.priority = (priority == null) ? Priority.NONE : priority;
    }

    public Priority getPriority() { return priority; }

    @Override
    public String getTitle() {
        return priority.badge() + super.getTitle();
    }
}
