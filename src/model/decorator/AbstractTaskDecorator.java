package model.decorator;

import model.ITask;
import model.TaskState;
import model.TaskVisitor;

/**
 * Base decorator for {@link ITask}.
 * Wraps another ITask and forwards the core read-only getters (id, title,
 * description, state). Subclasses override specific getters to add behavior
 * or presentation details without changing the wrapped task instance.
 * In this project it’s used for display tweaks (e.g., changing how the title is shown).
 * It does not affect persistence: DAOs work with the underlying task, not the decorator.
 */

public abstract class AbstractTaskDecorator implements ITask {
    protected final ITask inner;

    // Creates a decorator for the given task.
    protected AbstractTaskDecorator(ITask inner) { this.inner = inner; }

    /*
     * These getters just call the same getters on the wrapped task.
     * This keeps the decorator simple and without side effects.
     * Subclasses can override a getter (for example, getTitle) to change how it is shown.
     * All other values stay the same as in the original task.
     * Saving/DB logic uses the original task, not the decorator.
     */
    @Override public int getId() { return inner.getId(); }
    @Override public String getTitle() { return inner.getTitle(); }
    @Override public String getDescription() { return inner.getDescription(); }
    @Override public TaskState getState() { return inner.getState(); }
}
