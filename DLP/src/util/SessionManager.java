
package util;

import model.User;
import javafx.application.Platform;
import java.util.Timer;
import java.util.TimerTask;

public class SessionManager {
    
    private static SessionManager instance;
    private User currentUser;
    private Timer inactivityTimer;
    private static final int TIMEOUT_MINUTES = 15;
    private Runnable logoutCallback;

    private SessionManager() {}

    public static SessionManager getInstance() {
        if (instance == null) {
            instance = new SessionManager();
        }
        return instance;
    }

    public void startSession(User user, Runnable logoutCallback) {
        this.currentUser = user;
        this.logoutCallback = logoutCallback;
        resetInactivityTimer();
    }

    public void resetInactivityTimer() {
        if (inactivityTimer != null) {
            inactivityTimer.cancel();
        }
        inactivityTimer = new Timer(true);
        inactivityTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                Platform.runLater(() -> {
                    endSession();
                    if (logoutCallback != null) {
                        logoutCallback.run();
                    }
                });
            }
        }, TIMEOUT_MINUTES * 60 * 1000);
    }

    public void endSession() {
        currentUser = null;
        if (inactivityTimer != null) {
            inactivityTimer.cancel();
        }
    }

    public User getCurrentUser() { return currentUser; }
    public boolean isLoggedIn() { return currentUser != null; }
    public String getCurrentUserRole() { 
        return currentUser != null ? currentUser.getRole() : null; 
    }
    
}
