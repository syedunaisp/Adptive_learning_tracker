package tracker.ui.fx;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.*;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import tracker.data.dao.UserDAO;
import tracker.model.User;
import tracker.model.UserRole;
import tracker.security.SessionManager;

/**
 * JavaFX Login screen controller.
 * Authentication logic unchanged — still delegates to UserDAO + SessionManager.
 */
public class LoginController {

    private final Stage stage;
    private final UserDAO userDAO = new UserDAO();

    private TextField txtUsername;
    private PasswordField txtPassword;
    private TextField txtPasswordVisible;
    private Label lblError;
    private boolean passwordShown = false;
    private StackPane passwordStack;

    public LoginController(Stage stage) {
        this.stage = stage;
    }

    public Scene buildScene() {
        // Root: dark gradient background
        StackPane root = new StackPane();
        root.setStyle("-fx-background-color: linear-gradient(to bottom right, #0F172A, #1E3A5F);");
        root.setPrefSize(520, 620);

        // Card
        VBox card = new VBox(0);
        card.setStyle(
                "-fx-background-color: white;" +
                        "-fx-background-radius: 20;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.25), 24, 0, 0, 6);" +
                        "-fx-padding: 44 44 36 44;");
        card.setMaxWidth(420);
        card.setMaxHeight(540);
        card.setAlignment(Pos.TOP_LEFT);

        // Brand icon ◆
        Label iconLabel = new Label("◆");
        iconLabel.setStyle(
                "-fx-font-size: 38;" +
                        "-fx-font-family: 'Segoe UI';" +
                        "-fx-text-fill: " + FxStyles.C_PRIMARY + ";" +
                        "-fx-font-weight: bold;");
        iconLabel.setAlignment(Pos.CENTER);
        iconLabel.setMaxWidth(Double.MAX_VALUE);

        VBox.setMargin(iconLabel, new Insets(0, 0, 6, 0));

        // Title
        Label title = new Label("ALIP");
        title.setStyle(
                "-fx-font-size: 28;" +
                        "-fx-font-weight: bold;" +
                        "-fx-font-family: 'Segoe UI';" +
                        "-fx-text-fill: #1E293B;");
        title.setMaxWidth(Double.MAX_VALUE);
        title.setAlignment(Pos.CENTER);

        // Subtitle
        Label subtitle = new Label("Academic Risk Intelligence Platform");
        subtitle.setStyle("-fx-font-size: 13; -fx-font-family: 'Segoe UI'; -fx-text-fill: #64748B;");
        subtitle.setMaxWidth(Double.MAX_VALUE);
        subtitle.setAlignment(Pos.CENTER);

        VBox.setMargin(subtitle, new Insets(0, 0, 28, 0));

        // Username
        Label lblUser = new Label("Username");
        lblUser.setStyle(
                "-fx-font-size: 12; -fx-font-family: 'Segoe UI'; -fx-font-weight: bold; -fx-text-fill: #374151;");
        VBox.setMargin(lblUser, new Insets(0, 0, 6, 0));

        txtUsername = new TextField();
        txtUsername.setPromptText("Enter username");
        txtUsername.setStyle(FxStyles.textField());
        txtUsername.setPrefHeight(44);
        VBox.setMargin(txtUsername, new Insets(0, 0, 16, 0));

        // Password label
        Label lblPass = new Label("Password");
        lblPass.setStyle(
                "-fx-font-size: 12; -fx-font-family: 'Segoe UI'; -fx-font-weight: bold; -fx-text-fill: #374151;");
        VBox.setMargin(lblPass, new Insets(0, 0, 6, 0));

        // Password field with show/hide
        txtPassword = new PasswordField();
        txtPassword.setPromptText("Enter password");
        txtPassword.setStyle(FxStyles.textField());
        txtPassword.setPrefHeight(44);

        txtPasswordVisible = new TextField();
        txtPasswordVisible.setPromptText("Enter password");
        txtPasswordVisible.setStyle(FxStyles.textField());
        txtPasswordVisible.setPrefHeight(44);
        txtPasswordVisible.setVisible(false);
        txtPasswordVisible.setManaged(false);

        Button btnToggle = new Button("○");
        btnToggle.setStyle(
                "-fx-background-color: transparent;" +
                        "-fx-text-fill: #94A3B8;" +
                        "-fx-cursor: hand;" +
                        "-fx-font-size: 14;");
        btnToggle.setOnAction(e -> togglePassword(btnToggle));

        passwordStack = new StackPane();
        HBox passwordRow = new HBox(0);
        passwordRow.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(txtPassword, Priority.ALWAYS);
        HBox.setHgrow(txtPasswordVisible, Priority.ALWAYS);

        StackPane fieldStack = new StackPane(txtPassword, txtPasswordVisible);
        HBox.setHgrow(fieldStack, Priority.ALWAYS);
        passwordRow.getChildren().addAll(fieldStack, btnToggle);
        VBox.setMargin(passwordRow, new Insets(0, 0, 8, 0));

        // Error label
        lblError = new Label(" ");
        lblError.setStyle("-fx-font-size: 12; -fx-font-family: 'Segoe UI'; -fx-text-fill: #EF4444;");
        VBox.setMargin(lblError, new Insets(0, 0, 10, 0));

        // Login button
        Button btnLogin = new Button("Sign In");
        btnLogin.setStyle(FxStyles.primaryButton());
        btnLogin.setMaxWidth(Double.MAX_VALUE);
        btnLogin.setPrefHeight(46);
        btnLogin.setOnAction(e -> handleLogin());
        VBox.setMargin(btnLogin, new Insets(0, 0, 18, 0));

        // Separator
        Separator sep = new Separator();
        sep.setStyle("-fx-border-color: #E5E7EB;");
        VBox.setMargin(sep, new Insets(0, 0, 12, 0));

        // Hint
        Label hintLabel = new Label("Default accounts: admin/admin123  •  teacher/teacher123");
        hintLabel.setStyle("-fx-font-size: 10; -fx-font-family: 'Segoe UI'; -fx-text-fill: #94A3B8;");
        hintLabel.setMaxWidth(Double.MAX_VALUE);
        hintLabel.setAlignment(Pos.CENTER);

        // Version
        Label versionLabel = new Label("v3.0 — Database-Driven Edition");
        versionLabel.setStyle(
                "-fx-font-size: 11; -fx-font-style: italic; -fx-font-family: 'Segoe UI'; -fx-text-fill: #A0AEC0;");
        versionLabel.setMaxWidth(Double.MAX_VALUE);
        versionLabel.setAlignment(Pos.CENTER);
        VBox.setMargin(versionLabel, new Insets(4, 0, 0, 0));

        // Enter key support
        txtPassword.setOnAction(e -> handleLogin());
        txtPasswordVisible.setOnAction(e -> handleLogin());
        txtUsername.setOnAction(e -> txtPassword.requestFocus());

        card.getChildren().addAll(
                iconLabel, title, subtitle,
                lblUser, txtUsername,
                lblPass, passwordRow,
                lblError, btnLogin,
                sep, hintLabel, versionLabel);

        StackPane.setAlignment(card, Pos.CENTER);
        root.getChildren().add(card);

        return new Scene(root, 520, 620);
    }

