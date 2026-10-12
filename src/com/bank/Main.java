package com.bank;

import com.bank.config.DatabaseConfig;
import com.bank.server.ApiHandler;
import com.bank.server.StaticFileHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

/**
 * Main application entry point for Bank Management System.
 * Starts built-in Java HTTP Server and initializes REST endpoints.
 */
public class Main {
    private static final int PORT = 8080;

    public static void main(String[] args) {
        System.out.println("============================================================");
        System.out.println("   🏦  STARTING BANK MANAGEMENT SYSTEM APPLICATION");
        System.out.println("============================================================");

        // 1. Verify Database Connection
        System.out.print("🔍 Testing MySQL connection... ");
        if (DatabaseConfig.testConnection()) {
            System.out.println("✅ CONNECTED to database successfully!");
        } else {
            System.out.println("⚠️  WARNING: Could not connect to MySQL database.");
            System.out.println("   Please check your db.properties settings and verify that");
            System.out.println("   MySQL service is running and 'bank_db' is imported.");
        }

        // 2. Start HTTP Server
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);

            // API handler for /api/*
            server.createContext("/api", new ApiHandler());

            // Static file handler for HTML/CSS/JS frontend
            server.createContext("/", new StaticFileHandler("web/css"));

            // Use multi-threaded executor for handling concurrent client requests
            server.setExecutor(Executors.newFixedThreadPool(12));
            server.start();

            System.out.println("------------------------------------------------------------");
            System.out.println("🚀 Server running smoothly at:");
            System.out.println("   👉 Main Landing Page:   http://localhost:" + PORT);
            System.out.println("   👉 Customer Dashboard:  http://localhost:" + PORT + "/dashboard.html");
            System.out.println("   👉 Admin Portal:        http://localhost:" + PORT + "/admin.html");
            System.out.println("------------------------------------------------------------");
            System.out.println("💡 Press Ctrl+C in terminal anytime to stop the server.");
            System.out.println("============================================================");

        } catch (IOException e) {
            System.err.println("❌ Failed to start server on port " + PORT + ": " + e.getMessage());
            e.printStackTrace();
        }
    }
}
