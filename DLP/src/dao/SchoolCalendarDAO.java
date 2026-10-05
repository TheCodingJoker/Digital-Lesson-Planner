package dao;

import model.SchoolCalendar;
import util.DatabaseConnection;
import util.ErrorLogger;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class SchoolCalendarDAO {

    private ErrorLogger errorLogger = ErrorLogger.getInstance();

    public boolean createSchoolCalendar(SchoolCalendar calendar) {
        String sql = "INSERT INTO SchoolCalendar (academicYear, totalTeachingDays, startDate, endDate) VALUES (?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, calendar.getAcademicYear());
            pstmt.setInt(2, calendar.getTotalTeachingDays());
            pstmt.setString(3, calendar.getStartDate());
            pstmt.setString(4, calendar.getEndDate());

            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            errorLogger.logError("SchoolCalendarDAO", "createSchoolCalendar", "Error creating school calendar", e);
            return false;
        }
    }

    public boolean updateSchoolCalendar(SchoolCalendar calendar) {
        String sql = "UPDATE SchoolCalendar SET totalTeachingDays = ?, startDate = ?, endDate = ? WHERE academicYear = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, calendar.getTotalTeachingDays());
            pstmt.setString(2, calendar.getStartDate());
            pstmt.setString(3, calendar.getEndDate());
            pstmt.setInt(4, calendar.getAcademicYear());

            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            errorLogger.logError("SchoolCalendarDAO", "updateSchoolCalendar", "Error updating school calendar", e);
            return false;
        }
    }

    public SchoolCalendar getSchoolCalendarByYear(int academicYear) {
        String sql = "SELECT * FROM SchoolCalendar WHERE academicYear = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, academicYear);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return extractSchoolCalendar(rs);
            }
        } catch (SQLException e) {
            errorLogger.logError("SchoolCalendarDAO", "getSchoolCalendarByYear", "Error getting school calendar", e);
        }
        return null;
    }

    public List<SchoolCalendar> getAllSchoolCalendars() {
        List<SchoolCalendar> calendars = new ArrayList<>();
        String sql = "SELECT * FROM SchoolCalendar ORDER BY academicYear DESC";

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                calendars.add(extractSchoolCalendar(rs));
            }
        } catch (SQLException e) {
            errorLogger.logError("SchoolCalendarDAO", "getAllSchoolCalendars", "Error getting all school calendars", e);
        }
        return calendars;
    }

    public boolean deleteSchoolCalendar(int academicYear) {
        String sql = "DELETE FROM SchoolCalendar WHERE academicYear = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, academicYear);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            errorLogger.logError("SchoolCalendarDAO", "deleteSchoolCalendar", "Error deleting school calendar", e);
            return false;
        }
    }

    public SchoolCalendar getCurrentOrDefaultCalendar() {
        // Try to get current year calendar
        int currentYear = java.time.Year.now().getValue();
        SchoolCalendar calendar = getSchoolCalendarByYear(currentYear);

        if (calendar == null) {
            // Create default calendar for current year
            calendar = new SchoolCalendar();
            calendar.setAcademicYear(currentYear);
            calendar.setTotalTeachingDays(180);
            calendar.setStartDate(currentYear + "-01-15"); // Default start: January 15
            calendar.setEndDate(currentYear + "-12-01");   // Default end: December 1

            if (createSchoolCalendar(calendar)) {
                errorLogger.logInfo("SchoolCalendarDAO", "getCurrentOrDefaultCalendar",
                    "Created default calendar for year: " + currentYear);
            }
        }

        return calendar;
    }

    private SchoolCalendar extractSchoolCalendar(ResultSet rs) throws SQLException {
        SchoolCalendar calendar = new SchoolCalendar();
        calendar.setAcademicYear(rs.getInt("academicYear"));
        calendar.setTotalTeachingDays(rs.getInt("totalTeachingDays"));
        calendar.setStartDate(rs.getString("startDate"));
        calendar.setEndDate(rs.getString("endDate"));
        return calendar;
    }
}
