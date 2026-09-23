package com.smipl.lcrecon.model;

import java.util.Date;
import java.util.Map;

public class EmailTemplate {
    private long id;
    private String templateName;
    private String subjectTemplate;
    private String bodyTemplate;
    private boolean defaultTemplate;
    private boolean active;
    private Date createdAt;
    private Date updatedAt;

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public String getTemplateName() { return templateName; }
    public void setTemplateName(String templateName) { this.templateName = templateName; }
    public String getSubjectTemplate() { return subjectTemplate; }
    public void setSubjectTemplate(String subjectTemplate) { this.subjectTemplate = subjectTemplate; }
    public String getBodyTemplate() { return bodyTemplate; }
    public void setBodyTemplate(String bodyTemplate) { this.bodyTemplate = bodyTemplate; }
    public boolean isDefaultTemplate() { return defaultTemplate; }
    public void setDefaultTemplate(boolean defaultTemplate) { this.defaultTemplate = defaultTemplate; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
    public Date getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Date updatedAt) { this.updatedAt = updatedAt; }

    /**
     * Resolves the subject template by replacing placeholders like {{JOB_ID}}, {{JOB_NAME}}, etc.
     */
    public String resolveSubject(Map<String, String> placeholders) {
        return resolvePlaceholders(subjectTemplate, placeholders);
    }

    /**
     * Resolves the body template by replacing placeholders and converting to HTML.
     * Converts newlines to <br> and wraps in HTML body with styling.
     */
    public String resolveBody(Map<String, String> placeholders) {
        String resolved = resolvePlaceholders(bodyTemplate, placeholders);
        if (resolved == null) return "";

        // If body already contains HTML tags, return as-is
        if (resolved.contains("<html") || resolved.contains("<table") || resolved.contains("<div")) {
            return resolved;
        }

        // Convert plain text to HTML email
        String htmlBody = resolved
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\n", "<br>\n");

        return "<html><body style='font-family:Arial,sans-serif;font-size:14px;color:#333;line-height:1.6'>"
                + htmlBody
                + "</body></html>";
    }

    private String resolvePlaceholders(String template, Map<String, String> placeholders) {
        if (template == null || placeholders == null) {
            return template;
        }
        String resolved = template;
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            String placeholder = "{{" + entry.getKey() + "}}";
            String value = entry.getValue() != null ? entry.getValue() : "";
            resolved = resolved.replace(placeholder, value);
        }
        return resolved;
    }
}
