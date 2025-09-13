 package model;
// keep your existing `package ...;` line here

 import java.util.EnumSet;
 import java.util.Set;

 /**
  * State pattern (enum-based) representing the lifecycle of a task.
  * ToDo -> InProgress -> Completed -> (REOPEN) -> ToDo
  * כל מצב מגדיר את המעברים המותרים ממנו.
  */
 public enum TaskState {
     TO_DO {
         @Override public Set<TaskState> nextStates() {
             return EnumSet.of(IN_PROGRESS);
         }
         @Override public String badge() { return "To Do"; }
     },

     IN_PROGRESS {
         @Override public Set<TaskState> nextStates() {
             return EnumSet.of(COMPLETED);
         }
         @Override public String badge() { return "In Progress"; }
     },

     COMPLETED {
         @Override public Set<TaskState> nextStates() {
             // REOPEN מותר — מעבר חזרה ל-TO_DO
             return EnumSet.of(TO_DO);
         }
         @Override public boolean isTerminal() {
             // לא מצב סופי לחלוטין כי מרשים Reopen
             return false;
         }
         @Override public String badge() { return "Completed"; }
     };

     /** המצבים שאליהם מותר לעבור ממצב זה */
     public abstract Set<TaskState> nextStates();

     /** האם מותר מעבר למצב המבוקש */
     public final boolean canTransitionTo(TaskState next) {
         return nextStates().contains(next);
     }

     /** האם זה מצב סופי (ברירת מחדל: אין מעברים קדימה) */
     public boolean isTerminal() {
         return nextStates().isEmpty();
     }

     /** טקסט ידידותי ל-UI (תג/Badge) */
     public String badge() {
         return name();
     }
 }
