package controller;

import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import util.DatabaseConnection;
import util.SessionManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

public class AdminDashboardController {

    // =========================================================
    // TOP HEADER
    // =========================================================

    @FXML
    private Label welcomeLabel;

    // =========================================================
    // DASHBOARD STATISTICS
    // =========================================================

    @FXML
    private Label totalUsersLabel;

    @FXML
    private Label totalTeachersLabel;

    @FXML
    private Label totalStudentsLabel;

    @FXML
    private Label totalLessonPlansLabel;

    // =========================================================
    // DYNAMIC CONTENT
    // =========================================================

    @FXML
    private VBox contentBox;

    // =========================================================
    // TABLES
    // =========================================================

    @FXML
    private TableView<?> activityTable;

    @FXML
    private TableColumn<?, ?> activityColumn;

    @FXML
    private TableColumn<?, ?> userColumn;

    @FXML
    private TableColumn<?, ?> dateColumn;

    // =========================================================
    // INITIALIZE
    // =========================================================

    @FXML
    public void initialize() {

        System.out.println("Admin Dashboard initializing...");

        initializeDatabase();

        loadAdministratorName();

        loadDashboardStatistics();

        // Load School Calendar as the default tab
        showSchoolCalendar(null);

        System.out.println("Admin Dashboard loaded successfully.");
    }

    // =========================================================
    // ADMINISTRATOR NAME
    // =========================================================

    private void loadAdministratorName() {

        try {

            if (SessionManager.getInstance().getCurrentUser() != null) {

                String username =
                        SessionManager.getInstance()
                                .getCurrentUser()
                                .getUsername();

                welcomeLabel.setText(
                        "Welcome, " + username
                );

            } else {

                welcomeLabel.setText(
                        "Welcome, Administrator"
                );
            }

        } catch (Exception e) {

            e.printStackTrace();

            if (welcomeLabel != null) {
                welcomeLabel.setText(
                        "Welcome, Administrator"
                );
            }
        }
    }

    // =========================================================
    // DATABASE INITIALIZATION
    // =========================================================

    private void initializeDatabase() {

        try {

            Connection conn =
                    DatabaseConnection.getConnection();

            try (Statement stmt = conn.createStatement()) {

                // -------------------------------------------------
                // USERS TABLE
                // -------------------------------------------------

                stmt.execute(
                        "CREATE TABLE IF NOT EXISTS User (" +
                                "userId TEXT PRIMARY KEY, " +
                                "username TEXT UNIQUE NOT NULL, " +
                                "passwordHash TEXT NOT NULL, " +
                                "role TEXT NOT NULL, " +
                                "email TEXT UNIQUE NOT NULL, " +
                                "isActive INTEGER DEFAULT 1, " +
                                "createdAt TEXT DEFAULT " +
                                "(datetime('now','localtime'))" +
                                ")"
                );

                // -------------------------------------------------
                // SCHOOL EVENTS TABLE
                // -------------------------------------------------

                stmt.execute(
                        "CREATE TABLE IF NOT EXISTS school_events (" +
                                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                                "eventName TEXT NOT NULL, " +
                                "eventDate TEXT NOT NULL, " +
                                "eventType TEXT NOT NULL" +
                                ")"
                );

                // -------------------------------------------------
                // CAPS SUBJECTS TABLE
                // -------------------------------------------------

                stmt.execute(
                        "CREATE TABLE IF NOT EXISTS caps_subjects (" +
                                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                                "subjectName TEXT NOT NULL, " +
                                "grade TEXT NOT NULL, " +
                                "description TEXT" +
                                ")"
                );

                // -------------------------------------------------
                // AUDIT LOG TABLE
                // -------------------------------------------------

                stmt.execute(
                        "CREATE TABLE IF NOT EXISTS audit_log (" +
                                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                                "action TEXT NOT NULL, " +
                                "username TEXT NOT NULL, " +
                                "actionDate TEXT DEFAULT " +
                                "(datetime('now','localtime'))" +
                                ")"
                );
            }

            System.out.println(
                    "Administration database tables ready."
            );

        } catch (Exception e) {

            System.err.println(
                    "Database initialization failed: "
                            + e.getMessage()
            );

            e.printStackTrace();
        }
    }

