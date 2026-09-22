package model;

import java.io.Serializable;

public class Feedback implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    private String feedbackId;
    private String userId;
    private String feedbackType; // "BUG", "SUGGESTION", "OTHER"
    private String title;
    private String description;
    private String status; // "OPEN", "IN_PROGRESS", "RESOLVED", "CLOSED"
    private String priority; // "LOW", "MEDIUM", "HIGH", "CRITICAL"
    private String createdAt;
    private String updatedAt;
    private String adminNotes;
    
    public Feedback() {}
    
    public String getFeedbackId() { return feedbackId; }
    public void setFeedbackId(String feedbackId) { this.feedbackId = feedbackId; }
    
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    
    public String getFeedbackType() { return feedbackType; }
    public void setFeedbackType(String feedbackType) { this.feedbackType = feedbackType; }
    
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }
    
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    
    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }
    
    public String getAdminNotes() { return adminNotes; }
    public void setAdminNotes(String adminNotes) { this.adminNotes = adminNotes; }
}