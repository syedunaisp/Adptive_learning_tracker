package tracker;

import tracker.ui.MainFrame;

import javax.swing.*;

/**
 * Application entry point.
 * Launches the MainFrame on the Swing Event Dispatch Thread.
 *
 * No business logic resides here -- this class exists solely
 * to bootstrap the GUI.
 */
public class Main {

    public static void main(String[] args) {
        // Set system look-and-feel for a native appearance
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException
                | UnsupportedLookAndFeelException e) {
            // Fall back to default look-and-feel silently
            System.err.println("Could not set system look and feel: " + e.getMessage());
        }

        // Apply global UI defaults from StyleConstants
        tracker.ui.StyleConstants.configureUIManager();

        // Launch GUI on the Event Dispatch Thread (EDT)
        SwingUtilities.invokeLater(() -> {
            MainFrame frame = new MainFrame();
            frame.setVisible(true);
        });
    }
}
