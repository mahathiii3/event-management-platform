package eventmanagementapp;

import javax.swing.SwingUtilities;

public class Main {
    public static void main(String[] args) {

        // Run concurrency demo
        EventManagementApp.runConcurrentDemo();

        // Launch UI
        SwingUtilities.invokeLater(MainUI::new);
    }
}