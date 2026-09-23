package com.smipl.lcrecon.model;

import java.util.Date;

public class User {
    private long id;
    private String username;
    private String passwordHash;
    private String firstName;
    private String lastName;
    private String email;
    private String department;
    private String role;
    private boolean active;
    private Date createdAt;
    private Date updatedAt;

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
    public Date getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Date updatedAt) { this.updatedAt = updatedAt; }

    public String getFullName() {
        String fn = firstName != null ? firstName : "";
        String ln = lastName != null ? lastName : "";
        return (fn + " " + ln).trim();
    }

    public boolean isItAdmin() {
        return "IT_ADMIN".equals(role);
    }

    public boolean isSuperAdmin() {
        return "SUPER_ADMIN".equals(role);
    }

    public String getRoleBadgeClass() {
        if (isItAdmin()) return "bg-dark";
        if (isSuperAdmin()) return "bg-danger";
        return "bg-info";
    }

    public String getRoleDisplay() {
        if (isItAdmin()) return "IT Admin";
        if (isSuperAdmin()) return "Super Admin";
        return "Department User";
    }
}