    // =========================================================
    // DASHBOARD STATISTICS
    // =========================================================

    private void loadDashboardStatistics() {

        try {

            Connection conn =
                    DatabaseConnection.getConnection();

            // -------------------------------------------------
            // TOTAL USERS
            // -------------------------------------------------

            int totalUsers = getCount(
                    conn,
                    "SELECT COUNT(*) FROM User"
            );

            // -------------------------------------------------
            // TOTAL TEACHERS
            // -------------------------------------------------

            int totalTeachers = getCount(
                    conn,
                    "SELECT COUNT(*) FROM User " +
                            "WHERE LOWER(role) = 'teacher'"
            );

            // -------------------------------------------------
            // TOTAL STUDENTS
            // -------------------------------------------------

            int totalStudents = getCount(
                    conn,
                    "SELECT COUNT(*) FROM User " +
                            "WHERE LOWER(role) = 'student'"
            );

            // -------------------------------------------------
            // TOTAL LESSON PLANS
            // -------------------------------------------------

            int totalLessonPlans = 0;

            try {

                totalLessonPlans = getCount(
                        conn,
                        "SELECT COUNT(*) FROM lesson_plans"
                );

            } catch (Exception ignored) {

                // Table may not exist yet.
                totalLessonPlans = 0;
            }

            totalUsersLabel.setText(
                    String.valueOf(totalUsers)
            );

            totalTeachersLabel.setText(
                    String.valueOf(totalTeachers)
            );

            totalStudentsLabel.setText(
                    String.valueOf(totalStudents)
            );

            totalLessonPlansLabel.setText(
                    String.valueOf(totalLessonPlans)
            );

        } catch (Exception e) {

            e.printStackTrace();

            totalUsersLabel.setText("0");
            totalTeachersLabel.setText("0");
            totalStudentsLabel.setText("0");
            totalLessonPlansLabel.setText("0");
        }
    }

    // =========================================================
    // DATABASE COUNT
    // =========================================================

    private int getCount(
            Connection conn,
            String sql
    ) throws Exception {

        try (
                PreparedStatement ps =
                        conn.prepareStatement(sql);

                ResultSet rs =
                        ps.executeQuery()
        ) {

            if (rs.next()) {
                return rs.getInt(1);
            }
        }

        return 0;
    }

    // =========================================================
    // DASHBOARD
    // =========================================================

    @FXML
    private void showDashboard(ActionEvent event) {

        contentBox.getChildren().clear();

        Label title =
                new Label("Administration Dashboard");

        title.getStyleClass().add(
                "section-title"
        );

        Label description =
                new Label(
                        "Overview of the Digital Lesson Planner system."
                );

        description.getStyleClass().add(
                "page-subtitle"
        );

        VBox box =
                new VBox(
                        10,
                        title,
                        description
                );

        contentBox.getChildren().add(box);

        loadDashboardStatistics();
    }

    // =========================================================
    // USER MANAGEMENT
    // =========================================================

    @FXML
    private void showUserManagement(
            ActionEvent event
    ) {

        contentBox.getChildren().clear();

        Label title =
                new Label("User Management");

        title.getStyleClass().add(
                "section-title"
        );

        Button addUser =
                new Button("+ Add User");

        addUser.getStyleClass().add(
                "primary-button"
        );

        addUser.setOnAction(
                this::addUser
        );

        HBox header =
                new HBox(
                        20,
                        title,
                        addUser
                );

        contentBox.getChildren().add(header);

        try {

            Connection conn =
                    DatabaseConnection.getConnection();

            PreparedStatement ps =
                    conn.prepareStatement(
                            "SELECT username, role, email, isActive " +
                                    "FROM User ORDER BY username"
                    );

            ResultSet rs =
                    ps.executeQuery();

            while (rs.next()) {

                HBox row =
                        new HBox(25);

                row.getStyleClass().add(
                        "event-card"
                );

                Label username =
                        new Label(
                                "Username: "
                                        + rs.getString("username")
                        );

                Label role =
                        new Label(
                                "Role: "
                                        + rs.getString("role")
                        );

                Label email =
                        new Label(
                                "Email: "
                                        + rs.getString("email")
                        );

                String statusText =
                        rs.getInt("isActive") == 1
                                ? "Active"
                                : "Inactive";

                Label status =
                        new Label(
                                "Status: "
                                        + statusText
                        );

                row.getChildren().addAll(
                        username,
                        role,
                        email,
                        status
                );

                contentBox.getChildren().add(row);
            }

            rs.close();
            ps.close();

        } catch (Exception e) {

            e.printStackTrace();

            showMessage(
                    "Database Error",
                    "Unable to load users."
            );
        }
    }