    private void togglePassword(Button btn) {
        passwordShown = !passwordShown;
        if (passwordShown) {
            txtPasswordVisible.setText(txtPassword.getText());
            txtPassword.setVisible(false);
            txtPassword.setManaged(false);
            txtPasswordVisible.setVisible(true);
            txtPasswordVisible.setManaged(true);
            btn.setText("●");
        } else {
            txtPassword.setText(txtPasswordVisible.getText());
            txtPasswordVisible.setVisible(false);
            txtPasswordVisible.setManaged(false);
            txtPassword.setVisible(true);
            txtPassword.setManaged(true);
            btn.setText("○");
        }
    }

    private void handleLogin() {
        String username = txtUsername.getText().trim();
        String password = passwordShown
                ? txtPasswordVisible.getText().trim()
                : txtPassword.getText().trim();

        if (username.isEmpty()) {
            showError("Username is required.");
            return;
        }
        if (password.isEmpty()) {
            showError("Password is required.");
            return;
        }

        // Authenticate against database (same as Swing version)
        User user = userDAO.authenticate(username, password);
        if (user == null) {
            showError("Invalid username or password.");
            if (passwordShown)
                txtPasswordVisible.clear();
            else
                txtPassword.clear();
            return;
        }

        // Resolve linked student_id if student account
        String linkedStudentId = null;
        if (user.getLinkedStudentDbId() != null) {
            linkedStudentId = userDAO.resolveLinkedStudentId(user.getLinkedStudentDbId());
        }

        SessionManager.login(user, linkedStudentId);
        lblError.setText(" ");

        // Launch main window
        MainController mainController = new MainController(stage, user.getRole(), user.getUsername());
        stage.setScene(mainController.buildScene());
        stage.setResizable(true);
        stage.setWidth(1280);
        stage.setHeight(820);
        stage.setMinWidth(1000);
        stage.setMinHeight(700);
        stage.centerOnScreen();
    }

    private void showError(String msg) {
        lblError.setText(msg);
    }
}
