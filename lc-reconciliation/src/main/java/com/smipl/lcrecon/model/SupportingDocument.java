package com.smipl.lcrecon.model;

import java.util.Date;

public class SupportingDocument {
    private long id;
    private String uploadBatchId;
    private long documentTypeId;
    private String documentTypeName;
    private String documentTypeCode;
    private String fileName;
    private String filePath;
    private long fileSize;
    private int pageLimit;
    private String ocrText;
    private String ocrStatus;
    private String extractedJson;
    private Date createdAt;

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public String getUploadBatchId() { return uploadBatchId; }
    public void setUploadBatchId(String uploadBatchId) { this.uploadBatchId = uploadBatchId; }
    public long getDocumentTypeId() { return documentTypeId; }
    public void setDocumentTypeId(long documentTypeId) { this.documentTypeId = documentTypeId; }
    public String getDocumentTypeName() { return documentTypeName; }
    public void setDocumentTypeName(String documentTypeName) { this.documentTypeName = documentTypeName; }
    public String getDocumentTypeCode() { return documentTypeCode; }
    public void setDocumentTypeCode(String documentTypeCode) { this.documentTypeCode = documentTypeCode; }
    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }
    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }
    public long getFileSize() { return fileSize; }
    public void setFileSize(long fileSize) { this.fileSize = fileSize; }
    public int getPageLimit() { return pageLimit; }
    public void setPageLimit(int pageLimit) { this.pageLimit = pageLimit; }
    public String getOcrText() { return ocrText; }
    public void setOcrText(String ocrText) { this.ocrText = ocrText; }
    public String getOcrStatus() { return ocrStatus; }
    public void setOcrStatus(String ocrStatus) { this.ocrStatus = ocrStatus; }
    public String getExtractedJson() { return extractedJson; }
    public void setExtractedJson(String extractedJson) { this.extractedJson = extractedJson; }
    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
}