    // =========================================================
    // CAPS DATABASE
    // =========================================================

    @FXML
    private void showCapsDatabase(
            ActionEvent event
    ) {

        contentBox.getChildren().clear();

        Label title =
                new Label("CAPS Database");

        title.getStyleClass().add(
                "section-title"
        );

        Button addSubject =
                new Button("+ Add Subject");

        addSubject.getStyleClass().add(
                "primary-button"
        );

        addSubject.setOnAction(
                this::addSubject
        );

        HBox header =
                new HBox(
                        20,
                        title,
                        addSubject
                );

        contentBox.getChildren().add(header);

        try {

            Connection conn =
                    DatabaseConnection.getConnection();

            PreparedStatement ps =
                    conn.prepareStatement(
                            "SELECT subjectName, grade, description " +
                                    "FROM caps_subjects " +
                                    "ORDER BY grade, subjectName"
                    );

            ResultSet rs =
                    ps.executeQuery();

            boolean found = false;

            while (rs.next()) {

                found = true;

                VBox card =
                        new VBox(5);

                card.getStyleClass().add(
                        "event-card"
                );

                Label subject =
                        new Label(
                                rs.getString(
                                        "subjectName"
                                )
                        );

                subject.getStyleClass().add(
                        "event-title"
                );

                Label grade =
                        new Label(
                                "Grade: "
                                        + rs.getString("grade")
                        );

                Label description =
                        new Label(
                                rs.getString(
                                        "description"
                                ) == null
                                        ? ""
                                        : rs.getString(
                                                "description"
                                        )
                        );

                card.getChildren().addAll(
                        subject,
                        grade,
                        description
                );

                contentBox.getChildren().add(card);
            }

            if (!found) {

                Label empty =
                        new Label(
                                "No CAPS subjects have been added yet."
                        );

                contentBox.getChildren().add(
                        empty
                );
            }

            rs.close();
            ps.close();

        } catch (Exception e) {

            e.printStackTrace();

            showMessage(
                    "Database Error",
                    "Unable to load CAPS subjects."
            );
        }
    }

    // =========================================================
    // SCHOOL CALENDAR
    // =========================================================

    @FXML
    private void showSchoolCalendar(
            ActionEvent event
    ) {

        contentBox.getChildren().clear();

        Label title =
                new Label("School Events");

        title.getStyleClass().add(
                "section-title"
        );

        Button addEvent =
                new Button("+ Add Event");

        addEvent.getStyleClass().add(
                "primary-button"
        );

        addEvent.setOnAction(
                this::handleAddEvent
        );

        HBox header =
                new HBox(
                        20,
                        title,
                        addEvent
                );

        contentBox.getChildren().add(header);

        try {

            Connection conn =
                    DatabaseConnection.getConnection();

            PreparedStatement ps =
                    conn.prepareStatement(
                            "SELECT id, eventName, eventDate, eventType " +
                                    "FROM school_events " +
                                    "ORDER BY eventDate"
                    );

            ResultSet rs =
                    ps.executeQuery();

            boolean found = false;

            while (rs.next()) {

                found = true;

                int eventId =
                        rs.getInt("id");

                HBox row =
                        new HBox(20);

                row.getStyleClass().add(
                        "event-card"
                );

                Label icon =
                        new Label("▣");

                icon.getStyleClass().add(
                        "calendar-icon"
                );

                VBox details =
                        new VBox(3);

                Label name =
                        new Label(
                                rs.getString(
                                        "eventName"
                                )
                        );

                name.getStyleClass().add(
                        "event-title"
                );

                Label date =
                        new Label(
                                rs.getString(
                                        "eventDate"
                                )
                        );

                date.getStyleClass().add(
                        "event-date"
                );

                details.getChildren().addAll(
                        name,
                        date
                );

                Region spacer =
                        new Region();

                HBox.setHgrow(
                        spacer,
                        javafx.scene.layout.Priority.ALWAYS
                );

                Label type =
                        new Label(
                                rs.getString(
                                        "eventType"
                                )
                        );

                Button edit =
                        new Button("✎");

                edit.getStyleClass().add(
                        "edit-button"
                );

                edit.setOnAction(
                        e ->
                                handleEditEvent(
                                        e,
                                        eventId
                                )
                );

                Button delete =
                        new Button("▢");

                delete.getStyleClass().add(
                        "delete-button"
                );

                delete.setOnAction(
                        e ->
                                handleDeleteEvent(
                                        e,
                                        eventId
                                )
                );

                row.getChildren().addAll(
                        icon,
                        details,
                        spacer,
                        type,
                        edit,
                        delete
                );

                contentBox.getChildren().add(
                        row
                );
            }

            if (!found) {

                Label empty =
                        new Label(
                                "No school events have been added yet."
                        );

                contentBox.getChildren().add(
                        empty
                );
            }

            rs.close();
            ps.close();

        } catch (Exception e) {

            e.printStackTrace();

            showMessage(
                    "Database Error",
                    "Unable to load school events."
            );
        }
    }

