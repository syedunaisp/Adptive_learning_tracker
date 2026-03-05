package tracker.ui.fx;

/**
 * Interface for controllers that need access to the ViewManager.
 * Implement this to receive the ViewManager instance after FXML loading.
 */
public interface ViewManagerAware {
    void setViewManager(ViewManager viewManager);
}
