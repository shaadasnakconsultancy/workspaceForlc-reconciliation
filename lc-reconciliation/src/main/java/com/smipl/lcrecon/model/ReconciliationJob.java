package com.smipl.lcrecon.model;

import java.util.Date;

public class ReconciliationJob {
    private long id;
    private String uploadBatchId;
    private String lcNumber;
    private String jobName;
    private String shipmentDocName;
    private String status;
    private int totalSteps;
    private int completedSteps;
    private String currentStep;
    private String errorMessage;
    private String reportPath;
    private String nonComplianceReportPath;
    private String xlsxReportPath;
    private String xlsxNcReportPath;
    private boolean emailSent;
    private String emailError;
    private String notificationEmail;
    private long emailGroupId;
    private Date startedAt;
    private Date completedAt;
    private Date createdAt;
    private String createdBy;
    private String createdByDepartment;

    // Computed
    private int matchedCount;
    private int notMatchedCount;

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public String getUploadBatchId() { return uploadBatchId; }
    public void setUploadBatchId(String uploadBatchId) { this.uploadBatchId = uploadBatchId; }
    public String getLcNumber() { return lcNumber; }
    public void setLcNumber(String lcNumber) { this.lcNumber = lcNumber; }
    public String getJobName() { return jobName; }
    public void setJobName(String jobName) { this.jobName = jobName; }
    public String getShipmentDocName() { return shipmentDocName; }
    public void setShipmentDocName(String shipmentDocName) { this.shipmentDocName = shipmentDocName; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public int getTotalSteps() { return totalSteps; }
    public void setTotalSteps(int totalSteps) { this.totalSteps = totalSteps; }
    public int getCompletedSteps() { return completedSteps; }
    public void setCompletedSteps(int completedSteps) { this.completedSteps = completedSteps; }
    public String getCurrentStep() { return currentStep; }
    public void setCurrentStep(String currentStep) { this.currentStep = currentStep; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public String getReportPath() { return reportPath; }
    public void setReportPath(String reportPath) { this.reportPath = reportPath; }
    public String getNonComplianceReportPath() { return nonComplianceReportPath; }
    public void setNonComplianceReportPath(String nonComplianceReportPath) { this.nonComplianceReportPath = nonComplianceReportPath; }
    public String getXlsxReportPath() { return xlsxReportPath; }
    public void setXlsxReportPath(String xlsxReportPath) { this.xlsxReportPath = xlsxReportPath; }
    public String getXlsxNcReportPath() { return xlsxNcReportPath; }
    public void setXlsxNcReportPath(String xlsxNcReportPath) { this.xlsxNcReportPath = xlsxNcReportPath; }
    public boolean isEmailSent() { return emailSent; }
    public void setEmailSent(boolean emailSent) { this.emailSent = emailSent; }
    public String getEmailError() { return emailError; }
    public void setEmailError(String emailError) { this.emailError = emailError; }
    public String getNotificationEmail() { return notificationEmail; }
    public void setNotificationEmail(String notificationEmail) { this.notificationEmail = notificationEmail; }
    public long getEmailGroupId() { return emailGroupId; }
    public void setEmailGroupId(long emailGroupId) { this.emailGroupId = emailGroupId; }
    public Date getStartedAt() { return startedAt; }
    public void setStartedAt(Date startedAt) { this.startedAt = startedAt; }
    public Date getCompletedAt() { return completedAt; }
    public void setCompletedAt(Date completedAt) { this.completedAt = completedAt; }
    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    public String getCreatedByDepartment() { return createdByDepartment; }
    public void setCreatedByDepartment(String createdByDepartment) { this.createdByDepartment = createdByDepartment; }
    public int getMatchedCount() { return matchedCount; }
    public void setMatchedCount(int matchedCount) { this.matchedCount = matchedCount; }
    public int getNotMatchedCount() { return notMatchedCount; }
    public void setNotMatchedCount(int notMatchedCount) { this.notMatchedCount = notMatchedCount; }

    public int getProgressPercent() {
        if (totalSteps <= 0) return 0;
        return Math.min(100, (int) ((completedSteps * 100.0) / totalSteps));
    }

    public String getStatusBadgeClass() {
        switch (status != null ? status : "") {
            case "COMPLETED": return "bg-success";
            case "RUNNING": return "bg-primary";
            case "FAILED": return "bg-danger";
            case "ABORTED": return "bg-dark";
            case "QUEUED": return "bg-warning text-dark";
            default: return "bg-secondary";
        }
    }

    public String getJobDisplayName() {
        if (jobName != null && !jobName.isEmpty()) return jobName;
        if (lcNumber != null && !lcNumber.isEmpty()) return "LC-" + lcNumber;
        return "Job #" + id;
    }
}
