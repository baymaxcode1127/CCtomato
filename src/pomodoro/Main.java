package pomodoro;

import pomodoro.controller.AppController;

import javax.swing.*;

/**
 * Application entry point.
 */
public class Main {
    public static void main(String[] args) {
        // Set system look and feel
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            // Fall back to default
        }

        // Use SwingUtilities to ensure UI creation on EDT
        SwingUtilities.invokeLater(() -> {
            AppController controller = new AppController();
            controller.start();
        });
    }
}
