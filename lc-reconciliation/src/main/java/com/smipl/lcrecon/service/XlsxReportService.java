package com.smipl.lcrecon.service;

import com.smipl.lcrecon.model.ReconciliationResult;
import com.smipl.lcrecon.util.FileUtil;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

public class XlsxReportService {
    private static final Logger logger = LoggerFactory.getLogger(XlsxReportService.class);

    private static final String[] HEADERS = {
            "Parameter", "LC Clause No", "LC Data", "Document Data", "Status", "Reason"
    };

    /**
     * Generate full reconciliation report as XLSX.
     * Creates one sheet per document type with columns:
     * Parameter | LC Clause No | LC Data | {DocType} Data | Status | Reason
     *
     * Color coding:
     * - Complied rows: light green (#90ee90)
     * - Not Complied rows: light orange (#ffcc99)
     * - Not Applicable rows: light yellow (#fff3b0)
     */
    public String generateFullReport(long jobId, String lcNumber, List<ReconciliationResult> results,
                                     List<String> docTypes) throws Exception {
        return generateReport(jobId, lcNumber, null, null, results, docTypes, false);
    }

    public String generateFullReport(long jobId, String lcNumber, String jobName, String shipmentDocName,
                                     List<ReconciliationResult> results, List<String> docTypes) throws Exception {
        return generateReport(jobId, lcNumber, jobName, shipmentDocName, results, docTypes, false);
    }

    public String generateNonComplianceReport(long jobId, String lcNumber, List<ReconciliationResult> results,
                                              List<String> docTypes) throws Exception {
        return generateReport(jobId, lcNumber, null, null, results, docTypes, true);
    }

    public String generateNonComplianceReport(long jobId, String lcNumber, String jobName, String shipmentDocName,
                                              List<ReconciliationResult> results, List<String> docTypes) throws Exception {
        return generateReport(jobId, lcNumber, jobName, shipmentDocName, results, docTypes, true);
    }

    private String generateReport(long jobId, String lcNumber, String jobName, String shipmentDocName,
                                  List<ReconciliationResult> results,
                                  List<String> docTypes, boolean nonComplianceOnly) throws Exception {
        logger.info("Generating {} report for LC={}, jobId={}, docTypes={}",
                nonComplianceOnly ? "non-compliance" : "full", lcNumber, jobId, docTypes);

        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            // -- Create reusable cell styles --
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle compliedStyle = createStatusStyle(workbook, new byte[]{(byte) 0x90, (byte) 0xEE, (byte) 0x90});
            CellStyle notCompliedStyle = createStatusStyle(workbook, new byte[]{(byte) 0xFF, (byte) 0xCC, (byte) 0x99});
            CellStyle notApplicableStyle = createStatusStyle(workbook, new byte[]{(byte) 0xFF, (byte) 0xF3, (byte) 0xB0});
            CellStyle titleStyle = createTitleStyle(workbook);

            // Summary counters per doc type
            Map<String, int[]> summaryMap = new LinkedHashMap<>(); // docType -> [complied, notComplied, notApplicable, total]

            // -- Create one sheet per document type --
            for (String docType : docTypes) {
                List<ReconciliationResult> docResults = results.stream()
                        .filter(r -> docType.equals(r.getDocumentTypeName()))
                        .sorted(Comparator.comparingInt(ReconciliationResult::getDisplayOrder))
                        .collect(Collectors.toList());

                if (nonComplianceOnly) {
                    docResults = docResults.stream()
                            .filter(r -> !"Complied".equals(r.getStatus()))
                            .collect(Collectors.toList());
                }

                // Track summary counts (use unfiltered for summary even in non-compliance report)
                int[] counts = new int[4]; // complied, notComplied, notApplicable, total
                for (ReconciliationResult r : results) {
                    if (docType.equals(r.getDocumentTypeName())) {
                        counts[3]++;
                        if ("Complied".equals(r.getStatus())) counts[0]++;
                        else if ("Not Complied".equals(r.getStatus())) counts[1]++;
                        else counts[2]++;
                    }
                }
                summaryMap.put(docType, counts);

                // Sanitize sheet name (max 31 chars, no special chars)
                String sheetName = sanitizeSheetName(docType);
                Sheet sheet = workbook.createSheet(sheetName);

                int rowIdx = 0;

                // Title row
                Row titleRow = sheet.createRow(rowIdx++);
                Cell titleCell = titleRow.createCell(0);
                titleCell.setCellValue("LC Reconciliation Report - " + lcNumber);
                titleCell.setCellStyle(titleStyle);
                sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, HEADERS.length - 1));