    // =========================================================
    // AUDIT LOG
    // =========================================================

    @FXML
    private void showAuditLog(
            ActionEvent event
    ) {

        contentBox.getChildren().clear();

        Label title =
                new Label("Audit Log");

        title.getStyleClass().add(
                "section-title"
        );

        contentBox.getChildren().add(
                title
        );

        try {

            Connection conn =
                    DatabaseConnection.getConnection();

            PreparedStatement ps =
                    conn.prepareStatement(
                            "SELECT action, username, actionDate " +
                                    "FROM audit_log " +
                                    "ORDER BY id DESC"
                    );

            ResultSet rs =
                    ps.executeQuery();

            boolean found = false;

            while (rs.next()) {

                found = true;

                HBox row =
                        new HBox(20);

                row.getStyleClass().add(
                        "event-card"
                );

                Label action =
                        new Label(
                                rs.getString("action")
                        );

                Label username =
                        new Label(
                                rs.getString("username")
                        );

                Label date =
                        new Label(
                                rs.getString("actionDate")
                        );

                row.getChildren().addAll(
                        action,
                        username,
                        date
                );

                contentBox.getChildren().add(
                        row
                );
            }

            if (!found) {

                Label empty =
                        new Label(
                                "No audit records available."
                        );

                contentBox.getChildren().add(
                        empty
                );
            }

            rs.close();
            ps.close();

        } catch (Exception e) {

            e.printStackTrace();

            showMessage(
                    "Database Error",
                    "Unable to load audit log."
            );
        }
    }

    // =========================================================
    // ADD USER
    // =========================================================

    @FXML
    private void addUser(ActionEvent event) {

        Dialog<ButtonType> dialog =
                new Dialog<>();

        dialog.setTitle(
                "Add User"
        );

        dialog.setHeaderText(
                "Create a new user"
        );

        VBox box =
                new VBox(10);

        TextField username =
                new TextField();

        username.setPromptText(
                "Username"
        );

        TextField email =
                new TextField();

        email.setPromptText(
                "Email"
        );

        PasswordField password =
                new PasswordField();

        password.setPromptText(
                "Password"
        );

        ComboBox<String> role =
                new ComboBox<>(
                        FXCollections.observableArrayList(
                                "Administrator",
                                "Teacher",
                                "Student"
                        )
                );

        role.setValue("Teacher");

        box.getChildren().addAll(
                username,
                email,
                password,
                role
        );

        dialog.getDialogPane().setContent(
                box
        );

        dialog.getDialogPane()
                .getButtonTypes()
                .addAll(
                        ButtonType.OK,
                        ButtonType.CANCEL
                );

        dialog.setResultConverter(
                button -> button
        );

        dialog.showAndWait()
                .ifPresent(result -> {

                    if (result == ButtonType.OK) {

                        createUser(
                                username.getText(),
                                email.getText(),
                                password.getText(),
                                role.getValue()
                        );
                    }
                });
    }

