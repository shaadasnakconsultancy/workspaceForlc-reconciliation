package com.smipl.lcrecon.dao;

import com.smipl.lcrecon.util.DbUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;

public class SettingsDao extends BaseDao {

    public Map<String, String> getSettingsByGroup(String group) {
        Map<String, String> settings = new LinkedHashMap<>();
        String sql = "SELECT setting_key, setting_value FROM app_settings WHERE setting_group = ? ORDER BY setting_key";
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, group);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    settings.put(rs.getString("setting_key"), rs.getString("setting_value"));
                }
            }
        } catch (SQLException e) {
            logger.error("Error getting settings for group: {}", group, e);
        }
        return settings;
    }

    public String getSetting(String key) {
        String sql = "SELECT setting_value FROM app_settings WHERE setting_key = ?";
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, key);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("setting_value");
                }
            }
        } catch (SQLException e) {
            logger.error("Error getting setting: {}", key, e);
        }
        return null;
    }

    public void saveSetting(String key, String value, String group) {
        String sql = "MERGE INTO app_settings AS target " +
                     "USING (SELECT ? AS setting_key) AS source " +
                     "ON target.setting_key = source.setting_key " +
                     "WHEN MATCHED THEN UPDATE SET setting_value = ?, updated_at = GETDATE() " +
                     "WHEN NOT MATCHED THEN INSERT (setting_key, setting_value, setting_group) VALUES (?, ?, ?);";
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, key);
            ps.setString(2, value);
            ps.setString(3, key);
            ps.setString(4, value);
            ps.setString(5, group);
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.error("Error saving setting: {} = {}", key, value, e);
            throw new RuntimeException("Failed to save setting", e);
        }
    }

    public void saveSettings(Map<String, String> settings, String group) {
        String sql = "MERGE INTO app_settings AS target " +
                     "USING (SELECT ? AS setting_key) AS source " +
                     "ON target.setting_key = source.setting_key " +
                     "WHEN MATCHED THEN UPDATE SET setting_value = ?, updated_at = GETDATE() " +
                     "WHEN NOT MATCHED THEN INSERT (setting_key, setting_value, setting_group) VALUES (?, ?, ?);";
        Connection conn = null;
        try {
            conn = DbUtil.getConnection();
            conn.setAutoCommit(false);
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                for (Map.Entry<String, String> entry : settings.entrySet()) {
                    ps.setString(1, entry.getKey());
                    ps.setString(2, entry.getValue());
                    ps.setString(3, entry.getKey());
                    ps.setString(4, entry.getValue());
                    ps.setString(5, group);
                    ps.addBatch();
                }
                ps.executeBatch();
            }
            conn.commit();
            logger.info("Saved {} settings for group: {}", settings.size(), group);
        } catch (SQLException e) {
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException re) { logger.warn("Rollback failed", re); }
            }
            logger.error("Error saving settings for group: {}", group, e);
            throw new RuntimeException("Failed to save settings", e);
        } finally {
            if (conn != null) {
                try { conn.setAutoCommit(true); } catch (SQLException e) { logger.warn("Error resetting auto-commit", e); }
                closeQuietly(conn);
            }
        }
    }
}
