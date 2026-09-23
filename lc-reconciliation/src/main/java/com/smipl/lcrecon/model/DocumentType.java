package com.smipl.lcrecon.model;

import java.util.Date;

public class DocumentType {
    private long id;
    private String typeCode;
    private String typeName;
    private int pageLimit;
    private boolean active;
    private int displayOrder;
    private Date createdAt;
    private Date updatedAt;

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public String getTypeCode() { return typeCode; }
    public void setTypeCode(String typeCode) { this.typeCode = typeCode; }
    public String getTypeName() { return typeName; }
    public void setTypeName(String typeName) { this.typeName = typeName; }
    public int getPageLimit() { return pageLimit; }
    public void setPageLimit(int pageLimit) { this.pageLimit = pageLimit; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public int getDisplayOrder() { return displayOrder; }
    public void setDisplayOrder(int displayOrder) { this.displayOrder = displayOrder; }
    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
    public Date getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Date updatedAt) { this.updatedAt = updatedAt; }

    public String getPageLimitDisplay() {
        if (pageLimit <= 0) return "All Pages";
        if (pageLimit == 1) return "Page 1 only";
        return "Pages 1-" + pageLimit;
    }
}
