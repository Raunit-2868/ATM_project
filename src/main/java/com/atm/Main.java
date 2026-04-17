package com.atm;

import com.atm.db.DatabaseManager;
import com.atm.ui.LoginFrame;
import com.atm.util.AppLogger;

import javax.swing.*;

/**
 * Main - Application entry point.
 *
 * 1. Initializes the SQLite database (creates schema + seeds demo accounts).
 * 2. Launches the Swing GUI on the Event Dispatch Thread.
 */
public class Main {

    public static void main(String[] args) {
        AppLogger.info("=== SecureATM Application Starting ===");

        // ── 1. Initialize database ──────────────────────────────────────────
        try {
            DatabaseManager.getInstance().initialize();
        } catch (RuntimeException e) {
            AppLogger.error("Fatal: database could not be initialized.", e);
            JOptionPane.showMessageDialog(null,
                "Database initialization failed.\nPlease check atm.log for details.",
                "Fatal Error", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }

        // ── 2. Launch GUI on EDT ────────────────────────────────────────────
        SwingUtilities.invokeLater(() -> {
            // Disable metal-style window decorations for cleaner look
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {
                // Fall back to default look — not fatal
            }
            new LoginFrame().setVisible(true);
            AppLogger.info("Login screen displayed.");
        });
    }
}
