package tracker.ui.fx.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import tracker.data.dao.UserDAO;
import tracker.model.User;
import tracker.security.SessionManager;
import tracker.ui.fx.ViewManager;
import tracker.ui.fx.ViewManagerAware;

/**
 * Controller for the Login view.
 * Handles authentication via UserDAO and SessionManager.
 */
public class LoginController implements ViewManagerAware {

    @FXML
    private TextField usernameField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private Label errorLabel;

    private ViewManager viewManager;
    private final UserDAO userDAO = new UserDAO();

    @Override
    public void setViewManager(ViewManager viewManager) {
        this.viewManager = viewManager;
    }

    @FXML
    private void handleLogin() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();

        // Validation
        if (username.isEmpty()) {
            showError("Please enter your username.");
            return;
        }
        if (password.isEmpty()) {
            showError("Please enter your password.");
            return;
        }

        // Authenticate
        User user = userDAO.authenticate(username, password);
        if (user == null) {
            showError("Invalid username or password.");
            return;
        }

        if (!user.isEnabled()) {
            showError("This account has been disabled. Contact your administrator.");
            return;
        }

        // Start session with linked student business ID
        String studentId = null;
        int studentDbId = -1;
        if (user.getLinkedStudentDbId() != null) {
            studentDbId = user.getLinkedStudentDbId();
            studentId = userDAO.resolveLinkedStudentId(studentDbId);
        }
        SessionManager.login(user, studentId);

        // Set session in ViewManager
        viewManager.setSession(user.getId(), user.getRole().name(), studentDbId, user.getUsername());

        // Navigate to dashboard
        viewManager.showDashboard();
    }

    @FXML
    private void handleGoToRegister() {
        viewManager.showRegister();
    }

    private void showError(String message) {
        errorLabel.getStyleClass().removeAll("success-label");
        errorLabel.getStyleClass().add("error-label");
        errorLabel.setText(message);
    }
}
