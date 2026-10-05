package model;

public class Notification {

    private String notificationId;
    private String userId;
    private String type; // EVENT_DELETED, LESSON_RESCHEDULED, etc.
    private String title;
    private String message;
    private String relatedEntityId; // e.g., eventId, lessonId
    private boolean isRead;
    private String createdAt;

    public Notification() {}

    public Notification(String notificationId, String userId, String type,
                       String title, String message, String relatedEntityId) {
        this.notificationId = notificationId;
        this.userId = userId;
        this.type = type;
        this.title = title;
        this.message = message;
        this.relatedEntityId = relatedEntityId;
        this.isRead = false;
        this.createdAt = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date());
    }

    public String getNotificationId() {
        return notificationId;
    }

    public void setNotificationId(String notificationId) {
        this.notificationId = notificationId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getRelatedEntityId() {
        return relatedEntityId;
    }

    public void setRelatedEntityId(String relatedEntityId) {
        this.relatedEntityId = relatedEntityId;
    }

    public boolean isRead() {
        return isRead;
    }

    public void setRead(boolean read) {
        isRead = read;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }
}
