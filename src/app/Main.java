package app;

import dao.TasksDAODerby;
import view.MainFrame;
import viewModel.TasksViewModel;

import javax.swing.SwingUtilities;

public class Main {
    public static void main(String[] args) {
        var dao = TasksDAODerby.getInstance(); // Singleton DAO
        var vm  = new TasksViewModel(dao);     // ViewModel (calls load() inside)

        SwingUtilities.invokeLater(() -> {
            MainFrame frame = new MainFrame();
            frame.setViewModel(vm);            // wire VM into the UI
            frame.setVisible(true);
        });


    }
}
