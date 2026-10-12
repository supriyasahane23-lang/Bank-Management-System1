-- Bank Management System Database Schema
-- Run this script in MySQL Workbench or via MySQL CLI

CREATE DATABASE IF NOT EXISTS bank_db;
USE bank_db;

-- Table for customer accounts
CREATE TABLE IF NOT EXISTS accounts (
    id INT AUTO_INCREMENT PRIMARY KEY,
    account_number VARCHAR(20) NOT NULL UNIQUE,
    full_name VARCHAR(100) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    phone VARCHAR(20) NOT NULL,
    account_type VARCHAR(20) NOT NULL DEFAULT 'Savings',
    balance DECIMAL(15, 2) NOT NULL DEFAULT 0.00,
    pin VARCHAR(10) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Table for transaction records
CREATE TABLE IF NOT EXISTS transactions (
    id INT AUTO_INCREMENT PRIMARY KEY,
    account_number VARCHAR(20) NOT NULL,
    type VARCHAR(20) NOT NULL, -- DEPOSIT, WITHDRAWAL, TRANSFER_IN, TRANSFER_OUT
    amount DECIMAL(15, 2) NOT NULL,
    balance_after DECIMAL(15, 2) NOT NULL,
    description VARCHAR(255),
    related_account VARCHAR(20) NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (account_number) REFERENCES accounts(account_number) ON DELETE CASCADE
);

-- Table for bank administrators
CREATE TABLE IF NOT EXISTS admins (
    id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(100) NOT NULL,
    full_name VARCHAR(100) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Insert Default Admin User
INSERT INTO admins (username, password, full_name)
VALUES ('admin', 'admin123', 'System Administrator')
ON DUPLICATE KEY UPDATE full_name = VALUES(full_name);

-- Insert Demo Customer Accounts (if not existing)
INSERT INTO accounts (account_number, full_name, email, phone, account_type, balance, pin, status)
VALUES 
('1001001001', 'John Doe', 'john@example.com', '9876543210', 'Savings', 5000.00, '1234', 'ACTIVE'),
('1001001002', 'Alice Smith', 'alice@example.com', '9876543211', 'Current', 12500.00, '1234', 'ACTIVE')
ON DUPLICATE KEY UPDATE full_name = VALUES(full_name);

-- Insert Demo Transactions for John Doe
INSERT INTO transactions (account_number, type, amount, balance_after, description, related_account)
SELECT '1001001001', 'DEPOSIT', 5000.00, 5000.00, 'Initial Account Opening Deposit', NULL
WHERE NOT EXISTS (SELECT 1 FROM transactions WHERE account_number = '1001001001');

-- Insert Demo Transactions for Alice Smith
INSERT INTO transactions (account_number, type, amount, balance_after, description, related_account)
SELECT '1001001002', 'DEPOSIT', 12500.00, 12500.00, 'Initial Account Opening Deposit', NULL
WHERE NOT EXISTS (SELECT 1 FROM transactions WHERE account_number = '1001001002');
