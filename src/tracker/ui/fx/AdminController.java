package tracker.ui.fx;

import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import tracker.data.dao.UserDAO;
import tracker.model.*;

import java.util.List;

/**
 * Administration page controller.
 * User management, risk config, category mappings.
 * Same logic as Swing AdminPage.
 */
public class AdminController {

    private final MainController main;
    private final UserDAO userDAO = new UserDAO();

    private TextField txtUsername, txtPassword, txtLinkStudentId;
    private ComboBox<String> cmbRole;
    private TableView<UserRow> userTable;

    private ComboBox<String> cmbConfigKey;
    private TextField txtConfigValue;

    public AdminController(MainController main) {
        this.main = main;
    }

    public Region buildPage() {
        VBox page = new VBox(16);
        page.setStyle(FxStyles.PAGE_BG);
        page.setPadding(new Insets(20, 24, 20, 24));

        Label title = new Label("Administration");
        title.setStyle(FxStyles.title());

        ScrollPane scroll = new ScrollPane();
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background: #F1F5F9; -fx-background-color: #F1F5F9;");

        VBox content = new VBox(16);
        content.setPadding(new Insets(4));

        content.getChildren().addAll(buildUserMgmtCard(), buildRiskConfigCard(), buildCategoryCard());

        scroll.setContent(content);
        VBox.setVgrow(scroll, Priority.ALWAYS);
        page.getChildren().addAll(title, scroll);
        VBox.setVgrow(scroll, Priority.ALWAYS);

        loadUsers();
        return page;
    }

    // ----------------------
    // User Management Section
    // ----------------------

    private VBox buildUserMgmtCard() {
        VBox card = buildCard("User Management");

        GridPane form = new GridPane();
        form.setHgap(12);
        form.setVgap(10);

        txtUsername = styledField("Username");
        txtPassword = styledField("Password");
        txtLinkStudentId = styledField("e.g. S001 (STUDENT role only)");

        cmbRole = new ComboBox<>();
        cmbRole.getItems().addAll("ADMIN", "TEACHER", "STUDENT");
        cmbRole.setValue("TEACHER");
        cmbRole.setStyle("-fx-font-family: 'Segoe UI'; -fx-font-size: 13;");
        cmbRole.setPrefHeight(36);

        form.add(new Label("Username:") {
            {
                setStyle(FxStyles.label());
            }
        }, 0, 0);
        form.add(txtUsername, 1, 0);
        form.add(new Label("Password:") {
            {
                setStyle(FxStyles.label());
            }
        }, 2, 0);
        form.add(txtPassword, 3, 0);
        form.add(new Label("Role:") {
            {
                setStyle(FxStyles.label());
            }
        }, 0, 1);
        form.add(cmbRole, 1, 1);
        form.add(new Label("Link Student ID:") {
            {
                setStyle(FxStyles.label());
            }
        }, 2, 1);
        form.add(txtLinkStudentId, 3, 1);

        ColumnConstraints lc = new ColumnConstraints(110);
        ColumnConstraints fc = new ColumnConstraints(170, 170, Double.MAX_VALUE);
        fc.setHgrow(Priority.ALWAYS);
        for (int i = 0; i < 4; i++)
            form.getColumnConstraints().add(i % 2 == 0 ? lc : fc);

        HBox btns = new HBox(12);
        btns.setAlignment(Pos.CENTER_LEFT);
        Button btnCreate = new Button("Create User");
        btnCreate.setStyle(FxStyles.primaryButton());
        btnCreate.setOnAction(e -> handleCreateUser());
        Button btnToggle = new Button("Toggle Enabled");
        btnToggle.setStyle(FxStyles.coloredButton(FxStyles.C_ORANGE));
        btnToggle.setOnAction(e -> handleToggleUser());
        btns.getChildren().addAll(btnCreate, btnToggle);

        // User table
        userTable = new TableView<>();
        userTable.setStyle("-fx-font-family: 'Segoe UI'; -fx-font-size: 12;");
        userTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        userTable.setPrefHeight(160);

        addUserCol("ID", "id");
        addUserCol("Username", "username");
        addUserCol("Role", "role");
        addUserCol("Linked Student", "linked");
        addUserCol("Enabled", "enabled");
        addUserCol("Created", "created");

        card.getChildren().addAll(form, btns, userTable);
        card.setPrefHeight(380);
        return card;
    }

    private void handleCreateUser() {
        String uname = txtUsername.getText().trim();
        String pass = txtPassword.getText().trim();
        String role = cmbRole.getValue();
        String linkedId = txtLinkStudentId.getText().trim();

        if (uname.isEmpty() || pass.isEmpty()) {
            main.showError("Username and password are required.");
            return;
        }

        UserRole userRole;
        try {
            userRole = UserRole.valueOf(role);
        } catch (Exception e) {
            main.showError("Invalid role.");
            return;
        }

        Integer linkedDbId = null;
        if (!linkedId.isEmpty() && userRole == UserRole.STUDENT) {
            int dbId = main.dataManager.getStudentDAO().findDbIdByStudentId(linkedId);
            if (dbId < 0) {
                main.showError("Student ID '" + linkedId + "' not found.");
                return;
            }
            linkedDbId = dbId;
        }

        boolean ok = userDAO.createUser(uname, pass, userRole, linkedDbId);
        if (ok) {
            loadUsers();
            txtUsername.clear();
            txtPassword.clear();
            txtLinkStudentId.clear();
            main.showInfo("User '" + uname + "' created.");
        } else {
            main.showError("Failed to create user (username may already exist).");
        }
    }

