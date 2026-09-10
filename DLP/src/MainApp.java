
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

                stmt.execute("CREATE TABLE IF NOT EXISTS LessonPlan (" +
                    "lessonPlanId TEXT PRIMARY KEY, " +
                    "teacherId TEXT NOT NULL, " +
                    "title TEXT NOT NULL, " +
                    "subject TEXT NOT NULL, " +
                    "gradeLevel TEXT NOT NULL, " +
                    "curriculumReference TEXT, " +
                    "topic TEXT, " +
                    "durationMinutes INTEGER DEFAULT 60, " +
                    "objectives TEXT, " +
                    "teachingActivities TEXT, " +
                    "resources TEXT, " +
                    "assessmentMethod TEXT, " +
                    "lessonDate TEXT, " +
                    "status TEXT NOT NULL DEFAULT 'SCHEDULED', " +
                    "createdAt TEXT DEFAULT (datetime('now','localtime')), " +
                    "updatedAt TEXT DEFAULT (datetime('now','localtime')), " +
                    "FOREIGN KEY (teacherId) REFERENCES User(userId))");

                // Defensive migration for teachers already running an earlier build of this app
                addColumnIfMissing(stmt, "LessonPlan", "durationMinutes", "INTEGER DEFAULT 60");
                addColumnIfMissing(stmt, "LessonPlan", "teachingActivities", "TEXT");

                stmt.execute("CREATE TABLE IF NOT EXISTS SchoolClass (" +
                    "classId TEXT PRIMARY KEY, " +
                    "teacherId TEXT NOT NULL, " +
                    "className TEXT NOT NULL, " +
                    "subject TEXT NOT NULL, " +
                    "gradeLevel TEXT NOT NULL, " +
                    "studentCount INTEGER DEFAULT 0, " +
                    "notes TEXT, " +
                    "createdAt TEXT DEFAULT (datetime('now','localtime')), " +
                    "FOREIGN KEY (teacherId) REFERENCES User(userId))");

                // CAPS Database Table
                stmt.execute("CREATE TABLE IF NOT EXISTS CAPSEntry (" +
                    "capsCode TEXT PRIMARY KEY, " +
                    "subject TEXT NOT NULL, " +
                    "gradeLevel TEXT NOT NULL, " +
                    "term INTEGER NOT NULL, " +
                    "topic TEXT NOT NULL, " +
                    "outcomes TEXT NOT NULL, " +
                    "assessmentStandards TEXT, " +
                    "createdAt TEXT DEFAULT (datetime('now','localtime')))");

                // School Events Table
                stmt.execute("CREATE TABLE IF NOT EXISTS SchoolEvent (" +
                    "eventId TEXT PRIMARY KEY, " +
                    "eventName TEXT NOT NULL, " +
                    "eventDate TEXT NOT NULL, " +
                    "eventType TEXT NOT NULL, " + // FULL_DAY or HALF_DAY
                    "description TEXT, " +
                    "createdAt TEXT DEFAULT (datetime('now','localtime')))");

                // Audit Log Table
                stmt.execute("CREATE TABLE IF NOT EXISTS AuditLog (" +
                    "entryId TEXT PRIMARY KEY, " +
                    "userId TEXT NOT NULL, " +
                    "action TEXT NOT NULL, " +
                    "target TEXT NOT NULL, " +
                    "timestamp TEXT DEFAULT (datetime('now','localtime')), " +
                    "FOREIGN KEY (userId) REFERENCES User(userId))");
            }
            System.out.println("Database initialized successfully.");
        } catch (Exception e) {
            System.err.println("Database initialization failed: " + e.getMessage());
        }
    }

    /** Adds a column to an existing table if it doesn't already exist (safe re-run on every startup). */
    private void addColumnIfMissing(Statement stmt, String table, String column, String columnDef) {
        try {
            stmt.execute("ALTER TABLE " + table + " ADD COLUMN " + column + " " + columnDef);
        } catch (Exception e) {
            // Column already exists - nothing to do.
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
