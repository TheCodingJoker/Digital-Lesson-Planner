package controller;

import dao.LessonPlanDAO;
import dao.UserDAO;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.Region;
import javafx.stage.Stage;
import model.LessonPlan;
import model.TeacherProgress;
import model.User;
import util.SessionManager;

import java.util.ArrayList;
import java.util.List;

public class PrincipalDashboardController {

    // =========================================================
    // FXML FIELDS
    // =========================================================

    @FXML
    private Label principalNameLabel;

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
    private TableColumn<TeacherProgress, String> lessonCountColumn;
    @FXML
    private TableColumn<TeacherProgress, String> statusColumn;
    @FXML
    private TableColumn<TeacherProgress, String> actionsColumn;

    // =========================================================
    // DATABASE ACCESS
    // =========================================================

    private final UserDAO userDAO = new UserDAO();
    private final LessonPlanDAO lessonPlanDAO = new LessonPlanDAO();

    // =========================================================
    // INITIALIZE
    // =========================================================

    @FXML
    public void initialize() {
        loadPrincipalName();
        setupTeacherProgressTable();
        loadDashboardData();
    }

    // =========================================================
    // LOAD PRINCIPAL NAME
    // =========================================================

    private void loadPrincipalName() {
        try {
            if (SessionManager.getInstance().getCurrentUser() != null) {
                String username = SessionManager.getInstance().getCurrentUser().getUsername();
                if (principalNameLabel != null) {
                    principalNameLabel.setText("Welcome, " + username);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            if (principalNameLabel != null) {
                principalNameLabel.setText("Welcome, Principal");
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
        lessonCountColumn.setCellValueFactory(new PropertyValueFactory<>("lessonCountDisplay"));
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));

        // Custom cell factory for status column with color coding
        statusColumn.setCellFactory(column -> new TableCell<TeacherProgress, String>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(status);
                    switch (status) {
                        case "ON_TRACK":
                            setStyle("-fx-text-fill: #16a34a; -fx-font-weight: bold;");
                            break;
                        case "AT_RISK":
                            setStyle("-fx-text-fill: #d97706; -fx-font-weight: bold;");
                            break;
                        case "BEHIND":
                            setStyle("-fx-text-fill: #dc2626; -fx-font-weight: bold;");
                            break;
                        default:
                            setStyle("");
                    }
                }
            }
        });

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
        try {
            // Get all teachers
            List<User> teachers = userDAO.getTeachers();
            List<TeacherProgress> progressList = new ArrayList<>();

            int totalTeachers = teachers.size();
            double totalProgress = 0;
            int onTrackCount = 0;
            int atRiskCount = 0;
            int behindCount = 0;

            // Calculate progress for each teacher
            for (User teacher : teachers) {
                List<LessonPlan> lessons = lessonPlanDAO.getLessonsByUserId(teacher.getUserId());
                
                int totalLessons = lessons.size();
                int completedLessons = 0;
                
                for (LessonPlan lesson : lessons) {
                    if ("COMPLETED".equals(lesson.getStatus())) {
                        completedLessons++;
                    }
                }

                double progressPercentage = totalLessons > 0 ? 
                    (completedLessons * 100.0 / totalLessons) : 0;
                
                totalProgress += progressPercentage;

                // Determine status
                String status;
                if (progressPercentage >= 70) {
                    status = "ON_TRACK";
                    onTrackCount++;
                } else if (progressPercentage >= 50) {
                    status = "AT_RISK";
                    atRiskCount++;
                } else {
                    status = "BEHIND";
                    behindCount++;
                }

                // Get subject and grade from lessons (use first lesson's data)
                String subject = "";
                String grade = "";
                if (!lessons.isEmpty()) {
                    subject = lessons.get(0).getSubject();
                    grade = lessons.get(0).getGradeLevel();
                }

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
            
            double averageProgress = totalTeachers > 0 ? totalProgress / totalTeachers : 0;
            averageProgressLabel.setText(String.format("%.1f%%", averageProgress));
            
            onTrackLabel.setText(String.valueOf(onTrackCount));
            needSupportLabel.setText(String.valueOf(atRiskCount + behindCount));

            // Update status percentages
            if (totalTeachers > 0) {
                onTrackPercentLabel.setText(String.format("%.0f%%", (onTrackCount * 100.0 / totalTeachers)));
                atRiskPercentLabel.setText(String.format("%.0f%%", (atRiskCount * 100.0 / totalTeachers)));
                behindPercentLabel.setText(String.format("%.0f%%", (behindCount * 100.0 / totalTeachers)));
            } else {
                onTrackPercentLabel.setText("0%");
                atRiskPercentLabel.setText("0%");
                behindPercentLabel.setText("0%");
            }

            // Update subject progress (simplified - using all lessons)
            updateSubjectProgress(progressList);

            // Populate table
            teacherProgressTable.getItems().clear();
            teacherProgressTable.getItems().addAll(progressList);

        } catch (Exception e) {
            e.printStackTrace();
            showError("Dashboard Error", "Unable to load dashboard data: " + e.getMessage());
        }
    }

    // =========================================================
    // UPDATE SUBJECT PROGRESS
    // =========================================================

    private void updateSubjectProgress(List<TeacherProgress> progressList) {
        double mathProgress = 0;
        double scienceProgress = 0;
        double languageProgress = 0;
        int mathCount = 0;
        int scienceCount = 0;
        int languageCount = 0;

        for (TeacherProgress progress : progressList) {
            String subject = progress.getSubject().toLowerCase();
            if (subject.contains("math")) {
                mathProgress += progress.getProgressPercentage();
                mathCount++;
            } else if (subject.contains("science") || subject.contains("physics") || subject.contains("chemistry") || subject.contains("biology")) {
                scienceProgress += progress.getProgressPercentage();
                scienceCount++;
            } else if (subject.contains("english") || subject.contains("language") || subject.contains("afrikaans")) {
                languageProgress += progress.getProgressPercentage();
                languageCount++;
            }
        }

        mathProgressLabel.setText(mathCount > 0 ? String.format("%.1f%%", mathProgress / mathCount) : "0%");
        scienceProgressLabel.setText(scienceCount > 0 ? String.format("%.1f%%", scienceProgress / scienceCount) : "0%");
        languageProgressLabel.setText(languageCount > 0 ? String.format("%.1f%%", languageProgress / languageCount) : "0%");
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
        content.append("Status: ").append(progress.getStatus()).append("\n");
        
        alert.setContentText(content.toString());
        alert.showAndWait();
    }

    // =========================================================
    // NAVIGATION HANDLERS
    // =========================================================

    @FXML
    private void showDashboard() {
        loadDashboardData();
    }

    @FXML
    private void showTeachers() {
        // Navigate to teachers view (to be implemented)
        showInfo("Teachers View", "Teacher management view will be implemented.");
    }

    @FXML
    private void showCalendar() {
        // Navigate to calendar view (to be implemented)
        showInfo("Calendar View", "School calendar view will be implemented.");
    }

    @FXML
    private void showReports() {
        // Navigate to reports view (to be implemented)
        showInfo("Reports View", "Reports view will be implemented.");
    }

    @FXML
    private void refreshDashboard() {
        loadDashboardData();
    }

    @FXML
    private void handleExportReport() {
        // Export functionality (to be implemented with PDF/Word export)
        showInfo("Export Report", "PDF/Word export functionality will be implemented.");
    }

    @FXML
    private void handleLogout() {
        try {
            SessionManager.getInstance().endSession();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/LoginView.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) principalNameLabel.getScene().getWindow();
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

    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}