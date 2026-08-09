
package dao;

import model.SchoolClass;
import util.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ClassDAO {

    public boolean createClass(SchoolClass schoolClass) {
        String sql = "INSERT INTO SchoolClass " +
            "(classId, teacherId, className, subject, gradeLevel, studentCount, notes) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, schoolClass.getClassId());
            pstmt.setString(2, schoolClass.getTeacherId());
            pstmt.setString(3, schoolClass.getClassName());
            pstmt.setString(4, schoolClass.getSubject());
            pstmt.setString(5, schoolClass.getGradeLevel());
            pstmt.setInt(6, schoolClass.getStudentCount());
            pstmt.setString(7, schoolClass.getNotes());

            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error creating class: " + e.getMessage());
            return false;
        }
    }

    public boolean updateClass(SchoolClass schoolClass) {
        String sql = "UPDATE SchoolClass SET className = ?, subject = ?, gradeLevel = ?, " +
            "studentCount = ?, notes = ? WHERE classId = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, schoolClass.getClassName());
            pstmt.setString(2, schoolClass.getSubject());
            pstmt.setString(3, schoolClass.getGradeLevel());
            pstmt.setInt(4, schoolClass.getStudentCount());
            pstmt.setString(5, schoolClass.getNotes());
            pstmt.setString(6, schoolClass.getClassId());

            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error updating class: " + e.getMessage());
            return false;
        }
    }

    public boolean deleteClass(String classId) {
        String sql = "DELETE FROM SchoolClass WHERE classId = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, classId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error deleting class: " + e.getMessage());
            return false;
        }
    }

    public List<SchoolClass> findByTeacher(String teacherId) {
        List<SchoolClass> classes = new ArrayList<>();
        String sql = "SELECT * FROM SchoolClass WHERE teacherId = ? ORDER BY className ASC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, teacherId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                classes.add(extractClass(rs));
            }
        } catch (SQLException e) {
            System.err.println("Error finding classes: " + e.getMessage());
        }
        return classes;
    }

    public int countByTeacher(String teacherId) {
        String sql = "SELECT COUNT(*) AS total FROM SchoolClass WHERE teacherId = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, teacherId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) return rs.getInt("total");
        } catch (SQLException e) {
            System.err.println("Error counting classes: " + e.getMessage());
        }
        return 0;
    }

    private SchoolClass extractClass(ResultSet rs) throws SQLException {
        SchoolClass schoolClass = new SchoolClass();
        schoolClass.setClassId(rs.getString("classId"));
        schoolClass.setTeacherId(rs.getString("teacherId"));
        schoolClass.setClassName(rs.getString("className"));
        schoolClass.setSubject(rs.getString("subject"));
        schoolClass.setGradeLevel(rs.getString("gradeLevel"));
        schoolClass.setStudentCount(rs.getInt("studentCount"));
        schoolClass.setNotes(rs.getString("notes"));
        schoolClass.setCreatedAt(rs.getString("createdAt"));
        return schoolClass;
    }
}
