package tracker.ui.fx.controller;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import tracker.data.dao.UserDAO;
import tracker.model.UserRole;
import tracker.ui.fx.ViewManager;
import tracker.ui.fx.ViewManagerAware;

/**
 * Controller for the Registration view.
 * Creates new user accounts via UserDAO.
 */
public class RegisterController implements ViewManagerAware {

    @FXML
    private TextField usernameField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private PasswordField confirmPasswordField;
    @FXML
    private ComboBox<String> roleComboBox;
    @FXML
    private Label messageLabel;

    private ViewManager viewManager;
    private final UserDAO userDAO = new UserDAO();

    @Override
    public void setViewManager(ViewManager viewManager) {
        this.viewManager = viewManager;
    }

    @FXML
    private void initialize() {
        // Populate role dropdown
        roleComboBox.setItems(FXCollections.observableArrayList("STUDENT", "TEACHER"));
        roleComboBox.getSelectionModel().selectFirst();
    }

    @FXML
    private void handleRegister() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();
        String confirm = confirmPasswordField.getText();
        String roleStr = roleComboBox.getValue();

        // Validation
        if (username.isEmpty()) {
            showError("Username is required.");
            return;
        }
        if (username.length() < 3) {
            showError("Username must be at least 3 characters.");
            return;
        }
        if (password.isEmpty()) {
            showError("Password is required.");
            return;
        }
        if (password.length() < 6) {
            showError("Password must be at least 6 characters.");
            return;
        }
        if (!password.equals(confirm)) {
            showError("Passwords do not match.");
            return;
        }
        if (roleStr == null) {
            showError("Please select a role.");
            return;
        }

        // Check username uniqueness
        if (userDAO.findByUsername(username) != null) {
            showError("Username '" + username + "' is already taken.");
            return;
        }

        // Create user
        UserRole role = UserRole.valueOf(roleStr);
        boolean success = userDAO.createUser(username, password, role, null);

        if (success) {
            showSuccess("Account created! You can now sign in.");
            // Clear form
            usernameField.clear();
            passwordField.clear();
            confirmPasswordField.clear();
        } else {
            showError("Registration failed. Please try again.");
        }
    }

    @FXML
    private void handleGoToLogin() {
        viewManager.showLogin();
    }

    private void showError(String message) {
        messageLabel.getStyleClass().removeAll("success-label");
        messageLabel.getStyleClass().add("error-label");
        messageLabel.setText(message);
    }

    private void showSuccess(String message) {
        messageLabel.getStyleClass().removeAll("error-label");
        messageLabel.getStyleClass().add("success-label");
        messageLabel.setText(message);
    }
}
