package com.smipl.lcrecon.dao;

import com.smipl.lcrecon.model.DocumentType;
import com.smipl.lcrecon.util.DbUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class DocumentTypeDao extends BaseDao {

    private static final String SELECT_COLUMNS =
            "id, type_code, type_name, page_limit, is_active, display_order, created_at, updated_at";

    public List<DocumentType> findAll() {
        List<DocumentType> list = new ArrayList<>();
        String sql = "SELECT " + SELECT_COLUMNS + " FROM document_types ORDER BY display_order, type_name";
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            logger.error("Error listing document types", e);
        }
        return list;
    }

    public List<DocumentType> findActive() {
        List<DocumentType> list = new ArrayList<>();
        String sql = "SELECT " + SELECT_COLUMNS + " FROM document_types WHERE is_active = 1 ORDER BY display_order, type_name";
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            logger.error("Error listing active document types", e);
        }
        return list;
    }

    public DocumentType findById(long id) {
        String sql = "SELECT " + SELECT_COLUMNS + " FROM document_types WHERE id = ?";
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        } catch (SQLException e) {
            logger.error("Error finding document type by id: {}", id, e);
        }
        return null;
    }

    public DocumentType findByCode(String code) {
        String sql = "SELECT " + SELECT_COLUMNS + " FROM document_types WHERE type_code = ?";
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, code);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        } catch (SQLException e) {
            logger.error("Error finding document type by code: {}", code, e);
        }
        return null;
    }

    public void create(DocumentType dt) {
        String sql = "INSERT INTO document_types (type_code, type_name, page_limit, is_active, display_order) " +
                     "VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, dt.getTypeCode());
            ps.setString(2, dt.getTypeName());
            ps.setInt(3, dt.getPageLimit());
            ps.setBoolean(4, dt.isActive());
            ps.setInt(5, dt.getDisplayOrder());
            ps.executeUpdate();
            logger.info("Document type created: {}", dt.getTypeCode());
        } catch (SQLException e) {
            logger.error("Error creating document type: {}", dt.getTypeCode(), e);
            throw new RuntimeException("Failed to create document type", e);
        }
    }

    public void update(DocumentType dt) {
        String sql = "UPDATE document_types SET type_code = ?, type_name = ?, page_limit = ?, " +
                     "is_active = ?, display_order = ?, updated_at = GETDATE() WHERE id = ?";
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, dt.getTypeCode());
            ps.setString(2, dt.getTypeName());
            ps.setInt(3, dt.getPageLimit());
            ps.setBoolean(4, dt.isActive());
            ps.setInt(5, dt.getDisplayOrder());
            ps.setLong(6, dt.getId());
            ps.executeUpdate();
            logger.info("Document type updated: {}", dt.getTypeCode());
        } catch (SQLException e) {
            logger.error("Error updating document type: {}", dt.getTypeCode(), e);
            throw new RuntimeException("Failed to update document type", e);
        }
    }

    public void delete(long id) {
        String sql = "DELETE FROM document_types WHERE id = ?";
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            ps.executeUpdate();
            logger.info("Document type deleted: id={}", id);
        } catch (SQLException e) {
            logger.error("Error deleting document type: id={}", id, e);
            throw new RuntimeException("Failed to delete document type", e);
        }
    }

    private DocumentType mapRow(ResultSet rs) throws SQLException {
        DocumentType dt = new DocumentType();
        dt.setId(rs.getLong("id"));
        dt.setTypeCode(rs.getString("type_code"));
        dt.setTypeName(rs.getString("type_name"));
        dt.setPageLimit(rs.getInt("page_limit"));
        dt.setActive(rs.getBoolean("is_active"));
        dt.setDisplayOrder(rs.getInt("display_order"));
        dt.setCreatedAt(rs.getTimestamp("created_at"));
        dt.setUpdatedAt(rs.getTimestamp("updated_at"));
        return dt;
    }
}
