package com.bank.dao;

import com.bank.config.DatabaseConfig;
import com.bank.model.Admin;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Data Access Object (DAO) for Bank Administrators.
 */
public class AdminDAO {

    /**
     * Authenticates an admin user.
     */
    public Admin login(String username, String password) throws SQLException {
        String sql = "SELECT id, username, full_name, created_at FROM admins WHERE username = ? AND password = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username.trim());
            ps.setString(2, password.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new Admin(
                        rs.getInt("id"),
                        rs.getString("username"),
                        rs.getString("full_name"),
                        rs.getTimestamp("created_at")
                    );
                }
            }
        }
        return null;
    }
}