    // =========================================================
    // CREATE USER IN DATABASE
    // =========================================================

    private void createUser(
            String username,
            String email,
            String password,
            String role
    ) {

        if (
                username.trim().isEmpty()
                        || email.trim().isEmpty()
                        || password.isEmpty()
        ) {

            showMessage(
                    "Invalid Input",
                    "Please complete all fields."
            );

            return;
        }

        try {

            Connection conn =
                    DatabaseConnection.getConnection();

            String userId =
                    java.util.UUID.randomUUID()
                            .toString();

            PreparedStatement ps =
                    conn.prepareStatement(
                            "INSERT INTO User " +
                                    "(userId, username, passwordHash, role, email) " +
                                    "VALUES (?, ?, ?, ?, ?)"
                    );

            ps.setString(
                    1,
                    userId
            );

            ps.setString(
                    2,
                    username
            );

            // Temporary password storage.
            // Replace with BCrypt if your project already
            // uses BCrypt for authentication.
            ps.setString(
                    3,
                    password
            );

            ps.setString(
                    4,
                    role
            );

            ps.setString(
                    5,
                    email
            );

            ps.executeUpdate();

            ps.close();

            recordAudit(
                    "Created user: " + username
            );

            showMessage(
                    "Success",
                    "User created successfully."
            );

            loadDashboardStatistics();

            showUserManagement(null);

        } catch (Exception e) {

            e.printStackTrace();

            showMessage(
                    "Error",
                    "Unable to create user.\n"
                            + e.getMessage()
            );
        }
    }

    // =========================================================
    // ADD TEACHER
    // =========================================================

    @FXML
    private void addTeacher(ActionEvent event) {

        addUser(null);
    }

    // =========================================================
    // ADD SUBJECT
    // =========================================================

    @FXML
    private void addSubject(ActionEvent event) {

        Dialog<ButtonType> dialog =
                new Dialog<>();

        dialog.setTitle(
                "Add CAPS Subject"
        );

        dialog.setHeaderText(
                "Add a CAPS subject"
        );

        VBox box =
                new VBox(10);

        TextField subject =
                new TextField();

        subject.setPromptText(
                "Subject name"
        );

        TextField grade =
                new TextField();

        grade.setPromptText(
                "Grade"
        );

        TextArea description =
                new TextArea();

        description.setPromptText(
                "Description"
        );

        box.getChildren().addAll(
                subject,
                grade,
                description
        );

        dialog.getDialogPane().setContent(
                box
        );

        dialog.getDialogPane()
                .getButtonTypes()
                .addAll(
                        ButtonType.OK,
                        ButtonType.CANCEL
                );

        dialog.showAndWait()
                .ifPresent(result -> {

                    if (result == ButtonType.OK) {

                        saveSubject(
                                subject.getText(),
                                grade.getText(),
                                description.getText()
                        );
                    }
                });
    }

    // =========================================================
    // SAVE SUBJECT
    // =========================================================

    private void saveSubject(
            String subject,
            String grade,
            String description
    ) {

        if (
                subject.trim().isEmpty()
                        || grade.trim().isEmpty()
        ) {

            showMessage(
                    "Invalid Input",
                    "Subject and grade are required."
            );

            return;
        }

        try {

            Connection conn =
                    DatabaseConnection.getConnection();

            PreparedStatement ps =
                    conn.prepareStatement(
                            "INSERT INTO caps_subjects " +
                                    "(subjectName, grade, description) " +
                                    "VALUES (?, ?, ?)"
                    );

            ps.setString(
                    1,
                    subject
            );

            ps.setString(
                    2,
                    grade
            );

            ps.setString(
                    3,
                    description
            );

            ps.executeUpdate();

            ps.close();

            recordAudit(
                    "Added CAPS subject: "
                            + subject
            );

            showMessage(
                    "Success",
                    "Subject added successfully."
            );

            showCapsDatabase(null);

        } catch (Exception e) {

            e.printStackTrace();

            showMessage(
                    "Error",
                    "Unable to add subject."
            );
        }
    }

    // =========================================================
    // ADD SCHOOL EVENT
    // =========================================================

