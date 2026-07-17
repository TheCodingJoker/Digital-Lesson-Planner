
import util.DatabaseConnection;
import util.PasswordHasher;
import dao.UserDAO;
import model.User;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.sql.Connection;
import java.sql.Statement;
import java.util.UUID;

public class MainApp extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        System.out.println("Starting application...");
        
        // Initialize database and create default admin user
        initializeDatabase();
        createDefaultAdminIfNeeded();
        
        System.out.println("Loading FXML file...");
        // Load login view
        FXMLLoader loader = new FXMLLoader(
            getClass().getResource("/view/LoginView.fxml"));
        Parent root = loader.load();
        
        System.out.println("Setting up stage...");
        // Setup stage
        Scene scene = new Scene(root);
        primaryStage.setTitle("Digital Lesson Planner - Login");
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(800);
        primaryStage.setMinHeight(600);
        
        System.out.println("Showing stage...");
        primaryStage.show();
        System.out.println("Application started successfully!");
    }

    private void initializeDatabase() {
        try {
            Connection conn = DatabaseConnection.getConnection();
            // Create User table if not exists
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("CREATE TABLE IF NOT EXISTS User (" +
                    "userId TEXT PRIMARY KEY, " +
                    "username TEXT UNIQUE NOT NULL, " +
                    "passwordHash TEXT NOT NULL, " +
                    "role TEXT NOT NULL, " +
                    "email TEXT UNIQUE NOT NULL, " +
                    "isActive INTEGER DEFAULT 1, " +
                    "createdAt TEXT DEFAULT (datetime('now','localtime')))");
            }
            System.out.println("Database initialized successfully.");
        } catch (Exception e) {
            System.err.println("Database initialization failed: " + e.getMessage());
        }
    }

    private void createDefaultAdminIfNeeded() {
        UserDAO userDAO = new UserDAO();
        if (userDAO.findByUsername("admin") == null) {
            User admin = new User(
                UUID.randomUUID().toString(),
                "admin",
                PasswordHasher.hashPassword("admin123"),
                "ADMINISTRATOR",
                "admin@school.co.za"
            );
            if (userDAO.createUser(admin)) {
                System.out.println("Default admin user created (admin/admin123)");
            }
        }
        
        if (userDAO.findByUsername("teacher") == null) {
            User teacher = new User(
                UUID.randomUUID().toString(),
                "teacher",
                PasswordHasher.hashPassword("teacher123"),
                "TEACHER",
                "teacher@school.co.za"
            );
            userDAO.createUser(teacher);
            System.out.println("Default teacher user created (teacher/teacher123)");
        }
        
        if (userDAO.findByUsername("principal") == null) {
            User principal = new User(
                UUID.randomUUID().toString(),
                "principal",
                PasswordHasher.hashPassword("principal123"),
                "PRINCIPAL_HOD",
                "principal@school.co.za"
            );
            userDAO.createUser(principal);
            System.out.println("Default principal user created (principal/principal123)");
        }
    }

    @Override
    public void stop() {
        DatabaseConnection.closeConnection();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
