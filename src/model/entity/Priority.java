package model.entity;

public enum Priority {
    NONE, LOW, MEDIUM, HIGH;

    public String badge() {
        return switch (this) {
            case HIGH   -> "[HIGH] ";
            case MEDIUM -> "[MED] ";
            case LOW    -> "[LOW] ";
            default     -> "";
        };
    }
}
