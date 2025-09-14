 package model;


 import java.util.EnumSet;
 import java.util.Set;

 /**
  * State pattern (enum-based) representing the lifecycle of a task.
  * ToDo -> InProgress -> Completed -> (REOPEN) -> ToDo
  * Each state declares the set of states it can move to (see {@link #nextStates()}).
  * UI can use {@link #badge()} for a friendly label.
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

     // Reopen is allowed: COMPLETED → TO_DO
     COMPLETED {
         @Override public Set<TaskState> nextStates() {
             return EnumSet.of(TO_DO);
         }
         // Not a final state because reopen is allowed
         @Override public boolean isTerminal() {
             return false;
         }
         @Override public String badge() { return "Completed"; }
     };

     //Allowed next states from this state
     public abstract Set<TaskState> nextStates();

     /** @return true if {@code next} is one of {@link #nextStates()};
      *  null is treated as not allowed.
      */
     public final boolean canTransitionTo(TaskState next) {
         return nextStates().contains(next);
     }

     public boolean isTerminal() {
         return nextStates().isEmpty();
     }

     // Short, user-friendly label for UI.
     public String badge() {
         return name();
     }
 }
