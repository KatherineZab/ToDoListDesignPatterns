 package model;

public enum TaskState {
    TO_DO("To Do"),
    IN_PROGRESS("In Progress"),
    COMPLETED("Completed");

    private final String display;
    TaskState(String display) { this.display = display; }
    public String displayName() { return display; }

    public static TaskState fromDisplay(String s) {
        return switch (s) {
            case "To Do"       -> TO_DO;
            case "In Progress" -> IN_PROGRESS;
            case "Completed"   -> COMPLETED;
            default -> throw new IllegalArgumentException("Unknown state: " + s);
        };
    }
}
