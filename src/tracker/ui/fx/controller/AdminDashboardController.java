package tracker.ui.fx.controller;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import tracker.data.DataManager;
import tracker.data.dao.UserDAO;
import tracker.model.*;
import tracker.security.SessionManager;
import tracker.service.AnalyticsService;
import tracker.service.ai.RiskPredictor;
import tracker.service.ai.TrendAnalyzer;
import tracker.ui.fx.ViewManager;
import tracker.ui.fx.ViewManagerAware;
import tracker.ui.fx.util.TableColumnFormatter;

import java.util.List;
import java.util.Optional;

/**
 * Controller for the Admin Dashboard.
 * Dynamic panel switching for Overview, User Management, and Risk
 * Configuration.
 */
public class AdminDashboardController implements ViewManagerAware {

    @FXML
    private Label welcomeLabel;
    @FXML
    private StackPane contentArea;

    private ViewManager viewManager;
    private final DataManager dataManager = new DataManager();
    private final UserDAO userDAO = new UserDAO();
    private final tracker.data.dao.ClassDAO classDAO = new tracker.data.dao.ClassDAO();
    private final TrendAnalyzer trendAnalyzer = new TrendAnalyzer();
    private final RiskPredictor riskPredictor = new RiskPredictor(trendAnalyzer);
    private final AnalyticsService analyticsService = new AnalyticsService(riskPredictor, trendAnalyzer);

    @Override
    public void setViewManager(ViewManager viewManager) {
        this.viewManager = viewManager;
        welcomeLabel.setText("Admin: " + viewManager.getCurrentUsername());
        showOverview();
    }

    // =========================================================================
    // Sidebar Navigation
    // =========================================================================

    @FXML
    private void showOverview() {
        contentArea.getChildren().setAll(buildOverviewPanel());
    }

    @FXML
    private void showUsers() {
        contentArea.getChildren().setAll(buildUserManagementPanel());
    }

    @FXML
    private void showSettings() {
        contentArea.getChildren().setAll(buildRiskConfigPanel());
    }

    @FXML
    private void handleLogout() {
        SessionManager.logout();
        viewManager.clearSession();
        viewManager.showLogin();
    }

    // =========================================================================
    // Overview Panel
    // =========================================================================

