package com.smipl.lcrecon.dao;

import com.smipl.lcrecon.model.ReconciliationJob;
import com.smipl.lcrecon.model.ReconciliationResult;
import com.smipl.lcrecon.util.DbUtil;
import com.smipl.lcrecon.util.JsonUtil;

import java.sql.*;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

public class JobDao extends BaseDao {

    private static final TimeZone UTC = TimeZone.getTimeZone("UTC");

    /**
     * Read a timestamp column interpreting the stored value as UTC.
     * The DB server stores timestamps in UTC (GETDATE() == GETUTCDATE()); reading with a
     * UTC calendar yields the correct absolute instant, which then renders in the app's
     * display timezone (IST) correctly. Avoids a JVM-wide timezone change on the shared Tomcat.
     */
    private static Timestamp getUtcTimestamp(ResultSet rs, String col) throws SQLException {
        return rs.getTimestamp(col, Calendar.getInstance(UTC));
    }

    public long create(ReconciliationJob job) {
        String sql = "INSERT INTO reconciliation_jobs (upload_batch_id, lc_number, status, total_steps, " +
                     "completed_steps, current_step, notification_email, started_at, created_by, created_by_department, " +
                     "job_name, shipment_doc_name, email_group_id) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, GETDATE(), ?, ?, ?, ?, ?); SELECT SCOPE_IDENTITY();";
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, job.getUploadBatchId());
            ps.setString(2, job.getLcNumber());
            ps.setString(3, job.getStatus());
            ps.setInt(4, job.getTotalSteps());
            ps.setInt(5, job.getCompletedSteps());
            ps.setString(6, job.getCurrentStep());
            ps.setString(7, job.getNotificationEmail());
            ps.setString(8, job.getCreatedBy());
            ps.setString(9, job.getCreatedByDepartment());
            ps.setString(10, job.getJobName());
            ps.setString(11, job.getShipmentDocName());
            ps.setLong(12, job.getEmailGroupId());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    long id = rs.getLong(1);
                    job.setId(id);
                    logger.info("Reconciliation job created: id={}, lc={}", id, job.getLcNumber());
                    return id;
                }
            }
        } catch (SQLException e) {
            logger.error("Error creating reconciliation job for LC: {}", job.getLcNumber(), e);
            throw new RuntimeException("Failed to create reconciliation job", e);
        }
        return -1;
    }

    public ReconciliationJob findById(long id) {
        String sql = "SELECT id, upload_batch_id, lc_number, status, total_steps, completed_steps, " +
                     "current_step, error_message, report_path, non_compliance_report_path, email_sent, " +
                     "notification_email, started_at, completed_at, created_at, created_by, created_by_department, " +
                     "job_name, shipment_doc_name, xlsx_report_path, xlsx_nc_report_path, email_group_id, email_error " +
                     "FROM reconciliation_jobs WHERE id = ?";
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapJobRow(rs);
                }
            }
        } catch (SQLException e) {
            logger.error("Error finding job by id: {}", id, e);
        }
        return null;
    }

    private static final String JOB_COLUMNS = "id, upload_batch_id, lc_number, status, total_steps, completed_steps, " +
            "current_step, error_message, report_path, non_compliance_report_path, email_sent, " +
            "notification_email, started_at, completed_at, created_at, created_by, created_by_department, " +
            "job_name, shipment_doc_name, xlsx_report_path, xlsx_nc_report_path, email_group_id, email_error";

    /**
     * Paginated search with filters. Used by Dashboard, Job Monitor, Report lists.
     * @param fromDate  filter start date (yyyy-MM-dd), null to skip
     * @param toDate    filter end date (yyyy-MM-dd), null to skip
     * @param searchJobId filter by job ID, null to skip
     * @param statusFilter filter by status (e.g. "COMPLETED"), null for all
     * @param department department filter for non-admin, null for all
     * @param page      1-based page number
     * @param pageSize  records per page
     */
    public List<ReconciliationJob> findFiltered(String fromDate, String toDate, String searchJobId,
                                                 String statusFilter, String department,
                                                 int page, int pageSize) {
        List<ReconciliationJob> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder("SELECT ").append(JOB_COLUMNS).append(" FROM reconciliation_jobs WHERE 1=1 ");
        List<Object> params = new ArrayList<>();

        appendFilters(sql, params, fromDate, toDate, searchJobId, statusFilter, department);
        sql.append("ORDER BY created_at DESC OFFSET ? ROWS FETCH NEXT ? ROWS ONLY");
        params.add((page - 1) * pageSize);
        params.add(pageSize);

        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            setParams(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapJobRow(rs));
            }
        } catch (SQLException e) {
            logger.error("Error in findFiltered", e);
        }
        return list;
    }

    public int countFiltered(String fromDate, String toDate, String searchJobId,
                              String statusFilter, String department) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM reconciliation_jobs WHERE 1=1 ");
        List<Object> params = new ArrayList<>();
        appendFilters(sql, params, fromDate, toDate, searchJobId, statusFilter, department);

        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            setParams(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            logger.error("Error in countFiltered", e);
        }
        return 0;
    }

    private void appendFilters(StringBuilder sql, List<Object> params,
                                String fromDate, String toDate, String searchJobId,
                                String statusFilter, String department) {
        if (fromDate != null && !fromDate.isEmpty()) {
            sql.append("AND created_at >= ? ");
            params.add(fromDate + " 00:00:00");
        }
        if (toDate != null && !toDate.isEmpty()) {
            sql.append("AND created_at <= ? ");
            params.add(toDate + " 23:59:59");
        }
        if (searchJobId != null && !searchJobId.isEmpty()) {
            try { sql.append("AND id = ? "); params.add(Long.parseLong(searchJobId)); }
            catch (NumberFormatException ignored) {}
        }
        if (statusFilter != null && !statusFilter.isEmpty()) {
            sql.append("AND status = ? ");
            params.add(statusFilter);
        }
        if (department != null && !department.isEmpty()) {
            sql.append("AND created_by_department = ? ");
            params.add(department);
        }
    }

    private void setParams(PreparedStatement ps, List<Object> params) throws SQLException {
        for (int i = 0; i < params.size(); i++) {
            Object p = params.get(i);
            if (p instanceof Long) ps.setLong(i + 1, (Long) p);
            else if (p instanceof Integer) ps.setInt(i + 1, (Integer) p);
            else ps.setString(i + 1, p.toString());
        }
    }

    public List<ReconciliationJob> findAll(int limit) {
        List<ReconciliationJob> list = new ArrayList<>();
        String sql = "SELECT TOP (?) id, upload_batch_id, lc_number, status, total_steps, completed_steps, " +
                     "current_step, error_message, report_path, non_compliance_report_path, email_sent, " +
                     "notification_email, started_at, completed_at, created_at, created_by, created_by_department, " +
                     "job_name, shipment_doc_name, xlsx_report_path, xlsx_nc_report_path, email_group_id, email_error " +
                     "FROM reconciliation_jobs ORDER BY created_at DESC";
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapJobRow(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Error listing reconciliation jobs", e);
        }
        return list;
    }

    public List<ReconciliationJob> findByStatus(String status) {
        List<ReconciliationJob> list = new ArrayList<>();
        String sql = "SELECT id, upload_batch_id, lc_number, status, total_steps, completed_steps, " +
                     "current_step, error_message, report_path, non_compliance_report_path, email_sent, " +
                     "notification_email, started_at, completed_at, created_at, created_by, created_by_department, " +
                     "job_name, shipment_doc_name, xlsx_report_path, xlsx_nc_report_path, email_group_id, email_error " +
                     "FROM reconciliation_jobs WHERE status = ? ORDER BY created_at DESC";
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapJobRow(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Error finding jobs by status: {}", status, e);
        }
        return list;
    }

    public void updateTotalSteps(long id, int totalSteps) {
        String sql = "UPDATE reconciliation_jobs SET total_steps = ? WHERE id = ?";
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, totalSteps);
            ps.setLong(2, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.error("Error updating total_steps for job: id={}", id, e);
        }
    }

    public void updateStatus(long id, String status, String currentStep, int completedSteps) {
        String sql = "UPDATE reconciliation_jobs SET status = ?, current_step = ?, completed_steps = ? WHERE id = ?";
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setString(2, currentStep);
            ps.setInt(3, completedSteps);
            ps.setLong(4, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.error("Error updating job status: id={}", id, e);
            throw new RuntimeException("Failed to update job status", e);
        }
    }

    public void updateLcNumber(long id, String lcNumber) {
        String sql = "UPDATE reconciliation_jobs SET lc_number = ? WHERE id = ?";
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, lcNumber);
            ps.setLong(2, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.error("Error updating LC number for job: id={}", id, e);
        }
    }

    public void updateCompleted(long id, String status, String reportPath, String ncReportPath,
                               String xlsxReportPath, String xlsxNcReportPath, String errorMessage) {
        String sql = "UPDATE reconciliation_jobs SET status = ?, report_path = ?, non_compliance_report_path = ?, " +
                     "xlsx_report_path = ?, xlsx_nc_report_path = ?, error_message = ?, completed_at = GETDATE() WHERE id = ?";
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setString(2, reportPath);
            ps.setString(3, ncReportPath);
            ps.setString(4, xlsxReportPath);
            ps.setString(5, xlsxNcReportPath);
            ps.setString(6, errorMessage);
            ps.setLong(7, id);
            ps.executeUpdate();
            logger.info("Job completed: id={}, status={}", id, status);
        } catch (SQLException e) {
            logger.error("Error updating job completion: id={}", id, e);
            throw new RuntimeException("Failed to update job completion", e);
        }
    }

    /**
     * Record the outcome of the notification email. {@code error} is the reason it was not sent
     * (disabled, no recipient, SMTP failure) and is NULL on success.
     */
    public void updateEmailSent(long id, boolean sent, String error) {
        String sql = "UPDATE reconciliation_jobs SET email_sent = ?, email_error = ? WHERE id = ?";
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setBoolean(1, sent);
            ps.setString(2, truncate(error, 1000));
            ps.setLong(3, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.error("Error updating email_sent for job: id={}", id, e);
        }
    }

    public Map<String, Integer> getStatusCounts() {
        Map<String, Integer> counts = new LinkedHashMap<>();
        String sql = "SELECT status, COUNT(*) AS cnt FROM reconciliation_jobs GROUP BY status";
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                counts.put(rs.getString("status"), rs.getInt("cnt"));
            }
        } catch (SQLException e) {
            logger.error("Error getting job status counts", e);
        }
        return counts;
    }

    public void saveResults(long jobId, String lcNumber, List<ReconciliationResult> results) {
        String sql = "INSERT INTO reconciliation_results (job_id, lc_number, parameter_name, lc_clause_no, " +
                     "lc_value, document_type_code, document_type_name, document_value, status, reason, display_order) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        Connection conn = null;
        try {
            conn = DbUtil.getConnection();
            conn.setAutoCommit(false);
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                for (ReconciliationResult r : results) {
                    ps.setLong(1, jobId);
                    ps.setString(2, lcNumber);
                    ps.setString(3, r.getParameterName());
                    ps.setString(4, r.getLcClauseNo());
                    ps.setString(5, r.getLcValue());
                    ps.setString(6, r.getDocumentTypeCode());
                    ps.setString(7, r.getDocumentTypeName());
                    ps.setString(8, r.getDocumentValue());
                    ps.setString(9, r.getStatus());
                    ps.setString(10, r.getReason());
                    ps.setInt(11, r.getDisplayOrder());
                    ps.addBatch();
                }
                ps.executeBatch();
            }
            conn.commit();
            logger.info("Saved {} results for job: {}", results.size(), jobId);
        } catch (SQLException e) {
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException re) { logger.warn("Rollback failed", re); }
            }
            logger.error("Error saving results for job: {}", jobId, e);
            throw new RuntimeException("Failed to save reconciliation results", e);
        } finally {
            if (conn != null) {
                try { conn.setAutoCommit(true); } catch (SQLException e) { /* ignore */ }
                closeQuietly(conn);
            }
        }
    }

    public List<ReconciliationResult> getResults(long jobId) {
        List<ReconciliationResult> list = new ArrayList<>();
        String sql = "SELECT id, job_id, lc_number, parameter_name, lc_clause_no, lc_value, " +
                     "document_type_code, document_type_name, document_value, status, reason, " +
                     "display_order, created_at " +
                     "FROM reconciliation_results WHERE job_id = ? ORDER BY document_type_code, display_order";
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, jobId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ReconciliationResult r = new ReconciliationResult();
                    r.setId(rs.getLong("id"));
                    r.setJobId(rs.getLong("job_id"));
                    r.setLcNumber(rs.getString("lc_number"));
                    r.setParameterName(rs.getString("parameter_name"));
                    r.setLcClauseNo(rs.getString("lc_clause_no"));
                    r.setLcValue(rs.getString("lc_value"));
                    r.setDocumentTypeCode(rs.getString("document_type_code"));
                    r.setDocumentTypeName(rs.getString("document_type_name"));
                    r.setDocumentValue(rs.getString("document_value"));
                    r.setStatus(rs.getString("status"));
                    r.setReason(rs.getString("reason"));
                    r.setDisplayOrder(rs.getInt("display_order"));
                    r.setCreatedAt(rs.getTimestamp("created_at"));
                    list.add(r);
                }
            }
        } catch (SQLException e) {
            logger.error("Error getting results for job: {}", jobId, e);
        }
        return list;
    }

    public void addLog(long jobId, String level, String message) {
        String sql = "INSERT INTO job_logs (job_id, log_level, message) VALUES (?, ?, ?)";
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, jobId);
            ps.setString(2, level);
            ps.setString(3, message);
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.error("Error adding log for job: {}", jobId, e);
        }
    }

    public List<Map<String, String>> getLogs(long jobId) {
        List<Map<String, String>> logs = new ArrayList<>();
        String sql = "SELECT log_level, message, created_at FROM job_logs WHERE job_id = ? ORDER BY created_at";
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, jobId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, String> log = new LinkedHashMap<>();
                    log.put("level", rs.getString("log_level"));
                    log.put("message", rs.getString("message"));
                    log.put("timestamp", getUtcTimestamp(rs, "created_at").toString());
                    logs.add(log);
                }
            }
        } catch (SQLException e) {
            logger.error("Error getting logs for job: {}", jobId, e);
        }
        return logs;
    }

    private ReconciliationJob mapJobRow(ResultSet rs) throws SQLException {
        ReconciliationJob job = new ReconciliationJob();
        job.setId(rs.getLong("id"));
        job.setUploadBatchId(rs.getString("upload_batch_id"));
        job.setLcNumber(rs.getString("lc_number"));
        job.setStatus(rs.getString("status"));
        job.setTotalSteps(rs.getInt("total_steps"));
        job.setCompletedSteps(rs.getInt("completed_steps"));
        job.setCurrentStep(rs.getString("current_step"));
        job.setErrorMessage(rs.getString("error_message"));
        job.setReportPath(rs.getString("report_path"));
        job.setNonComplianceReportPath(rs.getString("non_compliance_report_path"));
        job.setEmailSent(rs.getBoolean("email_sent"));
        job.setEmailError(rs.getString("email_error"));
        job.setNotificationEmail(rs.getString("notification_email"));
        job.setStartedAt(getUtcTimestamp(rs, "started_at"));
        job.setCompletedAt(getUtcTimestamp(rs, "completed_at"));
        job.setCreatedAt(getUtcTimestamp(rs, "created_at"));
        job.setCreatedBy(rs.getString("created_by"));
        job.setCreatedByDepartment(rs.getString("created_by_department"));
        job.setJobName(rs.getString("job_name"));
        job.setShipmentDocName(rs.getString("shipment_doc_name"));
        job.setXlsxReportPath(rs.getString("xlsx_report_path"));
        job.setXlsxNcReportPath(rs.getString("xlsx_nc_report_path"));
        job.setEmailGroupId(rs.getLong("email_group_id"));
        return job;
    }

    public List<ReconciliationJob> findAllWithinDays(int limit, int days) {
        List<ReconciliationJob> list = new ArrayList<>();
        String sql = "SELECT TOP (?) id, upload_batch_id, lc_number, status, total_steps, completed_steps, " +
                     "current_step, error_message, report_path, non_compliance_report_path, email_sent, " +
                     "notification_email, started_at, completed_at, created_at, created_by, created_by_department, " +
                     "job_name, shipment_doc_name, xlsx_report_path, xlsx_nc_report_path, email_group_id, email_error " +
                     "FROM reconciliation_jobs WHERE created_at >= DATEADD(day, -?, GETDATE()) ORDER BY created_at DESC";
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, limit);
            ps.setInt(2, days);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapJobRow(rs));
            }
        } catch (SQLException e) {
            logger.error("Error listing jobs within {} days", days, e);
        }
        return list;
    }

    public List<ReconciliationJob> findAllByDepartmentWithinDays(int limit, String department, int days) {
        List<ReconciliationJob> list = new ArrayList<>();
        String sql = "SELECT TOP (?) id, upload_batch_id, lc_number, status, total_steps, completed_steps, " +
                     "current_step, error_message, report_path, non_compliance_report_path, email_sent, " +
                     "notification_email, started_at, completed_at, created_at, created_by, created_by_department, " +
                     "job_name, shipment_doc_name, xlsx_report_path, xlsx_nc_report_path, email_group_id, email_error " +
                     "FROM reconciliation_jobs WHERE created_by_department = ? AND created_at >= DATEADD(day, -?, GETDATE()) ORDER BY created_at DESC";
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, limit);
            ps.setString(2, department);
            ps.setInt(3, days);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapJobRow(rs));
            }
        } catch (SQLException e) {
            logger.error("Error listing jobs by dept {} within {} days", department, days, e);
        }
        return list;
    }

    public List<ReconciliationJob> findAllByDepartment(int limit, String department) {
        List<ReconciliationJob> list = new ArrayList<>();
        String sql = "SELECT TOP (?) id, upload_batch_id, lc_number, status, total_steps, completed_steps, " +
                     "current_step, error_message, report_path, non_compliance_report_path, email_sent, " +
                     "notification_email, started_at, completed_at, created_at, created_by, created_by_department, " +
                     "job_name, shipment_doc_name, xlsx_report_path, xlsx_nc_report_path, email_group_id, email_error " +
                     "FROM reconciliation_jobs WHERE created_by_department = ? ORDER BY created_at DESC";
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, limit);
            ps.setString(2, department);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapJobRow(rs));
            }
        } catch (SQLException e) {
            logger.error("Error listing jobs by department: {}", department, e);
        }
        return list;
    }

    public Map<String, Integer> getStatusCountsByDepartment(String department) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        String sql = "SELECT status, COUNT(*) AS cnt FROM reconciliation_jobs WHERE created_by_department = ? GROUP BY status";
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, department);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) counts.put(rs.getString("status"), rs.getInt("cnt"));
            }
        } catch (SQLException e) {
            logger.error("Error getting job status counts by department: {}", department, e);
        }
        return counts;
    }

    public void updateJobDetails(long id, String jobName, String shipmentDocName) {
        String sql = "UPDATE reconciliation_jobs SET job_name = ?, shipment_doc_name = ? WHERE id = ?";
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, jobName);
            ps.setString(2, shipmentDocName);
            ps.setLong(3, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.error("Error updating job details: id={}", id, e);
            throw new RuntimeException("Failed to update job details", e);
        }
    }

    public void rerunJob(long id) {
        String sql = "UPDATE reconciliation_jobs SET status = 'QUEUED', error_message = NULL, " +
                     "report_path = NULL, non_compliance_report_path = NULL, xlsx_report_path = NULL, xlsx_nc_report_path = NULL, " +
                     "total_steps = 0, completed_steps = 0, current_step = 'Re-queued', email_sent = 0, email_error = NULL, " +
                     "started_at = GETDATE(), completed_at = NULL WHERE id = ?";
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            ps.executeUpdate();
            logger.info("Job re-queued: id={}", id);
        } catch (SQLException e) {
            logger.error("Error re-running job: id={}", id, e);
            throw new RuntimeException("Failed to re-run job", e);
        }
    }

    public void deleteJob(long jobId) {
        Connection conn = null;
        try {
            conn = DbUtil.getConnection();
            conn.setAutoCommit(false);
            // Delete child records first
            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM job_api_costs WHERE job_id = ?")) {
                ps.setLong(1, jobId);
                ps.executeUpdate();
            }
            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM job_logs WHERE job_id = ?")) {
                ps.setLong(1, jobId);
                ps.executeUpdate();
            }
            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM reconciliation_results WHERE job_id = ?")) {
                ps.setLong(1, jobId);
                ps.executeUpdate();
            }
            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM reconciliation_jobs WHERE id = ?")) {
                ps.setLong(1, jobId);
                ps.executeUpdate();
            }
            conn.commit();
            logger.info("Job deleted: id={}", jobId);
        } catch (SQLException e) {
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException re) { logger.warn("Rollback failed", re); }
            }
            logger.error("Error deleting job: id={}", jobId, e);
            throw new RuntimeException("Failed to delete job", e);
        } finally {
            if (conn != null) {
                try { conn.setAutoCommit(true); } catch (SQLException e) { /* ignore */ }
                closeQuietly(conn);
            }
        }
    }
}