                // Subtitle row
                Row subtitleRow = sheet.createRow(rowIdx++);
                Cell subtitleCell = subtitleRow.createCell(0);
                StringBuilder subtitle = new StringBuilder("Document Type: " + docType);
                if (jobName != null && !jobName.isEmpty()) subtitle.append("  |  Job: ").append(jobName);
                if (shipmentDocName != null && !shipmentDocName.isEmpty()) subtitle.append("  |  Shipment: ").append(shipmentDocName);
                if (nonComplianceOnly) subtitle.append("  (Non-Compliance Only)");
                subtitleCell.setCellValue(subtitle.toString());
                sheet.addMergedRegion(new CellRangeAddress(1, 1, 0, HEADERS.length - 1));

                // Blank row
                rowIdx++;

                // Header row
                Row headerRow = sheet.createRow(rowIdx++);
                String[] docHeaders = Arrays.copyOf(HEADERS, HEADERS.length);
                docHeaders[3] = docType + " Data"; // Replace generic "Document Data" with specific doc type
                for (int i = 0; i < docHeaders.length; i++) {
                    Cell cell = headerRow.createCell(i);
                    cell.setCellValue(docHeaders[i]);
                    cell.setCellStyle(headerStyle);
                }

                // Data rows
                for (ReconciliationResult result : docResults) {
                    Row dataRow = sheet.createRow(rowIdx++);
                    CellStyle rowStyle = getStatusStyle(result.getStatus(), compliedStyle, notCompliedStyle, notApplicableStyle);

                    setCellValue(dataRow, 0, result.getParameterName(), rowStyle);
                    setCellValue(dataRow, 1, result.getLcClauseNo(), rowStyle);
                    setCellValue(dataRow, 2, result.getLcValue(), rowStyle);
                    setCellValue(dataRow, 3, result.getDocumentValue(), rowStyle);
                    setCellValue(dataRow, 4, result.getStatus(), rowStyle);
                    setCellValue(dataRow, 5, result.getReason(), rowStyle);
                }

