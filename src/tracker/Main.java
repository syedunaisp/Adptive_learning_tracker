package tracker;

import tracker.data.DBConnectionManager;
import tracker.data.DatabaseSchema;
import tracker.ui.LoginFrame;

import javax.swing.*;

/**
 * Application entry point.
 *
 * UPGRADED: Now initializes the SQLite database and schema before
 * launching the LoginFrame. The DB is created automatically on first run
 * with default roles, admin user, and configuration values.
 *
 * Startup sequence:
 *   1. Load SQLite JDBC driver
 *   2. Initialize database schema (CREATE TABLE IF NOT EXISTS)
 *   3. Seed default data (roles, admin user, config)
 *   4. Apply UI defaults
 *   5. Launch LoginFrame
 */
public class Main {

    public static void main(String[] args) {
        // --- Phase 1: Initialize Database ---
        System.out.println("ALIP v3.0 — Initializing database...");
        try {
            DBConnectionManager.initialize();
            DatabaseSchema.initializeSchema();
            System.out.println("Database ready.");
        } catch (Exception e) {
            System.err.println("FATAL: Database initialization failed: " + e.getMessage());
            e.printStackTrace();
            JOptionPane.showMessageDialog(null,
                "Database initialization failed:\n" + e.getMessage() +
                "\n\nEnsure sqlite-jdbc JAR is on the classpath.",
                "ALIP — Startup Error", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }

        // --- Phase 2: Set Look-and-Feel ---
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException
                | UnsupportedLookAndFeelException e) {
            System.err.println("Could not set system look and feel: " + e.getMessage());
        }

        // Apply global UI defaults from StyleConstants
        tracker.ui.StyleConstants.configureUIManager();

        // --- Phase 3: Launch Login Screen ---
        SwingUtilities.invokeLater(() -> {
            LoginFrame login = new LoginFrame();
            login.setVisible(true);
        });
    }
}
