
package util;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseConnection {
    
    private static final String DB_URL = "jdbc:sqlite:dlp_database.db";
    private static Connection connection = null;

    private DatabaseConnection() {}

    public static Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            try {
                Class.forName("org.sqlite.JDBC");
                connection = DriverManager.getConnection(DB_URL);
                
                try (Statement stmt = connection.createStatement()) {
                    stmt.execute("PRAGMA foreign_keys = ON");
                }
                
                initializeDatabase();
                System.out.println("Database connected successfully.");
            } catch (ClassNotFoundException e) {
                throw new SQLException("SQLite JDBC driver not found.", e);
            }
        }
        return connection;
    }

    private static void initializeDatabase() throws SQLException {
        try (InputStream is = DatabaseConnection.class
                .getClassLoader()
                .getResourceAsStream("database/schema.sql")) {
            
            if (is == null) {
                System.out.println("schema.sql not found, creating default tables...");
                createDefaultTables();
                return;
            }

            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.trim().startsWith("--") && !line.trim().isEmpty()) {
                    sb.append(line).append("\n");
                }
            }
            reader.close();

            String[] statements = sb.toString().split(";");
            try (Statement stmt = connection.createStatement()) {
                for (String sql : statements) {
                    if (!sql.trim().isEmpty()) {
                        stmt.execute(sql.trim());
                    }
                }
            }
            System.out.println("Database schema initialized.");
        } catch (Exception e) {
            System.err.println("Schema initialization failed: " + e.getMessage());
            createDefaultTables();
        }
    }

    private static void createDefaultTables() throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("CREATE TABLE IF NOT EXISTS User (" +
                "userId TEXT PRIMARY KEY, " +
                "username TEXT UNIQUE NOT NULL, " +
                "passwordHash TEXT NOT NULL, " +
                "role TEXT NOT NULL, " +
                "email TEXT UNIQUE NOT NULL, " +
                "isActive INTEGER DEFAULT 1, " +
                "createdAt TEXT DEFAULT (datetime('now','localtime')))");
            System.out.println("Default User table created.");
        }
    }

    public static void closeConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                System.out.println("Database connection closed.");
            }
        } catch (SQLException e) {
            System.err.println("Error closing connection: " + e.getMessage());
        }
    }
    
}
