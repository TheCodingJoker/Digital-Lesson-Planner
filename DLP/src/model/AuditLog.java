package model;

public class AuditLog {
    private String entryId;
    private String userId;
    private String action;
    private String target;
    private String timestamp;

    public AuditLog() {}

    public String getEntryId() { return entryId; }
    public void setEntryId(String entryId) { this.entryId = entryId; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public String getTarget() { return target; }
    public void setTarget(String target) { this.target = target; }

    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
}