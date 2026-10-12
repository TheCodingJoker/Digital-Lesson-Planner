package controller;

import dao.*;
import model.*;
import service.SchedulingEngine;
import util.SessionManager;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class AdminDashboardController {

    @FXML private Label userNameLabel;
    @FXML private Button logoutButton;
    
    @FXML private TabPane adminTabPane;
    
    // User Management
    @FXML private TableView<User> usersTable;
    @FXML private TableColumn<User, String> usernameColumn;
    @FXML private TableColumn<User, String> emailColumn;
    @FXML private TableColumn<User, String> roleColumn;
    @FXML private TableColumn<User, String> statusColumn;
    @FXML private TableColumn<User, Void> actionsColumn;
    
    // CAPS Database
    @FXML private TableView<CAPSEntry> capsTable;
    @FXML private TableColumn<CAPSEntry, String> capsCodeColumn;
    @FXML private TableColumn<CAPSEntry, String> capsSubjectColumn;
    @FXML private TableColumn<CAPSEntry, String> capsGradeColumn;
    @FXML private TableColumn<CAPSEntry, String> capsTermColumn;
    @FXML private TableColumn<CAPSEntry, String> capsTopicColumn;
    @FXML private TableColumn<CAPSEntry, Void> capsActionsColumn;
    
    // School Events
    @FXML private TableView<SchoolEvent> eventsTable;
    @FXML private TableColumn<SchoolEvent, String> eventNameColumn;
    @FXML private TableColumn<SchoolEvent, String> eventDateColumn;
    @FXML private TableColumn<SchoolEvent, String> eventTypeColumn;
    @FXML private TableColumn<SchoolEvent, Void> eventActionsColumn;
    
    // Audit Log
    @FXML private TableView<AuditLog> auditLogTable;
    @FXML private TableColumn<AuditLog, String> auditActionColumn;
    @FXML private TableColumn<AuditLog, String> auditTargetColumn;
    @FXML private TableColumn<AuditLog, String> auditTimestampColumn;

    // Feedback
    @FXML private TableView<Feedback> feedbackTable;
    @FXML private TableColumn<Feedback, String> feedbackTitleColumn;
    @FXML private TableColumn<Feedback, String> feedbackTypeColumn;
    @FXML private TableColumn<Feedback, String> feedbackPriorityColumn;
    @FXML private TableColumn<Feedback, String> feedbackStatusColumn;
    @FXML private TableColumn<Feedback, String> feedbackDateColumn;
    @FXML private TableColumn<Feedback, Void> feedbackActionsColumn;

    // School Calendar
    @FXML private TableView<SchoolCalendar> calendarTable;
    @FXML private TableColumn<SchoolCalendar, Integer> calendarYearColumn;
    @FXML private TableColumn<SchoolCalendar, Integer> calendarTotalDaysColumn;
    @FXML private TableColumn<SchoolCalendar, String> calendarStartDateColumn;
    @FXML private TableColumn<SchoolCalendar, String> calendarEndDateColumn;
    @FXML private TableColumn<SchoolCalendar, Void> calendarActionsColumn;

    private final UserDAO userDAO = new UserDAO();
    private final CAPSEntryDAO capsDAO = new CAPSEntryDAO();
    private final SchoolEventDAO eventDAO = new SchoolEventDAO();
    private final AuditLogDAO auditLogDAO = new AuditLogDAO();
    private final FeedbackDAO feedbackDAO = new FeedbackDAO();
    private final SchoolCalendarDAO calendarDAO = new SchoolCalendarDAO();
    
    private User currentUser;

    @FXML
    public void initialize() {
        currentUser = SessionManager.getInstance().getCurrentUser();
        if (currentUser != null && userNameLabel != null) {
            userNameLabel.setText(currentUser.getUsername());
        } else if (userNameLabel != null) {
            userNameLabel.setText("Admin");
        }

        setupUserTable();
        setupCAPSTable();
        setupEventsTable();
        setupAuditLogTable();
        setupFeedbackTable();
        setupCalendarTable();

        loadAllData();
        setupActivityMonitoring();
    }

    private void setupUserTable() {
        usernameColumn.setCellValueFactory(new PropertyValueFactory<>("username"));
        emailColumn.setCellValueFactory(new PropertyValueFactory<>("email"));
        roleColumn.setCellValueFactory(new PropertyValueFactory<>("role"));
        statusColumn.setCellValueFactory(cellData -> {
            User user = cellData.getValue();
            String status = user.isActive() ? "Active" : "Inactive";
            return new javafx.beans.property.SimpleStringProperty(status);
        });
        
        actionsColumn.setCellFactory(param -> new TableCell<>() {
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    User user = getTableView().getItems().get(getIndex());
                    HBox actions = new HBox(4);
                    
                    Button editButton = new Button("✏");
                    editButton.getStyleClass().add("table-action-button");
                    editButton.setOnAction(e -> handleEditUser(user));
                    
                    Button deleteButton = new Button("🗑");
                    deleteButton.getStyleClass().add("table-action-button-danger");
                    deleteButton.setOnAction(e -> handleDeleteUser(user));
                    
                    actions.getChildren().addAll(editButton, deleteButton);
                    setGraphic(actions);
                }
            }
        });
    }

    private void setupCAPSTable() {
        capsCodeColumn.setCellValueFactory(new PropertyValueFactory<>("capsCode"));
        capsSubjectColumn.setCellValueFactory(new PropertyValueFactory<>("subject"));
        capsGradeColumn.setCellValueFactory(new PropertyValueFactory<>("gradeLevel"));
        capsTermColumn.setCellValueFactory(cellData -> {
            CAPSEntry entry = cellData.getValue();
            return new javafx.beans.property.SimpleStringProperty(String.valueOf(entry.getTerm()));
        });
        capsTopicColumn.setCellValueFactory(new PropertyValueFactory<>("topic"));
        
        capsActionsColumn.setCellFactory(param -> new TableCell<>() {
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    CAPSEntry entry = getTableView().getItems().get(getIndex());
                    HBox actions = new HBox(4);
                    
                    Button editButton = new Button("✏");
                    editButton.getStyleClass().add("table-action-button");
                    editButton.setOnAction(e -> handleEditCAPSEntry(entry));
                    
                    Button deleteButton = new Button("🗑");
                    deleteButton.getStyleClass().add("table-action-button-danger");
                    deleteButton.setOnAction(e -> handleDeleteCAPSEntry(entry));
                    
                    actions.getChildren().addAll(editButton, deleteButton);
                    setGraphic(actions);
                }
            }
        });
    }

    private void setupEventsTable() {
        eventNameColumn.setCellValueFactory(new PropertyValueFactory<>("eventName"));
        eventDateColumn.setCellValueFactory(new PropertyValueFactory<>("eventDate"));
        eventTypeColumn.setCellValueFactory(new PropertyValueFactory<>("eventType"));
        
        eventActionsColumn.setCellFactory(param -> new TableCell<>() {
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    SchoolEvent event = getTableView().getItems().get(getIndex());
                    HBox actions = new HBox(4);
                    
                    Button editButton = new Button("✏");
                    editButton.getStyleClass().add("table-action-button");
                    editButton.setOnAction(e -> handleEditEvent(event));
                    
                    Button deleteButton = new Button("🗑");
                    deleteButton.getStyleClass().add("table-action-button-danger");
                    deleteButton.setOnAction(e -> handleDeleteEvent(event));
                    
                    actions.getChildren().addAll(editButton, deleteButton);
                    setGraphic(actions);
                }
            }
        });
    }

    private void setupAuditLogTable() {
        auditActionColumn.setCellValueFactory(new PropertyValueFactory<>("action"));
        auditTargetColumn.setCellValueFactory(new PropertyValueFactory<>("target"));
        auditTimestampColumn.setCellValueFactory(new PropertyValueFactory<>("timestamp"));
    }

    private void setupFeedbackTable() {
        feedbackTitleColumn.setCellValueFactory(new PropertyValueFactory<>("title"));
        feedbackTypeColumn.setCellValueFactory(new PropertyValueFactory<>("feedbackType"));
        feedbackPriorityColumn.setCellValueFactory(new PropertyValueFactory<>("priority"));
        feedbackStatusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
        feedbackDateColumn.setCellValueFactory(new PropertyValueFactory<>("createdAt"));

        feedbackActionsColumn.setCellFactory(param -> new TableCell<>() {
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    Feedback feedback = getTableView().getItems().get(getIndex());
                    HBox actions = new HBox(4);

                    Button viewButton = new Button("View");
                    viewButton.setOnAction(e -> viewFeedbackDetails(feedback));
                    viewButton.getStyleClass().add("table-action-button");

                    Button resolveButton = new Button("Resolve");
                    resolveButton.setOnAction(e -> resolveFeedback(feedback));
                    resolveButton.getStyleClass().add("table-action-button");

                    actions.getChildren().addAll(viewButton, resolveButton);
                    setGraphic(actions);
                }
            }
        });
    }

    private void setupCalendarTable() {
        calendarYearColumn.setCellValueFactory(new PropertyValueFactory<>("academicYear"));
        calendarTotalDaysColumn.setCellValueFactory(new PropertyValueFactory<>("totalTeachingDays"));
        calendarStartDateColumn.setCellValueFactory(new PropertyValueFactory<>("startDate"));
        calendarEndDateColumn.setCellValueFactory(new PropertyValueFactory<>("endDate"));

        calendarActionsColumn.setCellFactory(param -> new TableCell<>() {
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    SchoolCalendar calendar = getTableView().getItems().get(getIndex());
                    HBox actions = new HBox(4);

                    Button editButton = new Button("✏");
                    editButton.getStyleClass().add("table-action-button");
                    editButton.setOnAction(e -> handleEditCalendar(calendar));

                    actions.getChildren().addAll(editButton);
                    setGraphic(actions);
                }
            }
        });
    }

    private void loadAllData() {
        loadUsers();
        loadCAPSEntries();
        loadSchoolEvents();
        loadAuditLogs();
        loadFeedback();
        loadSchoolCalendars();
    }

    private void loadUsers() {
        List<User> users = userDAO.getAllUsers();
        usersTable.getItems().setAll(users);
    }

    private void loadCAPSEntries() {
        List<CAPSEntry> entries = capsDAO.getAllCAPSEntries();
        capsTable.getItems().setAll(entries);
    }

    private void loadSchoolEvents() {
        List<SchoolEvent> events = eventDAO.getAllSchoolEvents();
        eventsTable.getItems().setAll(events);
    }

    private void loadAuditLogs() {
        List<AuditLog> logs = auditLogDAO.getAllAuditLogs();
        auditLogTable.getItems().setAll(logs);
    }

    private void loadFeedback() {
        List<Feedback> feedbackList = feedbackDAO.getAllFeedback();
        feedbackTable.getItems().setAll(feedbackList);
    }

    private void loadSchoolCalendars() {
        calendarDAO.getCurrentOrDefaultCalendar();
        List<SchoolCalendar> calendars = calendarDAO.getAllSchoolCalendars();
        calendarTable.getItems().setAll(calendars);
    }

    @FXML
    private void handleAddUser() {
        showUserDialog(null);
    }

    @FXML
    private void handleEditUser(User user) {
        showUserDialog(user);
    }

    @FXML
    private void handleDeleteUser(User user) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Delete User");
        confirm.setHeaderText("Delete user: " + user.getUsername() + "?");
        confirm.setContentText("This action cannot be undone.");
        
        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            boolean success = userDAO.deleteUser(user.getUserId());
            if (success) {
                logAuditAction("Deleted user account", user.getUsername());
                loadUsers();
            }
        }
    }

    private void showUserDialog(User user) {
        // Simple implementation using alert for now
        Alert dialog = new Alert(Alert.AlertType.CONFIRMATION);
        dialog.setTitle(user == null ? "Add User" : "Edit User");
        dialog.setHeaderText(user == null ? "Create new user account" : "Edit user account");
        
        // Create form content
        TextField usernameField = new TextField();
        usernameField.setPromptText("Username");
        TextField emailField = new TextField();
        emailField.setPromptText("Email");
        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Password");
        ComboBox<String> roleCombo = new ComboBox<>();
        roleCombo.getItems().addAll("TEACHER", "PRINCIPAL_HOD", "ADMINISTRATOR");
        roleCombo.setValue("TEACHER");
        
        if (user != null) {
            usernameField.setText(user.getUsername());
            usernameField.setDisable(true);
            emailField.setText(user.getEmail());
            roleCombo.setValue(user.getRole());
        }
        
        VBox content = new VBox(12);
        content.getChildren().addAll(
            new Label("Username:"), usernameField,
            new Label("Email:"), emailField,
            new Label("Password:"), passwordField,
            new Label("Role:"), roleCombo
        );
        
        dialog.getDialogPane().setContent(content);
        
        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            if (user == null) {
                // Create new user
                User newUser = new User();
                newUser.setUserId(UUID.randomUUID().toString());
                newUser.setUsername(usernameField.getText());
                newUser.setEmail(emailField.getText());
                newUser.setPasswordHash(util.PasswordHasher.hashPassword(passwordField.getText()));
                newUser.setRole(roleCombo.getValue());
                newUser.setActive(true);
                
                boolean success = userDAO.createUser(newUser);
                if (success) {
                    logAuditAction("Created user account", usernameField.getText());
                    loadUsers();
                }
            } else {
                // Update existing user
                user.setEmail(emailField.getText());
                user.setRole(roleCombo.getValue());
                if (!passwordField.getText().isEmpty()) {
                    user.setPasswordHash(util.PasswordHasher.hashPassword(passwordField.getText()));
                }
                
                boolean success = userDAO.updateUser(user);
                if (success) {
                    logAuditAction("Updated user account", usernameField.getText());
                    loadUsers();
                }
            }
        }
    }

    @FXML
    private void handleAddCAPSEntry() {
        showCAPSEntryDialog(null);
    }

    @FXML
    private void handleEditCAPSEntry(CAPSEntry entry) {
        showCAPSEntryDialog(entry);
    }

    @FXML
    private void handleDeleteCAPSEntry(CAPSEntry entry) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Delete CAPS Entry");
        confirm.setHeaderText("Delete CAPS entry: " + entry.getCapsCode() + "?");
        confirm.setContentText("This action cannot be undone.");
        
        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            boolean success = capsDAO.deleteCAPSEntry(entry.getCapsCode());
            if (success) {
                logAuditAction("Deleted CAPS entry", entry.getCapsCode());
                loadCAPSEntries();
            }
        }
    }

    private void showCAPSEntryDialog(CAPSEntry entry) {
        // Simple implementation using alert for now
        Alert dialog = new Alert(Alert.AlertType.CONFIRMATION);
        dialog.setTitle(entry == null ? "Add CAPS Entry" : "Edit CAPS Entry");
        dialog.setHeaderText(entry == null ? "Add new CAPS curriculum entry" : "Edit CAPS curriculum entry");
        
        // Create form content
        TextField capsCodeField = new TextField();
        capsCodeField.setPromptText("CAPS Code (e.g., CAPS-M10-T1-001)");
        TextField subjectField = new TextField();
        subjectField.setPromptText("Subject");
        TextField gradeField = new TextField();
        gradeField.setPromptText("Grade Level (e.g., Grade 10)");
        ComboBox<Integer> termCombo = new ComboBox<>();
        termCombo.getItems().addAll(1, 2, 3, 4);
        termCombo.setValue(1);
        TextField topicField = new TextField();
        topicField.setPromptText("Topic");
        TextArea outcomesArea = new TextArea();
        outcomesArea.setPromptText("Learning Outcomes");
        outcomesArea.setPrefRowCount(3);
        TextArea standardsArea = new TextArea();
        standardsArea.setPromptText("Assessment Standards");
        standardsArea.setPrefRowCount(2);
        
        if (entry != null) {
            capsCodeField.setText(entry.getCapsCode());
            subjectField.setText(entry.getSubject());
            gradeField.setText(entry.getGradeLevel());
            termCombo.setValue(entry.getTerm());
            topicField.setText(entry.getTopic());
            outcomesArea.setText(entry.getOutcomes());
            standardsArea.setText(entry.getAssessmentStandards());
        }
        
        VBox content = new VBox(12);
        content.getChildren().addAll(
            new Label("CAPS Code:"), capsCodeField,
            new Label("Subject:"), subjectField,
            new Label("Grade Level:"), gradeField,
            new Label("Term:"), termCombo,
            new Label("Topic:"), topicField,
            new Label("Learning Outcomes:"), outcomesArea,
            new Label("Assessment Standards:"), standardsArea
        );
        
        dialog.getDialogPane().setContent(content);
        
        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            String capsCode = capsCodeField.getText().trim();
            if (capsCode.isEmpty()) {
                showErrorMessage("CAPS code cannot be empty.");
                return;
            }

            CAPSEntry capsEntry = new CAPSEntry();
            capsEntry.setCapsCode(capsCode);
            capsEntry.setSubject(subjectField.getText());
            capsEntry.setGradeLevel(gradeField.getText());
            capsEntry.setTerm(termCombo.getValue());
            capsEntry.setTopic(topicField.getText());
            capsEntry.setOutcomes(outcomesArea.getText());
            capsEntry.setAssessmentStandards(standardsArea.getText());
            
            boolean success;
            if (entry == null) {
                success = capsDAO.createCAPSEntry(capsEntry);
                if (success) {
                    logAuditAction("Created CAPS entry", capsCode);
                }
            } else {
                success = capsDAO.updateCAPSEntry(capsEntry, entry.getCapsCode());
                if (success) {
                    logAuditAction("Updated CAPS entry", capsCode);
                }
            }
            
            if (success) {
                loadCAPSEntries();
            } else {
                showErrorMessage("Could not save the CAPS entry. The CAPS code may already be in use.");
            }
        }
    }

    @FXML
    private void handleAddEvent() {
        showEventDialog(null);
    }

    @FXML
    private void handleEditEvent(SchoolEvent event) {
        showEventDialog(event);
    }

    @FXML
    private void handleDeleteEvent(SchoolEvent event) {
        int displacedLessonCount = SchedulingEngine.getInstance()
            .countLessonsRescheduledDueToEvent(event);
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Delete School Event");
        confirm.setHeaderText("Delete event: " + event.getEventName() + "?");
        confirm.setContentText("This action cannot be undone.\n\n"
            + displacedLessonCount + " lesson(s) were displaced by this event. "
            + "Deleting it will not move those lessons back; teachers will keep control "
            + "of their plans.");
        
        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            boolean success = eventDAO.deleteSchoolEvent(event.getEventId());
            if (success) {
                logAuditAction("Deleted school event", event.getEventName());
                loadSchoolEvents();
            }
        }
    }

    private void showEventDialog(SchoolEvent event) {
        // Simple implementation using alert for now
        Alert dialog = new Alert(Alert.AlertType.CONFIRMATION);
        dialog.setTitle(event == null ? "Add School Event" : "Edit School Event");
        dialog.setHeaderText(event == null ? "Add new school event" : "Edit school event");
        
        // Create form content
        TextField eventNameField = new TextField();
        eventNameField.setPromptText("Event Name");
        DatePicker eventDatePicker = new DatePicker();
        ComboBox<String> eventTypeCombo = new ComboBox<>();
        eventTypeCombo.getItems().addAll("FULL_DAY", "HALF_DAY");
        eventTypeCombo.setValue("FULL_DAY");
        TextArea descriptionArea = new TextArea();
        descriptionArea.setPromptText("Description");
        descriptionArea.setPrefRowCount(2);
        
        if (event != null) {
            eventNameField.setText(event.getEventName());
            eventDatePicker.setValue(java.time.LocalDate.parse(event.getEventDate()));
            eventTypeCombo.setValue(event.getEventType());
            descriptionArea.setText(event.getDescription());
        }
        
        VBox content = new VBox(12);
        content.getChildren().addAll(
            new Label("Event Name:"), eventNameField,
            new Label("Event Date:"), eventDatePicker,
            new Label("Event Type:"), eventTypeCombo,
            new Label("Description:"), descriptionArea
        );
        
        dialog.getDialogPane().setContent(content);
        
        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            SchoolEvent schoolEvent = event != null ? event : new SchoolEvent();
            schoolEvent.setEventId(event != null ? event.getEventId() : UUID.randomUUID().toString());
            schoolEvent.setEventName(eventNameField.getText());
            schoolEvent.setEventDate(eventDatePicker.getValue() != null ? eventDatePicker.getValue().toString() : "");
            schoolEvent.setEventType(eventTypeCombo.getValue());
            schoolEvent.setDescription(descriptionArea.getText());
            
            boolean success;
            if (event == null) {
                success = eventDAO.createSchoolEvent(schoolEvent);
                if (success) {
                    logAuditAction("Created school event", eventNameField.getText());
                }
            } else {
                success = eventDAO.updateSchoolEvent(schoolEvent);
                if (success) {
                    logAuditAction("Updated school event", eventNameField.getText());
                }
            }
            
            if (success) {
                loadSchoolEvents();
            }
        }
    }

    @FXML
    private void handleExportLog() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Export Audit Log");
        chooser.setInitialFileName("audit-log.csv");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV files", "*.csv"));
        File file = chooser.showSaveDialog(auditLogTable.getScene().getWindow());
        if (file == null) return;
        if (!file.getName().toLowerCase(java.util.Locale.ROOT).endsWith(".csv")) {
            file = new File(file.getParentFile(), file.getName() + ".csv");
        }

        List<AuditLog> logs;
        try {
            logs = auditLogDAO.getAllAuditLogsForExport();
        } catch (SQLException e) {
            e.printStackTrace();
            showErrorMessage("Unable to load audit log for export: " + e.getMessage());
            return;
        }

        try (BufferedWriter writer = Files.newBufferedWriter(file.toPath(), StandardCharsets.UTF_8)) {
            writer.write("Entry ID,User ID,Action,Target,Timestamp");
            writer.newLine();
            for (AuditLog log : logs) {
                writer.write(csvValue(log.getEntryId()));
                writer.write(",");
                writer.write(csvValue(log.getUserId()));
                writer.write(",");
                writer.write(csvValue(log.getAction()));
                writer.write(",");
                writer.write(csvValue(log.getTarget()));
                writer.write(",");
                writer.write(csvValue(log.getTimestamp()));
                writer.newLine();
            }
            showSuccessMessage("Audit log exported to " + file.getAbsolutePath());
        } catch (IOException e) {
            e.printStackTrace();
            showErrorMessage("Unable to export audit log: " + e.getMessage());
        }
    }

    private String csvValue(String value) {
        String safeValue = value == null ? "" : value;
        if (safeValue.matches("^[\\s]*[=+@-].*")) safeValue = "'" + safeValue;
        return "\"" + safeValue.replace("\"", "\"\"") + "\"";
    }

    @FXML
    private void handleLogout() {
        SessionManager.getInstance().endSession();
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/LoginView.fxml"));
            Parent loginView = loader.load();
            logoutButton.getScene().setRoot(loginView);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void viewFeedbackDetails(Feedback feedback) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Feedback Details");
        alert.setHeaderText(feedback.getTitle());

        StringBuilder content = new StringBuilder();
        content.append("Type: ").append(feedback.getFeedbackType()).append("\n");
        content.append("Priority: ").append(feedback.getPriority()).append("\n");
        content.append("Status: ").append(feedback.getStatus()).append("\n");
        content.append("Submitted: ").append(feedback.getCreatedAt()).append("\n\n");
        content.append("Description:\n").append(feedback.getDescription()).append("\n");

        if (feedback.getAdminNotes() != null && !feedback.getAdminNotes().isEmpty()) {
            content.append("\nAdmin Notes:\n").append(feedback.getAdminNotes());
        }

        alert.setContentText(content.toString());
        alert.showAndWait();
    }

    private void resolveFeedback(Feedback feedback) {
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Resolve Feedback");
        dialog.setHeaderText("Add resolution notes for: " + feedback.getTitle());

        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        TextArea notesArea = new TextArea();
        notesArea.setPromptText("Enter resolution notes...");
        notesArea.setPrefRowCount(5);

        VBox vbox = new VBox(notesArea);
        dialog.getDialogPane().setContent(vbox);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == ButtonType.OK) {
                return notesArea.getText();
            }
            return null;
        });

        dialog.showAndWait().ifPresent(notes -> {
            feedback.setStatus("RESOLVED");
            feedback.setAdminNotes(notes);
            feedback.setUpdatedAt(java.time.LocalDateTime.now().format(
                java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

            if (feedbackDAO.updateFeedback(feedback)) {
                showSuccessMessage("Feedback marked as resolved");
                loadFeedback();
            } else {
                showErrorMessage("Failed to update feedback");
            }
        });
    }

    private void logAuditAction(String action, String target) {
        if (currentUser != null) {
            AuditLog log = new AuditLog();
            log.setEntryId(UUID.randomUUID().toString());
            log.setUserId(currentUser.getUserId());
            log.setAction(action);
            log.setTarget(target);
            auditLogDAO.createAuditLog(log);
            loadAuditLogs();
        }
    }

    private void setupActivityMonitoring() {
        userNameLabel.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                newScene.addEventFilter(MouseEvent.MOUSE_CLICKED,
                    e -> SessionManager.getInstance().resetInactivityTimer());
                newScene.addEventFilter(KeyEvent.KEY_TYPED,
                    e -> SessionManager.getInstance().resetInactivityTimer());
            }
        });
    }

    private void showSuccessMessage(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Success");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showErrorMessage(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    @FXML
    private void handleEditCalendar(SchoolCalendar calendar) {
        showCalendarDialog(calendar);
    }

    private void showCalendarDialog(SchoolCalendar calendar) {
        Alert dialog = new Alert(Alert.AlertType.CONFIRMATION);
        dialog.setTitle(calendar == null ? "Add Academic Year" : "Edit School Calendar");
        dialog.setHeaderText(calendar == null
            ? "Set the system-wide teaching-day target for an academic year"
            : "Edit system-wide teaching-day settings");

        TextField yearField = new TextField();
        yearField.setPromptText("Academic Year (e.g., 2026)");
        yearField.setText(String.valueOf(calendar == null
            ? java.time.Year.now().getValue()
            : calendar.getAcademicYear()));
        yearField.setDisable(calendar != null);
        TextField totalDaysField = new TextField();
        totalDaysField.setPromptText("Teaching days for all users (default: 180)");
        totalDaysField.setText(String.valueOf(calendar == null ? 180 : calendar.getTotalTeachingDays()));
        DatePicker startDatePicker = new DatePicker();
        DatePicker endDatePicker = new DatePicker();
        int defaultYear = calendar == null ? java.time.Year.now().getValue() : calendar.getAcademicYear();
        startDatePicker.setValue(java.time.LocalDate.of(defaultYear, 1, 15));
        endDatePicker.setValue(java.time.LocalDate.of(defaultYear, 12, 1));

        if (calendar != null) {
            if (calendar.getStartDate() != null && !calendar.getStartDate().isEmpty()) {
                startDatePicker.setValue(java.time.LocalDate.parse(calendar.getStartDate()));
            }
            if (calendar.getEndDate() != null && !calendar.getEndDate().isEmpty()) {
                endDatePicker.setValue(java.time.LocalDate.parse(calendar.getEndDate()));
            }
        }

        VBox content = new VBox(12);
        content.getChildren().addAll(
            new Label("Academic Year:"), yearField,
            new Label("Teaching Days (system-wide):"), totalDaysField,
            new Label("Start Date:"), startDatePicker,
            new Label("End Date:"), endDatePicker
        );

        dialog.getDialogPane().setContent(content);

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                int academicYear = Integer.parseInt(yearField.getText().trim());
                int totalTeachingDays = Integer.parseInt(totalDaysField.getText().trim());
                if (academicYear < 1 || academicYear > 9999) {
                    showErrorMessage("Enter a valid academic year.");
                    return;
                }
                if (totalTeachingDays < 1 || totalTeachingDays > 366) {
                    showErrorMessage("Teaching days must be between 1 and 366.");
                    return;
                }
                if (startDatePicker.getValue() == null || endDatePicker.getValue() == null
                        || startDatePicker.getValue().isAfter(endDatePicker.getValue())) {
                    showErrorMessage("Choose valid start and end dates; the end date must not be before the start date.");
                    return;
                }

                SchoolCalendar updatedCalendar = calendar == null ? new SchoolCalendar() : calendar;
                updatedCalendar.setAcademicYear(academicYear);
                updatedCalendar.setTotalTeachingDays(totalTeachingDays);
                updatedCalendar.setStartDate(startDatePicker.getValue().toString());
                updatedCalendar.setEndDate(endDatePicker.getValue().toString());

                boolean success = calendar == null
                    ? calendarDAO.createSchoolCalendar(updatedCalendar)
                    : calendarDAO.updateSchoolCalendar(updatedCalendar);
                if (success) {
                    String action = calendar == null ? "Created school calendar" : "Updated school calendar";
                    logAuditAction(action, "Year " + academicYear + ": " + totalTeachingDays + " teaching days");
                    loadSchoolCalendars();
                    showSuccessMessage("System-wide teaching days saved for " + academicYear + ".");
                } else {
                    showErrorMessage("Could not save the academic year. It may already exist; edit its row instead.");
                }
            } catch (NumberFormatException e) {
                showErrorMessage("Academic year and teaching days must be whole numbers.");
            }
        }
    }

    @FXML
    private void handleAddSchoolCalendar() {
        showCalendarDialog(null);
    }
}