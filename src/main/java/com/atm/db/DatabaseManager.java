package com.atm.db;

import com.atm.util.AppLogger;

import java.io.File;
import java.net.URISyntaxException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * DatabaseManager - Singleton that manages SQLite connection and schema initialization.
 * Seeds default demo accounts on first run.
 *
 * The database file (atm.db) is placed next to the compiled classes/JAR so it
 * survives working-directory changes.
 */
public class DatabaseManager {

    static {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("SQLite JDBC driver not found", e);
        }
    }

    private static final String DB_URL = buildDbUrl();
    private static DatabaseManager instance;

    private DatabaseManager() {}

    /** Resolves the atm.db path relative to the running binary so it is stable. */
    private static String buildDbUrl() {
        try {
            File codeLocation = new File(
                DatabaseManager.class.getProtectionDomain()
                    .getCodeSource().getLocation().toURI());
            // codeLocation is either the bin/ dir (when run from run.bat) or the JAR
            File dir = codeLocation.isDirectory() ? codeLocation : codeLocation.getParentFile();
            // Go up to the project root (one level above bin/)
            File projectRoot = dir.getParentFile() != null ? dir.getParentFile() : dir;
            String path = projectRoot.getAbsolutePath() + File.separator + "atm.db";
            return "jdbc:sqlite:" + path;
        } catch (URISyntaxException e) {
            return "jdbc:sqlite:atm.db"; // safe fallback
        }
    }

    public static synchronized DatabaseManager getInstance() {
        if (instance == null) {
            instance = new DatabaseManager();
        }
        return instance;
    }

    /** Returns a new SQLite connection. Caller is responsible for closing it. */
    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL);
    }

    /** Creates schema tables and seeds demo data on first run. */
    public void initialize() {
        AppLogger.info("Database URL: " + DB_URL);
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {

            // Accounts table — partitioned by (customer_number, bank_name)
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS accounts (
                    customer_number  INTEGER NOT NULL,
                    bank_name        TEXT    NOT NULL,
                    pin_number       INTEGER NOT NULL,
                    owner_name       TEXT    NOT NULL,
                    checking_balance REAL    NOT NULL DEFAULT 0.0,
                    saving_balance   REAL    NOT NULL DEFAULT 0.0,
                    PRIMARY KEY (customer_number, bank_name)
                )
            """);

            // Transactions audit table — also partitioned by bank_name
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS transactions (
                    id               INTEGER PRIMARY KEY AUTOINCREMENT,
                    customer_number  INTEGER NOT NULL,
                    bank_name        TEXT    NOT NULL,
                    account_type     TEXT    NOT NULL,
                    action           TEXT    NOT NULL,
                    amount           REAL    NOT NULL,
                    balance_after    REAL    NOT NULL,
                    timestamp        DATETIME DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY (customer_number, bank_name)
                        REFERENCES accounts(customer_number, bank_name)
                )
            """);

            // ── Seed demo accounts (INSERT OR IGNORE = safe to re-run) ──

            // 1. SBI Accounts (PIN: 111111)
            stmt.execute("""
                INSERT OR IGNORE INTO accounts
                    (customer_number, bank_name, pin_number, owner_name, checking_balance, saving_balance)
                VALUES (345678, 'SBI Bank', 111111, 'Raunit Sharaff',    5000.00, 25000.00)
            """);
            stmt.execute("""
                INSERT OR IGNORE INTO accounts
                    (customer_number, bank_name, pin_number, owner_name, checking_balance, saving_balance)
                VALUES (123456, 'SBI Bank', 111111, 'Ujjwal Rauniyar', 4000.00, 15000.00)
            """);
            stmt.execute("""
                INSERT OR IGNORE INTO accounts
                    (customer_number, bank_name, pin_number, owner_name, checking_balance, saving_balance)
                VALUES (234567, 'SBI Bank', 111111, 'Kirtish Mishra',  4000.00, 15000.00)
            """);

            // 2. HDFC Accounts (PIN: 222222)
            stmt.execute("""
                INSERT OR IGNORE INTO accounts
                    (customer_number, bank_name, pin_number, owner_name, checking_balance, saving_balance)
                VALUES (345678, 'HDFC Bank', 222222, 'Raunit Sharaff',   7500.00, 50000.00)
            """);
            stmt.execute("""
                INSERT OR IGNORE INTO accounts
                    (customer_number, bank_name, pin_number, owner_name, checking_balance, saving_balance)
                VALUES (123456, 'HDFC Bank', 222222, 'Ujjwal Rauniyar', 3500.00, 12000.00)
            """);

            // 3. ICICI Accounts (PIN: 333333)
            stmt.execute("""
                INSERT OR IGNORE INTO accounts
                    (customer_number, bank_name, pin_number, owner_name, checking_balance, saving_balance)
                VALUES (345678, 'ICICI Bank', 333333, 'Raunit Sharaff', 12000.00, 85000.00)
            """);

            // 4. SecureATM PRO Accounts (PIN: 159159)
            stmt.execute("""
                INSERT OR IGNORE INTO accounts
                    (customer_number, bank_name, pin_number, owner_name, checking_balance, saving_balance)
                VALUES (345678, 'SecureATM (PRO)', 159159, 'Raunit Sharaff', 2500.00, 10000.00)
            """);

            AppLogger.info("Database initialized successfully with multi-bank partitions.");

        } catch (SQLException e) {
            AppLogger.error("Database initialization failed", e);
            throw new RuntimeException("Could not initialize database", e);
        }
    }
}
