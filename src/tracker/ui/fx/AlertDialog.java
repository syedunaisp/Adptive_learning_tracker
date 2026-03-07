package tracker.ui.fx;

import javafx.animation.FadeTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;
import javafx.util.Duration;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Modern, dark-themed dialog utility for ALIP v4.0.
 * Replaces generic OS-level Alert, TextInputDialog, and ChoiceDialog
 * with custom styled popups that match the application's dark theme.
 *
 * All methods are blocking (showAndWait equivalent).
 */
public final class AlertDialog {

    // ── Theme palette ─────────────────────────────────────────────
    private static final String BG_PRIMARY = "#0f0f1a";
    private static final String BG_CARD = "#1a1a2e";
    private static final String BORDER = "#2a2a4a";
    private static final String TEXT_PRIMARY = "#e0e0e8";
    private static final String TEXT_MUTED = "#8888aa";
    private static final String ACCENT = "#6c63ff";
    private static final String ACCENT_HOVER = "#7b73ff";
    private static final String SUCCESS = "#2ed573";
    private static final String WARNING = "#ffa502";
    private static final String DANGER = "#ff4757";

    private AlertDialog() {
    }

    // ══════════════════════════════════════════════════════════════
    // PUBLIC API
    // ══════════════════════════════════════════════════════════════

    /** Show a dark-themed ✅ info dialog. */
    public static void showInfo(String title, String message) {
        showMessage(title, message, "✅", ACCENT, null);
    }

    /** Show a dark-themed ❌ error dialog. */
    public static void showError(String title, String message) {
        showMessage(title, message, "❌", DANGER, null);
    }

    /** Show a dark-themed ⚠️ warning dialog. */
    public static void showWarning(String title, String message) {
        showMessage(title, message, "⚠️", WARNING, null);
    }

    /**
     * Show a dark-themed confirmation dialog (Confirm / Cancel).
     *
     * @return true if the user clicked "Confirm"
     */
    public static boolean showConfirm(String title, String message) {
        AtomicBoolean result = new AtomicBoolean(false);

        Stage stage = buildStage(480, -1);
        VBox card = buildCard();

        // Icon + header row
        HBox header = buildHeader("⚠️", WARNING, title);

        // Message
        Label msg = buildBodyLabel(message);

        // Buttons
        Button confirmBtn = buildPrimaryButton("Confirm", DANGER);
        Button cancelBtn = buildSecondaryButton("Cancel");

        confirmBtn.setOnAction(e -> {
            result.set(true);
            stage.close();
        });
        cancelBtn.setOnAction(e -> stage.close());

        HBox btnRow = buildButtonRow(cancelBtn, confirmBtn);
        card.getChildren().addAll(header, msg, btnRow);

        showStage(stage, card, "");
        return result.get();
    }

    /**
     * Show a dark-themed text input dialog.
     *
     * @param prompt       Label shown above the input
     * @param defaultValue Pre-filled value (may be null/empty)
     * @return Optional containing the entered text, or empty if cancelled
     */
    public static Optional<String> showInput(String title, String prompt, String defaultValue) {
        AtomicReference<String> result = new AtomicReference<>(null);

        Stage stage = buildStage(460, -1);
        VBox card = buildCard();

        HBox header = buildHeader("✏️", ACCENT, title);

        Label promptLabel = buildBodyLabel(prompt);

        TextField field = new TextField(defaultValue != null ? defaultValue : "");
        field.setStyle(
                "-fx-background-color: #16213e;" +
                        "-fx-text-fill: " + TEXT_PRIMARY + ";" +
                        "-fx-prompt-text-fill: " + TEXT_MUTED + ";" +
                        "-fx-border-color: " + BORDER + ";" +
                        "-fx-border-radius: 8;" +
                        "-fx-background-radius: 8;" +
                        "-fx-padding: 10 14;" +
                        "-fx-font-size: 14px;" +
                        "-fx-font-family: 'Segoe UI';");
        field.setMaxWidth(Double.MAX_VALUE);

        // Enter key submits
        field.setOnAction(e -> {
            result.set(field.getText());
            stage.close();
        });

        Button okBtn = buildPrimaryButton("OK", ACCENT);
        Button cancelBtn = buildSecondaryButton("Cancel");

        okBtn.setOnAction(e -> {
            result.set(field.getText());
            stage.close();
        });
        cancelBtn.setOnAction(e -> stage.close());

        HBox btnRow = buildButtonRow(cancelBtn, okBtn);
        card.getChildren().addAll(header, promptLabel, field, btnRow);

        showStage(stage, card, "");
        javafx.application.Platform.runLater(field::requestFocus);
        return Optional.ofNullable(result.get());
    }

