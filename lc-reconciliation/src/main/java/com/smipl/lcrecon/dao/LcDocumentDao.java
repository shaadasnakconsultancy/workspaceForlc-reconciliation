package com.smipl.lcrecon.dao;

import com.smipl.lcrecon.model.LcDocument;
import com.smipl.lcrecon.model.SupportingDocument;
import com.smipl.lcrecon.util.DbUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class LcDocumentDao extends BaseDao {

    public void create(LcDocument doc) {
        String sql = "INSERT INTO lc_documents (lc_number, file_name, file_path, file_size, " +
                     "is_addendum, upload_batch_id, ocr_status) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?); SELECT SCOPE_IDENTITY();";
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, doc.getLcNumber());
            ps.setString(2, doc.getFileName());
            ps.setString(3, doc.getFilePath());
            ps.setLong(4, doc.getFileSize());
            ps.setBoolean(5, doc.isAddendum());
            ps.setString(6, doc.getUploadBatchId());
            ps.setString(7, doc.getOcrStatus() != null ? doc.getOcrStatus() : "PENDING");
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    doc.setId(rs.getLong(1));
                }
            }
            logger.info("LC document created: id={}, file={}", doc.getId(), doc.getFileName());
        } catch (SQLException e) {
            logger.error("Error creating LC document: {}", doc.getFileName(), e);
            throw new RuntimeException("Failed to create LC document", e);
        }
    }

    public List<LcDocument> findByBatchId(String batchId) {
        List<LcDocument> list = new ArrayList<>();
        String sql = "SELECT id, lc_number, file_name, file_path, file_size, is_addendum, " +
                     "upload_batch_id, ocr_text, ocr_status, extracted_json, created_at, updated_at " +
                     "FROM lc_documents WHERE upload_batch_id = ? ORDER BY is_addendum, created_at";
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, batchId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapLcDocRow(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Error finding LC documents for batch: {}", batchId, e);
        }
        return list;
    }

    public LcDocument findById(long id) {
        String sql = "SELECT id, lc_number, file_name, file_path, file_size, is_addendum, " +
                     "upload_batch_id, ocr_text, ocr_status, extracted_json, created_at, updated_at " +
                     "FROM lc_documents WHERE id = ?";
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapLcDocRow(rs);
            }
        } catch (SQLException e) {
            logger.error("Error finding LC document by id: {}", id, e);
        }
        return null;
    }

    public SupportingDocument findSupportingById(long id) {
        String sql = "SELECT sd.id, sd.upload_batch_id, sd.document_type_id, " +
                     "dt.type_name AS document_type_name, dt.type_code AS document_type_code, " +
                     "sd.file_name, sd.file_path, sd.file_size, sd.page_limit, " +
                     "sd.ocr_text, sd.ocr_status, sd.extracted_json, sd.created_at " +
                     "FROM supporting_documents sd " +
                     "INNER JOIN document_types dt ON sd.document_type_id = dt.id " +
                     "WHERE sd.id = ?";
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapSupportingDocRow(rs);
            }
        } catch (SQLException e) {
            logger.error("Error finding supporting document by id: {}", id, e);
        }
        return null;
    }

    public void updateOcrStatus(long id, String status, String ocrText) {
        String sql = "UPDATE lc_documents SET ocr_status = ?, ocr_text = ?, updated_at = GETDATE() WHERE id = ?";
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setString(2, ocrText);
            ps.setLong(3, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.error("Error updating OCR status for LC document: id={}", id, e);
            throw new RuntimeException("Failed to update OCR status", e);
        }
    }

    public void updateExtractedJson(long id, String json) {
        String sql = "UPDATE lc_documents SET extracted_json = ?, updated_at = GETDATE() WHERE id = ?";
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, json);
            ps.setLong(2, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.error("Error updating extracted JSON for LC document: id={}", id, e);
            throw new RuntimeException("Failed to update extracted JSON", e);
        }
    }

    // --- Supporting Documents ---

    public void createSupportingDoc(SupportingDocument doc) {
        String sql = "INSERT INTO supporting_documents (upload_batch_id, document_type_id, file_name, " +
                     "file_path, file_size, page_limit, ocr_status) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?); SELECT SCOPE_IDENTITY();";
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, doc.getUploadBatchId());
            ps.setLong(2, doc.getDocumentTypeId());
            ps.setString(3, doc.getFileName());
            ps.setString(4, doc.getFilePath());
            ps.setLong(5, doc.getFileSize());
            ps.setInt(6, doc.getPageLimit());
            ps.setString(7, doc.getOcrStatus() != null ? doc.getOcrStatus() : "PENDING");
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    doc.setId(rs.getLong(1));
                }
            }
            logger.info("Supporting document created: id={}, file={}", doc.getId(), doc.getFileName());
        } catch (SQLException e) {
            logger.error("Error creating supporting document: {}", doc.getFileName(), e);
            throw new RuntimeException("Failed to create supporting document", e);
        }
    }

    public List<SupportingDocument> findSupportingByBatchId(String batchId) {
        List<SupportingDocument> list = new ArrayList<>();
        String sql = "SELECT sd.id, sd.upload_batch_id, sd.document_type_id, " +
                     "dt.type_name AS document_type_name, dt.type_code AS document_type_code, " +
                     "sd.file_name, sd.file_path, sd.file_size, sd.page_limit, " +
                     "sd.ocr_text, sd.ocr_status, sd.extracted_json, sd.created_at " +
                     "FROM supporting_documents sd " +
                     "INNER JOIN document_types dt ON sd.document_type_id = dt.id " +
                     "WHERE sd.upload_batch_id = ? ORDER BY dt.display_order, sd.created_at";
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, batchId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapSupportingDocRow(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Error finding supporting documents for batch: {}", batchId, e);
        }
        return list;
    }

    public void updateSupportingOcrStatus(long id, String status, String ocrText) {
        String sql = "UPDATE supporting_documents SET ocr_status = ?, ocr_text = ? WHERE id = ?";
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setString(2, ocrText);
            ps.setLong(3, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.error("Error updating OCR status for supporting document: id={}", id, e);
            throw new RuntimeException("Failed to update supporting OCR status", e);
        }
    }

    public void updateSupportingExtractedJson(long id, String json) {
        String sql = "UPDATE supporting_documents SET extracted_json = ? WHERE id = ?";
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, json);
            ps.setLong(2, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.error("Error updating extracted JSON for supporting document: id={}", id, e);
            throw new RuntimeException("Failed to update supporting extracted JSON", e);
        }
    }

    private LcDocument mapLcDocRow(ResultSet rs) throws SQLException {
        LcDocument doc = new LcDocument();
        doc.setId(rs.getLong("id"));
        doc.setLcNumber(rs.getString("lc_number"));
        doc.setFileName(rs.getString("file_name"));
        doc.setFilePath(rs.getString("file_path"));
        doc.setFileSize(rs.getLong("file_size"));
        doc.setAddendum(rs.getBoolean("is_addendum"));
        doc.setUploadBatchId(rs.getString("upload_batch_id"));
        doc.setOcrText(rs.getString("ocr_text"));
        doc.setOcrStatus(rs.getString("ocr_status"));
        doc.setExtractedJson(rs.getString("extracted_json"));
        doc.setCreatedAt(rs.getTimestamp("created_at"));
        doc.setUpdatedAt(rs.getTimestamp("updated_at"));
        return doc;
    }

    private SupportingDocument mapSupportingDocRow(ResultSet rs) throws SQLException {
        SupportingDocument doc = new SupportingDocument();
        doc.setId(rs.getLong("id"));
        doc.setUploadBatchId(rs.getString("upload_batch_id"));
        doc.setDocumentTypeId(rs.getLong("document_type_id"));
        doc.setDocumentTypeName(rs.getString("document_type_name"));
        doc.setDocumentTypeCode(rs.getString("document_type_code"));
        doc.setFileName(rs.getString("file_name"));
        doc.setFilePath(rs.getString("file_path"));
        doc.setFileSize(rs.getLong("file_size"));
        doc.setPageLimit(rs.getInt("page_limit"));
        doc.setOcrText(rs.getString("ocr_text"));
        doc.setOcrStatus(rs.getString("ocr_status"));
        doc.setExtractedJson(rs.getString("extracted_json"));
        doc.setCreatedAt(rs.getTimestamp("created_at"));
        return doc;
    }
}
