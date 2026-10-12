package com.bank.server;

import com.bank.dao.AccountDAO;
import com.bank.dao.AdminDAO;
import com.bank.dao.TransactionDAO;
import com.bank.model.Account;
import com.bank.model.Admin;
import com.bank.model.ApiResponse;
import com.bank.model.Transaction;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Handles all JSON REST API requests under /api/*
 */
public class ApiHandler implements HttpHandler {
    private final Gson gson = new Gson();
    private final AccountDAO accountDAO = new AccountDAO();
    private final TransactionDAO transactionDAO = new TransactionDAO();
    private final AdminDAO adminDAO = new AdminDAO();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        // Set CORS headers
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type, Authorization");
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");

        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(204, -1);
            return;
        }

        String path = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod();

        try {
            switch (path) {
                // --- Customer Endpoints ---
                case "/api/register":
                    if ("POST".equalsIgnoreCase(method)) handleRegister(exchange);
                    else methodNotAllowed(exchange);
                    break;

                case "/api/login":
                    if ("POST".equalsIgnoreCase(method)) handleLogin(exchange);
                    else methodNotAllowed(exchange);
                    break;

                case "/api/account":
                    if ("GET".equalsIgnoreCase(method)) handleGetAccount(exchange);
                    else methodNotAllowed(exchange);
                    break;

                case "/api/deposit":
                    if ("POST".equalsIgnoreCase(method)) handleDeposit(exchange);
                    else methodNotAllowed(exchange);
                    break;

                case "/api/withdraw":
                    if ("POST".equalsIgnoreCase(method)) handleWithdraw(exchange);
                    else methodNotAllowed(exchange);
                    break;

                case "/api/transfer":
                    if ("POST".equalsIgnoreCase(method)) handleTransfer(exchange);
                    else methodNotAllowed(exchange);
                    break;

                case "/api/change-pin":
                    if ("POST".equalsIgnoreCase(method)) handleChangePin(exchange);
                    else methodNotAllowed(exchange);
                    break;

                case "/api/transactions":
                    if ("GET".equalsIgnoreCase(method)) handleGetTransactions(exchange);
                    else methodNotAllowed(exchange);
                    break;

                // --- Admin Endpoints ---
                case "/api/admin/login":
                    if ("POST".equalsIgnoreCase(method)) handleAdminLogin(exchange);
                    else methodNotAllowed(exchange);
                    break;

                case "/api/admin/accounts":
                    if ("GET".equalsIgnoreCase(method)) handleAdminGetAccounts(exchange);
                    else methodNotAllowed(exchange);
                    break;

                case "/api/admin/transactions":
                    if ("GET".equalsIgnoreCase(method)) handleAdminGetTransactions(exchange);
                    else methodNotAllowed(exchange);
                    break;

                case "/api/admin/stats":
                    if ("GET".equalsIgnoreCase(method)) handleAdminGetStats(exchange);
                    else methodNotAllowed(exchange);
                    break;

                case "/api/admin/status":
                    if ("POST".equalsIgnoreCase(method)) handleAdminUpdateStatus(exchange);
                    else methodNotAllowed(exchange);
                    break;

                default:
                    sendJson(exchange, 404, ApiResponse.error("API endpoint not found: " + path));
                    break;
            }
        } catch (IllegalArgumentException | SecurityException | IllegalStateException e) {
            sendJson(exchange, 400, ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            e.printStackTrace();
            sendJson(exchange, 500, ApiResponse.error("Server error: " + e.getMessage()));
        }
    }

    // ==========================================
    // Customer Controllers
    // ==========================================

    private void handleRegister(HttpExchange exchange) throws Exception {
        JsonObject req = parseJsonRequest(exchange);
        String fullName = getRequiredString(req, "fullName");
        String email = getRequiredString(req, "email");
        String phone = getRequiredString(req, "phone");
        String accountType = req.has("accountType") ? req.get("accountType").getAsString() : "Savings";
        BigDecimal initialDeposit = req.has("initialDeposit") ? new BigDecimal(req.get("initialDeposit").getAsString()) : BigDecimal.ZERO;
        String pin = getRequiredString(req, "pin");

        if (pin.length() < 4) {
            throw new IllegalArgumentException("PIN must be at least 4 digits.");
        }

        Account account = accountDAO.createAccount(fullName, email, phone, accountType, initialDeposit, pin);
        sendJson(exchange, 201, ApiResponse.ok("Account successfully created!", account));
    }

    private void handleLogin(HttpExchange exchange) throws Exception {
        JsonObject req = parseJsonRequest(exchange);
        String identifier = getRequiredString(req, "identifier");
        String pin = getRequiredString(req, "pin");

        Account account = accountDAO.login(identifier, pin);
        if (account == null) {
            sendJson(exchange, 401, ApiResponse.error("Invalid Account Number/Email or PIN."));
            return;
        }

        sendJson(exchange, 200, ApiResponse.ok("Login successful!", account));
    }

    private void handleGetAccount(HttpExchange exchange) throws Exception {
        Map<String, String> query = parseQueryParams(exchange.getRequestURI().getQuery());
        String accNum = query.get("accountNumber");
        if (accNum == null || accNum.trim().isEmpty()) {
            throw new IllegalArgumentException("accountNumber parameter is required.");
        }

        Account account = accountDAO.findByAccountNumber(accNum);
        if (account == null) {
            sendJson(exchange, 404, ApiResponse.error("Account not found."));
            return;
        }

        sendJson(exchange, 200, ApiResponse.ok("Account details fetched", account));
    }

    private void handleDeposit(HttpExchange exchange) throws Exception {
        JsonObject req = parseJsonRequest(exchange);
        String accountNumber = getRequiredString(req, "accountNumber");
        BigDecimal amount = new BigDecimal(getRequiredString(req, "amount"));
        String description = req.has("description") ? req.get("description").getAsString() : "Cash Deposit";

        Account updated = accountDAO.deposit(accountNumber, amount, description);
        sendJson(exchange, 200, ApiResponse.ok("Deposit of $" + amount + " successful!", updated));
    }

    private void handleWithdraw(HttpExchange exchange) throws Exception {
        JsonObject req = parseJsonRequest(exchange);
        String accountNumber = getRequiredString(req, "accountNumber");
        BigDecimal amount = new BigDecimal(getRequiredString(req, "amount"));
        String pin = getRequiredString(req, "pin");
        String description = req.has("description") ? req.get("description").getAsString() : "Cash Withdrawal";

        Account updated = accountDAO.withdraw(accountNumber, amount, pin, description);
        sendJson(exchange, 200, ApiResponse.ok("Withdrawal of $" + amount + " successful!", updated));
    }

    private void handleTransfer(HttpExchange exchange) throws Exception {
        JsonObject req = parseJsonRequest(exchange);
        String fromAccount = getRequiredString(req, "fromAccount");
        String toAccount = getRequiredString(req, "toAccount");
        BigDecimal amount = new BigDecimal(getRequiredString(req, "amount"));
        String pin = getRequiredString(req, "pin");
        String description = req.has("description") ? req.get("description").getAsString() : "Fund Transfer";

        Account updated = accountDAO.transfer(fromAccount, toAccount, amount, pin, description);
        sendJson(exchange, 200, ApiResponse.ok("Successfully transferred $" + amount + " to Account #" + toAccount, updated));
    }

    private void handleChangePin(HttpExchange exchange) throws Exception {
        JsonObject req = parseJsonRequest(exchange);
        String accountNumber = getRequiredString(req, "accountNumber");
        String oldPin = getRequiredString(req, "oldPin");
        String newPin = getRequiredString(req, "newPin");

        if (newPin.length() < 4) {
            throw new IllegalArgumentException("New PIN must be at least 4 digits.");
        }

        boolean success = accountDAO.changePin(accountNumber, oldPin, newPin);
        if (success) {
            sendJson(exchange, 200, ApiResponse.ok("PIN changed successfully!"));
        } else {
            sendJson(exchange, 400, ApiResponse.error("Failed to update PIN."));
        }
    }

    private void handleGetTransactions(HttpExchange exchange) throws Exception {
        Map<String, String> query = parseQueryParams(exchange.getRequestURI().getQuery());
        String accNum = query.get("accountNumber");
        if (accNum == null || accNum.trim().isEmpty()) {
            throw new IllegalArgumentException("accountNumber parameter is required.");
        }

        int limit = query.containsKey("limit") ? Integer.parseInt(query.get("limit")) : 50;
        List<Transaction> list = transactionDAO.getTransactionsByAccount(accNum, limit);
        sendJson(exchange, 200, ApiResponse.ok("Transactions fetched", list));
    }

    // ==========================================
    // Admin Controllers
    // ==========================================

    private void handleAdminLogin(HttpExchange exchange) throws Exception {
        JsonObject req = parseJsonRequest(exchange);
        String username = getRequiredString(req, "username");
        String password = getRequiredString(req, "password");

        Admin admin = adminDAO.login(username, password);
        if (admin == null) {
            sendJson(exchange, 401, ApiResponse.error("Invalid admin credentials."));
            return;
        }

        sendJson(exchange, 200, ApiResponse.ok("Admin authentication successful!", admin));
    }

    private void handleAdminGetAccounts(HttpExchange exchange) throws Exception {
        List<Account> accounts = accountDAO.getAllAccounts();
        sendJson(exchange, 200, ApiResponse.ok("All accounts retrieved", accounts));
    }

    private void handleAdminGetTransactions(HttpExchange exchange) throws Exception {
        Map<String, String> query = parseQueryParams(exchange.getRequestURI().getQuery());
        int limit = query.containsKey("limit") ? Integer.parseInt(query.get("limit")) : 100;
        List<Transaction> transactions = transactionDAO.getAllTransactions(limit);
        sendJson(exchange, 200, ApiResponse.ok("All transactions retrieved", transactions));
    }

    private void handleAdminGetStats(HttpExchange exchange) throws Exception {
        Map<String, Object> stats = accountDAO.getBankStatistics();
        sendJson(exchange, 200, ApiResponse.ok("Bank stats retrieved", stats));
    }

    private void handleAdminUpdateStatus(HttpExchange exchange) throws Exception {
        JsonObject req = parseJsonRequest(exchange);
        String accountNumber = getRequiredString(req, "accountNumber");
        String status = getRequiredString(req, "status");

        boolean ok = accountDAO.updateAccountStatus(accountNumber, status);
        if (ok) {
            sendJson(exchange, 200, ApiResponse.ok("Account #" + accountNumber + " status updated to " + status.toUpperCase()));
        } else {
            sendJson(exchange, 400, ApiResponse.error("Failed to update account status."));
        }
    }

    // ==========================================
    // Helpers
    // ==========================================

    private JsonObject parseJsonRequest(HttpExchange exchange) throws IOException {
        try (InputStreamReader isr = new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8);
             BufferedReader br = new BufferedReader(isr)) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
            if (sb.length() == 0) return new JsonObject();
            return JsonParser.parseString(sb.toString()).getAsJsonObject();
        }
    }

    private String getRequiredString(JsonObject obj, String field) {
        if (!obj.has(field) || obj.get(field).isJsonNull() || obj.get(field).getAsString().trim().isEmpty()) {
            throw new IllegalArgumentException("Missing required parameter: '" + field + "'");
        }
        return obj.get(field).getAsString().trim();
    }

    private Map<String, String> parseQueryParams(String query) {
        Map<String, String> map = new HashMap<>();
        if (query == null || query.trim().isEmpty()) return map;

        String[] pairs = query.split("&");
        for (String pair : pairs) {
            String[] kv = pair.split("=");
            if (kv.length == 2) {
                map.put(URLDecoder.decode(kv[0], StandardCharsets.UTF_8),
                        URLDecoder.decode(kv[1], StandardCharsets.UTF_8));
            } else if (kv.length == 1) {
                map.put(URLDecoder.decode(kv[0], StandardCharsets.UTF_8), "");
            }
        }
        return map;
    }

    private void sendJson(HttpExchange exchange, int statusCode, ApiResponse response) throws IOException {
        String json = gson.toJson(response);
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private void methodNotAllowed(HttpExchange exchange) throws IOException {
        sendJson(exchange, 405, ApiResponse.error("HTTP method not allowed for this endpoint."));
    }
}
