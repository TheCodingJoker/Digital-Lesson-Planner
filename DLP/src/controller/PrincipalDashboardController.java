package controller;

import dao.LessonPlanDAO;
import dao.UserDAO;
import dao.SchoolCalendarDAO;
import dao.SchoolEventDAO;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import model.LessonPlan;
import model.TeacherProgress;
import model.User;
import model.SchoolCalendar;
import model.SchoolEvent;
import util.SessionManager;

import java.util.ArrayList;
import java.util.List;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public class PrincipalDashboardController {

    // =========================================================
    // FXML FIELDS
    // =========================================================

    @FXML
    private Label userNameLabel;
    @FXML
    private Button logoutButton;
    @FXML
    private Button navDashboardButton;
    @FXML
    private Button navTeachersButton;
    @FXML
    private Button navCalendarButton;
    @FXML
    private Button navReportsButton;
    @FXML
    private StackPane contentArea;

    // KPI Labels
    @FXML
    private Label totalTeachersLabel;
    @FXML
    private Label averageProgressLabel;
    @FXML
    private Label onTrackLabel;
    @FXML
    private Label needSupportLabel;

    // Status Labels
    @FXML
    private Label onTrackPercentLabel;
    @FXML
    private Label atRiskPercentLabel;
    @FXML
    private Label behindPercentLabel;

    // Subject Progress Labels
    @FXML
    private Label mathProgressLabel;
    @FXML
    private Label scienceProgressLabel;
    @FXML
    private Label languageProgressLabel;
    @FXML
    private Label humanitiesProgressLabel;

    // Progress Bars
    @FXML
    private ProgressBar mathProgressBar;
    @FXML
    private ProgressBar scienceProgressBar;
    @FXML
    private ProgressBar languageProgressBar;
    @FXML
    private ProgressBar humanitiesProgressBar;
    @FXML
    private ProgressBar onTrackProgressBar;
    @FXML
    private ProgressBar atRiskProgressBar;
    @FXML
    private ProgressBar behindProgressBar;

    // Teacher Progress Table
    @FXML
    private TableView<TeacherProgress> teacherProgressTable;
    @FXML
    private TableColumn<TeacherProgress, String> teacherNameColumn;
    @FXML
    private TableColumn<TeacherProgress, String> subjectColumn;
    @FXML
    private TableColumn<TeacherProgress, String> gradeColumn;
    @FXML
    private TableColumn<TeacherProgress, String> progressColumn;
    @FXML
    private TableColumn<TeacherProgress, String> lessonsColumn;
    @FXML
    private TableColumn<TeacherProgress, String> statusColumn;
    @FXML
    private TableColumn<TeacherProgress, String> actionsColumn;

    // =========================================================
    // DATABASE ACCESS
    // =========================================================

    private final UserDAO userDAO = new UserDAO();
    private final LessonPlanDAO lessonPlanDAO = new LessonPlanDAO();
    private final SchoolCalendarDAO schoolCalendarDAO = new SchoolCalendarDAO();
    private final SchoolEventDAO schoolEventDAO = new SchoolEventDAO();
    private List<TeacherProgress> currentTeacherProgress = new ArrayList<>();
    private Node dashboardView;
    private boolean dashboardDataLoaded;

    // =========================================================
    // INITIALIZE
    // =========================================================

    @FXML
    public void initialize() {
        dashboardView = contentArea.getChildren().get(0);
        loadPrincipalName();
        setupTeacherProgressTable();
        loadDashboardData();
        setupActivityMonitoring();
        setActiveNavigation(navDashboardButton);
    }

    private String formatStatus(String status) {
        if (status == null) return "";
        switch (status) {
            case "HIGH_COMPLETION":
                return "High Completion";
            case "MODERATE_COMPLETION":
                return "Moderate Completion";
            case "LOW_COMPLETION":
                return "Low Completion";
            case "NO_LESSONS":
                return "No Lessons";
            default:
                return status == null ? "" : status;
        }
    }

    private String getStatusStyle(String status) {
        if (status == null) return "";
        switch (status) {
            case "HIGH_COMPLETION":
                return "-fx-text-fill: #16a34a; -fx-font-weight: bold;";
            case "MODERATE_COMPLETION":
                return "-fx-text-fill: #d97706; -fx-font-weight: bold;";
            case "LOW_COMPLETION":
                return "-fx-text-fill: #dc2626; -fx-font-weight: bold;";
            case "NO_LESSONS":
                return "-fx-text-fill: #64748b; -fx-font-weight: bold;";
            default:
                return "";
        }
    }

    private void applyStatusCellFormatting(TableColumn<TeacherProgress, String> column) {
        column.setCellFactory(tableColumn -> new TableCell<>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                setText(empty ? null : formatStatus(status));
                setStyle(empty ? "" : getStatusStyle(status));
            }
        });
    }

    // =========================================================
    // LOAD PRINCIPAL NAME
    // =========================================================

    private void loadPrincipalName() {
        try {
            User currentUser = SessionManager.getInstance().getCurrentUser();
            if (currentUser != null && userNameLabel != null) {
                String username = currentUser.getUsername();
                userNameLabel.setText(username);
            } else if (userNameLabel != null) {
                userNameLabel.setText("Principal");
            }
        } catch (Exception e) {
            e.printStackTrace();
            if (userNameLabel != null) {
                userNameLabel.setText("Principal");
            }
        }
    }

    // =========================================================
    // SETUP TEACHER PROGRESS TABLE
    // =========================================================

    private void setupTeacherProgressTable() {
        teacherNameColumn.setCellValueFactory(new PropertyValueFactory<>("teacherName"));
        subjectColumn.setCellValueFactory(new PropertyValueFactory<>("subject"));
        gradeColumn.setCellValueFactory(new PropertyValueFactory<>("grade"));
        progressColumn.setCellValueFactory(new PropertyValueFactory<>("progressDisplay"));
        lessonsColumn.setCellValueFactory(new PropertyValueFactory<>("lessonCountDisplay"));
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));

        // Custom cell factory for progress column with progress bar
        progressColumn.setCellFactory(column -> new TableCell<TeacherProgress, String>() {
            private final ProgressBar progressBar = new ProgressBar();
            private final Label progressLabel = new Label();
            private final HBox container = new HBox(8);
            
            {
                progressBar.setPrefWidth(80);
                container.getChildren().addAll(progressBar, progressLabel);
            }

            @Override
            protected void updateItem(String progressDisplay, boolean empty) {
                super.updateItem(progressDisplay, empty);
                if (empty || progressDisplay == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    try {
                        double progress = Double.parseDouble(progressDisplay.replace("%", ""));
                        progressBar.setProgress(progress / 100);
                        progressLabel.setText(progressDisplay);
                        setText(null);
                        setGraphic(container);
                    } catch (NumberFormatException e) {
                        setText(progressDisplay);
                        setGraphic(null);
                    }
                }
            }
        });

        // Custom cell factory for status column with color coding
        applyStatusCellFormatting(statusColumn);

        // Actions column with View Details button
        actionsColumn.setCellFactory(column -> new TableCell<TeacherProgress, String>() {
            private final Button viewButton = new Button("View Details");
            
            {
                viewButton.setStyle("-fx-background-color: #2563eb; -fx-text-fill: white; -fx-font-size: 11px; -fx-padding: 4 8;");
                viewButton.setOnAction(event -> {
                    TeacherProgress progress = getTableView().getItems().get(getIndex());
                    showTeacherDetails(progress);
                });
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(viewButton);
                }
            }
        });
    }

    // =========================================================
    // LOAD DASHBOARD DATA
    // =========================================================

    private void loadDashboardData() {
        dashboardDataLoaded = false;
        currentTeacherProgress.clear();
        teacherProgressTable.getItems().clear();
        try {
            // Get all teachers
            List<User> teachers = userDAO.getTeachers();
            List<TeacherProgress> progressList = new ArrayList<>();

            int totalTeachers = teachers.size();
            double totalProgress = 0;
            int highCompletionCount = 0;
            int moderateCompletionCount = 0;
            int lowCompletionCount = 0;
            int teachersWithLessons = 0;
            int noLessonsCount = 0;

            List<LessonPlan> allLessons = new ArrayList<>();

            // Calculate progress for each teacher
            for (User teacher : teachers) {
                List<LessonPlan> lessons = lessonPlanDAO.getLessonsByUserId(teacher.getUserId());
                allLessons.addAll(lessons);
                
                int totalLessons = lessons.size();
                int completedLessons = 0;
                
                for (LessonPlan lesson : lessons) {
                    if (LessonPlan.STATUS_COMPLETED.equals(lesson.getStatus())) {
                        completedLessons++;
                    }
                }

                double progressPercentage = totalLessons > 0
                    ? (completedLessons * 100.0 / totalLessons) : 0;

                // Determine status
                String status;
                if (totalLessons == 0) {
                    status = "NO_LESSONS";
                    noLessonsCount++;
                } else if (progressPercentage >= 70) {
                    status = "HIGH_COMPLETION";
                    highCompletionCount++;
                    teachersWithLessons++;
                } else if (progressPercentage >= 50) {
                    status = "MODERATE_COMPLETION";
                    moderateCompletionCount++;
                    teachersWithLessons++;
                } else {
                    status = "LOW_COMPLETION";
                    lowCompletionCount++;
                    teachersWithLessons++;
                }
                if (totalLessons > 0) totalProgress += progressPercentage;

                String subject = getSingleOrMultipleValue(
                    getDistinctNonEmptyValues(lessons, true));
                String grade = getSingleOrMultipleValue(
                    getDistinctNonEmptyValues(lessons, false));

                TeacherProgress progress = new TeacherProgress(
                    teacher.getUserId(),
                    teacher.getUsername(),
                    subject,
                    grade,
                    progressPercentage,
                    completedLessons,
                    totalLessons,
                    status
                );
                progressList.add(progress);
            }

            // Update KPI labels
            totalTeachersLabel.setText(String.valueOf(totalTeachers));
            
            double averageProgress = teachersWithLessons > 0 ? totalProgress / teachersWithLessons : 0;
            averageProgressLabel.setText(String.format("%.1f%%", averageProgress));
            
            onTrackLabel.setText(String.valueOf(highCompletionCount));
            needSupportLabel.setText(String.valueOf(
                moderateCompletionCount + lowCompletionCount + noLessonsCount));

            // Aggregate every teacher's lessons by subject area.
            updateSubjectProgress(allLessons);
            
            // Update progress bars
            updateProgressBars(teachersWithLessons, highCompletionCount,
                moderateCompletionCount, lowCompletionCount);

            // Populate table
            teacherProgressTable.getItems().clear();
            teacherProgressTable.getItems().addAll(progressList);
            currentTeacherProgress = new ArrayList<>(progressList);
            dashboardDataLoaded = true;

        } catch (Exception e) {
            e.printStackTrace();
            showError("Dashboard Error", "Unable to load dashboard data: " + e.getMessage());
        }
    }

    // =========================================================
    // AGGREGATE COMPLETION BY SUBJECT AREA
    // =========================================================

    private List<String> getDistinctNonEmptyValues(List<LessonPlan> lessons, boolean subjects) {
        List<String> values = new ArrayList<>();
        for (LessonPlan lesson : lessons) {
            String value = subjects ? lesson.getSubject() : lesson.getGradeLevel();
            if (value != null && !value.isBlank() && !values.contains(value)) values.add(value);
        }
        return values;
    }

    private String getSingleOrMultipleValue(List<String> values) {
        if (values.isEmpty()) return "N/A";
        return values.size() == 1 ? values.get(0) : "Multiple";
    }

    private void updateSubjectProgress(List<LessonPlan> lessons) {
        double mathProgress = 0;
        double scienceProgress = 0;
        double languageProgress = 0;
        double humanitiesProgress = 0;
        int mathCount = 0;
        int scienceCount = 0;
        int languageCount = 0;
        int humanitiesCount = 0;

        for (LessonPlan lesson : lessons) {
            String subject = lesson.getSubject() == null ? "" : lesson.getSubject().toLowerCase();
            double progress = LessonPlan.STATUS_COMPLETED.equals(lesson.getStatus()) ? 100 : 0;
            if (subject.contains("math")) {
                mathProgress += progress;
                mathCount++;
            } else if (subject.contains("science") || subject.contains("physics") || subject.contains("chemistry") || subject.contains("biology")) {
                scienceProgress += progress;
                scienceCount++;
            } else if (subject.contains("english") || subject.contains("language") || subject.contains("afrikaans")) {
                languageProgress += progress;
                languageCount++;
            } else {
                humanitiesProgress += progress;
                humanitiesCount++;
            }
        }

        double mathPercent = mathCount > 0 ? mathProgress / mathCount : 0;
        double sciencePercent = scienceCount > 0 ? scienceProgress / scienceCount : 0;
        double languagePercent = languageCount > 0 ? languageProgress / languageCount : 0;
        double humanitiesPercent = humanitiesCount > 0 ? humanitiesProgress / humanitiesCount : 0;

        mathProgressLabel.setText(String.format("%.0f%%", mathPercent));
        scienceProgressLabel.setText(String.format("%.0f%%", sciencePercent));
        languageProgressLabel.setText(String.format("%.0f%%", languagePercent));
        humanitiesProgressLabel.setText(String.format("%.0f%%", humanitiesPercent));

        mathProgressBar.setProgress(mathPercent / 100);
        scienceProgressBar.setProgress(sciencePercent / 100);
        languageProgressBar.setProgress(languagePercent / 100);
        humanitiesProgressBar.setProgress(humanitiesPercent / 100);
    }
    
    private void updateProgressBars(
            int totalTeachers, int highCompletionCount, int moderateCompletionCount, int lowCompletionCount) {
        if (totalTeachers > 0) {
            double highCompletionPercent = highCompletionCount * 100.0 / totalTeachers;
            double moderateCompletionPercent = moderateCompletionCount * 100.0 / totalTeachers;
            double lowCompletionPercent = lowCompletionCount * 100.0 / totalTeachers;

            onTrackProgressBar.setProgress(highCompletionPercent / 100);
            atRiskProgressBar.setProgress(moderateCompletionPercent / 100);
            behindProgressBar.setProgress(lowCompletionPercent / 100);

            onTrackPercentLabel.setText(String.format("%.0f%%", highCompletionPercent));
            atRiskPercentLabel.setText(String.format("%.0f%%", moderateCompletionPercent));
            behindPercentLabel.setText(String.format("%.0f%%", lowCompletionPercent));
        } else {
            onTrackProgressBar.setProgress(0);
            atRiskProgressBar.setProgress(0);
            behindProgressBar.setProgress(0);

            onTrackPercentLabel.setText("0%");
            atRiskPercentLabel.setText("0%");
            behindPercentLabel.setText("0%");
        }
    }

    // =========================================================
    // SHOW TEACHER DETAILS
    // =========================================================

    private void showTeacherDetails(TeacherProgress progress) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Teacher Details");
        alert.setHeaderText(progress.getTeacherName());
        
        StringBuilder content = new StringBuilder();
        content.append("Subject: ").append(progress.getSubject()).append("\n");
        content.append("Grade: ").append(progress.getGrade()).append("\n");
        content.append("Progress: ").append(progress.getProgressDisplay()).append("\n");
        content.append("Lessons: ").append(progress.getLessonCountDisplay()).append("\n");
        content.append("Status: ").append(formatStatus(progress.getStatus())).append("\n");
        
        alert.setContentText(content.toString());
        alert.showAndWait();
    }

    // =========================================================
    // NAVIGATION HANDLERS
    // =========================================================

    @FXML
    private void showDashboard() {
        contentArea.getChildren().setAll(dashboardView);
        setActiveNavigation(navDashboardButton);
        loadDashboardData();
    }

    @FXML
    private void showTeachers() {
        loadDashboardData();
        if (!dashboardDataLoaded) return;
        TableView<TeacherProgress> table = createTeacherTable();
        VBox page = createPage("Teachers", "Review teaching activity and progress by teacher.");
        page.getChildren().add(table);
        VBox.setVgrow(table, Priority.ALWAYS);
        showPage(page, navTeachersButton);
    }

    @FXML
    private void showCalendar() {
        try {
            SchoolCalendar calendar = schoolCalendarDAO.getCurrentOrDefaultCalendar();
            if (calendar == null) {
                throw new IllegalStateException("No school calendar is configured.");
            }
            List<SchoolEvent> events = schoolEventDAO.getAllSchoolEvents();
            TableView<SchoolEvent> table = new TableView<>();
            table.getStyleClass().add("data-table");
            table.setPlaceholder(new Label("No school events are scheduled for this academic year."));
            table.getColumns().add(textColumn("Event", "eventName"));
            table.getColumns().add(textColumn("Date", "eventDate"));
            table.getColumns().add(textColumn("Type", "eventType"));
            table.getColumns().add(textColumn("Description", "description"));
            for (SchoolEvent event : events) {
                if (event.getAcademicYear() == calendar.getAcademicYear()) table.getItems().add(event);
            }

            VBox page = createPage("School Calendar", "Academic year " + calendar.getAcademicYear()
                + "  |  " + calendar.getTotalTeachingDays() + " teaching days  |  "
                + calendar.getStartDate() + " to " + calendar.getEndDate());
            page.getChildren().add(table);
            VBox.setVgrow(table, Priority.ALWAYS);
            showPage(page, navCalendarButton);
        } catch (Exception e) {
            e.printStackTrace();
            showError("Calendar Error", "Unable to load school calendar: " + e.getMessage());
        }
    }

    @FXML
    private void showReports() {
        loadDashboardData();
        if (!dashboardDataLoaded) return;
        int completed = currentTeacherProgress.stream().mapToInt(TeacherProgress::getCompletedLessons).sum();
        int planned = currentTeacherProgress.stream().mapToInt(TeacherProgress::getTotalLessons).sum();
        Label summary = new Label("Teachers: " + currentTeacherProgress.size()
            + "    |    Completed lessons: " + completed + " / " + planned);
        summary.getStyleClass().add("section-label");
        Button exportButton = new Button("Export CSV");
        exportButton.getStyleClass().add("primary-button");
        exportButton.setOnAction(event -> handleExportReport());
        HBox toolbar = new HBox(12, summary, new Pane(), exportButton);
        HBox.setHgrow(toolbar.getChildren().get(1), Priority.ALWAYS);

        TableView<TeacherProgress> table = createTeacherTable();
        VBox page = createPage("Progress Reports", "A current summary of lesson completion for all active teachers.");
        page.getChildren().addAll(toolbar, table);
        VBox.setVgrow(table, Priority.ALWAYS);
        showPage(page, navReportsButton);
    }

    @FXML
    private void refreshDashboard() {
        loadDashboardData();
    }

    @FXML
    private void handleExportReport() {
        if (!dashboardDataLoaded) loadDashboardData();
        if (!dashboardDataLoaded) return;
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Export Principal Progress Report");
        chooser.setInitialFileName("teacher-progress-report.csv");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV files", "*.csv"));
        File file = chooser.showSaveDialog(contentArea.getScene().getWindow());
        if (file == null) return;
        if (!file.getName().toLowerCase().endsWith(".csv")) {
            file = new File(file.getParentFile(), file.getName() + ".csv");
        }

        try (BufferedWriter writer = Files.newBufferedWriter(file.toPath(), StandardCharsets.UTF_8)) {
            writer.write("Teacher,Subject,Grade,Completed Lessons,Total Lessons,Progress,Status");
            writer.newLine();
            for (TeacherProgress progress : currentTeacherProgress) {
                writer.write(csvValue(progress.getTeacherName()));
                writer.write(",");
                writer.write(csvValue(progress.getSubject()));
                writer.write(",");
                writer.write(csvValue(progress.getGrade()));
                writer.write(",");
                writer.write(String.valueOf(progress.getCompletedLessons()));
                writer.write(",");
                writer.write(String.valueOf(progress.getTotalLessons()));
                writer.write(",");
                writer.write(csvValue(progress.getProgressDisplay()));
                writer.write(",");
                writer.write(csvValue(formatStatus(progress.getStatus())));
                writer.newLine();
            }
            showSuccessMessage("Progress report exported to " + file.getAbsolutePath());
        } catch (IOException e) {
            e.printStackTrace();
            showError("Export Error", "Unable to save the progress report: " + e.getMessage());
        }
    }

    private String csvValue(String value) {
        String safeValue = value == null ? "" : value;
        if (safeValue.matches("^[\\s]*[=+@-].*")) safeValue = "'" + safeValue;
        return "\"" + safeValue.replace("\"", "\"\"") + "\"";
    }

    private VBox createPage(String title, String description) {
        Label heading = new Label(title);
        heading.getStyleClass().add("app-header-title");
        Label subtitle = new Label(description);
        subtitle.getStyleClass().add("app-header-subtitle");
        VBox page = new VBox(16, heading, subtitle);
        page.getStyleClass().add("content-area");
        page.setFillWidth(true);
        return page;
    }

    private void showPage(Node page, Button activeButton) {
        contentArea.getChildren().setAll(page);
        setActiveNavigation(activeButton);
    }

    private void setActiveNavigation(Button activeButton) {
        for (Button button : List.of(navDashboardButton, navTeachersButton, navCalendarButton, navReportsButton)) {
            button.getStyleClass().remove("nav-button-active");
        }
        activeButton.getStyleClass().add("nav-button-active");
    }

    private TableView<TeacherProgress> createTeacherTable() {
        TableView<TeacherProgress> table = new TableView<>();
        table.getStyleClass().add("data-table");
        table.setPlaceholder(new Label("No active teachers found."));
        table.getColumns().add(textColumn("Teacher", "teacherName"));
        table.getColumns().add(textColumn("Subject", "subject"));
        table.getColumns().add(textColumn("Grade", "grade"));
        table.getColumns().add(textColumn("Lessons", "lessonCountDisplay"));
        table.getColumns().add(textColumn("Progress", "progressDisplay"));
        TableColumn<TeacherProgress, String> tableStatusColumn = textColumn("Status", "status");
        applyStatusCellFormatting(tableStatusColumn);
        table.getColumns().add(tableStatusColumn);
        TableColumn<TeacherProgress, String> detailsColumn = new TableColumn<>("Details");
        detailsColumn.setCellFactory(column -> new TableCell<>() {
            private final Button detailsButton = new Button("View");
            {
                detailsButton.setOnAction(event -> showTeacherDetails(getTableView().getItems().get(getIndex())));
            }
            @Override
            protected void updateItem(String value, boolean empty) {
                super.updateItem(value, empty);
                setGraphic(empty ? null : detailsButton);
            }
        });
        table.getColumns().add(detailsColumn);
        table.getItems().setAll(currentTeacherProgress);
        return table;
    }

    private <T> TableColumn<T, String> textColumn(String title, String property) {
        TableColumn<T, String> column = new TableColumn<>(title);
        column.setCellValueFactory(new PropertyValueFactory<>(property));
        return column;
    }

    @FXML
    private void handleLogout() {
        try {
            SessionManager.getInstance().endSession();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/LoginView.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) userNameLabel.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Digital Lesson Planner - Login");
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
            showError("Logout Error", "Unable to return to login screen.");
        }
    }

    // =========================================================
    // HELPER METHODS
    // =========================================================

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showSuccessMessage(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Success");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
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
}