    private void handleToggleUser() {
        UserRow row = userTable.getSelectionModel().getSelectedItem();
        if (row == null) {
            main.showError("Select a user to toggle.");
            return;
        }

        int userId = Integer.parseInt(row.id);
        boolean nowEnabled = "No".equals(row.enabled); // toggle
        boolean ok = userDAO.setEnabled(userId, nowEnabled);
        if (ok) {
            loadUsers();
            main.showInfo("User '" + row.username + "' " + (nowEnabled ? "enabled" : "disabled") + ".");
        } else {
            main.showError("Failed to update user status.");
        }
    }

    private void loadUsers() {
        if (userTable == null)
            return;
        userTable.getItems().clear();
        List<tracker.model.User> users = userDAO.findAll();
        for (tracker.model.User u : users) {
            userTable.getItems().add(new UserRow(
                    String.valueOf(u.getId()), u.getUsername(),
                    u.getRole() != null ? u.getRole().name() : "?",
                    u.getLinkedStudentDbId() != null ? u.getLinkedStudentDbId().toString() : "",
                    u.isEnabled() ? "Yes" : "No",
                    u.getCreatedAt() != null ? u.getCreatedAt() : ""));
        }
    }

    private void addUserCol(String header, String prop) {
        TableColumn<UserRow, String> col = new TableColumn<>(header);
        col.setCellValueFactory(data -> new SimpleStringProperty(
                switch (prop) {
                    case "id" -> data.getValue().id;
                    case "username" -> data.getValue().username;
                    case "role" -> data.getValue().role;
                    case "linked" -> data.getValue().linked;
                    case "enabled" -> data.getValue().enabled;
                    case "created" -> data.getValue().created;
                    default -> "";
                }));
        userTable.getColumns().add(col);
    }

    // ----------------------
    // Risk Config Section
    // ----------------------

    private VBox buildRiskConfigCard() {
        VBox card = buildCard("Risk Configuration (DB-backed)");
        card.setPrefHeight(160);

        GridPane form = new GridPane();
        form.setHgap(12);
        form.setVgap(10);

        cmbConfigKey = new ComboBox<>();
        cmbConfigKey.getItems().addAll(
                "risk.weight.average", "risk.weight.weak_count", "risk.weight.lowest",
                "risk.weight.trend", "risk.threshold.high", "risk.threshold.moderate");
        cmbConfigKey.getSelectionModel().selectFirst();
        cmbConfigKey.setStyle("-fx-font-family: 'Segoe UI'; -fx-font-size: 13;");
        cmbConfigKey.setPrefHeight(36);

        txtConfigValue = styledField("Value");

        Button btnSave = new Button("Save Config");
        btnSave.setStyle(FxStyles.coloredButton(FxStyles.C_GREEN));
        btnSave.setOnAction(e -> handleSaveConfig());

        form.add(new Label("Config Key:") {
            {
                setStyle(FxStyles.label());
            }
        }, 0, 0);
        form.add(cmbConfigKey, 1, 0);
        form.add(new Label("Value:") {
            {
                setStyle(FxStyles.label());
            }
        }, 2, 0);
        form.add(txtConfigValue, 3, 0);
        form.add(btnSave, 0, 1);
        GridPane.setColumnSpan(btnSave, 4);
        GridPane.setHalignment(btnSave, javafx.geometry.HPos.CENTER);

        ColumnConstraints lc = new ColumnConstraints(110);
        ColumnConstraints fc = new ColumnConstraints(200, 200, Double.MAX_VALUE);
        fc.setHgrow(Priority.ALWAYS);
        for (int i = 0; i < 4; i++)
            form.getColumnConstraints().add(i % 2 == 0 ? lc : fc);

        card.getChildren().add(form);
        return card;
    }

    private void handleSaveConfig() {
        String key = cmbConfigKey.getValue();
        String val = txtConfigValue.getText().trim();
        if (val.isEmpty()) {
            main.showError("Enter a value.");
            return;
        }

        try {
            Double.parseDouble(val);
        } catch (NumberFormatException e) {
            main.showError("Value must be a number.");
            return;
        }

        tracker.data.dao.ConfigDAO configDAO = new tracker.data.dao.ConfigDAO();
        configDAO.setValue(key, val);
        main.showInfo("Config saved: " + key + " = " + val);
    }

    // ----------------------
    // Category Mappings Section
    // ----------------------

    private VBox buildCategoryCard() {
        VBox card = buildCard("Subject Category Mappings");
        card.setPrefHeight(200);

        TextArea txt = new TextArea();
        txt.setEditable(false);
        txt.setStyle("-fx-font-family: 'Consolas'; -fx-font-size: 11;");
        StringBuilder sb = new StringBuilder();
        for (SubjectCategory cat : SubjectCategory.values()) {
            sb.append(cat.name()).append(": ").append(cat.toString()).append("\n");
        }
        txt.setText(sb.toString());
        VBox.setVgrow(txt, Priority.ALWAYS);
        card.getChildren().add(txt);
        return card;
    }

    // Helpers
    private TextField styledField(String prompt) {
        TextField tf = new TextField();
        tf.setPromptText(prompt);
        tf.setStyle(FxStyles.textField());
        tf.setPrefHeight(36);
        return tf;
    }

    private VBox buildCard(String titleText) {
        VBox card = new VBox(10);
        card.setStyle(FxStyles.CARD_STYLE);
        Label t = new Label(titleText);
        t.setStyle(FxStyles.sectionTitle());
        card.getChildren().add(t);
        return card;
    }

    public static class UserRow {
        public final String id, username, role, linked, enabled, created;

        public UserRow(String id, String username, String role, String linked, String enabled, String created) {
            this.id = id;
            this.username = username;
            this.role = role;
            this.linked = linked;
            this.enabled = enabled;
            this.created = created;
        }
    }
}
