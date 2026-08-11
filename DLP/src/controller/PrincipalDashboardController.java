package controller;

import dao.LessonPlanDAO;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import model.LessonPlan;
import util.SessionManager;

import java.util.List;

public class PrincipalDashboardController {

    // =========================================================
    // TOP BAR
    // =========================================================

    @FXML
    private Label principalNameLabel;

    // =========================================================
    // STATISTICS LABELS
    // =========================================================

    @FXML
    private Label totalLessonsLabel;

    @FXML
    private Label pendingLessonsLabel;

    @FXML
    private Label approvedLessonsLabel;

    @FXML
    private Label rejectedLessonsLabel;

    // =========================================================
    // MAIN CONTENT AREA
    // =========================================================

    @FXML
    private StackPane contentArea;

    // =========================================================
    // DATABASE
    // =========================================================

    private final LessonPlanDAO lessonPlanDAO =
            new LessonPlanDAO();

    // =========================================================
    // INITIALIZE
    // =========================================================

    @FXML
    public void initialize() {

        loadPrincipalName();

        loadStatistics();

        showDashboard();
    }

    // =========================================================
    // LOAD PRINCIPAL NAME
    // =========================================================

    private void loadPrincipalName() {

        try {

            if (SessionManager.getInstance().getCurrentUser() != null) {

                String username =
                        SessionManager.getInstance()
                                .getCurrentUser()
                                .getUsername();

                if (principalNameLabel != null) {

                    principalNameLabel.setText(
                            "Welcome, " + username
                    );
                }
            }

        } catch (Exception e) {

            e.printStackTrace();

            if (principalNameLabel != null) {

                principalNameLabel.setText(
                        "Welcome, Principal"
                );
            }
        }
    }

    // =========================================================
    // LOAD STATISTICS
    // =========================================================

    private void loadStatistics() {

        try {

            List<LessonPlan> lessons =
                    lessonPlanDAO.getAllLessonPlans();

            int total = lessons.size();

            int pending = 0;
            int approved = 0;
            int rejected = 0;

            for (LessonPlan lesson : lessons) {

                if (lesson.getStatus() == null) {
                    continue;
                }

                String status =
                        lesson.getStatus().toUpperCase();

                switch (status) {

                    case "PENDING":
                        pending++;
                        break;

                    case "APPROVED":
                        approved++;
                        break;

                    case "REJECTED":
                        rejected++;
                        break;

                    default:
                        break;
                }
            }

            if (totalLessonsLabel != null) {
                totalLessonsLabel.setText(
                        String.valueOf(total)
                );
            }

            if (pendingLessonsLabel != null) {
                pendingLessonsLabel.setText(
                        String.valueOf(pending)
                );
            }

            if (approvedLessonsLabel != null) {
                approvedLessonsLabel.setText(
                        String.valueOf(approved)
                );
            }

            if (rejectedLessonsLabel != null) {
                rejectedLessonsLabel.setText(
                        String.valueOf(rejected)
                );
            }

        } catch (Exception e) {

            e.printStackTrace();

            showError(
                    "Dashboard Error",
                    "Unable to load lesson statistics."
            );
        }
    }

    // =========================================================
    // SHOW DASHBOARD
    // =========================================================

    @FXML
    public void showDashboard() {

        loadStatistics();

        loadView(
                "/view/DashboardHome.fxml"
        );
    }

    // =========================================================
    // LESSON PLANS
    // =========================================================

    @FXML
    public void showLessonPlans() {

        loadView(
                "/view/LessonPlanView.fxml"
        );
    }

    // =========================================================
    // STUDENTS
    // =========================================================

    @FXML
    public void showStudents() {

        loadView(
                "/view/StudentView.fxml"
        );
    }

    // =========================================================
    // TEACHERS
    // =========================================================

    @FXML
    public void showTeachers() {

        loadView(
                "/view/TeacherView.fxml"
        );
    }

    // =========================================================
    // REPORTS
    // =========================================================

    @FXML
    public void showReports() {

        loadView(
                "/view/ReportsView.fxml"
        );
    }

    // =========================================================
    // LOAD VIEW INTO CONTENT AREA
    // =========================================================

    private void loadView(String fxmlFile) {

        try {

            FXMLLoader loader =
                    new FXMLLoader(
                            getClass().getResource(
                                    fxmlFile
                            )
                    );

            Parent root = loader.load();

            if (contentArea != null) {

                contentArea.getChildren().clear();

                contentArea.getChildren().add(root);
            }

        } catch (Exception e) {

            e.printStackTrace();

            showError(
                    "Navigation Error",
                    "Could not open:\n" + fxmlFile
            );
        }
    }

    // =========================================================
    // REFRESH DASHBOARD
    // =========================================================

    @FXML
    public void refreshDashboard() {

        loadStatistics();

        showDashboard();
    }

    // =========================================================
    // LOGOUT
    // =========================================================

    @FXML
    public void logout() {

        try {

            SessionManager
                    .getInstance()
                    .endSession();

            FXMLLoader loader =
                    new FXMLLoader(
                            getClass().getResource(
                                    "/view/LoginView.fxml"
                            )
                    );

            Parent root = loader.load();

            Stage stage =
                    (Stage) principalNameLabel
                            .getScene()
                            .getWindow();

            stage.setScene(
                    new Scene(root)
            );

            stage.setTitle(
                    "Digital Lesson Planner - Login"
            );

            stage.show();

        } catch (Exception e) {

            e.printStackTrace();

            showError(
                    "Logout Error",
                    "Unable to return to the login screen."
            );
        }
    }

    // =========================================================
    // ERROR MESSAGE
    // =========================================================

    private void showError(
            String title,
            String message) {

        Alert alert =
                new Alert(
                        Alert.AlertType.ERROR
                );

        alert.setTitle(title);

        alert.setHeaderText(null);

        alert.setContentText(message);

        alert.showAndWait();
    }
}
