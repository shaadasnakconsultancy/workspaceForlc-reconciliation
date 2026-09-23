package com.smipl.lcrecon.dao;

import com.smipl.lcrecon.model.PromptTemplate;
import com.smipl.lcrecon.util.DbUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class PromptTemplateDao extends BaseDao {

    private static final String SELECT_COLS =
            "pt.id, pt.document_type_id, dt.type_name AS document_type_name, " +
            "pt.prompt_name, pt.prompt_text, pt.response_schema, pt.is_active, pt.version, pt.created_at, pt.updated_at";

    public List<PromptTemplate> findAll() {
        List<PromptTemplate> list = new ArrayList<>();
        String sql = "SELECT " + SELECT_COLS + " FROM prompt_templates pt " +
                     "INNER JOIN document_types dt ON pt.document_type_id = dt.id " +
                     "ORDER BY dt.display_order, pt.version DESC";
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) {
            logger.error("Error listing prompt templates", e);
        }
        return list;
    }

    public List<PromptTemplate> findByDocumentTypeId(long docTypeId) {
        List<PromptTemplate> list = new ArrayList<>();
        String sql = "SELECT " + SELECT_COLS + " FROM prompt_templates pt " +
                     "INNER JOIN document_types dt ON pt.document_type_id = dt.id " +
                     "WHERE pt.document_type_id = ? ORDER BY pt.version DESC";
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, docTypeId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            logger.error("Error finding prompt templates for doc type: {}", docTypeId, e);
        }
        return list;
    }

    public PromptTemplate findActiveByDocumentTypeId(long docTypeId) {
        String sql = "SELECT " + SELECT_COLS + " FROM prompt_templates pt " +
                     "INNER JOIN document_types dt ON pt.document_type_id = dt.id " +
                     "WHERE pt.document_type_id = ? AND pt.is_active = 1";
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, docTypeId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapRow(rs);
            }
        } catch (SQLException e) {
            logger.error("Error finding active prompt for doc type id: {}", docTypeId, e);
        }
        return null;
    }

    public PromptTemplate findActiveByDocumentTypeCode(String code) {
        String sql = "SELECT " + SELECT_COLS + " FROM prompt_templates pt " +
                     "INNER JOIN document_types dt ON pt.document_type_id = dt.id " +
                     "WHERE dt.type_code = ? AND pt.is_active = 1";
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, code);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapRow(rs);
            }
        } catch (SQLException e) {
            logger.error("Error finding active prompt for doc type code: {}", code, e);
        }
        return null;
    }

    public void create(PromptTemplate pt) {
        String sql = "INSERT INTO prompt_templates (document_type_id, prompt_name, prompt_text, response_schema, is_active, version) " +
                     "VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, pt.getDocumentTypeId());
            ps.setString(2, pt.getPromptName());
            ps.setString(3, pt.getPromptText());
            ps.setString(4, pt.getResponseSchema());
            ps.setBoolean(5, pt.isActive());
            ps.setInt(6, pt.getVersion());
            ps.executeUpdate();
            logger.info("Prompt template created: {} v{}", pt.getPromptName(), pt.getVersion());
        } catch (SQLException e) {
            logger.error("Error creating prompt template: {}", pt.getPromptName(), e);
            throw new RuntimeException("Failed to create prompt template", e);
        }
    }

    public void update(PromptTemplate pt) {
        String sql = "UPDATE prompt_templates SET prompt_name = ?, prompt_text = ?, response_schema = ?, " +
                     "updated_at = GETDATE() WHERE id = ?";
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, pt.getPromptName());
            ps.setString(2, pt.getPromptText());
            ps.setString(3, pt.getResponseSchema());
            ps.setLong(4, pt.getId());
            ps.executeUpdate();
            logger.info("Prompt template updated: {}", pt.getPromptName());
        } catch (SQLException e) {
            logger.error("Error updating prompt template: {}", pt.getPromptName(), e);
            throw new RuntimeException("Failed to update prompt template", e);
        }
    }

    public void activate(long id) {
        String sql = "UPDATE prompt_templates SET is_active = 1, updated_at = GETDATE() WHERE id = ?";
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            ps.executeUpdate();
            logger.info("Prompt activated: id={}", id);
        } catch (SQLException e) {
            logger.error("Error activating prompt id: {}", id, e);
            throw new RuntimeException("Failed to activate prompt", e);
        }
    }

    public void deactivateById(long id) {
        String sql = "UPDATE prompt_templates SET is_active = 0, updated_at = GETDATE() WHERE id = ?";
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            ps.executeUpdate();
            logger.info("Prompt deactivated: id={}", id);
        } catch (SQLException e) {
            logger.error("Error deactivating prompt id: {}", id, e);
            throw new RuntimeException("Failed to deactivate prompt", e);
        }
    }

    public void delete(long id) {
        String sql = "DELETE FROM prompt_templates WHERE id = ?";
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            ps.executeUpdate();
            logger.info("Prompt deleted: id={}", id);
        } catch (SQLException e) {
            logger.error("Error deleting prompt id: {}", id, e);
            throw new RuntimeException("Failed to delete prompt", e);
        }
    }

    public void deactivateAllForDocType(long docTypeId) {
        String sql = "UPDATE prompt_templates SET is_active = 0, updated_at = GETDATE() WHERE document_type_id = ?";
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, docTypeId);
            int count = ps.executeUpdate();
            logger.info("Deactivated {} prompt templates for doc type: {}", count, docTypeId);
        } catch (SQLException e) {
            logger.error("Error deactivating prompts for doc type: {}", docTypeId, e);
            throw new RuntimeException("Failed to deactivate prompt templates", e);
        }
    }

    private PromptTemplate mapRow(ResultSet rs) throws SQLException {
        PromptTemplate pt = new PromptTemplate();
        pt.setId(rs.getLong("id"));
        pt.setDocumentTypeId(rs.getLong("document_type_id"));
        pt.setDocumentTypeName(rs.getString("document_type_name"));
        pt.setPromptName(rs.getString("prompt_name"));
        pt.setPromptText(rs.getString("prompt_text"));
        pt.setResponseSchema(rs.getString("response_schema"));
        pt.setActive(rs.getBoolean("is_active"));
        pt.setVersion(rs.getInt("version"));
        pt.setCreatedAt(rs.getTimestamp("created_at"));
        pt.setUpdatedAt(rs.getTimestamp("updated_at"));
        return pt;
    }
}
