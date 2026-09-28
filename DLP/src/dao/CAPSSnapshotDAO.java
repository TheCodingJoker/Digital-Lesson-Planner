package dao;

import model.CAPSSnapshot;
import model.CAPSEntry;
import util.DatabaseConnection;

import java.sql.*;
import java.util.UUID;

public class CAPSSnapshotDAO {
    
    public boolean createSnapshot(CAPSEntry capsEntry) {
        String snapshotId = UUID.randomUUID().toString();
        String capturedAt = getCurrentTimestamp();
        String originalData = capsEntryToJson(capsEntry);
        
        String sql = "INSERT INTO CAPSSnapshot (snapshotId, capturedAt, originalData) VALUES (?, ?, ?)";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, snapshotId);
            pstmt.setString(2, capturedAt);
            pstmt.setString(3, originalData);
            
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error creating CAPS snapshot: " + e.getMessage());
            return false;
        }
    }
    
    public CAPSSnapshot getSnapshotById(String snapshotId) {
        String sql = "SELECT * FROM CAPSSnapshot WHERE snapshotId = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, snapshotId);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                return extractSnapshot(rs);
            }
        } catch (SQLException e) {
            System.err.println("Error getting CAPS snapshot: " + e.getMessage());
        }
        return null;
    }
    
    public CAPSEntry restoreCAPSEntry(String snapshotId) {
        CAPSSnapshot snapshot = getSnapshotById(snapshotId);
        if (snapshot != null) {
            return jsonToCapsEntry(snapshot.getOriginalData());
        }
        return null;
    }
    
    private String capsEntryToJson(CAPSEntry entry) {
        // Simple JSON serialization
        StringBuilder json = new StringBuilder();
        json.append("{");
        json.append("\"capsCode\":\"").append(escapeJson(entry.getCapsCode())).append("\",");
        json.append("\"subject\":\"").append(escapeJson(entry.getSubject())).append("\",");
        json.append("\"gradeLevel\":\"").append(escapeJson(entry.getGradeLevel())).append("\",");
        json.append("\"term\":").append(entry.getTerm()).append(",");
        json.append("\"topic\":\"").append(escapeJson(entry.getTopic())).append("\",");
        json.append("\"outcomes\":\"").append(escapeJson(entry.getOutcomes())).append("\",");
        json.append("\"assessmentStandards\":\"").append(escapeJson(entry.getAssessmentStandards())).append("\"");
        json.append("}");
        return json.toString();
    }
    
    private CAPSEntry jsonToCapsEntry(String json) {
        CAPSEntry entry = new CAPSEntry();
        try {
            // Simple JSON parsing
            String[] parts = json.split("\"");
            for (int i = 0; i < parts.length; i++) {
                if (parts[i].equals("capsCode") && i + 2 < parts.length) {
                    entry.setCapsCode(parts[i + 2]);
                } else if (parts[i].equals("subject") && i + 2 < parts.length) {
                    entry.setSubject(parts[i + 2]);
                } else if (parts[i].equals("gradeLevel") && i + 2 < parts.length) {
                    entry.setGradeLevel(parts[i + 2]);
                } else if (parts[i].equals("term") && i + 1 < parts.length) {
                    entry.setTerm(Integer.parseInt(parts[i + 1].replace(",", "").replace("}", "")));
                } else if (parts[i].equals("topic") && i + 2 < parts.length) {
                    entry.setTopic(parts[i + 2]);
                } else if (parts[i].equals("outcomes") && i + 2 < parts.length) {
                    entry.setOutcomes(parts[i + 2]);
                } else if (parts[i].equals("assessmentStandards") && i + 2 < parts.length) {
                    entry.setAssessmentStandards(parts[i + 2]);
                }
            }
        } catch (Exception e) {
            System.err.println("Error parsing CAPS JSON: " + e.getMessage());
        }
        return entry;
    }
    
    private String escapeJson(String value) {
        if (value == null) return "";
        return value.replace("\\", "\\\\")
                   .replace("\"", "\\\"")
                   .replace("\n", "\\n")
                   .replace("\r", "\\r")
                   .replace("\t", "\\t");
    }
    
    private CAPSSnapshot extractSnapshot(ResultSet rs) throws SQLException {
        CAPSSnapshot snapshot = new CAPSSnapshot();
        snapshot.setSnapshotId(rs.getString("snapshotId"));
        snapshot.setCapturedAt(rs.getString("capturedAt"));
        snapshot.setOriginalData(rs.getString("originalData"));
        return snapshot;
    }
    
    private String getCurrentTimestamp() {
        return new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date());
    }
}