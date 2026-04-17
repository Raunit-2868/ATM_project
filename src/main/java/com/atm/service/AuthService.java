package com.atm.service;

import com.atm.db.AccountDAO;
import com.atm.model.Account;
import com.atm.util.AppLogger;

/**
 * AuthService - Handles user authentication.
 * Delegates credential lookup to AccountDAO.
 */
public class AuthService {

    private final AccountDAO accountDAO = new AccountDAO();

    /**
     * Attempts to log in with the given credentials.
     * @return Authenticated Account object.
     * @throws Exception if credentials are invalid.
     */
    public Account login(int customerNumber, int pinNumber, String bankName) throws Exception {
        AppLogger.info("Login attempt for customer: " + customerNumber + " at bank: " + bankName);
        Account account = accountDAO.findByCredentials(customerNumber, pinNumber, bankName);
        if (account == null) {
            throw new Exception("Invalid Customer Number or PIN for " + bankName + ". Please try again.");
        }
        AppLogger.info("Login successful: " + account.getOwnerName());
        return account;
    }
}