    private VBox buildOverviewPanel() {
        VBox panel = new VBox(20);
        panel.setPadding(new Insets(0));

        // --- Get only teacher-assigned students ---
        List<User> users = userDAO.findAll();
        java.util.Set<Integer> assignedClassIds = new java.util.HashSet<>();
        for (ClassRoom c : classDAO.loadAll()) {
            assignedClassIds.add(c.getId());
        }
        List<Student> students = new java.util.ArrayList<>();
        for (Student s : dataManager.getStudents()) {
            if (s.getClassId() > 0 && assignedClassIds.contains(s.getClassId())) {
                students.add(s);
            }
        }

        int atRisk = analyticsService.getAtRiskCount(students);
        double avg = analyticsService.getInstitutionalAverage(students);

        // --- Metric Cards Row ---
        HBox metricsRow = new HBox(16);
        metricsRow.getChildren().addAll(
                createMetricCard("Total Users", String.valueOf(users.size()), "#6c63ff"),
                createMetricCard("Total Students", String.valueOf(students.size()), "#2ed573"),
                createMetricCard("At Risk", String.valueOf(atRisk), "#ff4757"),
                createMetricCard("Inst. Average", String.format("%.1f", avg), "#ffa502"));
        for (javafx.scene.Node n : metricsRow.getChildren()) {
            HBox.setHgrow(n, Priority.ALWAYS);
        }

        // --- User Table ---
        VBox tableCard = new VBox(12);
        tableCard.getStyleClass().add("card");
        tableCard.setPadding(new Insets(16));

        Label tableTitle = new Label("All Users");
        tableTitle.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: white;");

        TableView<User> usersTable = buildUsersTable(users);
        usersTable.setPrefHeight(250);

        tableCard.getChildren().addAll(tableTitle, usersTable);

        // 2x2 Grid for analytics cards
        Label analyticsTitle = new Label("📊 System Analytics");
        analyticsTitle.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: white;");

        GridPane analyticsGrid = new GridPane();
        analyticsGrid.setHgap(16);
        analyticsGrid.setVgap(16);

        // Force equal 50/50 columns
        javafx.scene.layout.ColumnConstraints col1 = new javafx.scene.layout.ColumnConstraints();
        col1.setPercentWidth(50);
        col1.setFillWidth(true);
        javafx.scene.layout.ColumnConstraints col2 = new javafx.scene.layout.ColumnConstraints();
        col2.setPercentWidth(50);
        col2.setFillWidth(true);
        analyticsGrid.getColumnConstraints().addAll(col1, col2);

        // Force equal row heights
        javafx.scene.layout.RowConstraints row1 = new javafx.scene.layout.RowConstraints();
        row1.setVgrow(Priority.ALWAYS);
        row1.setFillHeight(true);
        javafx.scene.layout.RowConstraints row2 = new javafx.scene.layout.RowConstraints();
        row2.setVgrow(Priority.ALWAYS);
        row2.setFillHeight(true);
        analyticsGrid.getRowConstraints().addAll(row1, row2);

        // Risk Distribution card (0,0)
        java.util.Map<RiskScore.Level, Integer> riskDist = analyticsService.getRiskDistribution(students);
        int highCount = riskDist.getOrDefault(RiskScore.Level.HIGH, 0);
        int modCount = riskDist.getOrDefault(RiskScore.Level.MODERATE, 0);
        int lowCount = riskDist.getOrDefault(RiskScore.Level.LOW, 0);

        VBox riskCard = new VBox(10);
        riskCard.getStyleClass().add("card");
        riskCard.setPadding(new Insets(16));
        riskCard.setMaxHeight(Double.MAX_VALUE);
        Label riskTitle = new Label("🔴 Risk Distribution");
        riskTitle.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: white;");
        riskCard.getChildren().addAll(riskTitle,
                createStatRow("High Risk", String.valueOf(highCount), "#ff4757"),
                createStatRow("Moderate Risk", String.valueOf(modCount), "#ffa502"),
                createStatRow("Low Risk", String.valueOf(lowCount), "#2ed573"));

        // Performance Extremes card (1,0)
        Student top = analyticsService.getHighestPerformer(students);
        Student bottom = analyticsService.getLowestPerformer(students);

        VBox perfCard = new VBox(10);
        perfCard.getStyleClass().add("card");
        perfCard.setPadding(new Insets(16));
        perfCard.setMaxHeight(Double.MAX_VALUE);
        Label perfTitle = new Label("🏆 Performance Extremes");
        perfTitle.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: white;");
        perfCard.getChildren().add(perfTitle);
        if (top != null) {
            perfCard.getChildren().add(createStatRow("🥇 Highest",
                    top.getName() + " (" + String.format("%.1f", top.getAverageScore()) + ")", "#2ed573"));
        }
        if (bottom != null) {
            perfCard.getChildren().add(createStatRow("🥉 Lowest",
                    bottom.getName() + " (" + String.format("%.1f", bottom.getAverageScore()) + ")", "#ff4757"));
        }

        // Problem Areas card (0,1)
        String commonWeak = analyticsService.getMostCommonWeakSubject(students);
        tracker.model.SubjectCategory weakestCat = analyticsService.getWeakestCategory(students);

        VBox weakCard = new VBox(10);
        weakCard.getStyleClass().add("card");
        weakCard.setPadding(new Insets(16));
        weakCard.setMaxHeight(Double.MAX_VALUE);
        Label weakTitle = new Label("❗ Problem Areas");
        weakTitle.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: white;");
        weakCard.getChildren().addAll(weakTitle,
                createStatRow("Most Common Weak Subject", commonWeak, "#ffa502"));
        if (weakestCat != tracker.model.SubjectCategory.UNCATEGORIZED) {
            weakCard.getChildren().add(createStatRow("Weakest Category", weakestCat.getDisplayName(), "#ff6348"));
        }

        // Category Averages card (1,1)
        java.util.Map<tracker.model.SubjectCategory, Double> catAvgs = analyticsService.getCategoryAverages(students);
        VBox catCard = new VBox(10);
        catCard.getStyleClass().add("card");
        catCard.setPadding(new Insets(16));
        catCard.setMaxHeight(Double.MAX_VALUE);
        Label catTitle = new Label("📚 Category Averages");
        catTitle.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: white;");
        catCard.getChildren().add(catTitle);
        for (java.util.Map.Entry<tracker.model.SubjectCategory, Double> entry : catAvgs.entrySet()) {
            String color = entry.getValue() >= 70 ? "#2ed573" : entry.getValue() >= 50 ? "#ffa502" : "#ff4757";
            catCard.getChildren().add(
                    createStatRow(entry.getKey().getDisplayName(), String.format("%.1f", entry.getValue()), color));
        }

        analyticsGrid.add(riskCard, 0, 0);
        analyticsGrid.add(perfCard, 1, 0);
        analyticsGrid.add(weakCard, 0, 1);
        analyticsGrid.add(catCard, 1, 1);

        panel.getChildren().addAll(metricsRow, tableCard, analyticsTitle, analyticsGrid);

        // Wrap in scroll
        ScrollPane scroll = new ScrollPane(panel);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        VBox wrapper = new VBox(scroll);
        VBox.setVgrow(scroll, Priority.ALWAYS);
        return wrapper;
    }

