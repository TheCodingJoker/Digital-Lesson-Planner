
package controller;

import model.User;
import service.AuthenticationService;
import service.AuthenticationService.LoginResult;
import util.SessionManager;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;

public class LoginController {
    
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private RadioButton teacherRoleRadio;
    @FXML private RadioButton principalRoleRadio;
    @FXML private RadioButton adminRoleRadio;
    @FXML private ToggleGroup roleGroup;
    @FXML private Button loginButton;
    @FXML private Label errorLabel;
    
    private AuthenticationService authService;
    
    @FXML
    public void initialize() {
        authService = new AuthenticationService();
        
        // Allow Enter key to trigger login
        passwordField.setOnAction(event -> handleLogin());
        
        // Activity monitoring for session timeout
        setupActivityMonitoring();
    }

    @FXML
    private void handleLogin() {
        // Clear previous errors
        errorLabel.setText("");
        
        String username = usernameField.getText().trim();
        String password = passwordField.getText();
        String selectedRole = getSelectedRole();

        // Validate
        if (username.isEmpty()) {
            errorLabel.setText("Please enter your username.");
            return;
        }
        if (password.isEmpty()) {
            errorLabel.setText("Please enter your password.");
            return;
        }
        if (selectedRole == null) {
            errorLabel.setText("Please select your role.");
            return;
        }

        // Disable button during login
        loginButton.setDisable(true);
        loginButton.setText("Signing in...");

        // Login in background thread
        new Thread(() -> {
            LoginResult result = authService.login(username, password);
            
            Platform.runLater(() -> {
                loginButton.setDisable(false);
                loginButton.setText("Sign In");
                
                if (result.isSuccess()) {
                    User user = result.getUser();
                    
                    // Verify role matches
                    if (!user.getRole().equals(selectedRole)) {
                        errorLabel.setText("Role mismatch. Please select the correct role.");
                        return;
                    }
                    
                    // Start session
                    SessionManager.getInstance().startSession(user, () -> {
                        // Auto-logout callback - return to login
                        try {
                            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                                getClass().getResource("/view/LoginView.fxml"));
                            javafx.scene.Parent loginView = loader.load();
                            loginButton.getScene().setRoot(loginView);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    });
                    
                    // Navigate based on role
                    navigateToDashboard(user.getRole());
                } else {
                    errorLabel.setText(result.getMessage());
                }
            });
        }).start();
    }

    private String getSelectedRole() {
        if (teacherRoleRadio.isSelected()) return "TEACHER";
        if (principalRoleRadio.isSelected()) return "PRINCIPAL_HOD";
        if (adminRoleRadio.isSelected()) return "ADMINISTRATOR";
        return null;
    }

    private void navigateToDashboard(String role) {
        // For now, show alert since other dashboards aren't built yet
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Login Successful");
            alert.setHeaderText("Welcome, " + 
                SessionManager.getInstance().getCurrentUser().getUsername());
            alert.setContentText("Logged in as: " + role + 
                "\n\nDashboard implementation coming soon.");
            alert.showAndWait();
        });
    }

    private void setupActivityMonitoring() {
        // Monitor mouse and keyboard activity
        usernameField.addEventFilter(KeyEvent.KEY_TYPED,
            e -> SessionManager.getInstance().resetInactivityTimer());
        passwordField.addEventFilter(KeyEvent.KEY_TYPED,
            e -> SessionManager.getInstance().resetInactivityTimer());
        teacherRoleRadio.addEventFilter(MouseEvent.MOUSE_CLICKED,
            e -> SessionManager.getInstance().resetInactivityTimer());
        principalRoleRadio.addEventFilter(MouseEvent.MOUSE_CLICKED,
            e -> SessionManager.getInstance().resetInactivityTimer());
        adminRoleRadio.addEventFilter(MouseEvent.MOUSE_CLICKED,
            e -> SessionManager.getInstance().resetInactivityTimer());
        loginButton.addEventFilter(MouseEvent.MOUSE_CLICKED,
            e -> SessionManager.getInstance().resetInactivityTimer());
    }
    
    
}