    /**
     * Show a dark-themed dropdown choice dialog.
     *
     * @param choices List of choices to display
     * @return Optional of the selected item, or empty if cancelled
     */
    public static Optional<String> showChoice(String title, String prompt, List<String> choices) {
        AtomicReference<String> result = new AtomicReference<>(null);

        Stage stage = buildStage(480, -1);
        VBox card = buildCard();

        HBox header = buildHeader("🔽", ACCENT, title);
        Label promptLabel = buildBodyLabel(prompt);

        ComboBox<String> picker = new ComboBox<>();
        picker.getItems().addAll(choices);
        if (!choices.isEmpty())
            picker.getSelectionModel().selectFirst();
        picker.setMaxWidth(Double.MAX_VALUE);
        picker.setStyle(
                "-fx-background-color: #16213e;" +
                        "-fx-border-color: " + BORDER + ";" +
                        "-fx-border-radius: 8;" +
                        "-fx-background-radius: 8;" +
                        "-fx-font-size: 13px;" +
                        "-fx-font-family: 'Segoe UI';");

        Button okBtn = buildPrimaryButton("Select", ACCENT);
        Button cancelBtn = buildSecondaryButton("Cancel");

        okBtn.setOnAction(e -> {
            result.set(picker.getValue());
            stage.close();
        });
        cancelBtn.setOnAction(e -> stage.close());

        HBox btnRow = buildButtonRow(cancelBtn, okBtn);
        card.getChildren().addAll(header, promptLabel, picker, btnRow);

        showStage(stage, card, "");
        return Optional.ofNullable(result.get());
    }

