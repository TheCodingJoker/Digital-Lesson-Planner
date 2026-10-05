
package dao;

import model.LessonPlan;
import model.CAPSEntry;
import util.DatabaseConnection;
import util.ErrorLogger;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class LessonPlanDAO {
    
    private CAPSSnapshotDAO capsSnapshotDAO = new CAPSSnapshotDAO();
    private ErrorLogger errorLogger = ErrorLogger.getInstance();

    public boolean createLessonPlan(LessonPlan plan, CAPSEntry capsEntry) {
        // Create CAPS snapshot if CAPS entry is provided
        if (capsEntry != null) {
            String snapshotId = UUID.randomUUID().toString();
            plan.setSnapshotId(snapshotId);
            
            // Create the snapshot
            if (!capsSnapshotDAO.createSnapshot(capsEntry)) {
                System.err.println("Warning: Failed to create CAPS snapshot for lesson plan");
            }
        }
        
        String sql = "INSERT INTO LessonPlan " +
            "(lessonPlanId, teacherId, title, subject, gradeLevel, curriculumReference, " +
            "topic, durationMinutes, objectives, teachingActivities, assessmentMethod, resources, " +
            "lessonDate, status, snapshotId) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

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
            pstmt.setString(15, plan.getSnapshotId());

            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            String errorMsg = "Error creating lesson plan for teacher: " + plan.getTeacherId();
            System.err.println(errorMsg + ": " + e.getMessage());
            errorLogger.logError("LessonPlanDAO", "createLessonPlan", errorMsg, e);
            return false;
        }
    }

    public boolean createLessonPlan(LessonPlan plan) {
        return createLessonPlan(plan, null);
    }

    public boolean updateLessonPlan(LessonPlan plan) {
        String sql = "UPDATE LessonPlan SET title = ?, subject = ?, gradeLevel = ?, " +
            "curriculumReference = ?, topic = ?, durationMinutes = ?, objectives = ?, " +
            "teachingActivities = ?, assessmentMethod = ?, resources = ?, lessonDate = ?, " +
            "status = ?, reschedulingNote = ?, updatedAt = datetime('now','localtime') WHERE lessonPlanId = ?";

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
            pstmt.setString(13, plan.getReschedulingNote());
            pstmt.setString(14, plan.getLessonPlanId());

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

    public List<LessonPlan> getLessonsByUserId(String userId) {
        return findByTeacher(userId);
    }

    public List<LessonPlan> getAllLessonPlans() {
        List<LessonPlan> plans = new ArrayList<>();
        String sql = "SELECT * FROM LessonPlan ORDER BY lessonDate DESC, createdAt DESC";

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                plans.add(extractLessonPlan(rs));
            }
        } catch (SQLException e) {
            System.err.println("Error getting all lesson plans: " + e.getMessage());
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

    /**
     * Search lesson plans with comprehensive filters
     */
    public List<LessonPlan> searchLessonPlans(String teacherId, String subject, String gradeLevel,
                                               String term, String status, String startDate, String endDate,
                                               String keyword) {
        List<LessonPlan> plans = new ArrayList<>();
        StringBuilder sql = new StringBuilder("SELECT * FROM LessonPlan WHERE 1=1");
        List<Object> params = new ArrayList<>();

        if (teacherId != null && !teacherId.isEmpty()) {
            sql.append(" AND teacherId = ?");
            params.add(teacherId);
        }

        if (subject != null && !subject.isEmpty()) {
            sql.append(" AND subject = ?");
            params.add(subject);
        }

        if (gradeLevel != null && !gradeLevel.isEmpty()) {
            sql.append(" AND gradeLevel = ?");
            params.add(gradeLevel);
        }

        if (term != null && !term.isEmpty()) {
            sql.append(" AND curriculumReference = ?");
            params.add(term);
        }

        if (status != null && !status.isEmpty()) {
            sql.append(" AND status = ?");
            params.add(status);
        }

        if (startDate != null && !startDate.isEmpty()) {
            sql.append(" AND lessonDate >= ?");
            params.add(startDate);
        }

        if (endDate != null && !endDate.isEmpty()) {
            sql.append(" AND lessonDate <= ?");
            params.add(endDate);
        }

        if (keyword != null && !keyword.isEmpty()) {
            sql.append(" AND (title LIKE ? OR topic LIKE ? OR objectives LIKE ?)");
            String keywordPattern = "%" + keyword + "%";
            params.add(keywordPattern);
            params.add(keywordPattern);
            params.add(keywordPattern);
        }

        sql.append(" ORDER BY lessonDate DESC, createdAt DESC");

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql.toString())) {

            for (int i = 0; i < params.size(); i++) {
                pstmt.setObject(i + 1, params.get(i));
            }

            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                plans.add(extractLessonPlan(rs));
            }
        } catch (SQLException e) {
            errorLogger.logError("LessonPlanDAO", "searchLessonPlans", "Error searching lesson plans", e);
        }
        return plans;
    }

    /**
     * Get distinct subjects for a teacher
     */
    public List<String> getDistinctSubjects(String teacherId) {
        List<String> subjects = new ArrayList<>();
        String sql = "SELECT DISTINCT subject FROM LessonPlan WHERE teacherId = ? ORDER BY subject";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, teacherId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                subjects.add(rs.getString("subject"));
            }
        } catch (SQLException e) {
            System.err.println("Error getting distinct subjects: " + e.getMessage());
        }
        return subjects;
    }

    /**
     * Get distinct grade levels for a teacher
     */
    public List<String> getDistinctGradeLevels(String teacherId) {
        List<String> gradeLevels = new ArrayList<>();
        String sql = "SELECT DISTINCT gradeLevel FROM LessonPlan WHERE teacherId = ? ORDER BY gradeLevel";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, teacherId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                gradeLevels.add(rs.getString("gradeLevel"));
            }
        } catch (SQLException e) {
            System.err.println("Error getting distinct grade levels: " + e.getMessage());
        }
        return gradeLevels;
    }

    /**
     * Get distinct terms for a teacher
     */
    public List<String> getDistinctTerms(String teacherId) {
        List<String> terms = new ArrayList<>();
        String sql = "SELECT DISTINCT curriculumReference FROM LessonPlan WHERE teacherId = ? ORDER BY curriculumReference";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, teacherId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                terms.add(rs.getString("curriculumReference"));
            }
        } catch (SQLException e) {
            System.err.println("Error getting distinct terms: " + e.getMessage());
        }
        return terms;
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

        // Extract snapshotId if available
        try {
            plan.setSnapshotId(rs.getString("snapshotId"));
        } catch (SQLException e) {
            // Column might not exist in older database versions
            plan.setSnapshotId(null);
        }

        // Extract reschedulingNote if available
        try {
            plan.setReschedulingNote(rs.getString("reschedulingNote"));
        } catch (SQLException e) {
            // Column might not exist in older database versions
            plan.setReschedulingNote(null);
        }

        return plan;
    }
}
