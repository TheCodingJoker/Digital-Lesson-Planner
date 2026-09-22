package dao;

import model.AuditLog;
import util.DatabaseConnection;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class AuditLogDAO {
    
    public boolean createAuditLog(AuditLog log) {
        String sql = "INSERT INTO AuditLog (entryId, userId, action, target) VALUES (?, ?, ?, ?)";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, log.getEntryId());
            pstmt.setString(2, log.getUserId());
            pstmt.setString(3, log.getAction());
            pstmt.setString(4, log.getTarget());
            
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error creating audit log: " + e.getMessage());
            return false;
        }
    }

    public List<AuditLog> getAllAuditLogs() {
        List<AuditLog> logs = new ArrayList<>();
        String sql = "SELECT * FROM AuditLog ORDER BY timestamp DESC";
        
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                logs.add(extractAuditLog(rs));
            }
        } catch (SQLException e) {
            System.err.println("Error getting audit logs: " + e.getMessage());
        }
        return logs;
    }

    public List<AuditLog> getAuditLogsByUser(String userId) {
        List<AuditLog> logs = new ArrayList<>();
        String sql = "SELECT * FROM AuditLog WHERE userId = ? ORDER BY timestamp DESC";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, userId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                logs.add(extractAuditLog(rs));
            }
        } catch (SQLException e) {
            System.err.println("Error getting audit logs by user: " + e.getMessage());
        }
        return logs;
    }

    private AuditLog extractAuditLog(ResultSet rs) throws SQLException {
        AuditLog log = new AuditLog();
        log.setEntryId(rs.getString("entryId"));
        log.setUserId(rs.getString("userId"));
        log.setAction(rs.getString("action"));
        log.setTarget(rs.getString("target"));
        log.setTimestamp(rs.getString("timestamp"));
        return log;
    }
}