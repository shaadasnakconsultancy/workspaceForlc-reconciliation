package com.smipl.lcrecon.model;

import java.util.Date;

public class PromptTemplate {
    private long id;
    private long documentTypeId;
    private String documentTypeName;
    private String promptName;
    private String promptText;
    private String responseSchema;
    private boolean active;
    private int version;
    private Date createdAt;
    private Date updatedAt;

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public long getDocumentTypeId() { return documentTypeId; }
    public void setDocumentTypeId(long documentTypeId) { this.documentTypeId = documentTypeId; }
    public String getDocumentTypeName() { return documentTypeName; }
    public void setDocumentTypeName(String documentTypeName) { this.documentTypeName = documentTypeName; }
    public String getPromptName() { return promptName; }
    public void setPromptName(String promptName) { this.promptName = promptName; }
    public String getPromptText() { return promptText; }
    public void setPromptText(String promptText) { this.promptText = promptText; }
    public String getResponseSchema() { return responseSchema; }
    public void setResponseSchema(String responseSchema) { this.responseSchema = responseSchema; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public int getVersion() { return version; }
    public void setVersion(int version) { this.version = version; }
    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
    public Date getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Date updatedAt) { this.updatedAt = updatedAt; }
}
