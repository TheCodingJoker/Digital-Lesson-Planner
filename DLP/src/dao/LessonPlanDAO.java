
package dao;

import model.LessonPlan;
import util.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class LessonPlanDAO {

    public boolean createLessonPlan(LessonPlan plan) {
        String sql = "INSERT INTO LessonPlan " +
            "(lessonPlanId, teacherId, title, subject, gradeLevel, curriculumReference, " +
            "topic, durationMinutes, objectives, teachingActivities, assessmentMethod, resources, " +
            "lessonDate, status) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, plan.getLessonPlanId());
            pstmt.setString(2, plan.getTeacherId());
            pstmt.setString(3, plan.getTitle());
            pstmt.setString(4, plan.getSubject());
            pstmt.setString(5, plan.getGradeLevel());
            pstmt.setString(6, plan.getTerm());
            pstmt.setString(7, plan.getTopic());
            pstmt.setInt(8, plan.getDurationMinutes());
            pstmt.setString(9, plan.getObjectives());
            pstmt.setString(10, plan.getTeachingActivities());
            pstmt.setString(11, plan.getAssessmentMethod());
            pstmt.setString(12, plan.getResources());
            pstmt.setString(13, plan.getLessonDate());
            pstmt.setString(14, plan.getStatus());

            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error creating lesson plan: " + e.getMessage());
            return false;
        }
    }

    public boolean updateLessonPlan(LessonPlan plan) {
        String sql = "UPDATE LessonPlan SET title = ?, subject = ?, gradeLevel = ?, " +
            "curriculumReference = ?, topic = ?, durationMinutes = ?, objectives = ?, " +
            "teachingActivities = ?, assessmentMethod = ?, resources = ?, lessonDate = ?, " +
            "status = ?, updatedAt = datetime('now','localtime') WHERE lessonPlanId = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, plan.getTitle());
            pstmt.setString(2, plan.getSubject());
            pstmt.setString(3, plan.getGradeLevel());
            pstmt.setString(4, plan.getTerm());
            pstmt.setString(5, plan.getTopic());
            pstmt.setInt(6, plan.getDurationMinutes());
            pstmt.setString(7, plan.getObjectives());
            pstmt.setString(8, plan.getTeachingActivities());
            pstmt.setString(9, plan.getAssessmentMethod());
            pstmt.setString(10, plan.getResources());
            pstmt.setString(11, plan.getLessonDate());
            pstmt.setString(12, plan.getStatus());
            pstmt.setString(13, plan.getLessonPlanId());

            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error updating lesson plan: " + e.getMessage());
            return false;
        }
    }

    public boolean updateStatus(String lessonPlanId, String status) {
        String sql = "UPDATE LessonPlan SET status = ?, updatedAt = datetime('now','localtime') WHERE lessonPlanId = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, status);
            pstmt.setString(2, lessonPlanId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error updating lesson plan status: " + e.getMessage());
            return false;
        }
    }

    public boolean deleteLessonPlan(String lessonPlanId) {
        String sql = "DELETE FROM LessonPlan WHERE lessonPlanId = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, lessonPlanId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error deleting lesson plan: " + e.getMessage());
            return false;
        }
    }

    public List<LessonPlan> findByTeacher(String teacherId) {
        List<LessonPlan> plans = new ArrayList<>();
        String sql = "SELECT * FROM LessonPlan WHERE teacherId = ? ORDER BY lessonDate DESC, createdAt DESC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, teacherId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                plans.add(extractLessonPlan(rs));
            }
        } catch (SQLException e) {
            System.err.println("Error finding lesson plans: " + e.getMessage());
        }
        return plans;
    }

    /** Lesson plans for a teacher whose lessonDate falls within [monthStart, monthEnd] inclusive (ISO yyyy-MM-dd). */
    public List<LessonPlan> findByTeacherAndDateRange(String teacherId, String startIso, String endIso) {
        List<LessonPlan> plans = new ArrayList<>();
        String sql = "SELECT * FROM LessonPlan WHERE teacherId = ? AND lessonDate BETWEEN ? AND ? ORDER BY lessonDate ASC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, teacherId);
            pstmt.setString(2, startIso);
            pstmt.setString(3, endIso);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                plans.add(extractLessonPlan(rs));
            }
        } catch (SQLException e) {
            System.err.println("Error finding lesson plans in range: " + e.getMessage());
        }
        return plans;
    }

    public LessonPlan findById(String lessonPlanId) {
        String sql = "SELECT * FROM LessonPlan WHERE lessonPlanId = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, lessonPlanId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return extractLessonPlan(rs);
            }
        } catch (SQLException e) {
            System.err.println("Error finding lesson plan: " + e.getMessage());
        }
        return null;
    }

    public int countByTeacher(String teacherId) {
        String sql = "SELECT COUNT(*) AS total FROM LessonPlan WHERE teacherId = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, teacherId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) return rs.getInt("total");
        } catch (SQLException e) {
            System.err.println("Error counting lesson plans: " + e.getMessage());
        }
        return 0;
    }

    public int countByTeacherAndStatus(String teacherId, String status) {
        String sql = "SELECT COUNT(*) AS total FROM LessonPlan WHERE teacherId = ? AND status = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, teacherId);
            pstmt.setString(2, status);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) return rs.getInt("total");
        } catch (SQLException e) {
            System.err.println("Error counting lesson plans by status: " + e.getMessage());
        }
        return 0;
    }

    private LessonPlan extractLessonPlan(ResultSet rs) throws SQLException {
        LessonPlan plan = new LessonPlan();
        plan.setLessonPlanId(rs.getString("lessonPlanId"));
        plan.setTeacherId(rs.getString("teacherId"));
        plan.setTitle(rs.getString("title"));
        plan.setSubject(rs.getString("subject"));
        plan.setGradeLevel(rs.getString("gradeLevel"));
        plan.setTerm(rs.getString("curriculumReference"));
        plan.setTopic(rs.getString("topic"));
        plan.setDurationMinutes(rs.getInt("durationMinutes"));
        plan.setObjectives(rs.getString("objectives"));
        plan.setTeachingActivities(rs.getString("teachingActivities"));
        plan.setAssessmentMethod(rs.getString("assessmentMethod"));
        plan.setResources(rs.getString("resources"));
        plan.setLessonDate(rs.getString("lessonDate"));
        plan.setStatus(rs.getString("status"));
        plan.setCreatedAt(rs.getString("createdAt"));
        plan.setUpdatedAt(rs.getString("updatedAt"));
        return plan;
    }
}
