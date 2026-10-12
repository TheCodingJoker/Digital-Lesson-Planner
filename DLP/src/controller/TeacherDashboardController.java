
package controller;

import model.User;
import util.SessionManager;
import util.FeedbackDialog;
import dao.NotificationDAO;
import model.Notification;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import java.util.List;

public class TeacherDashboardController {

    @FXML private Label userNameLabel;
    @FXML private Button feedbackButton;
    @FXML private Button logoutButton;
    @FXML private Button notificationButton;
    @FXML private Label notificationBadge;

    @FXML private Button navDashboardButton;
    @FXML private Button navLessonPlansButton;
    @FXML private Button navCalendarButton;

    @FXML private StackPane contentArea;

    private NotificationDAO notificationDAO;

    @FXML
    public void initialize() {
        notificationDAO = new NotificationDAO();
        notificationButton.setTooltip(new Tooltip("Notifications"));

        User currentUser = SessionManager.getInstance().getCurrentUser();
        if (currentUser == null) {
            userNameLabel.setText("Teacher");
        } else {
            String displayName = formatDisplayName(currentUser.getUsername());
            userNameLabel.setText(displayName);
        }

        setActiveNav(navDashboardButton);
        loadFragment("/view/fragments/DashboardHomeView.fxml");
        setupActivityMonitoring();
        updateNotificationBadge();
    }

    private String formatDisplayName(String username) {
        if (username == null || username.isEmpty()) return "Teacher";
        // Capitalize first letter and replace underscores with spaces for better display
        String formatted = username.length() > 1
            ? username.substring(0, 1).toUpperCase() + username.substring(1).toLowerCase()
            : username.toUpperCase();
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
    private void handleNavCalendar() {
        setActiveNav(navCalendarButton);
        loadFragment("/view/fragments/LessonCalendarView.fxml");
    }

    private void loadFragment(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Node content = loader.load();
            contentArea.getChildren().setAll(content);
        } catch (Exception e) {
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Unable to Open Teacher Page");
            alert.setHeaderText(null);
            alert.setContentText("The requested page could not be loaded. " + e.getMessage());
            alert.showAndWait();
        }
    }

    private void setActiveNav(Button active) {
        Button[] navButtons = {
            navDashboardButton, navLessonPlansButton, navCalendarButton
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

    @FXML
    private void handleFeedback() {
        User currentUser = SessionManager.getInstance().getCurrentUser();
        FeedbackDialog.showFeedbackDialog(currentUser);
    }

    @FXML
    private void handleNotifications() {
        User currentUser = SessionManager.getInstance().getCurrentUser();
        if (currentUser == null) return;

        updateNotificationBadge();
        List<Notification> notifications = notificationDAO.getUnreadNotificationsByUser(currentUser.getUserId());

        if (notifications.isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Notifications");
            alert.setHeaderText("No new notifications");
            alert.setContentText("You have no unread notifications.");
            alert.showAndWait();
        } else {
            // Show notifications dialog
            showNotificationsDialog(notifications, currentUser.getUserId());
        }
    }

    private void showNotificationsDialog(List<Notification> notifications, String userId) {
        javafx.scene.control.Dialog<Void> dialog = new javafx.scene.control.Dialog<>();
        dialog.setTitle("Notifications");
        dialog.setHeaderText("You have " + notifications.size() + " unread notification(s)");

        javafx.scene.control.ListView<Notification> listView = new javafx.scene.control.ListView<>();
        listView.getItems().addAll(notifications);

        listView.setCellFactory(param -> new javafx.scene.control.ListCell<Notification>() {
            @Override
            protected void updateItem(Notification notification, boolean empty) {
                super.updateItem(notification, empty);
                if (empty || notification == null) {
                    setText(null);
                } else {
                    setText(notification.getTitle() + "\n" + notification.getMessage()
                        + "\n" + notification.getCreatedAt());
                    setStyle("-fx-text-fill: black; -fx-font-size: 12px;");
                }
            }
        });

        javafx.scene.layout.VBox vbox = new javafx.scene.layout.VBox(listView);
        vbox.setPrefSize(500, 300);

        dialog.getDialogPane().setContent(vbox);

        // Create custom button types
        ButtonType markAllReadButtonType = new ButtonType("Mark All as Read", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(
            markAllReadButtonType,
            ButtonType.CLOSE
        );

        dialog.setResultConverter(buttonType -> {
            if (buttonType == markAllReadButtonType) {
                if (notificationDAO.markAllAsRead(userId)) {
                    updateNotificationBadge();
                } else {
                    showError("Notifications could not be marked as read.");
                }
            }
            return null;
        });

        dialog.showAndWait();
    }

    private void updateNotificationBadge() {
        User currentUser = SessionManager.getInstance().getCurrentUser();
        if (currentUser == null || notificationBadge == null) return;

        List<Notification> unreadNotifications = notificationDAO.getUnreadNotificationsByUser(currentUser.getUserId());
        int count = unreadNotifications.size();

        if (count > 0) {
            notificationBadge.setText(String.valueOf(count));
            notificationBadge.setVisible(true);
            notificationBadge.setManaged(true);
        } else {
            notificationBadge.setVisible(false);
            notificationBadge.setManaged(false);
        }
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Action Failed");
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