    /**
     * Style an existing {@link DialogPane} (from a complex {@link Dialog}) to match
     * the dark theme.
     * Use this for multi-field form dialogs that can't be replaced with simple
     * static methods.
     *
     * @param dialogPane the pane to style
     * @param ownerScene the scene of the parent window (for CSS inheritance)
     */
    public static void styleDialogPane(DialogPane dialogPane, Scene ownerScene) {
        // Dark overall background
        dialogPane.setStyle(
                "-fx-background-color: " + BG_PRIMARY + ";" +
                        "-fx-border-color: " + BORDER + ";" +
                        "-fx-border-width: 1;" +
                        "-fx-border-radius: 12;" +
                        "-fx-background-radius: 12;");

        // Style header
        if (dialogPane.getHeader() != null) {
            dialogPane.getHeader().setStyle("-fx-background-color: " + BG_CARD + ";");
        }
        // Header text label
        dialogPane.lookupAll(".header-panel .label").forEach(n -> {
            if (n instanceof Label l) {
                l.setStyle("-fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: bold;");
            }
        });

        // Content text
        dialogPane.lookupAll(".content.label").forEach(n -> {
            if (n instanceof Label l) {
                l.setStyle("-fx-text-fill: " + TEXT_PRIMARY + "; -fx-font-size: 14px;");
            }
        });
        dialogPane.lookupAll(".label").forEach(n -> {
            if (n instanceof Label l && (l.getStyle() == null || l.getStyle().isEmpty())) {
                l.setStyle("-fx-text-fill: " + TEXT_PRIMARY + ";");
            }
        });

        // Style input fields inside the pane
        dialogPane.lookupAll(".text-field, .password-field").forEach(n -> {
            n.setStyle(
                    "-fx-background-color: #16213e;" +
                            "-fx-text-fill: " + TEXT_PRIMARY + ";" +
                            "-fx-prompt-text-fill: " + TEXT_MUTED + ";" +
                            "-fx-border-color: " + BORDER + ";" +
                            "-fx-border-radius: 8;" +
                            "-fx-background-radius: 8;" +
                            "-fx-padding: 10 14;" +
                            "-fx-font-size: 13px;");
        });

        // Style combo boxes
        dialogPane.lookupAll(".combo-box").forEach(n -> {
            n.setStyle(
                    "-fx-background-color: #16213e;" +
                            "-fx-border-color: " + BORDER + ";" +
                            "-fx-border-radius: 8;" +
                            "-fx-background-radius: 8;");
        });

        // Style buttons in button bar
        dialogPane.lookupAll(".button-bar .button").forEach(n -> {
            if (n instanceof Button btn) {
                if (btn.getText().equalsIgnoreCase("OK") ||
                        btn.getText().equalsIgnoreCase("Apply") ||
                        btn.getText().equalsIgnoreCase("Save") ||
                        btn.getText().equalsIgnoreCase("Save Changes") ||
                        btn.getStyleClass().contains("default")) {
                    btn.setStyle(
                            "-fx-background-color: " + ACCENT + ";" +
                                    "-fx-text-fill: white;" +
                                    "-fx-background-radius: 8;" +
                                    "-fx-border-color: transparent;" +
                                    "-fx-font-weight: bold;" +
                                    "-fx-padding: 10 24;" +
                                    "-fx-cursor: hand;");
                } else {
                    btn.setStyle(
                            "-fx-background-color: transparent;" +
                                    "-fx-text-fill: " + TEXT_MUTED + ";" +
                                    "-fx-background-radius: 8;" +
                                    "-fx-border-color: " + BORDER + ";" +
                                    "-fx-border-radius: 8;" +
                                    "-fx-padding: 10 24;" +
                                    "-fx-cursor: hand;");
                }
            }
        });

        // Copy CSS from owner scene if available
        if (ownerScene != null && !ownerScene.getStylesheets().isEmpty()) {
            Stage dlgStage = (Stage) dialogPane.getScene().getWindow();
            dlgStage.getScene().getStylesheets().addAll(ownerScene.getStylesheets());
        }
    }

    // ══════════════════════════════════════════════════════════════
    // INTERNAL HELPERS
    // ══════════════════════════════════════════════════════════════

    private static void showMessage(String title, String body, String icon, String iconColor, Window owner) {
        Stage stage = buildStage(440, -1);
        VBox card = buildCard();

        HBox header = buildHeader(icon, iconColor, title);
        Label msg = buildBodyLabel(body);

        Button okBtn = buildPrimaryButton("OK", iconColor);
        okBtn.setOnAction(e -> stage.close());

        HBox btnRow = buildButtonRow(okBtn);
        card.getChildren().addAll(header, msg, btnRow);

        showStage(stage, card, "");
    }

    private static Stage buildStage(double width, double height) {
        Stage stage = new Stage(StageStyle.TRANSPARENT);
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setResizable(false);
        stage.setWidth(width);
        if (height > 0)
            stage.setHeight(height);
        return stage;
    }

    private static void showStage(Stage stage, VBox card, String extraCss) {
        // Scrim layer (semi-transparent background)
        StackPane root = new StackPane(card);
        root.setStyle("-fx-background-color: rgba(0, 0, 0, 0.65);");
        root.setAlignment(Pos.CENTER);

        Scene scene = new Scene(root);
        scene.setFill(Color.TRANSPARENT);
        stage.setScene(scene);

        // Load theme CSS into dialog scene
        String cssPath = AlertDialog.class.getResource("/tracker/ui/fx/css/theme.css") != null
                ? AlertDialog.class.getResource("/tracker/ui/fx/css/theme.css").toExternalForm()
                : null;
        if (cssPath != null)
            scene.getStylesheets().add(cssPath);

        // Fade in
        card.setOpacity(0);
        stage.show();
        FadeTransition ft = new FadeTransition(Duration.millis(180), card);
        ft.setFromValue(0);
        ft.setToValue(1);
        ft.play();

        stage.centerOnScreen();
        stage.showAndWait();
    }

