package tracker.ui.fx;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import tracker.data.DataMigration;
import tracker.data.dao.UserDAO;
import tracker.model.User;
import tracker.security.PasswordHasher;
import tracker.security.SessionManager;

/**
 * Settings page controller.
 * Change password, data migration, session info.
 * Unchanged logic from Swing version.
 */
public class SettingsController {

    private final MainController main;
    private final UserDAO userDAO = new UserDAO();

    public SettingsController(MainController main) {
        this.main = main;
    }

    public Region buildPage() {
        VBox page = new VBox(16);
        page.setStyle(FxStyles.PAGE_BG);
        page.setPadding(new Insets(20, 24, 20, 24));

        Label title = new Label("Settings");
        title.setStyle(FxStyles.title());

        ScrollPane scroll = new ScrollPane();
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background: #F1F5F9; -fx-background-color: #F1F5F9;");

        VBox content = new VBox(16);
        content.setPadding(new Insets(4));

        content.getChildren().add(buildPasswordCard());

        if (main.currentRole.canEditData()) {
            content.getChildren().add(buildMigrationCard());
        }

        content.getChildren().add(buildSessionCard());

        scroll.setContent(content);
        VBox.setVgrow(scroll, Priority.ALWAYS);
        page.getChildren().addAll(title, scroll);
        VBox.setVgrow(scroll, Priority.ALWAYS);
        return page;
    }

    private VBox buildPasswordCard() {
        VBox card = buildCard("Change Password");
        card.setPrefHeight(260);

        GridPane form = new GridPane();
        form.setHgap(12);
        form.setVgap(10);

        PasswordField txtCurrent = new PasswordField();
        txtCurrent.setStyle(FxStyles.textField());
        txtCurrent.setPrefHeight(36);

        PasswordField txtNew = new PasswordField();
        txtNew.setStyle(FxStyles.textField());
        txtNew.setPrefHeight(36);

        PasswordField txtConfirm = new PasswordField();
        txtConfirm.setStyle(FxStyles.textField());
        txtConfirm.setPrefHeight(36);

        form.add(new Label("Current Password:") {
            {
                setStyle(FxStyles.label());
            }
        }, 0, 0);
        form.add(txtCurrent, 1, 0);
        form.add(new Label("New Password:") {
            {
                setStyle(FxStyles.label());
            }
        }, 0, 1);
        form.add(txtNew, 1, 1);
        form.add(new Label("Confirm Password:") {
            {
                setStyle(FxStyles.label());
            }
        }, 0, 2);
        form.add(txtConfirm, 1, 2);

        ColumnConstraints lc = new ColumnConstraints(150);
        ColumnConstraints fc = new ColumnConstraints(200, 200, Double.MAX_VALUE);
        fc.setHgrow(Priority.ALWAYS);
        form.getColumnConstraints().addAll(lc, fc);

        Button btnChange = new Button("Change Password");
        btnChange.setStyle(FxStyles.primaryButton());
        btnChange.setOnAction(e -> {
            String current = txtCurrent.getText().trim();
            String newPw = txtNew.getText().trim();
            String confirm = txtConfirm.getText().trim();

            if (current.isEmpty() || newPw.isEmpty() || confirm.isEmpty()) {
                main.showError("All password fields are required.");
                return;
            }
            if (newPw.length() < 4) {
                main.showError("New password must be at least 4 characters.");
                return;
            }
            if (!newPw.equals(confirm)) {
                main.showError("Passwords do not match.");
                return;
            }

            User currentUser = SessionManager.getCurrentUser();
            if (currentUser == null) {
                main.showError("No active session.");
                return;
            }

            if (!userDAO.verifyPassword(currentUser.getId(), current)) {
                main.showError("Current password is incorrect.");
                return;
            }

            boolean ok = userDAO.updatePassword(currentUser.getId(), newPw);
            if (ok) {
                txtCurrent.clear();
                txtNew.clear();
                txtConfirm.clear();
                main.showInfo("Password changed successfully.");
            } else {
                main.showError("Failed to change password.");
            }
        });

        card.getChildren().addAll(form, btnChange);
        return card;
    }

