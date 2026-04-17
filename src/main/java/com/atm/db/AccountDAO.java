package com.atm.db;

import com.atm.model.Account;
import com.atm.util.AppLogger;
import com.atm.util.CurrencyUtil;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * AccountDAO - Data Access Object for all account and transaction DB operations.
 * Uses the DAO pattern to isolate persistence from business logic.
 */
public class AccountDAO {

    /**
     * Finds an account matching the given credentials.
     * @return Account if found, null otherwise.
     */
    public Account findByCredentials(int customerNumber, int pinNumber, String bankName) {
        String sql = "SELECT * FROM accounts WHERE customer_number = ? AND pin_number = ? AND bank_name = ?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, customerNumber);
            ps.setInt(2, pinNumber);
            ps.setString(3, bankName);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapRow(rs);
            }
        } catch (SQLException e) {
            AppLogger.error("AccountDAO.findByCredentials failed", e);
        }
        return null;
    }

    /**
     * Refreshes account data from DB (used after balance changes to keep in-memory state current).
     */
    public Account refreshAccount(int customerNumber, String bankName) {
        String sql = "SELECT * FROM accounts WHERE customer_number = ? AND bank_name = ?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, customerNumber);
            ps.setString(2, bankName);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapRow(rs);
            }
        } catch (SQLException e) {
            AppLogger.error("AccountDAO.refreshAccount failed", e);
        }
        return null;
    }

    /**
     * Updates the checking or saving balance for an account.
     * @param accountType "CHECKING" or "SAVING"
     */
    public boolean updateBalance(int customerNumber, String bankName, String accountType, double newBalance) {
        String col = accountType.equalsIgnoreCase("CHECKING") ? "checking_balance" : "saving_balance";
        String sql = "UPDATE accounts SET " + col + " = ? WHERE customer_number = ? AND bank_name = ?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDouble(1, newBalance);
            ps.setInt(2, customerNumber);
            ps.setString(3, bankName);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            AppLogger.error("AccountDAO.updateBalance failed", e);
        }
        return false;
    }

    /**
     * Updates both checking and saving balances atomically (used for transfers).
     */
    public boolean updateBothBalances(int customerNumber, String bankName,
                                      double newChecking, double newSaving) {
        String sql = "UPDATE accounts SET checking_balance = ?, saving_balance = ? " +
                     "WHERE customer_number = ? AND bank_name = ?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDouble(1, newChecking);
            ps.setDouble(2, newSaving);
            ps.setInt(3, customerNumber);
            ps.setString(4, bankName);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            AppLogger.error("AccountDAO.updateBothBalances failed", e);
        }
        return false;
    }

    /**
     * Updates the PIN for an account after verifying the current PIN.
     * @return true if updated, false if current PIN didn't match or DB error.
     */
    public boolean updatePin(int customerNumber, String bankName, int currentPin, int newPin) {
        // First verify current PIN
        String verifySql = "SELECT COUNT(*) FROM accounts WHERE customer_number = ? AND bank_name = ? AND pin_number = ?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(verifySql)) {
            ps.setInt(1, customerNumber);
            ps.setString(2, bankName);
            ps.setInt(3, currentPin);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next() || rs.getInt(1) == 0) {
                    return false; // Wrong current PIN
                }
            }
        } catch (SQLException e) {
            AppLogger.error("AccountDAO.updatePin (verify) failed", e);
            return false;
        }

        // Update the PIN
        String updateSql = "UPDATE accounts SET pin_number = ? WHERE customer_number = ? AND bank_name = ?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(updateSql)) {
            ps.setInt(1, newPin);
            ps.setInt(2, customerNumber);
            ps.setString(3, bankName);
            boolean ok = ps.executeUpdate() > 0;
            if (ok) AppLogger.info("PIN updated for customer " + customerNumber);
            return ok;
        } catch (SQLException e) {
            AppLogger.error("AccountDAO.updatePin (update) failed", e);
            return false;
        }
    }

    /**
     * Inserts an audit row into the transactions table.
     */
    public boolean logTransaction(int customerNumber, String bankName, String accountType,
                                  String action, double amount, double balanceAfter) {
        String sql = """
            INSERT INTO transactions (customer_number, bank_name, account_type, action, amount, balance_after)
            VALUES (?, ?, ?, ?, ?, ?)
        """;
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, customerNumber);
            ps.setString(2, bankName);
            ps.setString(3, accountType);
            ps.setString(4, action);
            ps.setDouble(5, amount);
            ps.setDouble(6, balanceAfter);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            AppLogger.error("AccountDAO.logTransaction failed", e);
        }
        return false;
    }

    /**
     * Retrieves the last 20 transactions for display in the history dialog.
     * Each row: [accountType, action, formattedAmount, formattedBalanceAfter, timestamp]
     */
    public List<String[]> getTransactionHistory(int customerNumber, String bankName) {
        List<String[]> history = new ArrayList<>();
        String sql = """
            SELECT account_type, action, amount, balance_after, timestamp
            FROM transactions
            WHERE customer_number = ? AND bank_name = ?
            ORDER BY timestamp DESC
            LIMIT 20
        """;
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, customerNumber);
            ps.setString(2, bankName);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    history.add(new String[]{
                        rs.getString("account_type"),
                        rs.getString("action"),
                        CurrencyUtil.format(rs.getDouble("amount"), bankName),
                        CurrencyUtil.format(rs.getDouble("balance_after"), bankName),
                        rs.getString("timestamp")
                    });
                }
            }
        } catch (SQLException e) {
            AppLogger.error("AccountDAO.getTransactionHistory failed", e);
        }
        return history;
    }

    private Account mapRow(ResultSet rs) throws SQLException {
        return new Account(
            rs.getInt("customer_number"),
            rs.getString("bank_name"),
            rs.getInt("pin_number"),
            rs.getString("owner_name"),
            rs.getDouble("checking_balance"),
            rs.getDouble("saving_balance")
        );
    }
}
