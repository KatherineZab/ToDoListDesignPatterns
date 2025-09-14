package app;

import dao.TasksDAODerby;
import view.MainFrame;
import viewModel.TasksViewModel;

import javax.swing.SwingUtilities;

/**
 * Program entry point. Creates the DAO and ViewModel, then constructs and shows the main frame
 * on the Event Dispatch Thread (EDT).
 */

public class Main {

    // --- Bootstrap model & view-model dependencies (no UI work here) ---
    public static void main(String[] args) {
        // Singleton DAO
        var dao = TasksDAODerby.getInstance();
        // ViewModel
        var vm  = new TasksViewModel(dao);

        SwingUtilities.invokeLater(() -> {
            // Build & wire the view
            MainFrame frame = new MainFrame();
            frame.setViewModel(vm);

            // Show UI
            frame.setVisible(true);

        });


    }
}