                // Auto-size columns
                for (int i = 0; i < HEADERS.length; i++) {
                    sheet.autoSizeColumn(i);
                    // Set a minimum width and cap maximum width
                    int currentWidth = sheet.getColumnWidth(i);
                    int minWidth = 3500;  // ~12 characters
                    int maxWidth = 18000; // ~64 characters
                    if (currentWidth < minWidth) {
                        sheet.setColumnWidth(i, minWidth);
                    } else if (currentWidth > maxWidth) {
                        sheet.setColumnWidth(i, maxWidth);
                    }
                }
            }

            // -- Summary sheet --
            createSummarySheet(workbook, lcNumber, jobId, summaryMap, headerStyle, titleStyle,
                    compliedStyle, notCompliedStyle, notApplicableStyle, nonComplianceOnly);

            // -- Save file --
            String fileName;
            if (nonComplianceOnly) {
                fileName = String.format("NonCompliance_%s_%d.xlsx", sanitizeFileName(lcNumber), jobId);
            } else {
                fileName = String.format("Report_%s_%d.xlsx", sanitizeFileName(lcNumber), jobId);
            }

            Path reportDir = Paths.get(FileUtil.getReportBaseDir());
            Files.createDirectories(reportDir);
            Path filePath = reportDir.resolve(fileName);

            try (FileOutputStream fos = new FileOutputStream(filePath.toFile())) {
                workbook.write(fos);
            }

            logger.info("Report generated successfully: {}", filePath);
            return filePath.toString();
        }
    }

    private void createSummarySheet(XSSFWorkbook workbook, String lcNumber, long jobId,
                                    Map<String, int[]> summaryMap, CellStyle headerStyle,
                                    CellStyle titleStyle, CellStyle compliedStyle,
                                    CellStyle notCompliedStyle, CellStyle notApplicableStyle,
                                    boolean nonComplianceOnly) {
        Sheet summary = workbook.createSheet("Summary");

        // Move summary to the first position
        workbook.setSheetOrder("Summary", 0);

        int rowIdx = 0;

        // Title
        Row titleRow = summary.createRow(rowIdx++);
        Cell titleCell = titleRow.createCell(0);
        String title = "LC Reconciliation Summary - " + lcNumber;
        if (nonComplianceOnly) {
            title += " (Non-Compliance Report)";
        }
        titleCell.setCellValue(title);
        titleCell.setCellStyle(titleStyle);
        summary.addMergedRegion(new CellRangeAddress(0, 0, 0, 4));

        // Job info
        Row infoRow = summary.createRow(rowIdx++);
        infoRow.createCell(0).setCellValue("Job ID: " + jobId);
        Row dateRow = summary.createRow(rowIdx++);
        dateRow.createCell(0).setCellValue("Generated: " + new Date().toString());

        // Blank row
        rowIdx++;

        // Summary header
        String[] summaryHeaders = {"Document Type", "Complied", "Not Complied", "Not Applicable", "Total"};
        Row sHeaderRow = summary.createRow(rowIdx++);
        for (int i = 0; i < summaryHeaders.length; i++) {
            Cell cell = sHeaderRow.createCell(i);
            cell.setCellValue(summaryHeaders[i]);
            cell.setCellStyle(headerStyle);
        }

        // Summary data rows
        int totalComplied = 0, totalNotComplied = 0, totalNA = 0, grandTotal = 0;
        for (Map.Entry<String, int[]> entry : summaryMap.entrySet()) {
            Row dataRow = summary.createRow(rowIdx++);
            int[] counts = entry.getValue();

            dataRow.createCell(0).setCellValue(entry.getKey());

            Cell compliedCell = dataRow.createCell(1);
            compliedCell.setCellValue(counts[0]);
            compliedCell.setCellStyle(compliedStyle);

            Cell notCompliedCell = dataRow.createCell(2);
            notCompliedCell.setCellValue(counts[1]);
            if (counts[1] > 0) {
                notCompliedCell.setCellStyle(notCompliedStyle);
            }

            Cell naCell = dataRow.createCell(3);
            naCell.setCellValue(counts[2]);
            if (counts[2] > 0) {
                naCell.setCellStyle(notApplicableStyle);
            }

            dataRow.createCell(4).setCellValue(counts[3]);

            totalComplied += counts[0];
            totalNotComplied += counts[1];
            totalNA += counts[2];
            grandTotal += counts[3];
        }

        // Grand total row
        rowIdx++;
        Row totalRow = summary.createRow(rowIdx);
        Cell totalLabel = totalRow.createCell(0);
        totalLabel.setCellValue("TOTAL");
        totalLabel.setCellStyle(headerStyle);

        Cell tc = totalRow.createCell(1);
        tc.setCellValue(totalComplied);
        tc.setCellStyle(headerStyle);

        Cell tnc = totalRow.createCell(2);
        tnc.setCellValue(totalNotComplied);
        tnc.setCellStyle(headerStyle);

        Cell tna = totalRow.createCell(3);
        tna.setCellValue(totalNA);
        tna.setCellStyle(headerStyle);

        Cell gt = totalRow.createCell(4);
        gt.setCellValue(grandTotal);
        gt.setCellStyle(headerStyle);

        // Auto-size summary columns
        for (int i = 0; i < summaryHeaders.length; i++) {
            summary.autoSizeColumn(i);
            int currentWidth = summary.getColumnWidth(i);
            if (currentWidth < 4000) {
                summary.setColumnWidth(i, 4000);
            }
        }
    }

    // -- Style helpers --

    private CellStyle createHeaderStyle(XSSFWorkbook workbook) {
        CellStyle style = workbook.createCellStyle();

        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 11);
        style.setFont(font);

        // Grey background
        style.setFillForegroundColor(new XSSFColor(new byte[]{(byte) 0xD9, (byte) 0xD9, (byte) 0xD9}, null));
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        applyBorders(style);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setWrapText(true);

        return style;
    }

    private CellStyle createTitleStyle(XSSFWorkbook workbook) {
        CellStyle style = workbook.createCellStyle();

        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 14);
        style.setFont(font);

        style.setAlignment(HorizontalAlignment.LEFT);
        style.setVerticalAlignment(VerticalAlignment.CENTER);

        return style;
    }

    private CellStyle createStatusStyle(XSSFWorkbook workbook, byte[] rgb) {
        CellStyle style = workbook.createCellStyle();

        Font font = workbook.createFont();
        font.setFontHeightInPoints((short) 10);
        style.setFont(font);

        style.setFillForegroundColor(new XSSFColor(rgb, null));
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        applyBorders(style);
        style.setVerticalAlignment(VerticalAlignment.TOP);
        style.setWrapText(true);

        return style;
    }

    private void applyBorders(CellStyle style) {
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);

        short borderColor = IndexedColors.GREY_50_PERCENT.getIndex();
        style.setTopBorderColor(borderColor);
        style.setBottomBorderColor(borderColor);
        style.setLeftBorderColor(borderColor);
        style.setRightBorderColor(borderColor);
    }

    private CellStyle getStatusStyle(String status, CellStyle complied, CellStyle notComplied, CellStyle notApplicable) {
        if ("Complied".equals(status)) return complied;
        if ("Not Complied".equals(status)) return notComplied;
        return notApplicable;
    }

    private void setCellValue(Row row, int colIdx, String value, CellStyle style) {
        Cell cell = row.createCell(colIdx);
        cell.setCellValue(value != null ? value : "");
        cell.setCellStyle(style);
    }

    private String sanitizeSheetName(String name) {
        if (name == null || name.isEmpty()) return "Sheet";
        // Remove characters invalid in Excel sheet names
        String sanitized = name.replaceAll("[\\\\/:*?\\[\\]]", "_");
        if (sanitized.length() > 31) {
            sanitized = sanitized.substring(0, 31);
        }
        return sanitized;
    }

    private String sanitizeFileName(String name) {
        if (name == null || name.isEmpty()) return "unknown";
        return name.replaceAll("[^a-zA-Z0-9_\\-]", "_");
    }
}
