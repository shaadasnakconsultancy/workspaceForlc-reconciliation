package com.smipl.lcrecon.model;

import java.util.Date;

public class EmailGroupMember {
    private long id;
    private long groupId;
    private String emailAddress;
    private String memberName;
    private boolean active;
    private Date createdAt;

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public long getGroupId() { return groupId; }
    public void setGroupId(long groupId) { this.groupId = groupId; }
    public String getEmailAddress() { return emailAddress; }
    public void setEmailAddress(String emailAddress) { this.emailAddress = emailAddress; }
    public String getMemberName() { return memberName; }
    public void setMemberName(String memberName) { this.memberName = memberName; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
}
