package com.smipl.lcrecon.model;

import java.math.BigDecimal;
import java.util.Date;

public class JobApiCost {
    private long id;
    private long jobId;
    private String apiType; // "OCR" or "GPT"
    private String documentName;
    private int promptTokens;
    private int completionTokens;
    private int totalTokens;
    private int pagesProcessed;
    private BigDecimal costInr;
    private Date createdAt;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getJobId() {
        return jobId;
    }

    public void setJobId(long jobId) {
        this.jobId = jobId;
    }

    public String getApiType() {
        return apiType;
    }

    public void setApiType(String apiType) {
        this.apiType = apiType;
    }

    public String getDocumentName() {
        return documentName;
    }

    public void setDocumentName(String documentName) {
        this.documentName = documentName;
    }

    public int getPromptTokens() {
        return promptTokens;
    }

    public void setPromptTokens(int promptTokens) {
        this.promptTokens = promptTokens;
    }

    public int getCompletionTokens() {
        return completionTokens;
    }

    public void setCompletionTokens(int completionTokens) {
        this.completionTokens = completionTokens;
    }

    public int getTotalTokens() {
        return totalTokens;
    }

    public void setTotalTokens(int totalTokens) {
        this.totalTokens = totalTokens;
    }

    public int getPagesProcessed() {
        return pagesProcessed;
    }

    public void setPagesProcessed(int pagesProcessed) {
        this.pagesProcessed = pagesProcessed;
    }

    public BigDecimal getCostInr() {
        return costInr;
    }

    public void setCostInr(BigDecimal costInr) {
        this.costInr = costInr;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }
}