    @FXML
    private void handleAddEvent(
            ActionEvent event
    ) {

        Dialog<ButtonType> dialog =
                new Dialog<>();

        dialog.setTitle(
                "Add School Event"
        );

        dialog.setHeaderText(
                "Create a new school event"
        );

        VBox box =
                new VBox(10);

        TextField eventName =
                new TextField();

        eventName.setPromptText(
                "Event name"
        );

        DatePicker eventDate =
                new DatePicker();

        ComboBox<String> eventType =
                new ComboBox<>(
                        FXCollections.observableArrayList(
                                "Full Day",
                                "Half Day"
                        )
                );

        eventType.setValue(
                "Full Day"
        );

        box.getChildren().addAll(
                eventName,
                eventDate,
                eventType
        );

        dialog.getDialogPane().setContent(
                box
        );

        dialog.getDialogPane()
                .getButtonTypes()
                .addAll(
                        ButtonType.OK,
                        ButtonType.CANCEL
                );

        dialog.showAndWait()
                .ifPresent(result -> {

                    if (result == ButtonType.OK) {

                        if (
                                eventName.getText()
                                        .trim()
                                        .isEmpty()
                                || eventDate.getValue()
                                        == null
                        ) {

                            showMessage(
                                    "Invalid Input",
                                    "Please enter an event name and date."
                            );

                            return;
                        }

                        try {

                            Connection conn =
                                    DatabaseConnection
                                            .getConnection();

                            PreparedStatement ps =
                                    conn.prepareStatement(
                                            "INSERT INTO school_events " +
                                                    "(eventName, eventDate, eventType) " +
                                                    "VALUES (?, ?, ?)"
                                    );

                            ps.setString(
                                    1,
                                    eventName.getText()
                            );

                            ps.setString(
                                    2,
                                    eventDate.getValue()
                                            .toString()
                            );

                            ps.setString(
                                    3,
                                    eventType.getValue()
                            );

                            ps.executeUpdate();

                            ps.close();

                            recordAudit(
                                    "Added school event: "
                                            + eventName.getText()
                            );

                            showSchoolCalendar(null);

                        } catch (Exception e) {

                            e.printStackTrace();

                            showMessage(
                                    "Error",
                                    "Unable to add school event."
                            );
                        }
                    }
                });
    }

    // =========================================================
    // EDIT SCHOOL EVENT
    // =========================================================

    private void handleEditEvent(
            ActionEvent event,
            int eventId
    ) {

        try {

            Connection conn =
                    DatabaseConnection
                            .getConnection();

            PreparedStatement ps =
                    conn.prepareStatement(
                            "SELECT eventName, eventDate, eventType " +
                                    "FROM school_events WHERE id = ?"
                    );

            ps.setInt(
                    1,
                    eventId
            );

            ResultSet rs =
                    ps.executeQuery();

            if (!rs.next()) {

                showMessage(
                        "Error",
                        "Event not found."
                );

                return;
            }

            String oldName =
                    rs.getString("eventName");

            String oldDate =
                    rs.getString("eventDate");

            String oldType =
                    rs.getString("eventType");

            rs.close();
            ps.close();

            Dialog<ButtonType> dialog =
                    new Dialog<>();

            dialog.setTitle(
                    "Edit School Event"
            );

            VBox box =
                    new VBox(10);

            TextField name =
                    new TextField(oldName);

            DatePicker date =
                    new DatePicker(
                            java.time.LocalDate.parse(
                                    oldDate
                            )
                    );

            ComboBox<String> type =
                    new ComboBox<>(
                            FXCollections.observableArrayList(
                                    "Full Day",
                                    "Half Day"
                            )
                    );

            type.setValue(oldType);

            box.getChildren().addAll(
                    name,
                    date,
                    type
            );

            dialog.getDialogPane().setContent(
                    box
            );

            dialog.getDialogPane()
                    .getButtonTypes()
                    .addAll(
                            ButtonType.OK,
                            ButtonType.CANCEL
                    );

            dialog.showAndWait()
                    .ifPresent(result -> {

                        if (
                                result
                                        == ButtonType.OK
                        ) {

                            try {

                                PreparedStatement update =
                                        conn.prepareStatement(
                                                "UPDATE school_events " +
                                                        "SET eventName = ?, " +
                                                        "eventDate = ?, " +
                                                        "eventType = ? " +
                                                        "WHERE id = ?"
                                        );

                                update.setString(
                                        1,
                                        name.getText()
                                );

                                update.setString(
                                        2,
                                        date.getValue()
                                                .toString()
                                );

                                update.setString(
                                        3,
                                        type.getValue()
                                );

                                update.setInt(
                                        4,
                                        eventId
                                );

                                update.executeUpdate();

                                update.close();

                                recordAudit(
                                        "Edited school event: "
                                                + name.getText()
                                );

                                showSchoolCalendar(
                                        null
                                );

                            } catch (Exception e) {

                                e.printStackTrace();

                                showMessage(
                                        "Error",
                                        "Unable to update event."
                                );
                            }
                        }
                    });

        } catch (Exception e) {

            e.printStackTrace();

            showMessage(
                    "Error",
                    "Unable to edit event."
            );
        }
    }

