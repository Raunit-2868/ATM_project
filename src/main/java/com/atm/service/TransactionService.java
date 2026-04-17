package com.atm.service;

import com.atm.db.AccountDAO;
import com.atm.model.Account;
import com.atm.util.AppLogger;
import com.atm.util.CurrencyUtil;

/**
 * TransactionService - Core business logic for deposits, withdrawals, and transfers.
 * Validates inputs, updates the in-memory Account, and persists via AccountDAO.
 * Designed to be called from background threads (ExecutorService/SwingWorker).
 */
public class TransactionService {

    private final AccountDAO accountDAO = new AccountDAO();

    /**
     * Deposits the specified amount into the given account type.
     * @param account     The active session account (mutated in-place).
     * @param accountType "CHECKING" or "SAVING"
     * @param amount      Amount to deposit (must be > 0)
     * @return New balance after deposit.
     * @throws Exception on invalid amount or DB failure.
     */
    public double deposit(Account account, String accountType, double amount) throws Exception {
        if (amount <= 0) {
            throw new Exception("Deposit amount must be greater than " +
                    CurrencyUtil.format(0, account.getBankName()) + ".");
        }
        if (amount > 1_000_000) {
            throw new Exception("Deposit limit per transaction is " +
                    CurrencyUtil.format(1_000_000, account.getBankName()) + ".");
        }

        double newBalance;
        if (accountType.equalsIgnoreCase("CHECKING")) {
            newBalance = account.getCheckingBalance() + amount;
            persist(account, "CHECKING", "DEPOSIT", amount, newBalance);
            account.setCheckingBalance(newBalance);
        } else {
            newBalance = account.getSavingBalance() + amount;
            persist(account, "SAVING", "DEPOSIT", amount, newBalance);
            account.setSavingBalance(newBalance);
        }

        AppLogger.info(String.format("DEPOSIT  | Customer %d | %s | %s | New Balance: %s",
                account.getCustomerNumber(), accountType,
                CurrencyUtil.format(amount, account.getBankName()),
                CurrencyUtil.format(newBalance, account.getBankName())));
        return newBalance;
    }

    /**
     * Withdraws the specified amount from the given account type.
     * @param account     The active session account (mutated in-place).
     * @param accountType "CHECKING" or "SAVING"
     * @param amount      Amount to withdraw (must be > 0 and <= balance)
     * @return New balance after withdrawal.
     * @throws Exception on insufficient funds, invalid amount, or DB failure.
     */
    public double withdraw(Account account, String accountType, double amount) throws Exception {
        if (amount <= 0) {
            throw new Exception("Withdrawal amount must be greater than " +
                    CurrencyUtil.format(0, account.getBankName()) + ".");
        }

        double currentBalance = accountType.equalsIgnoreCase("CHECKING")
                ? account.getCheckingBalance()
                : account.getSavingBalance();

        if (amount > currentBalance) {
            throw new Exception(String.format(
                "Insufficient funds. Available: %s, Requested: %s",
                CurrencyUtil.format(currentBalance, account.getBankName()),
                CurrencyUtil.format(amount, account.getBankName())));
        }

        double newBalance = currentBalance - amount;
        if (accountType.equalsIgnoreCase("CHECKING")) {
            persist(account, "CHECKING", "WITHDRAWAL", amount, newBalance);
            account.setCheckingBalance(newBalance);
        } else {
            persist(account, "SAVING", "WITHDRAWAL", amount, newBalance);
            account.setSavingBalance(newBalance);
        }

        AppLogger.info(String.format("WITHDRAW | Customer %d | %s | %s | New Balance: %s",
                account.getCustomerNumber(), accountType,
                CurrencyUtil.format(amount, account.getBankName()),
                CurrencyUtil.format(newBalance, account.getBankName())));
        return newBalance;
    }

    /**
     * Transfers funds between Checking and Savings within the same account.
     * @param account  The active session account (mutated in-place).
     * @param fromType "CHECKING" or "SAVING" — source account
     * @param toType   "CHECKING" or "SAVING" — destination account
     * @param amount   Amount to transfer (must be > 0 and <= source balance)
     * @return double[]{newFromBalance, newToBalance}
     * @throws Exception on insufficient funds, invalid amount, or DB failure.
     */
    public double[] transfer(Account account, String fromType, String toType, double amount) throws Exception {
        if (fromType.equalsIgnoreCase(toType)) {
            throw new Exception("Source and destination accounts must be different.");
        }
        if (amount <= 0) {
            throw new Exception("Transfer amount must be greater than " +
                    CurrencyUtil.format(0, account.getBankName()) + ".");
        }

        double fromBalance = fromType.equalsIgnoreCase("CHECKING")
                ? account.getCheckingBalance() : account.getSavingBalance();
        double toBalance   = fromType.equalsIgnoreCase("CHECKING")
                ? account.getSavingBalance() : account.getCheckingBalance();

        if (amount > fromBalance) {
            throw new Exception(String.format(
                "Insufficient funds in %s account. Available: %s",
                fromType.equalsIgnoreCase("CHECKING") ? "Checking" : "Savings",
                CurrencyUtil.format(fromBalance, account.getBankName())));
        }

        double newFromBalance = fromBalance - amount;
        double newToBalance   = toBalance   + amount;

        // Determine actual checking/saving values for DB update
        double newChecking = fromType.equalsIgnoreCase("CHECKING") ? newFromBalance : newToBalance;
        double newSaving   = fromType.equalsIgnoreCase("SAVING")   ? newFromBalance : newToBalance;

        // Persist atomically
        boolean balOk = accountDAO.updateBothBalances(
                account.getCustomerNumber(), account.getBankName(), newChecking, newSaving);
        boolean logFrom = accountDAO.logTransaction(account.getCustomerNumber(), account.getBankName(),
                fromType.toUpperCase(), "TRANSFER OUT", amount, newFromBalance);
        boolean logTo = accountDAO.logTransaction(account.getCustomerNumber(), account.getBankName(),
                toType.toUpperCase(), "TRANSFER IN", amount, newToBalance);

        if (!balOk || !logFrom || !logTo) {
            throw new Exception("Database error: transfer could not be saved.");
        }

        // Update in-memory account
        account.setCheckingBalance(newChecking);
        account.setSavingBalance(newSaving);

        AppLogger.info(String.format("TRANSFER | Customer %d | %s→%s | %s",
                account.getCustomerNumber(), fromType, toType,
                CurrencyUtil.format(amount, account.getBankName())));

        return new double[]{newFromBalance, newToBalance};
    }

    /** Persists balance update + audit log in a single logical operation. */
    private void persist(Account account, String type, String action, double amount, double newBalance)
            throws Exception {
        boolean balanceOk = accountDAO.updateBalance(account.getCustomerNumber(), account.getBankName(), type, newBalance);
        boolean logOk     = accountDAO.logTransaction(account.getCustomerNumber(), account.getBankName(), type, action, amount, newBalance);
        if (!balanceOk || !logOk) {
            throw new Exception("Database error: transaction could not be saved.");
        }
    }
}
