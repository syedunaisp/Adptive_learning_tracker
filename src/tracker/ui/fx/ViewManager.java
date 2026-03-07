package tracker.ui.fx;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;

import java.io.IOException;

/**
 * Manages view navigation by swapping content in the root StackPane.
 *
 * Equivalent to the Swing CardLayout approach but using FXML-loaded views.
 * Each view is loaded fresh on navigation (no caching for simplicity in v1).
 */
public class ViewManager {

    private final StackPane rootPane;
    private final Scene scene;

    /** Shared state: current logged-in user ID and role. */
    private int currentUserId = -1;
    private String currentRole = null;
    private int currentStudentDbId = -1;
    private String currentUsername = null;

    public ViewManager(StackPane rootPane, Scene scene) {
        this.rootPane = rootPane;
        this.scene = scene;
    }

    // =========================================================================
    // Navigation
    // =========================================================================

    public void showLogin() {
        loadView("/tracker/ui/fx/view/LoginView.fxml");
    }

    public void showRegister() {
        loadView("/tracker/ui/fx/view/RegisterView.fxml");
    }

    public void showDashboard() {
        if (currentRole == null) {
            showLogin();
            return;
        }
        switch (currentRole) {
            case "STUDENT":
                loadView("/tracker/ui/fx/view/StudentDashboardView.fxml");
                break;
            case "TEACHER":
                loadView("/tracker/ui/fx/view/TeacherDashboardView.fxml");
                break;
            case "ADMIN":
                loadView("/tracker/ui/fx/view/AdminDashboardView.fxml");
                break;
            default:
                loadView("/tracker/ui/fx/view/TeacherDashboardView.fxml");
                break;
        }
    }

    // =========================================================================
    // Session State
    // =========================================================================

    public void setSession(int userId, String role, int studentDbId, String username) {
        this.currentUserId = userId;
        this.currentRole = role;
        this.currentStudentDbId = studentDbId;
        this.currentUsername = username;
    }

    public void clearSession() {
        this.currentUserId = -1;
        this.currentRole = null;
        this.currentStudentDbId = -1;
        this.currentUsername = null;
    }

    public int getCurrentUserId() {
        return currentUserId;
    }

    public String getCurrentRole() {
        return currentRole;
    }

    public int getCurrentStudentDbId() {
        return currentStudentDbId;
    }

    public String getCurrentUsername() {
        return currentUsername;
    }

    // =========================================================================
    // Internal
    // =========================================================================

    private void loadView(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent view = loader.load();

            // Inject ViewManager into controller if it implements ViewManagerAware
            Object controller = loader.getController();
            if (controller instanceof ViewManagerAware) {
                ((ViewManagerAware) controller).setViewManager(this);
            }

            rootPane.getChildren().clear();
            rootPane.getChildren().add(view);
        } catch (IOException e) {
            System.err.println("[ViewManager] Failed to load view: " + fxmlPath);
            e.printStackTrace();
        }
    }
}
