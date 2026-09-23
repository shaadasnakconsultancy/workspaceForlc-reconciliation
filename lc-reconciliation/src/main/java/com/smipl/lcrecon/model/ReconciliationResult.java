package com.smipl.lcrecon.model;

import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

public class ReconciliationResult {
    private long id;
    private long jobId;
    private String lcNumber;
    private String parameterName;
    private String lcClauseNo;
    private String lcValue;            // LCClauseDescription
    private String documentDataField;  // e.g., "InvoiceData", "BLData"
    private String documentValue;      // The extracted value from the document
    private String status;             // Complied, Not Complied, Not Applicable
    private String reason;             // ReasonOfNonCompliance
    private String documentTypeCode;   // Which document this result is for
    private String documentTypeName;
    private int displayOrder;
    private Date createdAt;

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public long getJobId() { return jobId; }
    public void setJobId(long jobId) { this.jobId = jobId; }
    public String getLcNumber() { return lcNumber; }
    public void setLcNumber(String lcNumber) { this.lcNumber = lcNumber; }
    public String getParameterName() { return parameterName; }
    public void setParameterName(String parameterName) { this.parameterName = parameterName; }
    public String getLcClauseNo() { return lcClauseNo; }
    public void setLcClauseNo(String lcClauseNo) { this.lcClauseNo = lcClauseNo; }
    public String getLcValue() { return lcValue; }
    public void setLcValue(String lcValue) { this.lcValue = lcValue; }
    public String getDocumentDataField() { return documentDataField; }
    public void setDocumentDataField(String documentDataField) { this.documentDataField = documentDataField; }
    public String getDocumentValue() { return documentValue; }
    public void setDocumentValue(String documentValue) { this.documentValue = documentValue; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public String getDocumentTypeCode() { return documentTypeCode; }
    public void setDocumentTypeCode(String documentTypeCode) { this.documentTypeCode = documentTypeCode; }
    public String getDocumentTypeName() { return documentTypeName; }
    public void setDocumentTypeName(String documentTypeName) { this.documentTypeName = documentTypeName; }
    public int getDisplayOrder() { return displayOrder; }
    public void setDisplayOrder(int displayOrder) { this.displayOrder = displayOrder; }
    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }

    // --- Legacy compatibility for existing report JSP ---
    // These allow the reconciliation-report.jsp to still work
    private Map<String, String> documentValues = new LinkedHashMap<>();
    private Map<String, String> cellMatchStatus = new LinkedHashMap<>();
    private String matchResult;

    public Map<String, String> getDocumentValues() { return documentValues; }
    public void setDocumentValues(Map<String, String> documentValues) { this.documentValues = documentValues; }
    public Map<String, String> getCellMatchStatus() { return cellMatchStatus; }
    public void setCellMatchStatus(Map<String, String> cellMatchStatus) { this.cellMatchStatus = cellMatchStatus; }
    public String getMatchResult() { return matchResult; }
    public void setMatchResult(String matchResult) { this.matchResult = matchResult; }

    public String getResultCssClass() {
        if ("Complied".equals(status)) return "matched";
        if ("Not Complied".equals(status)) return "not-matched";
        if ("Not Applicable".equals(status)) return "lc-missing";
        // Legacy fallback
        if ("MATCHED".equals(matchResult)) return "matched";
        if ("NOT_MATCHED".equals(matchResult)) return "not-matched";
        if ("LC_MISSING".equals(matchResult)) return "lc-missing";
        return "";
    }

    public String getResultDisplay() {
        if (status != null) return status;
        if ("MATCHED".equals(matchResult)) return "Complied";
        if ("NOT_MATCHED".equals(matchResult)) return "Not Complied";
        if ("LC_MISSING".equals(matchResult)) return "Not Applicable";
        return matchResult != null ? matchResult : "";
    }

    public String getStatusIndicator() {
        if ("Complied".equals(status) || "MATCHED".equals(matchResult)) return "tick";
        if ("Not Complied".equals(status) || "NOT_MATCHED".equals(matchResult)) return "cross";
        return "no-compare";
    }
}
