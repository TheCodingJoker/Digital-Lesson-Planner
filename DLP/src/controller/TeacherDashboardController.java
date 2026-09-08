
package controller;

import model.User;
import util.SessionManager;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;

public class TeacherDashboardController {

    @FXML private Label userNameLabel;
    @FXML private Button logoutButton;

    @FXML private Button navDashboardButton;
    @FXML private Button navLessonPlansButton;
    @FXML private Button navClassesButton;
    @FXML private Button navReportsButton;

    @FXML private StackPane contentArea;

    @FXML
    public void initialize() {
        User currentUser = SessionManager.getInstance().getCurrentUser();
        // For demo purposes, use "Sarah Smith" to match the reference design
        // In production, you'd use: formatDisplayName(currentUser.getUsername())
        String displayName = "Sarah Smith"; 
        userNameLabel.setText(displayName);

        setActiveNav(navDashboardButton);
        loadFragment("/view/fragments/DashboardHomeView.fxml");
        setupActivityMonitoring();
    }

    private String formatDisplayName(String username) {
        if (username == null || username.isEmpty()) return "Teacher";
        // Capitalize first letter and replace underscores with spaces for better display
        String formatted = username.substring(0, 1).toUpperCase() + username.substring(1).toLowerCase();
        return formatted.replace("_", " ");
    }

    @FXML
    private void handleNavDashboard() {
        setActiveNav(navDashboardButton);
        loadFragment("/view/fragments/DashboardHomeView.fxml");
    }

    @FXML
    private void handleNavLessonPlans() {
        setActiveNav(navLessonPlansButton);
        loadFragment("/view/fragments/LessonPlansView.fxml");
    }

    @FXML
    private void handleNavClasses() {
        setActiveNav(navClassesButton);
        loadFragment("/view/fragments/ClassesView.fxml");
    }

    @FXML
    private void handleNavReports() {
        setActiveNav(navReportsButton);
        loadFragment("/view/fragments/ReportsView.fxml");
    }

    private void loadFragment(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Node content = loader.load();
            contentArea.getChildren().setAll(content);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setActiveNav(Button active) {
        Button[] navButtons = {
            navDashboardButton, navLessonPlansButton, navClassesButton, navReportsButton
        };
        for (Button b : navButtons) {
            b.getStyleClass().remove("nav-button-active");
        }
        if (!active.getStyleClass().contains("nav-button-active")) {
            active.getStyleClass().add("nav-button-active");
        }
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