    private static VBox buildCard() {
        VBox card = new VBox(16);
        card.setStyle(
                "-fx-background-color: " + BG_CARD + ";" +
                        "-fx-background-radius: 14;" +
                        "-fx-border-color: " + BORDER + ";" +
                        "-fx-border-radius: 14;" +
                        "-fx-border-width: 1;" +
                        "-fx-padding: 28 32 24 32;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.6), 32, 0, 0, 8);");
        card.setMaxWidth(480);
        card.setMinWidth(380);
        return card;
    }

    private static HBox buildHeader(String icon, String color, String title) {
        Label iconLabel = new Label(icon);
        iconLabel.setStyle(
                "-fx-font-size: 24px;" +
                        "-fx-text-fill: " + color + ";");

        Label titleLabel = new Label(title);
        titleLabel.setStyle(
                "-fx-font-size: 17px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-text-fill: white;" +
                        "-fx-font-family: 'Segoe UI';" +
                        "-fx-wrap-text: true;");
        titleLabel.setMaxWidth(370);

        HBox header = new HBox(12, iconLabel, titleLabel);
        header.setAlignment(Pos.CENTER_LEFT);
        return header;
    }

    private static Label buildBodyLabel(String text) {
        Label label = new Label(text);
        label.setStyle(
                "-fx-text-fill: " + TEXT_PRIMARY + ";" +
                        "-fx-font-size: 13px;" +
                        "-fx-font-family: 'Segoe UI';" +
                        "-fx-wrap-text: true;" +
                        "-fx-line-spacing: 2;");
        label.setWrapText(true);
        label.setMaxWidth(420);
        return label;
    }

    private static Button buildPrimaryButton(String text, String color) {
        Button btn = new Button(text);
        btn.setMinWidth(100);
        btn.setStyle(
                "-fx-background-color: " + color + ";" +
                        "-fx-text-fill: white;" +
                        "-fx-background-radius: 8;" +
                        "-fx-border-color: transparent;" +
                        "-fx-font-size: 13px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-font-family: 'Segoe UI';" +
                        "-fx-padding: 10 24;" +
                        "-fx-cursor: hand;");
        // Hover effect via mouse events
        btn.setOnMouseEntered(e -> btn.setOpacity(0.85));
        btn.setOnMouseExited(e -> btn.setOpacity(1.0));
        return btn;
    }

    private static Button buildSecondaryButton(String text) {
        Button btn = new Button(text);
        btn.setMinWidth(100);
        btn.setStyle(
                "-fx-background-color: transparent;" +
                        "-fx-text-fill: " + TEXT_MUTED + ";" +
                        "-fx-background-radius: 8;" +
                        "-fx-border-color: " + BORDER + ";" +
                        "-fx-border-radius: 8;" +
                        "-fx-font-size: 13px;" +
                        "-fx-font-family: 'Segoe UI';" +
                        "-fx-padding: 10 24;" +
                        "-fx-cursor: hand;");
        btn.setOnMouseEntered(e -> btn.setOpacity(0.8));
        btn.setOnMouseExited(e -> btn.setOpacity(1.0));
        return btn;
    }

    private static HBox buildButtonRow(Button... buttons) {
        HBox row = new HBox(12);
        row.setAlignment(Pos.CENTER_RIGHT);
        row.getChildren().addAll(buttons);
        VBox.setMargin(row, new Insets(8, 0, 0, 0));
        return row;
    }
}
