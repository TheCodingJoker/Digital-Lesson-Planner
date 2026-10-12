package dao;

import model.SchoolEvent;
import util.DatabaseConnection;
import util.ErrorLogger;
import service.SchedulingEngine;
import java.sql.*;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class SchoolEventDAO {

    private ErrorLogger errorLogger = ErrorLogger.getInstance();

    public boolean createSchoolEvent(SchoolEvent event) {
        int academicYear;
        try {
            academicYear = LocalDate.parse(event.getEventDate()).getYear();
        } catch (DateTimeException e) {
            errorLogger.logError("SchoolEventDAO", "createSchoolEvent", "Invalid school event date", e);
            return false;
        }
        String sql = "INSERT INTO SchoolEvent (eventId, eventName, eventDate, eventType, description, academicYear, createdAt) VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            event.setAcademicYear(academicYear);
            pstmt.setString(1, event.getEventId());
            pstmt.setString(2, event.getEventName());
            pstmt.setString(3, event.getEventDate());
            pstmt.setString(4, event.getEventType());
            pstmt.setString(5, event.getDescription());
            pstmt.setInt(6, academicYear);
            pstmt.setString(7, event.getCreatedAt());

            boolean success = pstmt.executeUpdate() > 0;

            // Apply half-day constraints if this is a half-day event
            if (success && "HALF_DAY".equals(event.getEventType())) {
                SchedulingEngine.getInstance().applyHalfDayConstraints(event.getAcademicYear());
            }

            return success;
        } catch (SQLException e) {
            errorLogger.logError("SchoolEventDAO", "createSchoolEvent", "Error creating school event", e);
            return false;
        }
    }

    public boolean updateSchoolEvent(SchoolEvent event) {
        int academicYear;
        try {
            academicYear = LocalDate.parse(event.getEventDate()).getYear();
        } catch (DateTimeException e) {
            errorLogger.logError("SchoolEventDAO", "updateSchoolEvent", "Invalid school event date", e);
            return false;
        }
        String sql = "UPDATE SchoolEvent SET eventName = ?, eventDate = ?, eventType = ?, description = ?, academicYear = ? WHERE eventId = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            event.setAcademicYear(academicYear);
            pstmt.setString(1, event.getEventName());
            pstmt.setString(2, event.getEventDate());
            pstmt.setString(3, event.getEventType());
            pstmt.setString(4, event.getDescription());
            pstmt.setInt(5, academicYear);
            pstmt.setString(6, event.getEventId());

            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            errorLogger.logError("SchoolEventDAO", "updateSchoolEvent", "Error updating school event", e);
            return false;
        }
    }

    public boolean deleteSchoolEvent(String eventId) {
        // First, get the event details before deletion
        SchoolEvent event = getSchoolEventById(eventId);
        if (event == null) {
            return false;
        }

        String sql = "DELETE FROM SchoolEvent WHERE eventId = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, eventId);
            boolean success = pstmt.executeUpdate() > 0;

            // Notify scheduling engine about event deletion
            if (success) {
                SchedulingEngine.getInstance().handleEventDeletion(event);
            }

            return success;
        } catch (SQLException e) {
            errorLogger.logError("SchoolEventDAO", "deleteSchoolEvent", "Error deleting school event", e);
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
            errorLogger.logError("SchoolEventDAO", "getAllSchoolEvents", "Error getting school events", e);
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
            errorLogger.logError("SchoolEventDAO", "findByEventId", "Error finding school event", e);
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

        try {
            event.setAcademicYear(LocalDate.parse(event.getEventDate()).getYear());
        } catch (DateTimeException e) {
            event.setAcademicYear(rs.getInt("academicYear"));
        }
        
        return event;
    }
    
    public SchoolEvent getSchoolEventById(String eventId) {
        return findByEventId(eventId);
    }
}