    private VBox buildMigrationCard() {
        VBox card = buildCard("Data Migration");
        card.setPrefHeight(240);

        HBox btns = new HBox(12);
        btns.setAlignment(Pos.CENTER_LEFT);

        TextArea txtResult = new TextArea(
                "Click a button above to import data from a legacy report file.\n\n" +
                        "This will parse the report format and insert student/subject/score records.\n" +
                        "Existing records are skipped (no duplicates).");
        txtResult.setEditable(false);
        txtResult.setWrapText(true);
        txtResult.setStyle("-fx-font-family: 'Segoe UI'; -fx-font-size: 12;");
        VBox.setVgrow(txtResult, Priority.ALWAYS);

        Button btnMigrate = new Button("Import from academic_report.txt");
        btnMigrate.setStyle(FxStyles.coloredButton(FxStyles.C_ORANGE));
        btnMigrate.setOnAction(e -> {
            String result = DataMigration.migrateFromReportFile();
            txtResult.setText(result);
            main.dataManager.refreshCache();
            main.showInfo("Migration complete.");
        });

        Button btnMigrateFile = new Button("Import from File...");
        btnMigrateFile.setStyle(FxStyles.primaryButton());
        btnMigrateFile.setOnAction(e -> {
            javafx.stage.FileChooser fc = new javafx.stage.FileChooser();
            fc.setTitle("Select Report File");
            fc.getExtensionFilters().add(new javafx.stage.FileChooser.ExtensionFilter("Text Files", "*.txt"));
            java.io.File file = fc.showOpenDialog(null);
            if (file != null) {
                String result = DataMigration.migrateFromReportFile(file.getAbsolutePath());
                txtResult.setText(result);
                main.dataManager.refreshCache();
                main.showInfo("Migration complete.");
            }
        });

        btns.getChildren().addAll(btnMigrate, btnMigrateFile);
        card.getChildren().addAll(btns, txtResult);
        return card;
    }

    private VBox buildSessionCard() {
        VBox card = buildCard("Session Information");
        card.setPrefHeight(220);

        User currentUser = SessionManager.getCurrentUser();
        StringBuilder sb = new StringBuilder();
        sb.append("Username:     ").append(main.currentUsername).append("\n");
        sb.append("Role:         ").append(main.currentRole.getLabel()).append("\n");
        String linkedId = SessionManager.getLinkedStudentId();
        if (linkedId != null)
            sb.append("Linked Student: ").append(linkedId).append("\n");
        sb.append("\nPermissions:\n");
        sb.append("  Edit Data:      ").append(main.currentRole.canEditData() ? "Yes" : "No").append("\n");
        sb.append("  Analytics:      ").append(main.currentRole.canAccessAnalytics() ? "Yes" : "No").append("\n");
        sb.append("  Simulation:     ").append(main.currentRole.canAccessSimulation() ? "Yes" : "No").append("\n");
        sb.append("  Export Reports: ").append(main.currentRole.canExportReports() ? "Yes" : "No").append("\n");
        sb.append("  Admin Access:   ").append(main.currentRole.canAccessAdmin() ? "Yes" : "No").append("\n");
        sb.append("\nApplication:  ALIP v3.0 — Database-Driven Edition\n");
        sb.append("Database:     SQLite (alip_data.db)\n");

        TextArea txt = new TextArea(sb.toString());
        txt.setEditable(false);
        txt.setStyle("-fx-font-family: 'Consolas'; -fx-font-size: 12;");
        VBox.setVgrow(txt, Priority.ALWAYS);
        card.getChildren().add(txt);

        return card;
    }

    private VBox buildCard(String titleText) {
        VBox card = new VBox(10);
        card.setStyle(FxStyles.CARD_STYLE);
        Label t = new Label(titleText);
        t.setStyle(FxStyles.sectionTitle());
        card.getChildren().add(t);
        return card;
    }
}
