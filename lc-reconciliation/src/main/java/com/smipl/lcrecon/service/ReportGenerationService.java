package com.smipl.lcrecon.service;

import com.smipl.lcrecon.model.ReconciliationResult;
import com.smipl.lcrecon.util.FileUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class ReportGenerationService {
    private static final Logger logger = LoggerFactory.getLogger(ReportGenerationService.class);

    public String generateFullReport(long jobId, String lcNumber, List<ReconciliationResult> results,
                                      List<String> docTypes) throws Exception {
        String html = buildReportHtml(lcNumber, results, docTypes, false);
        String fileName = "Report_" + lcNumber + "_" + jobId + ".html";
        String path = FileUtil.saveReport(html, fileName);
        logger.info("Full report generated: {}", path);
        return path;
    }

    public String generateNonComplianceReport(long jobId, String lcNumber, List<ReconciliationResult> results,
                                               List<String> docTypes) throws Exception {
        String html = buildReportHtml(lcNumber, results, docTypes, true);
        String fileName = "NonCompliance_" + lcNumber + "_" + jobId + ".html";
        String path = FileUtil.saveReport(html, fileName);
        logger.info("Non-compliance report generated: {}", path);
        return path;
    }

    private String buildReportHtml(String lcNumber, List<ReconciliationResult> allResults,
                                    List<String> docTypes, boolean nonComplianceOnly) {
        // Group results by document type, then build a consolidated view
        // Each document type has its own set of Parameter rows from GPT

        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html><html><head><meta charset=\"UTF-8\">\n");
        html.append("<title>LC Document Reconciliation Report</title>\n");
        html.append(getReportCss());
        html.append("</head><body>\n");

        String title = nonComplianceOnly ? "Non-Compliance Report" : "LC Document Reconciliation Report";
        html.append("<h2 style=\"text-align:center; margin-bottom:4px;\">").append(title).append("</h2>\n");
        html.append("<p style=\"text-align:center; font-weight:bold; margin-top:0; margin-bottom:8px;\">LC Number: ").append(esc(lcNumber)).append("</p>\n");

        // Legend
        html.append("<div class=\"legend\">\n");
        html.append("<div class=\"legend-item\"><span class=\"cell-indicator tick\">&#10003;</span><span>Complied</span></div>\n");
        html.append("<div class=\"legend-item\"><span class=\"cell-indicator cross\">&#10007;</span><span>Not Complied</span></div>\n");
        html.append("<div class=\"legend-item\"><span class=\"cell-indicator no-compare\">&#8856;</span><span>Not Applicable</span></div>\n");
        html.append("</div>\n");

        // Generate a separate table per document type
        for (String docType : docTypes) {
            List<ReconciliationResult> docResults = new ArrayList<>();
            for (ReconciliationResult r : allResults) {
                if (docType.equals(r.getDocumentTypeName())) {
                    if (nonComplianceOnly && "Complied".equals(r.getStatus())) continue;
                    docResults.add(r);
                }
            }

            if (docResults.isEmpty() && nonComplianceOnly) continue;

            html.append("<h3 style=\"margin-top:20px;\">").append(esc(docType)).append("</h3>\n");
            html.append("<table><tr>");
            html.append("<th style=\"width:40px\">#</th>");
            html.append("<th class=\"param-col\">Parameter</th>");
            html.append("<th>LC Clause No</th>");
            html.append("<th>LC Data</th>");
            html.append("<th>").append(esc(docType)).append(" Data</th>");
            html.append("<th>Status</th>");
            html.append("<th>Reason</th>");
            html.append("</tr>\n");

            int rowNum = 0;
            for (ReconciliationResult row : docResults) {
                String statusClass = "Complied".equals(row.getStatus()) ? "matched" :
                        "Not Complied".equals(row.getStatus()) ? "not-matched" : "lc-missing";
                String indicator = "Complied".equals(row.getStatus()) ? "tick" :
                        "Not Complied".equals(row.getStatus()) ? "cross" : "no-compare";
                String indicatorChar = "Complied".equals(row.getStatus()) ? "&#10003;" :
                        "Not Complied".equals(row.getStatus()) ? "&#10007;" : "&#8856;";

                html.append("<tr>");
                html.append("<td>").append(++rowNum).append("</td>");
                html.append("<td class=\"param-col\">").append(esc(row.getParameterName())).append("</td>");
                html.append("<td>").append(esc(row.getLcClauseNo())).append("</td>");
                html.append("<td class=\"data-cell\">").append(esc(row.getLcValue())).append("</td>");
                html.append("<td class=\"data-cell\">").append(esc(row.getDocumentValue()));
                html.append("<span class=\"cell-indicator ").append(indicator).append("\">").append(indicatorChar).append("</span>");
                html.append("</td>");
                html.append("<td class=\"").append(statusClass).append("\">").append(esc(row.getStatus())).append("</td>");
                html.append("<td>").append(esc(row.getReason())).append("</td>");
                html.append("</tr>\n");
            }
            html.append("</table>\n");
        }

        html.append("</body></html>");
        return html.toString();
    }

    private String getReportCss() {
        return "<style>\n" +
                "* { box-sizing: border-box; }\n" +
                "body { font-family: Arial, sans-serif; margin: 20px; }\n" +
                "table { border-collapse: collapse; width: 100%; table-layout: fixed; margin-bottom: 10px; }\n" +
                "th, td { border: 1px solid #999; padding: 8px; text-align: left; vertical-align: top; position: relative; }\n" +
                "th { background-color: #f2f2f2; }\n" +
                "th.param-col, td.param-col { width: 180px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }\n" +
                "td.data-cell { white-space: pre-wrap; overflow-wrap: anywhere; word-break: break-word; padding-right: 34px; padding-bottom: 20px; }\n" +
                ".matched { background-color: #90ee90; font-weight: bold; }\n" +
                ".lc-missing { background-color: #fff3b0; font-weight: bold; }\n" +
                ".not-matched { background-color: #ffcc99; font-weight: bold; }\n" +
                ".cell-indicator { width: 18px; height: 18px; border-radius: 50%; display: inline-flex; align-items: center; justify-content: center; font-size: 12px; font-weight: 900; box-shadow: 0 0 3px rgba(0,0,0,0.3); }\n" +
                "td.data-cell .cell-indicator { position: absolute; right: 6px; bottom: 4px; }\n" +
                ".cell-indicator.tick { background-color: #e6f6e6; color: #0a8a0a; border: 1px solid #0a8a0a; }\n" +
                ".cell-indicator.cross { background-color: #fdeaea; color: #c00000; border: 1px solid #c00000; }\n" +
                ".cell-indicator.no-compare { background-color: #f1f1f1; color: #666666; border: 1px solid #666666; }\n" +
                ".legend { display: flex; gap: 14px; justify-content: center; align-items: center; flex-wrap: wrap; margin: 6px 0 14px 0; font-size: 13px; }\n" +
                ".legend-item { display: flex; align-items: center; gap: 6px; }\n" +
                ".legend .cell-indicator { position: static; }\n" +
                "</style>\n";
    }

    private String esc(String value) {
        if (value == null) return "";
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
}
