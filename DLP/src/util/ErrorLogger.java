package util;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ErrorLogger {
    
    private static ErrorLogger instance;
    private String logDirectory;
    private SimpleDateFormat dateFormat;
    
    private ErrorLogger() {
        // Initialize log directory path
        String appData = System.getenv("APPDATA");
        if (appData == null) {
            appData = System.getProperty("user.home");
        }
        
        logDirectory = appData + File.separator + "DLP" + File.separator + "Logs";
        dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        
        // Create log directory if it doesn't exist
        createLogDirectory();
    }
    
    public static synchronized ErrorLogger getInstance() {
        if (instance == null) {
            instance = new ErrorLogger();
        }
        return instance;
    }
    
    private void createLogDirectory() {
        File logDir = new File(logDirectory);
        if (!logDir.exists()) {
            if (logDir.mkdirs()) {
                System.out.println("Created log directory: " + logDirectory);
            } else {
                System.err.println("Failed to create log directory: " + logDirectory);
            }
        }
    }
    
    public void logError(String className, String methodName, String errorMessage, Throwable throwable) {
        String timestamp = dateFormat.format(new Date());
        String logFileName = "error_" + new SimpleDateFormat("yyyy-MM-dd").format(new Date()) + ".log";
        String logFilePath = logDirectory + File.separator + logFileName;
        
        try (PrintWriter writer = new PrintWriter(new FileWriter(logFilePath, true))) {
            writer.println("[" + timestamp + "] ERROR in " + className + "." + methodName);
            writer.println("Message: " + errorMessage);
            
            if (throwable != null) {
                writer.println("Exception: " + throwable.getClass().getName());
                writer.println("Stack Trace:");
                throwable.printStackTrace(writer);
            }
            
            writer.println("--------------------------------------------------");
            writer.flush();
        } catch (IOException e) {
            System.err.println("Failed to write to error log: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    public void logInfo(String className, String methodName, String message) {
        String timestamp = dateFormat.format(new Date());
        String logFileName = "info_" + new SimpleDateFormat("yyyy-MM-dd").format(new Date()) + ".log";
        String logFilePath = logDirectory + File.separator + logFileName;
        
        try (PrintWriter writer = new PrintWriter(new FileWriter(logFilePath, true))) {
            writer.println("[" + timestamp + "] INFO in " + className + "." + methodName);
            writer.println("Message: " + message);
            writer.println("--------------------------------------------------");
            writer.flush();
        } catch (IOException e) {
            System.err.println("Failed to write to info log: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    public void logWarning(String className, String methodName, String message) {
        String timestamp = dateFormat.format(new Date());
        String logFileName = "warning_" + new SimpleDateFormat("yyyy-MM-dd").format(new Date()) + ".log";
        String logFilePath = logDirectory + File.separator + logFileName;
        
        try (PrintWriter writer = new PrintWriter(new FileWriter(logFilePath, true))) {
            writer.println("[" + timestamp + "] WARNING in " + className + "." + methodName);
            writer.println("Message: " + message);
            writer.println("--------------------------------------------------");
            writer.flush();
        } catch (IOException e) {
            System.err.println("Failed to write to warning log: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    public void logAudit(String userId, String action, String target) {
        String timestamp = dateFormat.format(new Date());
        String logFileName = "audit_" + new SimpleDateFormat("yyyy-MM-dd").format(new Date()) + ".log";
        String logFilePath = logDirectory + File.separator + logFileName;
        
        try (PrintWriter writer = new PrintWriter(new FileWriter(logFilePath, true))) {
            writer.println("[" + timestamp + "] AUDIT");
            writer.println("User ID: " + userId);
            writer.println("Action: " + action);
            writer.println("Target: " + target);
            writer.println("--------------------------------------------------");
            writer.flush();
        } catch (IOException e) {
            System.err.println("Failed to write to audit log: " + e.getMessage());
            e.printStackTrace();
        }
    }
}