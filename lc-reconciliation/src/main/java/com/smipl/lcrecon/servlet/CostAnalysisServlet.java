package com.smipl.lcrecon.servlet;

import com.smipl.lcrecon.dao.JobCostDao;
import com.smipl.lcrecon.dao.JobDao;
import com.smipl.lcrecon.dao.SettingsDao;
import com.smipl.lcrecon.model.JobApiCost;
import com.smipl.lcrecon.model.ReconciliationJob;
import com.smipl.lcrecon.util.JsonUtil;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;

@WebServlet("/cost-analysis")
public class CostAnalysisServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        JobCostDao costDao = (JobCostDao) getServletContext().getAttribute("jobCostDao");
        JobDao jobDao = (JobDao) getServletContext().getAttribute("jobDao");
        SettingsDao settingsDao = (SettingsDao) getServletContext().getAttribute("settingsDao");

        String format = request.getParameter("format");
        String view = request.getParameter("view");

        // JSON API for charts
        if ("json".equals(format) && "monthly".equals(request.getParameter("type"))) {
            List<Map<String, Object>> monthly = costDao.getMonthlyCosts(12);
            JsonUtil.writeJsonResponse(response, monthly);
            return;
        }

        // Excel download for full report
        if ("xlsx".equals(format)) {
            exportFullReportExcel(request, response, costDao, settingsDao);
            return;
        }

        // Job-specific cost breakdown
        String jobIdStr = request.getParameter("jobId");
        if (jobIdStr != null) {
            long jobId = Long.parseLong(jobIdStr);
            List<JobApiCost> jobCosts = costDao.findByJobId(jobId);
            Map<String, Object> jobCostTotal = costDao.getTotalByJobId(jobId);
            ReconciliationJob job = jobDao.findById(jobId);
            // Load USD rate for display
            Map<String, String> costSettings = settingsDao.getSettingsByGroup("COST_RATES");
            double usdToInr = parseDouble(costSettings.getOrDefault("cost_usd_to_inr", "85.00"));
            request.setAttribute("jobCosts", jobCosts);
            request.setAttribute("jobCostTotal", jobCostTotal);
            request.setAttribute("job", job);
            request.setAttribute("usdToInr", usdToInr);
            request.getRequestDispatcher("/WEB-INF/jsp/cost-analysis.jsp").forward(request, response);
            return;
        }

        // Full Report view
        if ("report".equals(view)) {
            // Sanitized - these filter values are reflected back into the page
            String fromDate = com.smipl.lcrecon.util.InputValidator.safeDate(request.getParameter("fromDate"));
            String toDate = com.smipl.lcrecon.util.InputValidator.safeDate(request.getParameter("toDate"));
            String searchJobId = com.smipl.lcrecon.util.InputValidator.stripUnsafe(request.getParameter("searchJobId"));
            int page = 1;
            int pageSize = 25;
            try { page = Integer.parseInt(request.getParameter("page")); } catch (Exception ignored) {}
            try { pageSize = Integer.parseInt(request.getParameter("pageSize")); } catch (Exception ignored) {}

            // Paginate by JOB (not by cost record) to keep all records of a job together
            List<Map<String, Object>> jobsList = costDao.getJobsPaginated(fromDate, toDate, searchJobId, page, pageSize);
            int totalJobsCount = costDao.countJobsMatching(fromDate, toDate, searchJobId);
            int totalPages = (int) Math.ceil((double) totalJobsCount / pageSize);

            // Get all cost details for these jobs
            java.util.List<Long> pageJobIds = new java.util.ArrayList<>();
            for (Map<String, Object> job : jobsList) {
                pageJobIds.add(((Number) job.get("jobId")).longValue());
            }
            List<Map<String, Object>> costDetails = costDao.getCostDetailsForJobs(pageJobIds);

            Map<String, Object> filteredSummary = costDao.getFilteredSummary(fromDate, toDate, searchJobId);

            // Load USD rate for conversion display
            Map<String, String> costSettings = settingsDao.getSettingsByGroup("COST_RATES");
            double usdToInr = parseDouble(costSettings.getOrDefault("cost_usd_to_inr", "85.00"));

            request.setAttribute("jobsList", jobsList);
            request.setAttribute("costDetails", costDetails);
            request.setAttribute("filteredSummary", filteredSummary);
            request.setAttribute("totalRecords", totalJobsCount);
            request.setAttribute("totalPages", totalPages);
            request.setAttribute("currentPage", page);
            request.setAttribute("pageSize", pageSize);
            request.setAttribute("fromDate", fromDate);
            request.setAttribute("toDate", toDate);
            request.setAttribute("searchJobId", searchJobId);
            request.setAttribute("usdToInr", usdToInr);
            request.setAttribute("viewMode", "report");
            request.getRequestDispatcher("/WEB-INF/jsp/cost-analysis.jsp").forward(request, response);
            return;
        }

        // Default: Analytics dashboard
        Map<String, Object> summary = costDao.getAnalyticsSummary();
        List<Map<String, Object>> topJobs = costDao.getTopCostlyJobs(20);
        Map<String, String> costSettings = settingsDao.getSettingsByGroup("COST_RATES");
        double usdToInr = parseDouble(costSettings.getOrDefault("cost_usd_to_inr", "85.00"));

        request.setAttribute("summary", summary);
        request.setAttribute("topJobs", topJobs);
        request.setAttribute("usdToInr", usdToInr);
        request.getRequestDispatcher("/WEB-INF/jsp/cost-analysis.jsp").forward(request, response);
    }

    private void exportFullReportExcel(HttpServletRequest request, HttpServletResponse response,
                                        JobCostDao costDao, SettingsDao settingsDao) throws IOException {
        String fromDate = request.getParameter("fromDate");
        String toDate = request.getParameter("toDate");
        String searchJobId = request.getParameter("searchJobId");

        // Get ALL records (no pagination for export)
        List<Map<String, Object>> data = costDao.getFullReport(fromDate, toDate, searchJobId, 1, 100000);
        Map<String, Object> summary = costDao.getFilteredSummary(fromDate, toDate, searchJobId);
        Map<String, String> costSettings = settingsDao.getSettingsByGroup("COST_RATES");
        double usdToInr = parseDouble(costSettings.getOrDefault("cost_usd_to_inr", "85.00"));

        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Cost Report");

            // Styles
            CellStyle headerStyle = wb.createCellStyle();
            Font headerFont = wb.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            CellStyle currencyStyle = wb.createCellStyle();
            currencyStyle.setDataFormat(wb.createDataFormat().getFormat("#,##0.0000"));

            int rowIdx = 0;

            // Title
            Row titleRow = sheet.createRow(rowIdx++);
            titleRow.createCell(0).setCellValue("LC Reconciliation - API Cost Report");
            titleRow.getCell(0).setCellStyle(headerStyle);

            // Summary
            Row summaryRow = sheet.createRow(rowIdx++);
            summaryRow.createCell(0).setCellValue("Total Cost (INR): " + summary.getOrDefault("totalCost", "0"));
            summaryRow.createCell(3).setCellValue("Jobs: " + summary.getOrDefault("totalJobs", "0"));
            summaryRow.createCell(5).setCellValue("USD/INR Rate: " + usdToInr);
            rowIdx++;

            // Headers
            String[] headers = {"#", "Job ID", "Job Name", "LC Number", "Shipment Doc", "API Type",
                    "Document", "Input Tokens", "Output Tokens", "Total Tokens", "Pages",
                    "Cost (INR)", "Cost (USD)", "Date"};
            Row headerRow = sheet.createRow(rowIdx++);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // Data
            int seq = 0;
            double totalInr = 0;
            for (Map<String, Object> row : data) {
                Row dataRow = sheet.createRow(rowIdx++);
                seq++;
                BigDecimal costInr = (BigDecimal) row.getOrDefault("costInr", BigDecimal.ZERO);
                BigDecimal costUsd = usdToInr > 0 ? costInr.divide(BigDecimal.valueOf(usdToInr), 6, RoundingMode.HALF_UP) : BigDecimal.ZERO;
                totalInr += costInr.doubleValue();

                dataRow.createCell(0).setCellValue(seq);
                dataRow.createCell(1).setCellValue(((Number) row.get("jobId")).longValue());
                dataRow.createCell(2).setCellValue((String) row.getOrDefault("jobName", ""));
                dataRow.createCell(3).setCellValue((String) row.getOrDefault("lcNumber", ""));
                dataRow.createCell(4).setCellValue((String) row.getOrDefault("shipmentDocName", ""));
                dataRow.createCell(5).setCellValue((String) row.get("apiType"));
                dataRow.createCell(6).setCellValue((String) row.getOrDefault("documentName", ""));
                dataRow.createCell(7).setCellValue(((Number) row.get("promptTokens")).intValue());
                dataRow.createCell(8).setCellValue(((Number) row.get("completionTokens")).intValue());
                dataRow.createCell(9).setCellValue(((Number) row.get("totalTokens")).intValue());
                dataRow.createCell(10).setCellValue(((Number) row.get("pagesProcessed")).intValue());
                Cell inrCell = dataRow.createCell(11);
                inrCell.setCellValue(costInr.doubleValue());
                inrCell.setCellStyle(currencyStyle);
                Cell usdCell = dataRow.createCell(12);
                usdCell.setCellValue(costUsd.doubleValue());
                usdCell.setCellStyle(currencyStyle);
                dataRow.createCell(13).setCellValue(row.get("createdAt") != null ? row.get("createdAt").toString() : "");
            }

            // Total row
            Row totalRow = sheet.createRow(rowIdx);
            totalRow.createCell(0).setCellValue("TOTAL");
            totalRow.getCell(0).setCellStyle(headerStyle);
            Cell totalInrCell = totalRow.createCell(11);
            totalInrCell.setCellValue(totalInr);
            totalInrCell.setCellStyle(currencyStyle);
            Cell totalUsdCell = totalRow.createCell(12);
            totalUsdCell.setCellValue(usdToInr > 0 ? totalInr / usdToInr : 0);
            totalUsdCell.setCellStyle(currencyStyle);

            // Auto-size
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition", "attachment; filename=CostReport.xlsx");
            wb.write(response.getOutputStream());
        }
    }

    private double parseDouble(String s) {
        try { return Double.parseDouble(s); } catch (Exception e) { return 0; }
    }
}
