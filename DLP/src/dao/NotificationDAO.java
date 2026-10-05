package dao;

import model.Notification;
import util.DatabaseConnection;
import util.ErrorLogger;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class NotificationDAO {

    private ErrorLogger errorLogger = ErrorLogger.getInstance();

    public boolean createNotification(Notification notification) {
        String sql = "INSERT INTO Notification (notificationId, userId, type, title, message, " +
                     "relatedEntityId, isRead, createdAt) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, notification.getNotificationId());
            pstmt.setString(2, notification.getUserId());
            pstmt.setString(3, notification.getType());
            pstmt.setString(4, notification.getTitle());
            pstmt.setString(5, notification.getMessage());
            pstmt.setString(6, notification.getRelatedEntityId());
            pstmt.setBoolean(7, notification.isRead());
            pstmt.setString(8, notification.getCreatedAt());

            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            errorLogger.logError("NotificationDAO", "createNotification", "Error creating notification", e);
            return false;
        }
    }

    public List<Notification> getNotificationsByUser(String userId) {
        List<Notification> notifications = new ArrayList<>();
        String sql = "SELECT * FROM Notification WHERE userId = ? ORDER BY createdAt DESC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, userId);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                notifications.add(extractNotification(rs));
            }
        } catch (SQLException e) {
            errorLogger.logError("NotificationDAO", "getNotificationsByUser", "Error getting notifications", e);
        }
        return notifications;
    }

    public List<Notification> getUnreadNotificationsByUser(String userId) {
        List<Notification> notifications = new ArrayList<>();
        String sql = "SELECT * FROM Notification WHERE userId = ? AND isRead = 0 ORDER BY createdAt DESC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, userId);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                notifications.add(extractNotification(rs));
            }
        } catch (SQLException e) {
            errorLogger.logError("NotificationDAO", "getUnreadNotificationsByUser", "Error getting unread notifications", e);
        }
        return notifications;
    }

    public boolean markAsRead(String notificationId) {
        String sql = "UPDATE Notification SET isRead = 1 WHERE notificationId = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, notificationId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            errorLogger.logError("NotificationDAO", "markAsRead", "Error marking notification as read", e);
            return false;
        }
    }

    public boolean markAllAsRead(String userId) {
        String sql = "UPDATE Notification SET isRead = 1 WHERE userId = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, userId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            errorLogger.logError("NotificationDAO", "markAllAsRead", "Error marking all notifications as read", e);
            return false;
        }
    }

    public boolean deleteNotification(String notificationId) {
        String sql = "DELETE FROM Notification WHERE notificationId = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, notificationId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            errorLogger.logError("NotificationDAO", "deleteNotification", "Error deleting notification", e);
            return false;
        }
    }

    private Notification extractNotification(ResultSet rs) throws SQLException {
        Notification notification = new Notification();
        notification.setNotificationId(rs.getString("notificationId"));
        notification.setUserId(rs.getString("userId"));
        notification.setType(rs.getString("type"));
        notification.setTitle(rs.getString("title"));
        notification.setMessage(rs.getString("message"));
        notification.setRelatedEntityId(rs.getString("relatedEntityId"));
        notification.setRead(rs.getBoolean("isRead"));
        notification.setCreatedAt(rs.getString("createdAt"));
        return notification;
    }
}
