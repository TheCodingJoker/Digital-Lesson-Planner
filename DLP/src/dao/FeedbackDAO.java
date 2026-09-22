package dao;

import model.Feedback;
import util.DatabaseConnection;
import util.ErrorLogger;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class FeedbackDAO {
    
    private ErrorLogger errorLogger;
    
    public FeedbackDAO() {
        errorLogger = ErrorLogger.getInstance();
    }
    
    public boolean createFeedback(Feedback feedback) {
        String sql = "INSERT INTO Feedback (feedbackId, userId, feedbackType, title, description, status, priority, createdAt, updatedAt) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, feedback.getFeedbackId());
            pstmt.setString(2, feedback.getUserId());
            pstmt.setString(3, feedback.getFeedbackType());
            pstmt.setString(4, feedback.getTitle());
            pstmt.setString(5, feedback.getDescription());
            pstmt.setString(6, feedback.getStatus());
            pstmt.setString(7, feedback.getPriority());
            pstmt.setString(8, feedback.getCreatedAt());
            pstmt.setString(9, feedback.getUpdatedAt());
            
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            errorLogger.logError("FeedbackDAO", "createFeedback", "Failed to create feedback", e);
            return false;
        }
    }
    
    public boolean updateFeedback(Feedback feedback) {
        String sql = "UPDATE Feedback SET status = ?, priority = ?, adminNotes = ?, updatedAt = ? WHERE feedbackId = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, feedback.getStatus());
            pstmt.setString(2, feedback.getPriority());
            pstmt.setString(3, feedback.getAdminNotes());
            pstmt.setString(4, feedback.getUpdatedAt());
            pstmt.setString(5, feedback.getFeedbackId());
            
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            errorLogger.logError("FeedbackDAO", "updateFeedback", "Failed to update feedback", e);
            return false;
        }
    }
    
    public List<Feedback> getAllFeedback() {
        List<Feedback> feedbackList = new ArrayList<>();
        String sql = "SELECT * FROM Feedback ORDER BY createdAt DESC";
        
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                feedbackList.add(extractFeedback(rs));
            }
        } catch (SQLException e) {
            errorLogger.logError("FeedbackDAO", "getAllFeedback", "Failed to get all feedback", e);
        }
        return feedbackList;
    }
    
    public List<Feedback> getFeedbackByUserId(String userId) {
        List<Feedback> feedbackList = new ArrayList<>();
        String sql = "SELECT * FROM Feedback WHERE userId = ? ORDER BY createdAt DESC";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, userId);
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                feedbackList.add(extractFeedback(rs));
            }
        } catch (SQLException e) {
            errorLogger.logError("FeedbackDAO", "getFeedbackByUserId", "Failed to get feedback by user", e);
        }
        return feedbackList;
    }
    
    public Feedback findByFeedbackId(String feedbackId) {
        String sql = "SELECT * FROM Feedback WHERE feedbackId = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, feedbackId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return extractFeedback(rs);
            }
        } catch (SQLException e) {
            errorLogger.logError("FeedbackDAO", "findByFeedbackId", "Failed to find feedback", e);
        }
        return null;
    }
    
    private Feedback extractFeedback(ResultSet rs) throws SQLException {
        Feedback feedback = new Feedback();
        feedback.setFeedbackId(rs.getString("feedbackId"));
        feedback.setUserId(rs.getString("userId"));
        feedback.setFeedbackType(rs.getString("feedbackType"));
        feedback.setTitle(rs.getString("title"));
        feedback.setDescription(rs.getString("description"));
        feedback.setStatus(rs.getString("status"));
        feedback.setPriority(rs.getString("priority"));
        feedback.setCreatedAt(rs.getString("createdAt"));
        feedback.setUpdatedAt(rs.getString("updatedAt"));
        feedback.setAdminNotes(rs.getString("adminNotes"));
        return feedback;
    }
}