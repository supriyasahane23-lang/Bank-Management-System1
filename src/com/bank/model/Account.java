package com.bank.model;

import java.math.BigDecimal;
import java.sql.Timestamp;

/**
 * Model representing a customer bank account.
 */
public class Account {
    private int id;
    private String accountNumber;
    private String fullName;
    private String email;
    private String phone;
    private String accountType; // "Savings" or "Current"
    private BigDecimal balance;
    private transient String pin; // transient: avoid serializing PIN in responses
    private String status;      // "ACTIVE", "FROZEN", "CLOSED"
    private Timestamp createdAt;

    public Account() {}

    public Account(String accountNumber, String fullName, String email, String phone, 
                   String accountType, BigDecimal balance, String pin, String status) {
        this.accountNumber = accountNumber;
        this.fullName = fullName;
        this.email = email;
        this.phone = phone;
        this.accountType = accountType;
        this.balance = balance;
        this.pin = pin;
        this.status = status;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getAccountNumber() { return accountNumber; }
    public void setAccountNumber(String accountNumber) { this.accountNumber = accountNumber; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getAccountType() { return accountType; }
    public void setAccountType(String accountType) { this.accountType = accountType; }

    public BigDecimal getBalance() { return balance; }
    public void setBalance(BigDecimal balance) { this.balance = balance; }

    public String getPin() { return pin; }
    public void setPin(String pin) { this.pin = pin; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
}
