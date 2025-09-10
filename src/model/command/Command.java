package model.command;

public interface Command {
    void execute();  // מה שקורה קדימה
    void undo();     // מה שקורה אחורה (Undo)
}
