package tracker.ui.fx;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

/**
 * JavaFX application entry point for ALIP v4.0.
 *
 * Replaces the Swing-based MainFrame as the primary UI container.
 * Delegates all view management to {@link ViewManager}.
 */
public class ALIPApplication extends Application {

    private static final String APP_TITLE = "ALIP — Adaptive Learning Intelligence Platform";
    private static final double MIN_WIDTH = 1024;
    private static final double MIN_HEIGHT = 700;

    private ViewManager viewManager;

    @Override
    public void start(Stage primaryStage) {
        // Initialize database schema
        tracker.data.DatabaseSchema.initializeSchema();

        // Root container
        StackPane rootPane = new StackPane();
        Scene scene = new Scene(rootPane, MIN_WIDTH, MIN_HEIGHT);

        // Load CSS theme
        String css = getClass().getResource("/tracker/ui/fx/css/theme.css").toExternalForm();
        scene.getStylesheets().add(css);

        // Initialize view manager
        viewManager = new ViewManager(rootPane, scene);

        // Configure stage
        primaryStage.setTitle(APP_TITLE);
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(MIN_WIDTH);
        primaryStage.setMinHeight(MIN_HEIGHT);
        primaryStage.setMaximized(true);

        // Show login view
        viewManager.showLogin();

        primaryStage.show();
    }

    /**
     * Fallback main method for environments without JavaFX launcher support.
     */
    public static void main(String[] args) {
        launch(args);
    }
}
