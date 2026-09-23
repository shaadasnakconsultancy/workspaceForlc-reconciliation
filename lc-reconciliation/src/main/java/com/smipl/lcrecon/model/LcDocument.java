package com.smipl.lcrecon.model;

import java.util.Date;

public class LcDocument {
    private long id;
    private String lcNumber;
    private String fileName;
    private String filePath;
    private long fileSize;
    private boolean addendum;
    private String uploadBatchId;
    private String ocrText;
    private String ocrStatus;
    private String extractedJson;
    private Date createdAt;
    private Date updatedAt;

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public String getLcNumber() { return lcNumber; }
    public void setLcNumber(String lcNumber) { this.lcNumber = lcNumber; }
    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }
    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }
    public long getFileSize() { return fileSize; }
    public void setFileSize(long fileSize) { this.fileSize = fileSize; }
    public boolean isAddendum() { return addendum; }
    public void setAddendum(boolean addendum) { this.addendum = addendum; }
    public String getUploadBatchId() { return uploadBatchId; }
    public void setUploadBatchId(String uploadBatchId) { this.uploadBatchId = uploadBatchId; }
    public String getOcrText() { return ocrText; }
    public void setOcrText(String ocrText) { this.ocrText = ocrText; }
    public String getOcrStatus() { return ocrStatus; }
    public void setOcrStatus(String ocrStatus) { this.ocrStatus = ocrStatus; }
    public String getExtractedJson() { return extractedJson; }
    public void setExtractedJson(String extractedJson) { this.extractedJson = extractedJson; }
    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
    public Date getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Date updatedAt) { this.updatedAt = updatedAt; }
}
