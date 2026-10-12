# 🏦 ApexBank - Bank Management System

A full-stack, beginner-friendly **Bank Management System** built with **Core Java**, **MySQL**, **HTML5**, **CSS3**, and **JavaScript**.

---

## 📑 Table of Contents
1. [Project Overview](#-project-overview)
2. [Architecture & How It Works](#-architecture--how-it-works)
3. [Project Structure](#-project-structure)
4. [Prerequisites](#-prerequisites)
5. [Database Setup](#-database-setup)
6. [How to Run the Application](#-how-to-run-the-application)
7. [Default Demo Credentials](#-default-demo-credentials)
8. [Features Walkthrough](#-features-walkthrough)
9. [Key Concepts for Beginners](#-key-concepts-for-beginners)

---

## 🌟 Project Overview
This project is designed to help beginners master full-stack software development by building a real-world financial application:
- **Core Java Backend**: Uses built-in `com.sun.net.httpserver.HttpServer` and JDBC. No heavy frameworks or complex setup required.
- **MySQL Database**: Stores bank accounts, administrators, and transaction history.
- **Modern Responsive Frontend**: Built with clean HTML5, modern CSS3 (glassmorphic dark theme), and vanilla JavaScript with `fetch()` API.

---

## 🏛️ Architecture & How It Works

```
┌─────────────────────────────────────────────────────────────┐
│                       Browser Frontend                      │
│        (HTML5 / CSS3 / JavaScript / Fetch API)             │
│        • index.html (Portal & Login)                        │
│        • dashboard.html (Customer Banking)                  │
│        • admin.html (Manager Console)                       │
└──────────────────────────────▲──────────────────────────────┘
                               │ HTTP JSON Requests (REST API)
                               ▼
┌─────────────────────────────────────────────────────────────┐
│                    Java Backend Server                      │
│               (com.bank.server.HttpServer)                  │
│  • StaticFileHandler : Serves HTML/CSS/JS assets            │
│  • ApiHandler        : Handles /api/ endpoints              │
│  • Model             : Account, Transaction, Admin POJOs    │
│  • DAO               : JDBC PreparedStatements & ACID logic │
└──────────────────────────────▲──────────────────────────────┘
                               │ JDBC (mysql-connector-j)
                               ▼
┌─────────────────────────────────────────────────────────────┐
│                        MySQL Database                       │
│                         (bank_db)                           │
│  • accounts     : Customer profiles, balance, PIN, status   │
│  • transactions : Audit trail of deposits, transfers, etc.  │
│  • admins       : System manager credentials                │
└─────────────────────────────────────────────────────────────┘
```

---

## 📁 Project Structure

```
Bank Management System/
│
├── database.sql                  # MySQL database schema and sample data
├── db.properties                 # Database connection settings (URL, user, password)
├── run.bat                       # Windows 1-click batch script to compile & run
├── run.ps1                       # PowerShell launcher script
├── README.md                     # Complete project documentation
│
├── lib/                          # External libraries
│   ├── mysql-connector-j-8.3.0.jar # MySQL JDBC Driver
│   └── gson-2.10.1.jar           # Google Gson for JSON parsing
│
├── bin/                          # Compiled Java bytecode (.class files)
│
├── src/                          # Java source code
│   └── com/bank/
│       ├── Main.java             # Server entry point (starts server on port 8080)
│       ├── config/
│       │   └── DatabaseConfig.java # JDBC connection manager
│       ├── model/
│       │   ├── Account.java      # Customer account entity
│       │   ├── Transaction.java  # Transaction entity
│       │   ├── Admin.java        # Admin user entity
│       │   └── ApiResponse.java  # Standard JSON response envelope
│       ├── dao/
│       │   ├── AccountDAO.java   # Account operations & ACID transfers
│       │   ├── TransactionDAO.java # Transaction queries
│       │   └── AdminDAO.java     # Admin authentication
│       └── server/
│           ├── StaticFileHandler.java # Serves static web pages & assets
│           └── ApiHandler.java   # REST API endpoint router
│
└── web/                          # Frontend web interface
    ├── index.html                # Landing page & Login/Register modal
    ├── dashboard.html            # Customer banking dashboard
    ├── admin.html                # Admin control & audit console
    ├── css/
    │   └── styles.css            # Responsive styling & themes
    └── js/
        ├── app.js                # Core frontend banking logic
        └── admin.js              # Admin portal frontend logic
```

---

## 🛠️ Prerequisites
- **Java Development Kit (JDK 17+)** (Run `java -version` and `javac -version` to verify)
- **MySQL Server 8.0+** (Running locally on port 3306)

---

## 🗄️ Database Setup
The database schema is provided in `database.sql`.

If you need to initialize or reset the database:
1. Open **MySQL Workbench** or your terminal.
2. Run the script:
   ```powershell
   Get-Content database.sql -Raw | mysql -u root -p
   ```
3. Verify `db.properties` has your correct credentials:
   ```properties
   db.url=jdbc:mysql://localhost:3306/bank_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
   db.user=root
   db.password=root
   ```

---

## 🚀 How to Run the Application

### Option 1: One-Click (Recommended on Windows)
Simply double-click `run.bat` or run in terminal:
```cmd
.\run.bat
```

### Option 2: PowerShell
```powershell
.\run.ps1
```

### Option 3: Manual Command Line
1. **Compile the Java sources:**
   ```powershell
   javac -encoding UTF-8 -cp "lib/*" -d bin src/com/bank/model/*.java src/com/bank/config/*.java src/com/bank/dao/*.java src/com/bank/server/*.java src/com/bank/*.java
   ```

2. **Run the server:**
   ```powershell
   java -cp "bin;lib/*" com.bank.Main
   ```

3. **Open in your web browser:**
   - **Customer Portal & Landing Page:** [http://localhost:8080](http://localhost:8080)
   - **Customer Dashboard:** [http://localhost:8080/dashboard.html](http://localhost:8080/dashboard.html)
   - **Admin Management Console:** [http://localhost:8080/admin.html](http://localhost:8080/admin.html)

---

## 🔑 Default Demo Credentials

| Role | Identifier / Username | Password / PIN | Initial Balance |
| :--- | :--- | :--- | :--- |
| **Customer 1** | `1001001001` (or `john@example.com`) | `1234` | $5,000.00 |
| **Customer 2** | `1001001002` (or `alice@example.com`) | `1234` | $12,500.00 |
| **Administrator** | `admin` | `admin123` | System-wide access |

---

## 💡 Features Walkthrough

### 1. Customer Operations
- **Open New Account**: Create an account with a unique 10-digit number, personal details, account type (Savings/Current), security PIN, and optional initial deposit.
- **Deposit Funds**: Instantly deposit funds with description notes.
- **Withdraw Funds**: Withdraw money with balance checks and PIN security validation.
- **Atomic Fund Transfer**: Transfer money to another account. Both sender debit and recipient credit happen within a database transaction. If anything fails, changes are completely rolled back!
- **Real-Time Mini Statement**: Live ledger showing timestamps, transaction types, amounts, and running balances.
- **Privacy Mode**: Eye icon toggle to hide/reveal balance.
- **Change PIN**: Update security PIN securely.

### 2. Admin Operations
- **System Overview**: View total bank deposits, total accounts, active accounts, and total transaction volume.
- **Account Directory**: Search and filter accounts by name, number, or email.
- **Account Controls**: Freeze, unfreeze, or close accounts in real-time.
- **Live Audit Trail**: View every single transaction that takes place across the entire bank.

---

## 🧠 Key Concepts for Beginners

### 1. Why `PreparedStatement`?
```java
String sql = "SELECT * FROM accounts WHERE account_number = ? AND pin = ?";
PreparedStatement ps = conn.prepareStatement(sql);
ps.setString(1, identifier);
ps.setString(2, pin);
```
Using `PreparedStatement` with parameterized placeholders (`?`) protects the application against **SQL Injection attacks**. User input is treated strictly as data, never as executable SQL.

### 2. How Real Banking Transactions (ACID) Work:
```java
conn.setAutoCommit(false); // Begin transaction
try {
    // 1. Deduct money from sender
    // 2. Add money to receiver
    // 3. Record TRANSFER_OUT transaction
    // 4. Record TRANSFER_IN transaction
    conn.commit(); // Save all changes atomically
} catch (Exception e) {
    conn.rollback(); // Cancel everything if any error occurs!
    throw e;
} finally {
    conn.setAutoCommit(true);
}
```
This guarantees that money is never deducted from the sender if the recipient's account fails to credit.

### 3. Asynchronous Web Frontend with `fetch()`:
The frontend never needs to refresh the entire page. It calls Java endpoints using JavaScript `fetch()`, receives JSON responses, and smoothly updates the DOM.
