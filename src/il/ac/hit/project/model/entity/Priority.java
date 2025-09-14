package il.ac.hit.project.model.entity;

/**
 * Priority levels for tasks.
 * Used by the UI (PriorityDecorator) and by sorting (ByPriority).
 * Values are stored as uppercase strings in the DB; when missing, NONE is used.
 * Semantic order (highest first): HIGH → MEDIUM → LOW → NONE.
 * use the strategy comparator.
 */
public enum Priority {
    NONE, LOW, MEDIUM, HIGH;
}
