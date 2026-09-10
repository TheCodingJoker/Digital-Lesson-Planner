
package dao;

import model.User;
import util.DatabaseConnection;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UserDAO {
    
     public User findByUsername(String username) {
        String sql = "SELECT * FROM User WHERE username = ? AND isActive = 1";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                return extractUser(rs);
            }
        } catch (SQLException e) {
            System.err.println("Error finding user: " + e.getMessage());
        }
        return null;
    }

    public User findByEmail(String email) {
        String sql = "SELECT * FROM User WHERE email = ? AND isActive = 1";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, email);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                return extractUser(rs);
            }
        } catch (SQLException e) {
            System.err.println("Error finding user: " + e.getMessage());
        }
        return null;
    }

    public boolean createUser(User user) {
        String sql = "INSERT INTO User (userId, username, passwordHash, role, email) VALUES (?, ?, ?, ?, ?)";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, user.getUserId());
            pstmt.setString(2, user.getUsername());
            pstmt.setString(3, user.getPasswordHash());
            pstmt.setString(4, user.getRole());
            pstmt.setString(5, user.getEmail());
            
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error creating user: " + e.getMessage());
            return false;
        }
    }

    private User extractUser(ResultSet rs) throws SQLException {
        User user = new User();
        user.setUserId(rs.getString("userId"));
        user.setUsername(rs.getString("username"));
        user.setPasswordHash(rs.getString("passwordHash"));
        user.setRole(rs.getString("role"));
        user.setEmail(rs.getString("email"));
        user.setActive(rs.getBoolean("isActive"));
        user.setCreatedAt(rs.getString("createdAt"));
        return user;
    }

    public List<User> getAllUsers() {
        List<User> users = new ArrayList<>();
        String sql = "SELECT * FROM User ORDER BY createdAt DESC";
        
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                users.add(extractUser(rs));
            }
        } catch (SQLException e) {
            System.err.println("Error getting all users: " + e.getMessage());
        }
        return users;
    }

    public boolean updateUser(User user) {
        String sql = "UPDATE User SET email = ?, role = ?, passwordHash = ? WHERE userId = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, user.getEmail());
            pstmt.setString(2, user.getRole());
            pstmt.setString(3, user.getPasswordHash());
            pstmt.setString(4, user.getUserId());
            
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error updating user: " + e.getMessage());
            return false;
        }
    }

    public boolean deleteUser(String userId) {
        String sql = "DELETE FROM User WHERE userId = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, userId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error deleting user: " + e.getMessage());
            return false;
        }
    }
    
}
