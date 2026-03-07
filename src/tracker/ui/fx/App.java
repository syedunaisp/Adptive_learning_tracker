package tracker.ui.fx;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * JavaFX Application entry point for ALIP.
 * Database is initialized before this class is launched (in Main.java).
 */
public class App extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        LoginController loginController = new LoginController(primaryStage);
        Scene scene = loginController.buildScene();

        primaryStage.setTitle("ALIP — Adaptive Learning Intelligence Platform");
        primaryStage.setScene(scene);
        primaryStage.setResizable(false);
        primaryStage.show();
    }

    public static void launch(String[] args) {
        Application.launch(App.class, args);
    }
}
