package com.atm.model;

/**
 * Account - Pure data model (POJO) representing a bank account.
 * No I/O logic — all business logic is handled by the service layer.
 */
public class Account {

    private int customerNumber;
    private String bankName;
    private int pinNumber;
    private String ownerName;
    private double checkingBalance;
    private double savingBalance;

    public Account() {}

    public Account(int customerNumber, String bankName, int pinNumber, String ownerName,
                   double checkingBalance, double savingBalance) {
        this.customerNumber = customerNumber;
        this.bankName = bankName;
        this.pinNumber = pinNumber;
        this.ownerName = ownerName;
        this.checkingBalance = checkingBalance;
        this.savingBalance = savingBalance;
    }

    public int getCustomerNumber()              { return customerNumber; }
    public void setCustomerNumber(int n)        { this.customerNumber = n; }

    public String getBankName()                 { return bankName; }
    public void setBankName(String name)        { this.bankName = name; }

    public int getPinNumber()                   { return pinNumber; }
    public void setPinNumber(int n)             { this.pinNumber = n; }

    public String getOwnerName()                { return ownerName; }
    public void setOwnerName(String name)       { this.ownerName = name; }

    public double getCheckingBalance()          { return checkingBalance; }
    public void setCheckingBalance(double b)    { this.checkingBalance = b; }

    public double getSavingBalance()            { return savingBalance; }
    public void setSavingBalance(double b)      { this.savingBalance = b; }

    @Override
    public String toString() {
        return "Account{customerNumber=" + customerNumber + ", owner='" + ownerName + "'}";
    }
}
