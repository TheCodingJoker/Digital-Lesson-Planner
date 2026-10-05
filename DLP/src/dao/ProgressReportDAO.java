package dao;

import model.ProgressReport;
import util.DatabaseConnection;
import util.ErrorLogger;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ProgressReportDAO {

    private ErrorLogger errorLogger = ErrorLogger.getInstance();

    public boolean createProgressReport(ProgressReport report) {
        String sql = "INSERT INTO ProgressReport (reportId, teacherId, subject, completionPercentage, " +
                     "totalLessons, completedLessons, generatedAt, term) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, report.getReportId());
            pstmt.setString(2, report.getTeacherId());
            pstmt.setString(3, report.getSubject());
            pstmt.setDouble(4, report.getCompletionPercentage());
            pstmt.setInt(5, report.getTotalLessons());
            pstmt.setInt(6, report.getCompletedLessons());
            pstmt.setString(7, report.getGeneratedAt());
            pstmt.setString(8, report.getTerm());

            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            errorLogger.logError("ProgressReportDAO", "createProgressReport", "Error creating progress report", e);
            return false;
        }
    }

    public ProgressReport getProgressReportById(String reportId) {
        String sql = "SELECT * FROM ProgressReport WHERE reportId = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, reportId);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return extractProgressReport(rs);
            }
        } catch (SQLException e) {
            errorLogger.logError("ProgressReportDAO", "getProgressReportById", "Error getting progress report", e);
        }
        return null;
    }

    public List<ProgressReport> getProgressReportsByTeacher(String teacherId) {
        List<ProgressReport> reports = new ArrayList<>();
        String sql = "SELECT * FROM ProgressReport WHERE teacherId = ? ORDER BY generatedAt DESC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, teacherId);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                reports.add(extractProgressReport(rs));
            }
        } catch (SQLException e) {
            errorLogger.logError("ProgressReportDAO", "getProgressReportsByTeacher", "Error getting progress reports", e);
        }
        return reports;
    }

    public List<ProgressReport> getProgressReportsByTeacherAndTerm(String teacherId, String term) {
        List<ProgressReport> reports = new ArrayList<>();
        String sql = "SELECT * FROM ProgressReport WHERE teacherId = ? AND term = ? ORDER BY generatedAt DESC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, teacherId);
            pstmt.setString(2, term);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                reports.add(extractProgressReport(rs));
            }
        } catch (SQLException e) {
            errorLogger.logError("ProgressReportDAO", "getProgressReportsByTeacherAndTerm", "Error getting progress reports", e);
        }
        return reports;
    }

    public List<ProgressReport> getAllProgressReports() {
        List<ProgressReport> reports = new ArrayList<>();
        String sql = "SELECT * FROM ProgressReport ORDER BY generatedAt DESC";

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                reports.add(extractProgressReport(rs));
            }
        } catch (SQLException e) {
            errorLogger.logError("ProgressReportDAO", "getAllProgressReports", "Error getting all progress reports", e);
        }
        return reports;
    }

    public List<ProgressReport> getLatestProgressReportsByTeacher() {
        // Get the most recent progress report for each teacher
        List<ProgressReport> reports = new ArrayList<>();
        String sql = "SELECT * FROM ProgressReport pr1 WHERE generatedAt = " +
                     "(SELECT MAX(generatedAt) FROM ProgressReport pr2 WHERE pr2.teacherId = pr1.teacherId) " +
                     "ORDER BY generatedAt DESC";

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                reports.add(extractProgressReport(rs));
            }
        } catch (SQLException e) {
            errorLogger.logError("ProgressReportDAO", "getLatestProgressReportsByTeacher", "Error getting latest progress reports", e);
        }
        return reports;
    }

    public boolean deleteProgressReport(String reportId) {
        String sql = "DELETE FROM ProgressReport WHERE reportId = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, reportId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            errorLogger.logError("ProgressReportDAO", "deleteProgressReport", "Error deleting progress report", e);
            return false;
        }
    }

    public boolean deleteProgressReportsByTeacher(String teacherId) {
        String sql = "DELETE FROM ProgressReport WHERE teacherId = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, teacherId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            errorLogger.logError("ProgressReportDAO", "deleteProgressReportsByTeacher", "Error deleting progress reports", e);
            return false;
        }
    }

    /**
     * Generate and save a progress report for a teacher
     */
    public ProgressReport generateProgressReport(String teacherId, String subject, String term) {
        LessonPlanDAO lessonPlanDAO = new LessonPlanDAO();
        List<model.LessonPlan> lessons = lessonPlanDAO.findByTeacher(teacherId);

        // Filter by subject and term if provided
        if (subject != null && !subject.isEmpty()) {
            lessons.removeIf(lesson -> !subject.equals(lesson.getSubject()));
        }
        if (term != null && !term.isEmpty()) {
            lessons.removeIf(lesson -> !term.equals(lesson.getTerm()));
        }

        int totalLessons = lessons.size();
        int completedLessons = 0;

        for (model.LessonPlan lesson : lessons) {
            if ("COMPLETED".equals(lesson.getStatus())) {
                completedLessons++;
            }
        }

        double completionPercentage = totalLessons > 0 ? (completedLessons * 100.0 / totalLessons) : 0;

        String reportId = UUID.randomUUID().toString();
        String generatedAt = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date());

        ProgressReport report = new ProgressReport(
            reportId,
            teacherId,
            subject != null ? subject : "All Subjects",
            completionPercentage,
            totalLessons,
            completedLessons,
            generatedAt,
            term != null ? term : "All Terms"
        );

        if (createProgressReport(report)) {
            return report;
        }

        return null;
    }

    private ProgressReport extractProgressReport(ResultSet rs) throws SQLException {
        ProgressReport report = new ProgressReport();
        report.setReportId(rs.getString("reportId"));
        report.setTeacherId(rs.getString("teacherId"));
        report.setSubject(rs.getString("subject"));
        report.setCompletionPercentage(rs.getDouble("completionPercentage"));
        report.setTotalLessons(rs.getInt("totalLessons"));
        report.setCompletedLessons(rs.getInt("completedLessons"));
        report.setGeneratedAt(rs.getString("generatedAt"));
        report.setTerm(rs.getString("term"));
        return report;
    }
}
