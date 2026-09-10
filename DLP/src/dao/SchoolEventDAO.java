package dao;

import model.SchoolEvent;
import util.DatabaseConnection;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class SchoolEventDAO {
    
    public boolean createSchoolEvent(SchoolEvent event) {
        String sql = "INSERT INTO SchoolEvent (eventId, eventName, eventDate, eventType, description) VALUES (?, ?, ?, ?, ?)";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, event.getEventId());
            pstmt.setString(2, event.getEventName());
            pstmt.setString(3, event.getEventDate());
            pstmt.setString(4, event.getEventType());
            pstmt.setString(5, event.getDescription());
            
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error creating school event: " + e.getMessage());
            return false;
        }
    }

    public boolean updateSchoolEvent(SchoolEvent event) {
        String sql = "UPDATE SchoolEvent SET eventName = ?, eventDate = ?, eventType = ?, description = ? WHERE eventId = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, event.getEventName());
            pstmt.setString(2, event.getEventDate());
            pstmt.setString(3, event.getEventType());
            pstmt.setString(4, event.getDescription());
            pstmt.setString(5, event.getEventId());
            
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error updating school event: " + e.getMessage());
            return false;
        }
    }

    public boolean deleteSchoolEvent(String eventId) {
        String sql = "DELETE FROM SchoolEvent WHERE eventId = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, eventId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error deleting school event: " + e.getMessage());
            return false;
        }
    }

    public List<SchoolEvent> getAllSchoolEvents() {
        List<SchoolEvent> events = new ArrayList<>();
        String sql = "SELECT * FROM SchoolEvent ORDER BY eventDate ASC";
        
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                events.add(extractSchoolEvent(rs));
            }
        } catch (SQLException e) {
            System.err.println("Error getting school events: " + e.getMessage());
        }
        return events;
    }

    public SchoolEvent findByEventId(String eventId) {
        String sql = "SELECT * FROM SchoolEvent WHERE eventId = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, eventId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return extractSchoolEvent(rs);
            }
        } catch (SQLException e) {
            System.err.println("Error finding school event: " + e.getMessage());
        }
        return null;
    }

    private SchoolEvent extractSchoolEvent(ResultSet rs) throws SQLException {
        SchoolEvent event = new SchoolEvent();
        event.setEventId(rs.getString("eventId"));
        event.setEventName(rs.getString("eventName"));
        event.setEventDate(rs.getString("eventDate"));
        event.setEventType(rs.getString("eventType"));
        event.setDescription(rs.getString("description"));
        event.setCreatedAt(rs.getString("createdAt"));
        return event;
    }
}