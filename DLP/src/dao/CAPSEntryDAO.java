package dao;

import model.CAPSEntry;
import util.DatabaseConnection;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CAPSEntryDAO {
    
    public boolean createCAPSEntry(CAPSEntry entry) {
        String sql = "INSERT INTO CAPSEntry (capsCode, subject, gradeLevel, term, topic, outcomes, assessmentStandards) VALUES (?, ?, ?, ?, ?, ?, ?)";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, entry.getCapsCode());
            pstmt.setString(2, entry.getSubject());
            pstmt.setString(3, entry.getGradeLevel());
            pstmt.setInt(4, entry.getTerm());
            pstmt.setString(5, entry.getTopic());
            pstmt.setString(6, entry.getOutcomes());
            pstmt.setString(7, entry.getAssessmentStandards());
            
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error creating CAPS entry: " + e.getMessage());
            return false;
        }
    }

    public boolean updateCAPSEntry(CAPSEntry entry) {
        String sql = "UPDATE CAPSEntry SET subject = ?, gradeLevel = ?, term = ?, topic = ?, outcomes = ?, assessmentStandards = ? WHERE capsCode = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, entry.getSubject());
            pstmt.setString(2, entry.getGradeLevel());
            pstmt.setInt(3, entry.getTerm());
            pstmt.setString(4, entry.getTopic());
            pstmt.setString(5, entry.getOutcomes());
            pstmt.setString(6, entry.getAssessmentStandards());
            pstmt.setString(7, entry.getCapsCode());
            
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error updating CAPS entry: " + e.getMessage());
            return false;
        }
    }

    public boolean deleteCAPSEntry(String capsCode) {
        String sql = "DELETE FROM CAPSEntry WHERE capsCode = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, capsCode);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error deleting CAPS entry: " + e.getMessage());
            return false;
        }
    }

    public List<CAPSEntry> getAllCAPSEntries() {
        List<CAPSEntry> entries = new ArrayList<>();
        String sql = "SELECT * FROM CAPSEntry ORDER BY createdAt DESC";
        
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                entries.add(extractCAPSEntry(rs));
            }
        } catch (SQLException e) {
            System.err.println("Error getting CAPS entries: " + e.getMessage());
        }
        return entries;
    }

    public CAPSEntry findByCapsCode(String capsCode) {
        String sql = "SELECT * FROM CAPSEntry WHERE capsCode = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, capsCode);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return extractCAPSEntry(rs);
            }
        } catch (SQLException e) {
            System.err.println("Error finding CAPS entry: " + e.getMessage());
        }
        return null;
    }
    
    public List<CAPSEntry> searchCAPSEntries(String gradeLevel, String subject, int term) {
        List<CAPSEntry> entries = new ArrayList<>();
        String sql = "SELECT * FROM CAPSEntry WHERE gradeLevel = ? AND subject = ? AND term = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, gradeLevel);
            pstmt.setString(2, subject);
            pstmt.setInt(3, term);
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                entries.add(extractCAPSEntry(rs));
            }
        } catch (SQLException e) {
            System.err.println("Error searching CAPS entries: " + e.getMessage());
        }
        return entries;
    }

    private CAPSEntry extractCAPSEntry(ResultSet rs) throws SQLException {
        CAPSEntry entry = new CAPSEntry();
        entry.setCapsCode(rs.getString("capsCode"));
        entry.setSubject(rs.getString("subject"));
        entry.setGradeLevel(rs.getString("gradeLevel"));
        entry.setTerm(rs.getInt("term"));
        entry.setTopic(rs.getString("topic"));
        entry.setOutcomes(rs.getString("outcomes"));
        entry.setAssessmentStandards(rs.getString("assessmentStandards"));
        entry.setCreatedAt(rs.getString("createdAt"));
        return entry;
    }
}