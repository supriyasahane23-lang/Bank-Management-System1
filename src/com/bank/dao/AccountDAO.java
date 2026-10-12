package com.bank.dao;

import com.bank.config.DatabaseConfig;
import com.bank.model.Account;
import com.bank.model.Transaction;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Data Access Object (DAO) for Bank Accounts.
 * Handles all database operations for accounts with ACID transaction safety.
 */
public class AccountDAO {

    /**
     * Generates a unique 10-digit account number.
     */
    private String generateUniqueAccountNumber(Connection conn) throws SQLException {
        Random random = new Random();
        while (true) {
            String accNum = "100" + (1000000 + random.nextInt(9000000));
            String checkSql = "SELECT 1 FROM accounts WHERE account_number = ?";
            try (PreparedStatement ps = conn.prepareStatement(checkSql)) {
                ps.setString(1, accNum);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        return accNum; // unique
                    }
                }
            }
        }
    }

    /**
     * Registers a new bank account.
     */
    public Account createAccount(String fullName, String email, String phone, 
                                 String accountType, BigDecimal initialDeposit, String pin) throws SQLException {
        String sql = "INSERT INTO accounts (account_number, full_name, email, phone, account_type, balance, pin, status) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, 'ACTIVE')";
        
        try (Connection conn = DatabaseConfig.getConnection()) {
            conn.setAutoCommit(false);
            try {
                String accountNumber = generateUniqueAccountNumber(conn);
                BigDecimal balance = (initialDeposit != null && initialDeposit.compareTo(BigDecimal.ZERO) > 0) 
                                      ? initialDeposit : BigDecimal.ZERO;

                try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setString(1, accountNumber);
                    ps.setString(2, fullName);
                    ps.setString(3, email);
                    ps.setString(4, phone);
                    ps.setString(5, accountType != null ? accountType : "Savings");
                    ps.setBigDecimal(6, balance);
                    ps.setString(7, pin);
                    ps.executeUpdate();
                }

                // If opening with initial deposit, log initial deposit transaction
                if (balance.compareTo(BigDecimal.ZERO) > 0) {
                    String txSql = "INSERT INTO transactions (account_number, type, amount, balance_after, description) VALUES (?, 'DEPOSIT', ?, ?, 'Initial Account Opening Deposit')";
                    try (PreparedStatement txPs = conn.prepareStatement(txSql)) {
                        txPs.setString(1, accountNumber);
                        txPs.setBigDecimal(2, balance);
                        txPs.setBigDecimal(3, balance);
                        txPs.executeUpdate();
                    }
                }

                conn.commit();

                Account acc = new Account(accountNumber, fullName, email, phone, accountType, balance, null, "ACTIVE");
                return acc;
            } catch (SQLException ex) {
                conn.rollback();
                throw ex;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    /**
     * Authenticates customer using account number (or email) and PIN.
     */
    public Account login(String identifier, String pin) throws SQLException {
        String sql = "SELECT * FROM accounts WHERE (account_number = ? OR email = ?) AND pin = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, identifier.trim());
            ps.setString(2, identifier.trim());
            ps.setString(3, pin.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToAccount(rs, false);
                }
            }
        }
        return null;
    }

    /**
     * Finds an account by account number.
     */
    public Account findByAccountNumber(String accountNumber) throws SQLException {
        String sql = "SELECT * FROM accounts WHERE account_number = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, accountNumber.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToAccount(rs, false);
                }
            }
        }
        return null;
    }

    /**
     * Deposits money into an account.
     */
    public Account deposit(String accountNumber, BigDecimal amount, String description) throws SQLException {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Deposit amount must be greater than zero.");
        }

        try (Connection conn = DatabaseConfig.getConnection()) {
            conn.setAutoCommit(false);
            try {
                // Check if account exists and is ACTIVE
                String selectSql = "SELECT balance, status, full_name, email, phone, account_type FROM accounts WHERE account_number = ? FOR UPDATE";
                BigDecimal currentBalance;
                String status, fullName, email, phone, accountType;

                try (PreparedStatement ps = conn.prepareStatement(selectSql)) {
                    ps.setString(1, accountNumber);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            throw new IllegalArgumentException("Account number " + accountNumber + " does not exist.");
                        }
                        status = rs.getString("status");
                        if (!"ACTIVE".equalsIgnoreCase(status)) {
                            throw new IllegalStateException("Account is currently " + status + ". Transactions not allowed.");
                        }
                        currentBalance = rs.getBigDecimal("balance");
                        fullName = rs.getString("full_name");
                        email = rs.getString("email");
                        phone = rs.getString("phone");
                        accountType = rs.getString("account_type");
                    }
                }

                BigDecimal newBalance = currentBalance.add(amount);

                // Update balance
                String updateSql = "UPDATE accounts SET balance = ? WHERE account_number = ?";
                try (PreparedStatement ps = conn.prepareStatement(updateSql)) {
                    ps.setBigDecimal(1, newBalance);
                    ps.setString(2, accountNumber);
                    ps.executeUpdate();
                }

                // Insert transaction record
                String txSql = "INSERT INTO transactions (account_number, type, amount, balance_after, description) VALUES (?, 'DEPOSIT', ?, ?, ?)";
                try (PreparedStatement ps = conn.prepareStatement(txSql)) {
                    ps.setString(1, accountNumber);
                    ps.setBigDecimal(2, amount);
                    ps.setBigDecimal(3, newBalance);
                    ps.setString(4, (description == null || description.trim().isEmpty()) ? "Cash Deposit" : description.trim());
                    ps.executeUpdate();
                }

                conn.commit();

                Account updated = new Account(accountNumber, fullName, email, phone, accountType, newBalance, null, status);
                return updated;
            } catch (Exception ex) {
                conn.rollback();
                throw ex;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    /**
     * Withdraws money from an account after validating PIN and sufficient funds.
     */
    public Account withdraw(String accountNumber, BigDecimal amount, String pin, String description) throws SQLException {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Withdrawal amount must be greater than zero.");
        }

        try (Connection conn = DatabaseConfig.getConnection()) {
            conn.setAutoCommit(false);
            try {
                // Verify account, PIN and get balance
                String selectSql = "SELECT balance, pin, status, full_name, email, phone, account_type FROM accounts WHERE account_number = ? FOR UPDATE";
                BigDecimal currentBalance;
                String status, dbPin, fullName, email, phone, accountType;

                try (PreparedStatement ps = conn.prepareStatement(selectSql)) {
                    ps.setString(1, accountNumber);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            throw new IllegalArgumentException("Account number " + accountNumber + " does not exist.");
                        }
                        dbPin = rs.getString("pin");
                        status = rs.getString("status");
                        currentBalance = rs.getBigDecimal("balance");
                        fullName = rs.getString("full_name");
                        email = rs.getString("email");
                        phone = rs.getString("phone");
                        accountType = rs.getString("account_type");
                    }
                }

                if (!dbPin.equals(pin)) {
                    throw new SecurityException("Incorrect security PIN entered.");
                }

                if (!"ACTIVE".equalsIgnoreCase(status)) {
                    throw new IllegalStateException("Account is currently " + status + ". Transactions not allowed.");
                }

                if (currentBalance.compareTo(amount) < 0) {
                    throw new IllegalArgumentException("Insufficient balance! Available balance is $" + currentBalance);
                }

                BigDecimal newBalance = currentBalance.subtract(amount);

                // Update balance
                String updateSql = "UPDATE accounts SET balance = ? WHERE account_number = ?";
                try (PreparedStatement ps = conn.prepareStatement(updateSql)) {
                    ps.setBigDecimal(1, newBalance);
                    ps.setString(2, accountNumber);
                    ps.executeUpdate();
                }

                // Insert transaction record
                String txSql = "INSERT INTO transactions (account_number, type, amount, balance_after, description) VALUES (?, 'WITHDRAWAL', ?, ?, ?)";
                try (PreparedStatement ps = conn.prepareStatement(txSql)) {
                    ps.setString(1, accountNumber);
                    ps.setBigDecimal(2, amount);
                    ps.setBigDecimal(3, newBalance);
                    ps.setString(4, (description == null || description.trim().isEmpty()) ? "Cash Withdrawal" : description.trim());
                    ps.executeUpdate();
                }

                conn.commit();

                Account updated = new Account(accountNumber, fullName, email, phone, accountType, newBalance, null, status);
                return updated;
            } catch (Exception ex) {
                conn.rollback();
                throw ex;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    /**
     * Transfers money between two accounts atomically (ACID transaction).
     */
    public Account transfer(String fromAccount, String toAccount, BigDecimal amount, String pin, String description) throws SQLException {
        if (fromAccount.equals(toAccount)) {
            throw new IllegalArgumentException("Sender and recipient account cannot be the same.");
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Transfer amount must be greater than zero.");
        }

        try (Connection conn = DatabaseConfig.getConnection()) {
            conn.setAutoCommit(false);
            try {
                // 1. Fetch sender account
                String senderSql = "SELECT balance, pin, status, full_name, email, phone, account_type FROM accounts WHERE account_number = ? FOR UPDATE";
                BigDecimal senderBalance;
                String senderPin, senderStatus, senderName, senderEmail, senderPhone, senderAccType;

                try (PreparedStatement ps = conn.prepareStatement(senderSql)) {
                    ps.setString(1, fromAccount);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            throw new IllegalArgumentException("Sender account does not exist.");
                        }
                        senderBalance = rs.getBigDecimal("balance");
                        senderPin = rs.getString("pin");
                        senderStatus = rs.getString("status");
                        senderName = rs.getString("full_name");
                        senderEmail = rs.getString("email");
                        senderPhone = rs.getString("phone");
                        senderAccType = rs.getString("account_type");
                    }
                }

                if (!senderPin.equals(pin)) {
                    throw new SecurityException("Incorrect security PIN entered.");
                }

                if (!"ACTIVE".equalsIgnoreCase(senderStatus)) {
                    throw new IllegalStateException("Your account is currently " + senderStatus + ". Transfer disallowed.");
                }

                if (senderBalance.compareTo(amount) < 0) {
                    throw new IllegalArgumentException("Insufficient balance for transfer! Available: $" + senderBalance);
                }

                // 2. Fetch recipient account
                String receiverSql = "SELECT balance, status, full_name FROM accounts WHERE account_number = ? FOR UPDATE";
                BigDecimal receiverBalance;
                String receiverStatus, receiverName;

                try (PreparedStatement ps = conn.prepareStatement(receiverSql)) {
                    ps.setString(1, toAccount);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            throw new IllegalArgumentException("Recipient account " + toAccount + " does not exist.");
                        }
                        receiverBalance = rs.getBigDecimal("balance");
                        receiverStatus = rs.getString("status");
                        receiverName = rs.getString("full_name");
                    }
                }

                if (!"ACTIVE".equalsIgnoreCase(receiverStatus)) {
                    throw new IllegalStateException("Recipient account is currently " + receiverStatus + ". Cannot receive funds.");
                }

                // 3. Compute new balances
                BigDecimal newSenderBalance = senderBalance.subtract(amount);
                BigDecimal newReceiverBalance = receiverBalance.add(amount);

                // 4. Update sender balance
                try (PreparedStatement ps = conn.prepareStatement("UPDATE accounts SET balance = ? WHERE account_number = ?")) {
                    ps.setBigDecimal(1, newSenderBalance);
                    ps.setString(2, fromAccount);
                    ps.executeUpdate();
                }

                // 5. Update recipient balance
                try (PreparedStatement ps = conn.prepareStatement("UPDATE accounts SET balance = ? WHERE account_number = ?")) {
                    ps.setBigDecimal(1, newReceiverBalance);
                    ps.setString(2, toAccount);
                    ps.executeUpdate();
                }

                String memo = (description == null || description.trim().isEmpty()) ? "Fund Transfer" : description.trim();

                // 6. Record sender transaction (TRANSFER_OUT)
                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO transactions (account_number, type, amount, balance_after, description, related_account) VALUES (?, 'TRANSFER_OUT', ?, ?, ?, ?)")) {
                    ps.setString(1, fromAccount);
                    ps.setBigDecimal(2, amount);
                    ps.setBigDecimal(3, newSenderBalance);
                    ps.setString(4, memo + " (To: " + receiverName + " #" + toAccount + ")");
                    ps.setString(5, toAccount);
                    ps.executeUpdate();
                }

                // 7. Record recipient transaction (TRANSFER_IN)
                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO transactions (account_number, type, amount, balance_after, description, related_account) VALUES (?, 'TRANSFER_IN', ?, ?, ?, ?)")) {
                    ps.setString(1, toAccount);
                    ps.setBigDecimal(2, amount);
                    ps.setBigDecimal(3, newReceiverBalance);
                    ps.setString(4, memo + " (From: " + senderName + " #" + fromAccount + ")");
                    ps.setString(5, fromAccount);
                    ps.executeUpdate();
                }

                // 8. Commit atomic transaction
                conn.commit();

                return new Account(fromAccount, senderName, senderEmail, senderPhone, senderAccType, newSenderBalance, null, senderStatus);
            } catch (Exception ex) {
                conn.rollback();
                throw ex;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    /**
     * Changes security PIN.
     */
    public boolean changePin(String accountNumber, String oldPin, String newPin) throws SQLException {
        String verifySql = "SELECT pin FROM accounts WHERE account_number = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(verifySql)) {
            ps.setString(1, accountNumber);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next() || !rs.getString("pin").equals(oldPin)) {
                    throw new SecurityException("Existing PIN is incorrect.");
                }
            }
        }

        String updateSql = "UPDATE accounts SET pin = ? WHERE account_number = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(updateSql)) {
            ps.setString(1, newPin);
            ps.setString(2, accountNumber);
            return ps.executeUpdate() > 0;
        }
    }

    /**
     * Lists all registered accounts (Admin functionality).
     */
    public List<Account> getAllAccounts() throws SQLException {
        List<Account> list = new ArrayList<>();
        String sql = "SELECT * FROM accounts ORDER BY id DESC";
        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapResultSetToAccount(rs, false));
            }
        }
        return list;
    }

    /**
     * Updates account status (ACTIVE, FROZEN, CLOSED) - Admin functionality.
     */
    public boolean updateAccountStatus(String accountNumber, String newStatus) throws SQLException {
        String sql = "UPDATE accounts SET status = ? WHERE account_number = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, newStatus.toUpperCase());
            ps.setString(2, accountNumber);
            return ps.executeUpdate() > 0;
        }
    }

    /**
     * Retrieves overall bank statistics for Admin Dashboard.
     */
    public Map<String, Object> getBankStatistics() throws SQLException {
        Map<String, Object> stats = new HashMap<>();
        String sql = "SELECT " +
                     "COUNT(*) AS total_accounts, " +
                     "COALESCE(SUM(balance), 0) AS total_reserve, " +
                     "SUM(CASE WHEN status = 'ACTIVE' THEN 1 ELSE 0 END) AS active_accounts, " +
                     "(SELECT COUNT(*) FROM transactions) AS total_transactions " +
                     "FROM accounts";
        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                stats.put("totalAccounts", rs.getInt("total_accounts"));
                stats.put("totalReserve", rs.getBigDecimal("total_reserve"));
                stats.put("activeAccounts", rs.getInt("active_accounts"));
                stats.put("totalTransactions", rs.getInt("total_transactions"));
            }
        }
        return stats;
    }

    private Account mapResultSetToAccount(ResultSet rs, boolean includePin) throws SQLException {
        Account a = new Account();
        a.setId(rs.getInt("id"));
        a.setAccountNumber(rs.getString("account_number"));
        a.setFullName(rs.getString("full_name"));
        a.setEmail(rs.getString("email"));
        a.setPhone(rs.getString("phone"));
        a.setAccountType(rs.getString("account_type"));
        a.setBalance(rs.getBigDecimal("balance"));
        if (includePin) {
            a.setPin(rs.getString("pin"));
        }
        a.setStatus(rs.getString("status"));
        a.setCreatedAt(rs.getTimestamp("created_at"));
        return a;
    }
}
