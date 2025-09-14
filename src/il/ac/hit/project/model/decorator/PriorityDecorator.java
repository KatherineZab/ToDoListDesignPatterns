package il.ac.hit.project.model.decorator;

import il.ac.hit.project.model.ITask;
import il.ac.hit.project.model.entity.Priority;

/**
 * PriorityDecorator - Actually decorates ITask titles with priority indicators.
 * Now performs real decoration by enhancing the title based on priority level.
 * It wraps another ITask and only changes how the title looks.
 * The original task is not modified; DAOs still work with the wrapped task.
 * Used for display only (UI layer).
 */
public final class PriorityDecorator extends AbstractTaskDecorator {
    private final Priority priority;

    public PriorityDecorator(ITask inner, Priority priority) {
        super(inner);
        this.priority = (priority == null) ? Priority.NONE : priority;
    }

    //Decorates the title with priority indicators - this is the core Decorator behavior
    @Override
    public String getTitle() {
        String baseTitle = inner.getTitle();
        return switch (priority) {
            case HIGH -> "● " + baseTitle;     // Red circle
            case MEDIUM -> "● " + baseTitle;   // Orange circle
            case LOW -> "● " + baseTitle;      // Gray circle
            case NONE -> baseTitle;
        };
    }


    //Allows View layer to access the priority for additional styling
    public Priority getPriority() {
        return priority;
    }
}