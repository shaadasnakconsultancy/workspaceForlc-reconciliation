package com.smipl.lcrecon.model;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

public class EmailGroup {
    private long id;
    private String groupName;
    private String description;
    private boolean active;
    private Date createdAt;
    private Date updatedAt;
    private int memberCount;
    private List<EmailGroupMember> members = new ArrayList<>();

    public int getMemberCount() { return memberCount; }
    public void setMemberCount(int memberCount) { this.memberCount = memberCount; }
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public String getGroupName() { return groupName; }
    public void setGroupName(String groupName) { this.groupName = groupName; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
    public Date getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Date updatedAt) { this.updatedAt = updatedAt; }
    public List<EmailGroupMember> getMembers() { return members; }
    public void setMembers(List<EmailGroupMember> members) { this.members = members; }

    /**
     * Returns comma-separated email list from active members.
     */
    public String getMemberEmails() {
        if (members == null || members.isEmpty()) {
            return "";
        }
        return members.stream()
                .filter(EmailGroupMember::isActive)
                .map(EmailGroupMember::getEmailAddress)
                .collect(Collectors.joining(","));
    }
}
