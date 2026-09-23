package com.smipl.lcrecon.dao;

import com.smipl.lcrecon.model.User;
import com.smipl.lcrecon.util.DbUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class UserDao extends BaseDao {

    private static final String SELECT_COLS =
            "id, username, password_hash, first_name, last_name, email, department, role, is_active, created_at, updated_at";

    public User findByUsername(String username) {
        String sql = "SELECT " + SELECT_COLS + " FROM users WHERE username = ?";
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapRow(rs);
            }
        } catch (SQLException e) {
            logger.error("Error finding user by username: {}", username, e);
        }
        return null;
    }

    public User findById(long id) {
        String sql = "SELECT " + SELECT_COLS + " FROM users WHERE id = ?";
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapRow(rs);
            }
        } catch (SQLException e) {
            logger.error("Error finding user by id: {}", id, e);
        }
        return null;
    }

    public List<User> findAll() {
        List<User> users = new ArrayList<>();
        String sql = "SELECT " + SELECT_COLS + " FROM users ORDER BY first_name, last_name";
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) users.add(mapRow(rs));
        } catch (SQLException e) {
            logger.error("Error listing users", e);
        }
        return users;
    }

    public List<User> findByDepartment(String department) {
        List<User> users = new ArrayList<>();
        String sql = "SELECT " + SELECT_COLS + " FROM users WHERE department = ? ORDER BY first_name, last_name";
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, department);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) users.add(mapRow(rs));
            }
        } catch (SQLException e) {
            logger.error("Error finding users by department: {}", department, e);
        }
        return users;
    }

    public void create(User user) {
        String sql = "INSERT INTO users (username, password_hash, first_name, last_name, email, department, role, is_active) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, user.getUsername());
            ps.setString(2, user.getPasswordHash());
            ps.setString(3, user.getFirstName());
            ps.setString(4, user.getLastName());
            ps.setString(5, user.getEmail());
            ps.setString(6, user.getDepartment());
            ps.setString(7, user.getRole());
            ps.setBoolean(8, user.isActive());
            ps.executeUpdate();
            logger.info("User created: {}", user.getUsername());
        } catch (SQLException e) {
            logger.error("Error creating user: {}", user.getUsername(), e);
            throw new RuntimeException("Failed to create user", e);
        }
    }

    public void update(User user) {
        String sql = "UPDATE users SET first_name = ?, last_name = ?, email = ?, department = ?, " +
                     "role = ?, is_active = ?, updated_at = GETDATE() WHERE id = ?";
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, user.getFirstName());
            ps.setString(2, user.getLastName());
            ps.setString(3, user.getEmail());
            ps.setString(4, user.getDepartment());
            ps.setString(5, user.getRole());
            ps.setBoolean(6, user.isActive());
            ps.setLong(7, user.getId());
            ps.executeUpdate();
            logger.info("User updated: {} (id={})", user.getUsername(), user.getId());
        } catch (SQLException e) {
            logger.error("Error updating user id: {}", user.getId(), e);
            throw new RuntimeException("Failed to update user", e);
        }
    }

    public void delete(long id) {
        String sql = "DELETE FROM users WHERE id = ?";
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            ps.executeUpdate();
            logger.info("User deleted: id={}", id);
        } catch (SQLException e) {
            logger.error("Error deleting user id: {}", id, e);
            throw new RuntimeException("Failed to delete user", e);
        }
    }

    public void updatePassword(long userId, String passwordHash) {
        String sql = "UPDATE users SET password_hash = ?, updated_at = GETDATE() WHERE id = ?";
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, passwordHash);
            ps.setLong(2, userId);
            ps.executeUpdate();
            logger.info("Password updated for user id: {}", userId);
        } catch (SQLException e) {
            logger.error("Error updating password for user id: {}", userId, e);
            throw new RuntimeException("Failed to update password", e);
        }
    }

    private User mapRow(ResultSet rs) throws SQLException {
        User user = new User();
        user.setId(rs.getLong("id"));
        user.setUsername(rs.getString("username"));
        user.setPasswordHash(rs.getString("password_hash"));
        user.setFirstName(rs.getString("first_name"));
        user.setLastName(rs.getString("last_name"));
        user.setEmail(rs.getString("email"));
        user.setDepartment(rs.getString("department"));
        user.setRole(rs.getString("role"));
        user.setActive(rs.getBoolean("is_active"));
        user.setCreatedAt(rs.getTimestamp("created_at"));
        user.setUpdatedAt(rs.getTimestamp("updated_at"));
        return user;
    }
}
