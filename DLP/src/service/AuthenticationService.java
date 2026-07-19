
package service;

import dao.UserDAO;
import model.User;
import util.PasswordHasher;

public class AuthenticationService {
    
    private final UserDAO userDAO;

    public AuthenticationService() {
        this.userDAO = new UserDAO();
    }

    public LoginResult login(String username, String password) {
        
        // Validate inputs
        if (username == null || username.trim().isEmpty()) {
            return new LoginResult(false, "Please enter your username.", null);
        }
        if (password == null || password.isEmpty()) {
            return new LoginResult(false, "Please enter your password.", null);
        }

        // Find user
        User user = userDAO.findByUsername(username.trim());
        if (user == null) {
            user = userDAO.findByEmail(username.trim());
        }

        if (user == null) {
            return new LoginResult(false, "Invalid username or password.", null);
        }

        if (!user.isActive()) {
            return new LoginResult(false, "Account deactivated. Contact administrator.", null);
        }

        // Verify password
        if (!PasswordHasher.verifyPassword(password, user.getPasswordHash())) {
            return new LoginResult(false, "Invalid username or password.", null);
        }

        return new LoginResult(true, "Login successful!", user);
    }

    public static class LoginResult {
        private final boolean success;
        private final String message;
        private final User user;

        public LoginResult(boolean success, String message, User user) {
            this.success = success;
            this.message = message;
            this.user = user;
        }

        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public User getUser() { return user; }
    }
    
}
