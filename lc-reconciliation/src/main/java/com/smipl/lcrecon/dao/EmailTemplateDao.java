package com.smipl.lcrecon.dao;

import com.smipl.lcrecon.model.EmailTemplate;
import com.smipl.lcrecon.util.DbUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class EmailTemplateDao extends BaseDao {

    private static final String SELECT_COLUMNS =
            "id, template_name, subject_template, body_template, is_default, is_active, created_at, updated_at";

    public List<EmailTemplate> findAll() {
        List<EmailTemplate> list = new ArrayList<>();
        String sql = "SELECT " + SELECT_COLUMNS + " FROM email_templates ORDER BY template_name";
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            logger.error("Error listing email templates", e);
        }
        return list;
    }

    public EmailTemplate findById(long id) {
        String sql = "SELECT " + SELECT_COLUMNS + " FROM email_templates WHERE id = ?";
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        } catch (SQLException e) {
            logger.error("Error finding email template by id: {}", id, e);
        }
        return null;
    }

    /**
     * Returns the template where is_default = 1.
     */
    public EmailTemplate findDefault() {
        String sql = "SELECT " + SELECT_COLUMNS + " FROM email_templates WHERE is_default = 1";
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return mapRow(rs);
            }
        } catch (SQLException e) {
            logger.error("Error finding default email template", e);
        }
        return null;
    }

    public void create(EmailTemplate template) {
        String sql = "INSERT INTO email_templates (template_name, subject_template, body_template, is_default, is_active) " +
                     "VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, template.getTemplateName());
            ps.setString(2, template.getSubjectTemplate());
            ps.setString(3, template.getBodyTemplate());
            ps.setBoolean(4, template.isDefaultTemplate());
            ps.setBoolean(5, template.isActive());
            ps.executeUpdate();
            logger.info("Email template created: {}", template.getTemplateName());
        } catch (SQLException e) {
            logger.error("Error creating email template: {}", template.getTemplateName(), e);
            throw new RuntimeException("Failed to create email template", e);
        }
    }

    public void update(EmailTemplate template) {
        String sql = "UPDATE email_templates SET template_name = ?, subject_template = ?, body_template = ?, " +
                     "is_default = ?, is_active = ?, updated_at = GETDATE() WHERE id = ?";
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, template.getTemplateName());
            ps.setString(2, template.getSubjectTemplate());
            ps.setString(3, template.getBodyTemplate());
            ps.setBoolean(4, template.isDefaultTemplate());
            ps.setBoolean(5, template.isActive());
            ps.setLong(6, template.getId());
            ps.executeUpdate();
            logger.info("Email template updated: {}", template.getTemplateName());
        } catch (SQLException e) {
            logger.error("Error updating email template: {}", template.getTemplateName(), e);
            throw new RuntimeException("Failed to update email template", e);
        }
    }

    public void delete(long id) {
        String sql = "DELETE FROM email_templates WHERE id = ?";
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            ps.executeUpdate();
            logger.info("Email template deleted: id={}", id);
        } catch (SQLException e) {
            logger.error("Error deleting email template: id={}", id, e);
            throw new RuntimeException("Failed to delete email template", e);
        }
    }

    /**
     * Sets the specified template as default (is_default=1) and clears default from all others.
     */
    public void setDefault(long id) {
        String clearSql = "UPDATE email_templates SET is_default = 0, updated_at = GETDATE() WHERE is_default = 1";
        String setSql = "UPDATE email_templates SET is_default = 1, updated_at = GETDATE() WHERE id = ?";
        try (Connection conn = DbUtil.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement ps1 = conn.prepareStatement(clearSql)) {
                ps1.executeUpdate();
            }
            try (PreparedStatement ps2 = conn.prepareStatement(setSql)) {
                ps2.setLong(1, id);
                ps2.executeUpdate();
            }
            conn.commit();
            logger.info("Email template set as default: id={}", id);
        } catch (SQLException e) {
            logger.error("Error setting default email template: id={}", id, e);
            throw new RuntimeException("Failed to set default email template", e);
        }
    }

    private EmailTemplate mapRow(ResultSet rs) throws SQLException {
        EmailTemplate t = new EmailTemplate();
        t.setId(rs.getLong("id"));
        t.setTemplateName(rs.getString("template_name"));
        t.setSubjectTemplate(rs.getString("subject_template"));
        t.setBodyTemplate(rs.getString("body_template"));
        t.setDefaultTemplate(rs.getBoolean("is_default"));
        t.setActive(rs.getBoolean("is_active"));
        t.setCreatedAt(rs.getTimestamp("created_at"));
        t.setUpdatedAt(rs.getTimestamp("updated_at"));
        return t;
    }
}
