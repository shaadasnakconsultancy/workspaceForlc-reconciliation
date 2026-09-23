package com.smipl.lcrecon.dao;

import com.smipl.lcrecon.model.JobApiCost;
import com.smipl.lcrecon.util.DbUtil;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class JobCostDao extends BaseDao {

    public long save(JobApiCost cost) {
        String sql = "INSERT INTO job_api_costs (job_id, api_type, document_name, prompt_tokens, " +
                     "completion_tokens, total_tokens, pages_processed, cost_inr) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?); SELECT SCOPE_IDENTITY();";
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, cost.getJobId());
            ps.setString(2, cost.getApiType());
            ps.setString(3, cost.getDocumentName());
            ps.setInt(4, cost.getPromptTokens());
            ps.setInt(5, cost.getCompletionTokens());
            ps.setInt(6, cost.getTotalTokens());
            ps.setInt(7, cost.getPagesProcessed());
            ps.setBigDecimal(8, cost.getCostInr() != null ? cost.getCostInr() : BigDecimal.ZERO);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    long id = rs.getLong(1);
                    cost.setId(id);
                    logger.info("Job API cost saved: id={}, jobId={}, type={}", id, cost.getJobId(), cost.getApiType());
                    return id;
                }
            }
        } catch (SQLException e) {
            logger.error("Error saving job API cost for jobId={}", cost.getJobId(), e);
            throw new RuntimeException("Failed to save job API cost", e);
        }
        return -1;
    }

    public List<JobApiCost> findByJobId(long jobId) {
        List<JobApiCost> list = new ArrayList<>();
        String sql = "SELECT id, job_id, api_type, document_name, prompt_tokens, completion_tokens, " +
                     "total_tokens, pages_processed, cost_inr, created_at " +
                     "FROM job_api_costs WHERE job_id = ? ORDER BY created_at";
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, jobId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Error finding costs by jobId={}", jobId, e);
        }
        return list;
    }

    public Map<String, Object> getTotalByJobId(long jobId) {
        Map<String, Object> totals = new LinkedHashMap<>();
        String sql = "SELECT " +
                     "ISNULL(SUM(CASE WHEN api_type = 'OCR' THEN cost_inr ELSE 0 END), 0) AS ocrCost, " +
                     "ISNULL(SUM(CASE WHEN api_type = 'GPT' THEN cost_inr ELSE 0 END), 0) AS gptCost, " +
                     "ISNULL(SUM(cost_inr), 0) AS totalCost, " +
                     "ISNULL(SUM(total_tokens), 0) AS totalTokens, " +
                     "ISNULL(SUM(pages_processed), 0) AS totalPages " +
                     "FROM job_api_costs WHERE job_id = ?";
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, jobId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    totals.put("ocrCost", rs.getBigDecimal("ocrCost"));
                    totals.put("gptCost", rs.getBigDecimal("gptCost"));
                    totals.put("totalCost", rs.getBigDecimal("totalCost"));
                    totals.put("totalTokens", rs.getLong("totalTokens"));
                    totals.put("totalPages", rs.getLong("totalPages"));
                }
            }
        } catch (SQLException e) {
            logger.error("Error getting cost totals for jobId={}", jobId, e);
        }
        return totals;
    }

    public Map<String, Object> getAnalyticsSummary() {
        Map<String, Object> summary = new LinkedHashMap<>();
        String sql = "SELECT " +
                     "ISNULL(SUM(cost_inr), 0) AS totalCost, " +
                     "COUNT(DISTINCT job_id) AS totalJobs, " +
                     "CASE WHEN COUNT(DISTINCT job_id) > 0 " +
                     "  THEN SUM(cost_inr) / COUNT(DISTINCT job_id) ELSE 0 END AS avgCostPerJob, " +
                     "ISNULL(SUM(CASE WHEN api_type = 'OCR' THEN cost_inr ELSE 0 END), 0) AS totalOcrCost, " +
                     "ISNULL(SUM(CASE WHEN api_type = 'GPT' THEN cost_inr ELSE 0 END), 0) AS totalGptCost, " +
                     "ISNULL(SUM(total_tokens), 0) AS totalTokens, " +
                     "ISNULL(SUM(pages_processed), 0) AS totalPages " +
                     "FROM job_api_costs";
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                summary.put("totalCost", rs.getBigDecimal("totalCost"));
                summary.put("totalJobs", rs.getInt("totalJobs"));
                summary.put("avgCostPerJob", rs.getBigDecimal("avgCostPerJob"));
                summary.put("totalOcrCost", rs.getBigDecimal("totalOcrCost"));
                summary.put("totalGptCost", rs.getBigDecimal("totalGptCost"));
                summary.put("totalTokens", rs.getLong("totalTokens"));
                summary.put("totalPages", rs.getLong("totalPages"));
            }
        } catch (SQLException e) {
            logger.error("Error getting analytics summary", e);
        }
        return summary;
    }

    public List<Map<String, Object>> getTopCostlyJobs(int limit) {
        List<Map<String, Object>> list = new ArrayList<>();
        String sql = "SELECT TOP (?) c.job_id AS jobId, j.job_name AS jobName, j.lc_number AS lcNumber, " +
                     "j.shipment_doc_name AS shipmentDocName, " +
                     "SUM(c.cost_inr) AS totalCost, " +
                     "SUM(CASE WHEN c.api_type = 'OCR' THEN c.cost_inr ELSE 0 END) AS ocrCost, " +
                     "SUM(CASE WHEN c.api_type = 'GPT' THEN c.cost_inr ELSE 0 END) AS gptCost, " +
                     "MIN(c.created_at) AS createdAt " +
                     "FROM job_api_costs c " +
                     "JOIN reconciliation_jobs j ON c.job_id = j.id " +
                     "GROUP BY c.job_id, j.job_name, j.lc_number, j.shipment_doc_name " +
                     "ORDER BY totalCost DESC";
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("jobId", rs.getLong("jobId"));
                    row.put("jobName", rs.getString("jobName"));
                    row.put("lcNumber", rs.getString("lcNumber"));
                    row.put("shipmentDocName", rs.getString("shipmentDocName"));
                    row.put("totalCost", rs.getBigDecimal("totalCost"));
                    row.put("ocrCost", rs.getBigDecimal("ocrCost"));
                    row.put("gptCost", rs.getBigDecimal("gptCost"));
                    row.put("createdAt", rs.getTimestamp("createdAt"));
                    list.add(row);
                }
            }
        } catch (SQLException e) {
            logger.error("Error getting top costly jobs", e);
        }
        return list;
    }

    public List<Map<String, Object>> getMonthlyCosts(int months) {
        List<Map<String, Object>> list = new ArrayList<>();
        String sql = "SELECT " +
                     "FORMAT(created_at, 'yyyy-MM') AS month, " +
                     "ISNULL(SUM(CASE WHEN api_type = 'OCR' THEN cost_inr ELSE 0 END), 0) AS ocrCost, " +
                     "ISNULL(SUM(CASE WHEN api_type = 'GPT' THEN cost_inr ELSE 0 END), 0) AS gptCost, " +
                     "ISNULL(SUM(cost_inr), 0) AS totalCost " +
                     "FROM job_api_costs " +
                     "WHERE created_at >= DATEADD(MONTH, -?, GETDATE()) " +
                     "GROUP BY FORMAT(created_at, 'yyyy-MM') " +
                     "ORDER BY month";
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, months);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("month", rs.getString("month"));
                    row.put("ocrCost", rs.getBigDecimal("ocrCost"));
                    row.put("gptCost", rs.getBigDecimal("gptCost"));
                    row.put("totalCost", rs.getBigDecimal("totalCost"));
                    list.add(row);
                }
            }
        } catch (SQLException e) {
            logger.error("Error getting monthly costs", e);
        }
        return list;
    }

    /**
     * Get a page of JOBS (not cost records) with their aggregated totals.
     * Each row in the result is one job. Pagination is by job.
     */
    public List<Map<String, Object>> getJobsPaginated(String fromDate, String toDate, String searchJobId,
                                                       int page, int pageSize) {
        List<Map<String, Object>> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT c.job_id, j.job_name, j.lc_number, j.shipment_doc_name, ");
        sql.append("COUNT(*) as callCount, SUM(c.cost_inr) as totalCost, MAX(c.created_at) as latestDate ");
        sql.append("FROM job_api_costs c JOIN reconciliation_jobs j ON c.job_id = j.id WHERE 1=1 ");

        List<Object> params = new ArrayList<>();
        if (fromDate != null && !fromDate.isEmpty()) {
            sql.append("AND c.created_at >= ? ");
            params.add(fromDate + " 00:00:00");
        }
        if (toDate != null && !toDate.isEmpty()) {
            sql.append("AND c.created_at <= ? ");
            params.add(toDate + " 23:59:59");
        }
        if (searchJobId != null && !searchJobId.isEmpty()) {
            try { sql.append("AND c.job_id = ? "); params.add(Long.parseLong(searchJobId)); }
            catch (NumberFormatException ignored) {}
        }
        sql.append("GROUP BY c.job_id, j.job_name, j.lc_number, j.shipment_doc_name ");
        sql.append("ORDER BY MAX(c.created_at) DESC ");
        sql.append("OFFSET ? ROWS FETCH NEXT ? ROWS ONLY");
        params.add((page - 1) * pageSize);
        params.add(pageSize);

        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                Object p = params.get(i);
                if (p instanceof Long) ps.setLong(i + 1, (Long) p);
                else if (p instanceof Integer) ps.setInt(i + 1, (Integer) p);
                else ps.setString(i + 1, p.toString());
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("jobId", rs.getLong("job_id"));
                    row.put("jobName", rs.getString("job_name"));
                    row.put("lcNumber", rs.getString("lc_number"));
                    row.put("shipmentDocName", rs.getString("shipment_doc_name"));
                    row.put("callCount", rs.getInt("callCount"));
                    row.put("totalCost", rs.getBigDecimal("totalCost"));
                    row.put("latestDate", rs.getTimestamp("latestDate"));
                    list.add(row);
                }
            }
        } catch (SQLException e) {
            logger.error("Error getting jobs paginated", e);
        }
        return list;
    }

    /**
     * Count total DISTINCT jobs matching filters (for pagination).
     */
    public int countJobsMatching(String fromDate, String toDate, String searchJobId) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT COUNT(DISTINCT c.job_id) FROM job_api_costs c WHERE 1=1 ");
        List<Object> params = new ArrayList<>();
        if (fromDate != null && !fromDate.isEmpty()) {
            sql.append("AND c.created_at >= ? ");
            params.add(fromDate + " 00:00:00");
        }
        if (toDate != null && !toDate.isEmpty()) {
            sql.append("AND c.created_at <= ? ");
            params.add(toDate + " 23:59:59");
        }
        if (searchJobId != null && !searchJobId.isEmpty()) {
            try { sql.append("AND c.job_id = ? "); params.add(Long.parseLong(searchJobId)); }
            catch (NumberFormatException ignored) {}
        }

        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                Object p = params.get(i);
                if (p instanceof Long) ps.setLong(i + 1, (Long) p);
                else ps.setString(i + 1, p.toString());
            }
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            logger.error("Error counting jobs matching", e);
        }
        return 0;
    }

    /**
     * Get cost details for a specific list of job IDs.
     */
    public List<Map<String, Object>> getCostDetailsForJobs(List<Long> jobIds) {
        List<Map<String, Object>> list = new ArrayList<>();
        if (jobIds == null || jobIds.isEmpty()) return list;

        StringBuilder sql = new StringBuilder();
        sql.append("SELECT c.id, c.job_id, c.api_type, c.document_name, c.prompt_tokens, c.completion_tokens, ");
        sql.append("c.total_tokens, c.pages_processed, c.cost_inr, c.created_at ");
        sql.append("FROM job_api_costs c WHERE c.job_id IN (");
        for (int i = 0; i < jobIds.size(); i++) sql.append(i == 0 ? "?" : ",?");
        sql.append(") ORDER BY c.job_id DESC, c.created_at");

        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < jobIds.size(); i++) ps.setLong(i + 1, jobIds.get(i));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("id", rs.getLong("id"));
                    row.put("jobId", rs.getLong("job_id"));
                    row.put("apiType", rs.getString("api_type"));
                    row.put("documentName", rs.getString("document_name"));
                    row.put("promptTokens", rs.getInt("prompt_tokens"));
                    row.put("completionTokens", rs.getInt("completion_tokens"));
                    row.put("totalTokens", rs.getInt("total_tokens"));
                    row.put("pagesProcessed", rs.getInt("pages_processed"));
                    row.put("costInr", rs.getBigDecimal("cost_inr"));
                    row.put("createdAt", rs.getTimestamp("created_at"));
                    list.add(row);
                }
            }
        } catch (SQLException e) {
            logger.error("Error getting cost details for jobs", e);
        }
        return list;
    }

    /**
     * Full report: all API cost rows with job info, date range, search, pagination.
     * Returns each individual API call with unit rates.
     */
    public List<Map<String, Object>> getFullReport(String fromDate, String toDate, String searchJobId,
                                                    int page, int pageSize) {
        List<Map<String, Object>> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT c.id, c.job_id, j.job_name, j.lc_number, j.shipment_doc_name, ");
        sql.append("c.api_type, c.document_name, c.prompt_tokens, c.completion_tokens, c.total_tokens, ");
        sql.append("c.pages_processed, c.cost_inr, c.created_at ");
        sql.append("FROM job_api_costs c ");
        sql.append("JOIN reconciliation_jobs j ON c.job_id = j.id ");
        sql.append("WHERE 1=1 ");

        List<Object> params = new ArrayList<>();
        if (fromDate != null && !fromDate.isEmpty()) {
            sql.append("AND c.created_at >= ? ");
            params.add(fromDate + " 00:00:00");
        }
        if (toDate != null && !toDate.isEmpty()) {
            sql.append("AND c.created_at <= ? ");
            params.add(toDate + " 23:59:59");
        }
        if (searchJobId != null && !searchJobId.isEmpty()) {
            sql.append("AND c.job_id = ? ");
            params.add(Long.parseLong(searchJobId));
        }

        sql.append("ORDER BY c.created_at DESC ");
        sql.append("OFFSET ? ROWS FETCH NEXT ? ROWS ONLY");
        params.add((page - 1) * pageSize);
        params.add(pageSize);

        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                Object p = params.get(i);
                if (p instanceof Long) ps.setLong(i + 1, (Long) p);
                else if (p instanceof Integer) ps.setInt(i + 1, (Integer) p);
                else ps.setString(i + 1, p.toString());
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("id", rs.getLong("id"));
                    row.put("jobId", rs.getLong("job_id"));
                    row.put("jobName", rs.getString("job_name"));
                    row.put("lcNumber", rs.getString("lc_number"));
                    row.put("shipmentDocName", rs.getString("shipment_doc_name"));
                    row.put("apiType", rs.getString("api_type"));
                    row.put("documentName", rs.getString("document_name"));
                    row.put("promptTokens", rs.getInt("prompt_tokens"));
                    row.put("completionTokens", rs.getInt("completion_tokens"));
                    row.put("totalTokens", rs.getInt("total_tokens"));
                    row.put("pagesProcessed", rs.getInt("pages_processed"));
                    row.put("costInr", rs.getBigDecimal("cost_inr"));
                    row.put("createdAt", rs.getTimestamp("created_at"));
                    list.add(row);
                }
            }
        } catch (SQLException e) {
            logger.error("Error getting full cost report", e);
        }
        return list;
    }

    /**
     * Count total records matching filters (for pagination).
     */
    public int getFullReportCount(String fromDate, String toDate, String searchJobId) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT COUNT(*) FROM job_api_costs c WHERE 1=1 ");
        List<Object> params = new ArrayList<>();
        if (fromDate != null && !fromDate.isEmpty()) {
            sql.append("AND c.created_at >= ? ");
            params.add(fromDate + " 00:00:00");
        }
        if (toDate != null && !toDate.isEmpty()) {
            sql.append("AND c.created_at <= ? ");
            params.add(toDate + " 23:59:59");
        }
        if (searchJobId != null && !searchJobId.isEmpty()) {
            sql.append("AND c.job_id = ? ");
            params.add(Long.parseLong(searchJobId));
        }

        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                Object p = params.get(i);
                if (p instanceof Long) ps.setLong(i + 1, (Long) p);
                else ps.setString(i + 1, p.toString());
            }
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            logger.error("Error getting full report count", e);
        }
        return 0;
    }

    /**
     * Summary totals for filtered records (for report header).
     */
    public Map<String, Object> getFilteredSummary(String fromDate, String toDate, String searchJobId) {
        Map<String, Object> summary = new LinkedHashMap<>();
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT ISNULL(SUM(cost_inr),0) AS totalCost, ");
        sql.append("COUNT(DISTINCT job_id) AS totalJobs, ");
        sql.append("ISNULL(SUM(CASE WHEN api_type='OCR' THEN cost_inr ELSE 0 END),0) AS totalOcrCost, ");
        sql.append("ISNULL(SUM(CASE WHEN api_type='GPT' THEN cost_inr ELSE 0 END),0) AS totalGptCost, ");
        sql.append("ISNULL(SUM(total_tokens),0) AS totalTokens, ");
        sql.append("ISNULL(SUM(pages_processed),0) AS totalPages, ");
        sql.append("COUNT(*) AS totalRecords ");
        sql.append("FROM job_api_costs c WHERE 1=1 ");

        List<Object> params = new ArrayList<>();
        if (fromDate != null && !fromDate.isEmpty()) {
            sql.append("AND c.created_at >= ? ");
            params.add(fromDate + " 00:00:00");
        }
        if (toDate != null && !toDate.isEmpty()) {
            sql.append("AND c.created_at <= ? ");
            params.add(toDate + " 23:59:59");
        }
        if (searchJobId != null && !searchJobId.isEmpty()) {
            sql.append("AND c.job_id = ? ");
            params.add(Long.parseLong(searchJobId));
        }

        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                Object p = params.get(i);
                if (p instanceof Long) ps.setLong(i + 1, (Long) p);
                else ps.setString(i + 1, p.toString());
            }
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    summary.put("totalCost", rs.getBigDecimal("totalCost"));
                    summary.put("totalJobs", rs.getInt("totalJobs"));
                    summary.put("totalOcrCost", rs.getBigDecimal("totalOcrCost"));
                    summary.put("totalGptCost", rs.getBigDecimal("totalGptCost"));
                    summary.put("totalTokens", rs.getLong("totalTokens"));
                    summary.put("totalPages", rs.getLong("totalPages"));
                    summary.put("totalRecords", rs.getInt("totalRecords"));
                }
            }
        } catch (SQLException e) {
            logger.error("Error getting filtered summary", e);
        }
        return summary;
    }

    public void deleteByJobId(long jobId) {
        String sql = "DELETE FROM job_api_costs WHERE job_id = ?";
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, jobId);
            int deleted = ps.executeUpdate();
            logger.info("Deleted {} cost records for jobId={}", deleted, jobId);
        } catch (SQLException e) {
            logger.error("Error deleting costs for jobId={}", jobId, e);
            throw new RuntimeException("Failed to delete job API costs", e);
        }
    }

    private JobApiCost mapRow(ResultSet rs) throws SQLException {
        JobApiCost cost = new JobApiCost();
        cost.setId(rs.getLong("id"));
        cost.setJobId(rs.getLong("job_id"));
        cost.setApiType(rs.getString("api_type"));
        cost.setDocumentName(rs.getString("document_name"));
        cost.setPromptTokens(rs.getInt("prompt_tokens"));
        cost.setCompletionTokens(rs.getInt("completion_tokens"));
        cost.setTotalTokens(rs.getInt("total_tokens"));
        cost.setPagesProcessed(rs.getInt("pages_processed"));
        cost.setCostInr(rs.getBigDecimal("cost_inr"));
        cost.setCreatedAt(rs.getTimestamp("created_at"));
        return cost;
    }
}