    // =========================================================
    // DELETE SCHOOL EVENT
    // =========================================================

    private void handleDeleteEvent(
            ActionEvent event,
            int eventId
    ) {

        Alert confirmation =
                new Alert(
                        Alert.AlertType.CONFIRMATION
                );

        confirmation.setTitle(
                "Delete Event"
        );

        confirmation.setHeaderText(
                "Delete this school event?"
        );

        confirmation.setContentText(
                "This action cannot be undone."
        );

        confirmation.showAndWait()
                .ifPresent(result -> {

                    if (
                            result
                                    == ButtonType.OK
                    ) {

                        try {

                            Connection conn =
                                    DatabaseConnection
                                            .getConnection();

                            PreparedStatement ps =
                                    conn.prepareStatement(
                                            "DELETE FROM school_events " +
                                                    "WHERE id = ?"
                                    );

                            ps.setInt(
                                    1,
                                    eventId
                            );

                            ps.executeUpdate();

                            ps.close();

                            recordAudit(
                                    "Deleted school event ID: "
                                            + eventId
                            );

                            showSchoolCalendar(
                                    null
                            );

                        } catch (Exception e) {

                            e.printStackTrace();

                            showMessage(
                                    "Error",
                                    "Unable to delete event."
                            );
                        }
                    }
                });
    }

    // =========================================================
    // AUDIT RECORD
    // =========================================================

    private void recordAudit(
            String action
    ) {

        try {

            Connection conn =
                    DatabaseConnection
                            .getConnection();

            String username =
                    "Administrator";

            if (
                    SessionManager
                            .getInstance()
                            .getCurrentUser()
                            != null
            ) {

                username =
                        SessionManager
                                .getInstance()
                                .getCurrentUser()
                                .getUsername();
            }

            PreparedStatement ps =
                    conn.prepareStatement(
                            "INSERT INTO audit_log " +
                                    "(action, username) " +
                                    "VALUES (?, ?)"
                    );

            ps.setString(
                    1,
                    action
            );

            ps.setString(
                    2,
                    username
            );

            ps.executeUpdate();

            ps.close();

        } catch (Exception e) {

            System.err.println(
                    "Unable to record audit log: "
                            + e.getMessage()
            );
        }
    }

    // =========================================================
    // LOGOUT
    // =========================================================

    @FXML
    private void handleLogout(
            ActionEvent event
    ) {

        try {

            SessionManager
                    .getInstance()
                    .endSession();

            FXMLLoader loader =
                    new FXMLLoader(
                            getClass()
                                    .getResource(
                                            "/view/LoginView.fxml"
                                    )
                    );

            Parent loginView =
                    loader.load();

            welcomeLabel
                    .getScene()
                    .setRoot(
                            loginView
                    );

        } catch (Exception e) {

            e.printStackTrace();

            showMessage(
                    "Error",
                    "Unable to return to the login screen."
            );
        }
    }

    // =========================================================
    // MESSAGE BOX
    // =========================================================

    private void showMessage(
            String title,
            String message
    ) {

        Alert alert =
                new Alert(
                        Alert.AlertType.INFORMATION
                );

        alert.setTitle(
                title
        );

        alert.setHeaderText(
                null
        );

        alert.setContentText(
                message
        );

        alert.showAndWait();
    }
}