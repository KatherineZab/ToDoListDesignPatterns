package app;

import dao.TasksDAODerby;
import view.MainFrame;
import view.TasksPanel;
import viewmodel.TasksViewModel;

import javax.swing.SwingUtilities;

public class Main {
    public static void main(String[] args) throws Exception {
        var dao = TasksDAODerby.getInstance();   // ה-DAO שלך (Singleton)
        var vm  = new TasksViewModel(dao);       // ה-ViewModel הדק

        SwingUtilities.invokeLater(() -> {
            MainFrame frame = new MainFrame();   // כמו אצלך
            frame.setViewModel(vm);              // ← מוסיפים (נממש עוד רגע ב-MainFrame)
            frame.setVisible(true);
        });

        // טען נתונים מה-DB (יקרה פעם אחת בהפעלה)
        vm.load();
    }
}
