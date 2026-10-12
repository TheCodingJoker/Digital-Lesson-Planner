package util;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnection {

    private static final String ENCRYPTED_DB_PATH = "dlp.db.enc";
    private static final String TEMP_DB_PATH = "dlp_temp.db";
    private static final String UNENCRYPTED_DB_PATH = "dlp.db";
    private static Connection connection;
    private static ErrorLogger errorLogger;

    static {
        errorLogger = ErrorLogger.getInstance();
    }

    /**
     * Initializes the database by decrypting if necessary.
     * This should be called once at application startup.
     */
    public static void initializeDatabase() throws IOException {
        try {
            File encryptedFile = new File(ENCRYPTED_DB_PATH);
            File tempFile = new File(TEMP_DB_PATH);

            Files.deleteIfExists(tempFile.toPath());

            // If encrypted database exists, decrypt it to temp file
            if (encryptedFile.exists()) {
                errorLogger.logInfo("DatabaseConnection", "initializeDatabase", "Decrypting database...");
                DatabaseEncryption.decryptFile(ENCRYPTED_DB_PATH, TEMP_DB_PATH);
            } else if (!new File(UNENCRYPTED_DB_PATH).exists()) {
                // First run - no database exists yet
                // Use the unencrypted path for initial creation
                System.out.println("No existing database found. Will create new database.");
            } else {
                // Unencrypted database exists - this is a migration scenario
                // We'll encrypt it on shutdown
                System.out.println("Found unencrypted database. Will encrypt on shutdown.");
            }
        } catch (Exception e) {
            errorLogger.logError("DatabaseConnection", "initializeDatabase", "Failed to initialize database encryption", e);
            System.err.println("Database encryption initialization failed: " + e.getMessage());
            throw new IOException("Could not initialize the protected database.", e);
        }
    }

    /**
     * Encrypts and saves the database.
     * This should be called once at application shutdown.
     */
    public static void encryptAndClose() {
        try {
            closeConnection();

            File tempFile = new File(TEMP_DB_PATH);
            File unencryptedFile = new File(UNENCRYPTED_DB_PATH);
            File encryptedFile = new File(ENCRYPTED_DB_PATH);

            // Determine which file to encrypt
            String sourcePath = tempFile.exists() ? TEMP_DB_PATH : UNENCRYPTED_DB_PATH;

            if (new File(sourcePath).exists()) {
                errorLogger.logInfo("DatabaseConnection", "encryptAndClose", "Encrypting database...");
                DatabaseEncryption.encryptFile(sourcePath, ENCRYPTED_DB_PATH);

                // Delete the unencrypted/temp file
                Files.delete(new File(sourcePath).toPath());

                // Also delete the other variant if it exists
                if (sourcePath.equals(TEMP_DB_PATH) && unencryptedFile.exists()) {
                    Files.delete(unencryptedFile.toPath());
                } else if (sourcePath.equals(UNENCRYPTED_DB_PATH) && tempFile.exists()) {
                    Files.delete(tempFile.toPath());
                }

                errorLogger.logInfo("DatabaseConnection", "encryptAndClose", "Database encrypted successfully.");
            }
        } catch (Exception e) {
            errorLogger.logError("DatabaseConnection", "encryptAndClose", "Failed to encrypt database", e);
            System.err.println("Database encryption failed: " + e.getMessage());
        }
    }

    /**
     * Gets the appropriate database URL based on current state.
     */
    private static String getDatabaseUrl() {
        File tempFile = new File(TEMP_DB_PATH);
        if (tempFile.exists()) {
            return "jdbc:sqlite:" + TEMP_DB_PATH;
        }
        return "jdbc:sqlite:" + UNENCRYPTED_DB_PATH;
    }

    public static Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            connection = DriverManager.getConnection(getDatabaseUrl());
        }
        return connection;
    }

    public static void closeConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            errorLogger.logError("DatabaseConnection", "closeConnection", "Error closing connection", e);
        }
    }
}