    private HBox createStatRow(String label, String value, String valueColor) {
        HBox row = new HBox(8);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(4, 8, 4, 8));
        row.setStyle("-fx-background-color: rgba(255,255,255,0.05); -fx-background-radius: 6;");

        Label lbl = new Label(label);
        lbl.setStyle("-fx-text-fill: #a4b0be; -fx-font-size: 13px;");
        lbl.setMinWidth(180);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label val = new Label(value);
        val.setStyle("-fx-text-fill: " + valueColor + "; -fx-font-weight: bold; -fx-font-size: 13px;");

        row.getChildren().addAll(lbl, spacer, val);
        return row;
    }

    private VBox createMetricCard(String title, String value, String color) {
        VBox card = new VBox(4);
        card.getStyleClass().add("card");
        card.setPadding(new Insets(16));

        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("secondary");

        Label valueLabel = new Label(value);
        valueLabel.setStyle("-fx-font-size: 28px; -fx-font-weight: bold; -fx-text-fill: " + color + ";");

        card.getChildren().addAll(titleLabel, valueLabel);
        return card;
    }

    // =========================================================================
    // User Management Panel
    // =========================================================================

    private VBox buildUserManagementPanel() {
        VBox panel = new VBox(16);
        panel.setPadding(new Insets(0));

        Label title = new Label("👤 User Management");
        title.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: white;");

        // --- Action buttons ---
        HBox actionsBar = new HBox(12);
        actionsBar.setAlignment(Pos.CENTER_LEFT);

        Button createBtn = new Button("➕ Create User");
        createBtn.getStyleClass().add("button-primary");
        createBtn.setStyle("-fx-padding: 8 16;");

        Button toggleBtn = new Button("🔒 Toggle Access");
        toggleBtn.getStyleClass().add("button");
        toggleBtn.setStyle("-fx-background-color: #ffa502; -fx-text-fill: white; -fx-padding: 8 16;");
        toggleBtn.setDisable(true);

        Button linkBtn = new Button("🔗 Link Student");
        linkBtn.getStyleClass().add("button");
        linkBtn.setStyle("-fx-background-color: #6c63ff; -fx-text-fill: white; -fx-padding: 8 16;");
        linkBtn.setDisable(true);

        Button refreshBtn = new Button("🔄 Refresh");
        refreshBtn.getStyleClass().add("button");
        refreshBtn.setStyle("-fx-background-color: #2ed573; -fx-text-fill: white; -fx-padding: 8 16;");

        actionsBar.getChildren().addAll(createBtn, toggleBtn, linkBtn, new Region(), refreshBtn);
        HBox.setHgrow(actionsBar.getChildren().get(3), Priority.ALWAYS);

        // --- Users Table ---
        List<User> users = userDAO.findAll();
        TableView<User> usersTable = buildFullUsersTable(users);
        VBox.setVgrow(usersTable, Priority.ALWAYS);

        // Enable action buttons when a user is selected
        usersTable.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            boolean selected = (newVal != null);
            toggleBtn.setDisable(!selected);
            linkBtn.setDisable(!selected);
        });

        // --- Button actions ---
        createBtn.setOnAction(e -> {
            handleCreateUser();
            usersTable.setItems(FXCollections.observableArrayList(userDAO.findAll()));
        });

        toggleBtn.setOnAction(e -> {
            User selected = usersTable.getSelectionModel().getSelectedItem();
            if (selected != null) {
                boolean newState = !selected.isEnabled();
                userDAO.setEnabled(selected.getId(), newState);
                usersTable.setItems(FXCollections.observableArrayList(userDAO.findAll()));
                showAlert(Alert.AlertType.INFORMATION, "Access Updated",
                        selected.getUsername() + " is now " + (newState ? "Active" : "Disabled"));
            }
        });

        linkBtn.setOnAction(e -> {
            User selected = usersTable.getSelectionModel().getSelectedItem();
            if (selected != null) {
                handleLinkStudent(selected);
                usersTable.setItems(FXCollections.observableArrayList(userDAO.findAll()));
            }
        });

        refreshBtn.setOnAction(e -> {
            usersTable.setItems(FXCollections.observableArrayList(userDAO.findAll()));
        });

        // --- Card wrapper ---
        VBox tableCard = new VBox(12, actionsBar, usersTable);
        tableCard.getStyleClass().add("card");
        tableCard.setPadding(new Insets(16));
        VBox.setVgrow(tableCard, Priority.ALWAYS);

        panel.getChildren().addAll(title, tableCard);
        VBox.setVgrow(panel, Priority.ALWAYS);
        return panel;
    }

    private TableView<User> buildUsersTable(List<User> users) {
        TableView<User> table = new TableView<>(FXCollections.observableArrayList(users));
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<User, String> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(cd -> new SimpleStringProperty(String.valueOf(cd.getValue().getId())));

        TableColumn<User, String> nameCol = new TableColumn<>("Username");
        nameCol.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getUsername()));
        nameCol.setCellFactory(new tracker.ui.fx.util.AvatarCellFactory<>());

        TableColumn<User, String> roleCol = new TableColumn<>("Role");
        roleCol.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getRole().name()));

        TableColumn<User, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(cd -> new SimpleStringProperty(
                cd.getValue().isEnabled() ? "✓ Active" : "✗ Disabled"));

        table.getColumns().addAll(idCol, nameCol, roleCol, statusCol);
        TableColumnFormatter.bindColumnWidth(idCol, table, 0.10);
        TableColumnFormatter.bindColumnWidth(nameCol, table, 0.45);
        TableColumnFormatter.bindColumnWidth(roleCol, table, 0.20);
        TableColumnFormatter.bindColumnWidth(statusCol, table, 0.25);

        return table;
    }

    private TableView<User> buildFullUsersTable(List<User> users) {
        TableView<User> table = new TableView<>(FXCollections.observableArrayList(users));
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setPrefHeight(400);

        TableColumn<User, String> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(cd -> new SimpleStringProperty(String.valueOf(cd.getValue().getId())));

        TableColumn<User, String> nameCol = new TableColumn<>("Username");
        nameCol.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getUsername()));
        nameCol.setCellFactory(new tracker.ui.fx.util.AvatarCellFactory<>());

        TableColumn<User, String> roleCol = new TableColumn<>("Role");
        roleCol.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getRole().name()));

        TableColumn<User, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(cd -> {
            boolean enabled = cd.getValue().isEnabled();
            return new SimpleStringProperty(enabled ? "✓ Active" : "✗ Disabled");
        });
        statusCol.setCellFactory(col -> new TableCell<User, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    if (item.contains("Active")) {
                        setStyle("-fx-text-fill: #2ed573; -fx-font-weight: bold;");
                    } else {
                        setStyle("-fx-text-fill: #ff4757; -fx-font-weight: bold;");
                    }
                }
            }
        });

        TableColumn<User, String> linkedCol = new TableColumn<>("Linked Student");
        linkedCol.setCellValueFactory(cd -> {
            Integer linkedDbId = cd.getValue().getLinkedStudentDbId();
            String linkedBizId = cd.getValue().getLinkedStudentId();
            if (linkedBizId != null && !linkedBizId.isEmpty()) {
                return new SimpleStringProperty(linkedBizId);
            } else if (linkedDbId != null && linkedDbId > 0) {
                String resolved = userDAO.resolveLinkedStudentId(linkedDbId);
                return new SimpleStringProperty(resolved != null ? resolved : "DB#" + linkedDbId);
            }
            return new SimpleStringProperty("—");
        });

        TableColumn<User, String> createdCol = new TableColumn<>("Created");
        createdCol.setCellValueFactory(cd -> new SimpleStringProperty(
                cd.getValue().getCreatedAt() != null ? cd.getValue().getCreatedAt() : "—"));

        table.getColumns().addAll(idCol, nameCol, roleCol, statusCol, linkedCol, createdCol);
        TableColumnFormatter.bindColumnWidth(idCol, table, 0.06);
        TableColumnFormatter.bindColumnWidth(nameCol, table, 0.25);
        TableColumnFormatter.bindColumnWidth(roleCol, table, 0.12);
        TableColumnFormatter.bindColumnWidth(statusCol, table, 0.14);
        TableColumnFormatter.bindColumnWidth(linkedCol, table, 0.20);
        TableColumnFormatter.bindColumnWidth(createdCol, table, 0.23);

        return table;
    }

    // =========================================================================
    // Risk Configuration Panel
    // =========================================================================

    private VBox buildRiskConfigPanel() {
        VBox panel = new VBox(16);
        panel.setPadding(new Insets(0));

        Label title = new Label("🔧 Risk Engine Configuration");
        title.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: white;");

        // --- Weights Card ---
        VBox weightsCard = new VBox(16);
        weightsCard.getStyleClass().add("card");
        weightsCard.setPadding(new Insets(20));

        Label weightsTitle = new Label("Risk Factor Weights");
        weightsTitle.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: white;");
        Label weightsSubtitle = new Label("Weights must sum to 1.0 (100%)");
        weightsSubtitle.setStyle("-fx-text-fill: #a4b0be; -fx-font-size: 12px;");

        GridPane weightsGrid = new GridPane();
        weightsGrid.setHgap(16);
        weightsGrid.setVgap(12);

        TextField avgWeight = createConfigField(String.valueOf(RiskScore.W_AVERAGE));
        TextField weakWeight = createConfigField(String.valueOf(RiskScore.W_WEAK_COUNT));
        TextField lowestWeight = createConfigField(String.valueOf(RiskScore.W_LOWEST));
        TextField trendWeight = createConfigField(String.valueOf(RiskScore.W_TREND));

        weightsGrid.add(createConfigLabel("📊 Average Score Weight:", "#6c63ff"), 0, 0);
        weightsGrid.add(avgWeight, 1, 0);
        weightsGrid.add(createConfigLabel("📉 Weak Subject Density Weight:", "#ffa502"), 0, 1);
        weightsGrid.add(weakWeight, 1, 1);
        weightsGrid.add(createConfigLabel("⬇️ Lowest Score Severity Weight:", "#ff4757"), 0, 2);
        weightsGrid.add(lowestWeight, 1, 2);
        weightsGrid.add(createConfigLabel("📈 Trend Direction Weight:", "#2ed573"), 0, 3);
        weightsGrid.add(trendWeight, 1, 3);

        Label weightSumLabel = new Label();
        updateWeightSum(weightSumLabel, avgWeight, weakWeight, lowestWeight, trendWeight);

        // Live sum update
        avgWeight.textProperty().addListener(
                (o, ov, nv) -> updateWeightSum(weightSumLabel, avgWeight, weakWeight, lowestWeight, trendWeight));
        weakWeight.textProperty().addListener(
                (o, ov, nv) -> updateWeightSum(weightSumLabel, avgWeight, weakWeight, lowestWeight, trendWeight));
        lowestWeight.textProperty().addListener(
                (o, ov, nv) -> updateWeightSum(weightSumLabel, avgWeight, weakWeight, lowestWeight, trendWeight));
        trendWeight.textProperty().addListener(
                (o, ov, nv) -> updateWeightSum(weightSumLabel, avgWeight, weakWeight, lowestWeight, trendWeight));

        weightsCard.getChildren().addAll(weightsTitle, weightsSubtitle, weightsGrid, new Separator(), weightSumLabel);

        // --- Thresholds Card ---
        VBox thresholdsCard = new VBox(16);
        thresholdsCard.getStyleClass().add("card");
        thresholdsCard.setPadding(new Insets(20));

        Label thresholdsTitle = new Label("Risk Level Thresholds");
        thresholdsTitle.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: white;");
        Label thresholdsSub = new Label("Score ranges: Low Risk → Moderate Risk → High Risk");
        thresholdsSub.setStyle("-fx-text-fill: #a4b0be; -fx-font-size: 12px;");

        GridPane thresholdsGrid = new GridPane();
        thresholdsGrid.setHgap(16);
        thresholdsGrid.setVgap(12);

        TextField highThreshold = createConfigField(String.valueOf(RiskScore.THRESHOLD_HIGH));
        TextField modThreshold = createConfigField(String.valueOf(RiskScore.THRESHOLD_MODERATE));

        thresholdsGrid.add(createConfigLabel("🔴 High Risk Threshold (score ≥):", "#ff4757"), 0, 0);
        thresholdsGrid.add(highThreshold, 1, 0);
        thresholdsGrid.add(createConfigLabel("🟠 Moderate Risk Threshold (score ≥):", "#ffa502"), 0, 1);
        thresholdsGrid.add(modThreshold, 1, 1);

        thresholdsCard.getChildren().addAll(thresholdsTitle, thresholdsSub, thresholdsGrid);

        // --- Apply Button ---
        HBox buttonBar = new HBox(12);
        buttonBar.setAlignment(Pos.CENTER_LEFT);

        Button applyBtn = new Button("✅ Apply Changes");
        applyBtn.getStyleClass().add("button-primary");
        applyBtn.setStyle("-fx-padding: 10 24; -fx-font-size: 14px;");

        Button resetBtn = new Button("🔄 Reset Defaults");
        resetBtn.getStyleClass().add("button");
        resetBtn.setStyle(
                "-fx-background-color: #576574; -fx-text-fill: white; -fx-padding: 10 24; -fx-font-size: 14px;");

        Label statusLabel = new Label();
        statusLabel.setStyle("-fx-font-size: 13px;");

        buttonBar.getChildren().addAll(applyBtn, resetBtn, statusLabel);

        applyBtn.setOnAction(e -> {
            try {
                double wAvg = Double.parseDouble(avgWeight.getText().trim());
                double wWeak = Double.parseDouble(weakWeight.getText().trim());
                double wLow = Double.parseDouble(lowestWeight.getText().trim());
                double wTrend = Double.parseDouble(trendWeight.getText().trim());
                double tHigh = Double.parseDouble(highThreshold.getText().trim());
                double tMod = Double.parseDouble(modThreshold.getText().trim());

                double sum = wAvg + wWeak + wLow + wTrend;
                if (Math.abs(sum - 1.0) > 0.01) {
                    statusLabel.setText("❌ Weights must sum to 1.0 (current: " + String.format("%.2f", sum) + ")");
                    statusLabel.setStyle("-fx-text-fill: #ff4757; -fx-font-size: 13px;");
                    return;
                }
                if (tHigh <= tMod) {
                    statusLabel.setText("❌ High threshold must be greater than Moderate threshold");
                    statusLabel.setStyle("-fx-text-fill: #ff4757; -fx-font-size: 13px;");
                    return;
                }

                RiskScore.W_AVERAGE = wAvg;
                RiskScore.W_WEAK_COUNT = wWeak;
                RiskScore.W_LOWEST = wLow;
                RiskScore.W_TREND = wTrend;
                RiskScore.THRESHOLD_HIGH = tHigh;
                RiskScore.THRESHOLD_MODERATE = tMod;

                statusLabel.setText("✅ Configuration applied successfully!");
                statusLabel.setStyle("-fx-text-fill: #2ed573; -fx-font-size: 13px;");
            } catch (NumberFormatException ex) {
                statusLabel.setText("❌ Invalid number format. Please enter valid decimals.");
                statusLabel.setStyle("-fx-text-fill: #ff4757; -fx-font-size: 13px;");
            }
        });

        resetBtn.setOnAction(e -> {
            RiskScore.W_AVERAGE = 0.35;
            RiskScore.W_WEAK_COUNT = 0.25;
            RiskScore.W_LOWEST = 0.25;
            RiskScore.W_TREND = 0.15;
            RiskScore.THRESHOLD_HIGH = 60.0;
            RiskScore.THRESHOLD_MODERATE = 30.0;

            avgWeight.setText("0.35");
            weakWeight.setText("0.25");
            lowestWeight.setText("0.25");
            trendWeight.setText("0.15");
            highThreshold.setText("60.0");
            modThreshold.setText("30.0");

            statusLabel.setText("🔄 Reset to defaults.");
            statusLabel.setStyle("-fx-text-fill: #ffa502; -fx-font-size: 13px;");
        });

        // --- System Info Card ---
        VBox sysInfoCard = new VBox(8);
        sysInfoCard.getStyleClass().add("card");
        sysInfoCard.setPadding(new Insets(16));

        Label sysTitle = new Label("System Information");
        sysTitle.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: white;");

        Label dbInfo = new Label("Database: alip_data.db (SQLite WAL mode)");
        dbInfo.setStyle("-fx-text-fill: #a4b0be;");
        Label schemaInfo = new Label("Schema Version: 4");
        schemaInfo.setStyle("-fx-text-fill: #a4b0be;");
        Label fxInfo = new Label("UI Framework: JavaFX 21.0.2 (OpenJFX)");
        fxInfo.setStyle("-fx-text-fill: #a4b0be;");
        Label jdkInfo = new Label("JDK: " + System.getProperty("java.version"));
        jdkInfo.setStyle("-fx-text-fill: #a4b0be;");

        sysInfoCard.getChildren().addAll(sysTitle, dbInfo, schemaInfo, fxInfo, jdkInfo);

        panel.getChildren().addAll(title, weightsCard, thresholdsCard, buttonBar, sysInfoCard);

        // Wrap in scroll pane for overflow
        ScrollPane scroll = new ScrollPane(panel);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;");

        VBox wrapper = new VBox(scroll);
        VBox.setVgrow(scroll, Priority.ALWAYS);
        return wrapper;
    }

    private Label createConfigLabel(String text, String color) {
        Label label = new Label(text);
        label.setStyle("-fx-text-fill: " + color + "; -fx-font-weight: bold; -fx-font-size: 13px;");
        label.setMinWidth(280);
        return label;
    }

    private TextField createConfigField(String value) {
        TextField tf = new TextField(value);
        tf.setPrefWidth(100);
        tf.setMaxWidth(120);
        tf.setStyle(
                "-fx-background-color: #16213e; -fx-text-fill: white; -fx-border-color: #3a3f5c; -fx-border-radius: 5; -fx-background-radius: 5;");
        return tf;
    }

    private void updateWeightSum(Label sumLabel, TextField... fields) {
        try {
            double sum = 0;
            for (TextField f : fields) {
                sum += Double.parseDouble(f.getText().trim());
            }
            String color = Math.abs(sum - 1.0) < 0.01 ? "#2ed573" : "#ff4757";
            String icon = Math.abs(sum - 1.0) < 0.01 ? "✅" : "⚠️";
            sumLabel.setText(icon + " Current sum: " + String.format("%.2f", sum) + " / 1.00");
            sumLabel.setStyle("-fx-text-fill: " + color + "; -fx-font-weight: bold; -fx-font-size: 13px;");
        } catch (NumberFormatException e) {
            sumLabel.setText("⚠️ Invalid number in weights");
            sumLabel.setStyle("-fx-text-fill: #ff4757; -fx-font-weight: bold; -fx-font-size: 13px;");
        }
    }

    // =========================================================================
    // Dialogs
    // =========================================================================

    private void handleCreateUser() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Create User");
        dialog.setHeaderText("Create a new user account");
        dialog.getDialogPane().setPrefWidth(450);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        VBox box = new VBox(12);
        box.setPadding(new Insets(16));

        TextField usernameField = new TextField();
        usernameField.setPromptText("Username");
        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Password");
        ComboBox<String> roleBox = new ComboBox<>(FXCollections.observableArrayList("STUDENT", "TEACHER", "ADMIN"));
        roleBox.getSelectionModel().selectFirst();
        roleBox.setMaxWidth(Double.MAX_VALUE);

        // Student linking dropdown
        List<Student> allStudents = dataManager.getStudents();
        ComboBox<String> studentPicker = new ComboBox<>();
        studentPicker.setMaxWidth(Double.MAX_VALUE);
        studentPicker.setPromptText("Select a student...");
        studentPicker.getItems().add("— None —");
        for (Student s : allStudents) {
            int dbId = dataManager.getStudentDAO().findDbIdByStudentId(s.getId());
            studentPicker.getItems().add(s.getName() + " (" + s.getId() + ") [DB#" + dbId + "]");
        }
        studentPicker.getSelectionModel().selectFirst();

        Label linkNote = new Label("Link this account to an existing student record.");
        linkNote.setStyle("-fx-text-fill: #a4b0be; -fx-font-size: 11px;");

        Label statusLabel = new Label();

        box.getChildren().addAll(
                new Label("Username:"), usernameField,
                new Label("Password:"), passwordField,
                new Label("Role:"), roleBox,
                new Separator(),
                new Label("Link to Student (optional):"), studentPicker, linkNote,
                statusLabel);
        dialog.getDialogPane().setContent(box);

        // Use event filter on OK button to validate before closing
        final javafx.scene.control.Button okButton = (javafx.scene.control.Button) dialog.getDialogPane()
                .lookupButton(ButtonType.OK);
        okButton.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            String username = usernameField.getText().trim();
            String password = passwordField.getText();
            String roleStr = roleBox.getValue();

            if (username.length() < 3) {
                statusLabel.setText("Username must be at least 3 characters.");
                statusLabel.setStyle("-fx-text-fill: #ff4757;");
                event.consume(); // Prevent dialog from closing
                return;
            }
            if (password.length() < 6) {
                statusLabel.setText("Password must be at least 6 characters.");
                statusLabel.setStyle("-fx-text-fill: #ff4757;");
                event.consume();
                return;
            }
            if (userDAO.findByUsername(username) != null) {
                statusLabel.setText("Username already exists.");
                statusLabel.setStyle("-fx-text-fill: #ff4757;");
                event.consume();
                return;
            }

            UserRole role = UserRole.valueOf(roleStr);
            Integer linkedStudentDbId = extractDbIdFromSelection(studentPicker.getValue());

            boolean success = userDAO.createUser(username, password, role, linkedStudentDbId);
            if (!success) {
                statusLabel.setText("Failed to create user. Check console for errors.");
                statusLabel.setStyle("-fx-text-fill: #ff4757;");
                event.consume();
                return;
            }
            // Success — dialog will close normally
        });

        dialog.showAndWait();
    }

    private void handleLinkStudent(User user) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Link Student");
        dialog.setHeaderText("Link a student record to: " + user.getUsername());
        dialog.getDialogPane().setPrefWidth(450);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        VBox box = new VBox(12);
        box.setPadding(new Insets(16));

        List<Student> allStudents = dataManager.getStudents();
        ComboBox<String> studentPicker = new ComboBox<>();
        studentPicker.setMaxWidth(Double.MAX_VALUE);
        studentPicker.getItems().add("— None (unlink) —");

        int selectedIndex = 0;
        int idx = 1;
        for (Student s : allStudents) {
            int dbId = dataManager.getStudentDAO().findDbIdByStudentId(s.getId());
            String entry = s.getName() + " (" + s.getId() + ") [DB#" + dbId + "]";
            studentPicker.getItems().add(entry);
            if (user.getLinkedStudentDbId() != null && user.getLinkedStudentDbId() == dbId) {
                selectedIndex = idx;
            }
            idx++;
        }
        studentPicker.getSelectionModel().select(selectedIndex);

        Label info = new Label("Select a student to link, or choose 'None' to unlink.");
        info.setStyle("-fx-text-fill: #a4b0be; -fx-font-size: 11px;");

        box.getChildren().addAll(new Label("Select Student:"), studentPicker, info);
        dialog.getDialogPane().setContent(box);

        dialog.setResultConverter(btn -> {
            if (btn == ButtonType.OK) {
                Integer dbId = extractDbIdFromSelection(studentPicker.getValue());
                userDAO.linkStudent(user.getId(), dbId);
                if (dbId == null) {
                    showAlert(Alert.AlertType.INFORMATION, "Unlinked",
                            "Student link removed for " + user.getUsername());
                } else {
                    showAlert(Alert.AlertType.INFORMATION, "Linked",
                            user.getUsername() + " linked to student DB#" + dbId);
                }
            }
            return btn;
        });

        dialog.showAndWait();
    }

    /**
     * Extracts the DB ID from a student picker entry like "Aarav Sharma
     * (STU_C9DB57F) [DB#3]".
     * Returns null if "None" is selected.
     */
    private Integer extractDbIdFromSelection(String selection) {
        if (selection == null || selection.contains("None")) {
            return null;
        }
        try {
            int start = selection.lastIndexOf("[DB#") + 4;
            int end = selection.lastIndexOf("]");
            return Integer.parseInt(selection.substring(start, end));
        } catch (Exception e) {
            return null;
        }
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
