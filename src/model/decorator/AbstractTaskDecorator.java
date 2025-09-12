package model.decorator;

import model.ITask;
import model.TaskState;
import model.TaskVisitor;

/** דקורטור בסיסי שמאציל ל-ITask עטוף */
public abstract class AbstractTaskDecorator implements ITask {
    protected final ITask inner;

    protected AbstractTaskDecorator(ITask inner) { this.inner = inner; }

    @Override public int getId() { return inner.getId(); }
    @Override public String getTitle() { return inner.getTitle(); }
    @Override public String getDescription() { return inner.getDescription(); }
    @Override public TaskState getState() { return inner.getState(); }
}
