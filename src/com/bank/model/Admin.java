package com.bank.model;

import java.sql.Timestamp;

/**
 * Model representing an administrative user.
 */
public class Admin {
    private int id;
    private String username;
    private transient String password;
    private String fullName;
    private Timestamp createdAt;

    public Admin() {}

    public Admin(int id, String username, String fullName, Timestamp createdAt) {
        this.id = id;
        this.username = username;
        this.fullName = fullName;
        this.createdAt = createdAt;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
}
