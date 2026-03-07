package tracker.ui.fx;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import tracker.data.DataManager;
import tracker.data.FileManager;
import tracker.model.UserRole;
import tracker.security.SessionManager;
import tracker.service.AnalyticsService;
import tracker.service.RecommendationEngine;
import tracker.service.SimulationService;
import tracker.service.ai.AdaptivePlanner;
import tracker.service.ai.RiskPredictor;
import tracker.service.ai.TrendAnalyzer;

import java.util.ArrayList;
import java.util.List;

/**
 * JavaFX Main application controller.
 * Manages the sidebar navigation and content area switcher.
 * All backend dependencies mirror the original MainFrame exactly.
 */
public class MainController {

    private final Stage stage;
    final UserRole currentRole;
    final String currentUsername;

    // Backend — unchanged from Swing version
    final DataManager dataManager;
    final FileManager fileManager;
    final RecommendationEngine recommendationEngine;
    final TrendAnalyzer trendAnalyzer;
    final RiskPredictor riskPredictor;
    final AdaptivePlanner adaptivePlanner;
    final AnalyticsService analyticsService;
    final SimulationService simulationService;

    // Layout
    private StackPane contentArea;
    private final List<Label> sidebarItems = new ArrayList<>();
    private Label selectedItem;

    // Page controllers
    private DashboardController dashboardCtrl;
    private StudentsController studentsCtrl;
    private SubjectsController subjectsCtrl;
    private ReportsController reportsCtrl;
    private AnalyticsController analyticsCtrl;
    private SimulationController simulationCtrl;
    private ManageController manageCtrl;
    private AdminController adminCtrl;
    private SettingsController settingsCtrl;
    private StudyStrategyController strategyCtrl;

    // Page name constants
    static final String PAGE_DASHBOARD = "Dashboard";
    static final String PAGE_STUDENTS = "Students";
    static final String PAGE_SUBJECTS = "Subjects";
    static final String PAGE_REPORTS = "Reports";
    static final String PAGE_ANALYTICS = "Analytics";
    static final String PAGE_SIMULATION = "Simulation";
    static final String PAGE_MANAGE = "Manage";
    static final String PAGE_ADMIN = "Admin";
    static final String PAGE_SETTINGS = "Settings";
    static final String PAGE_STRATEGY = "Study Strategy Advisor";

    public MainController(Stage stage, UserRole role, String username) {
        this.stage = stage;
        this.currentRole = role;
        this.currentUsername = username;

        this.dataManager = DataManager.getInstance();
        this.fileManager = new FileManager();
        this.recommendationEngine = new RecommendationEngine();
        this.trendAnalyzer = new TrendAnalyzer();
        this.riskPredictor = new RiskPredictor(trendAnalyzer);
        this.adaptivePlanner = new AdaptivePlanner();
        this.analyticsService = new AnalyticsService(riskPredictor, trendAnalyzer);
        this.simulationService = new SimulationService(riskPredictor, trendAnalyzer, adaptivePlanner);
    }

    public Scene buildScene() {
        BorderPane root = new BorderPane();
        root.setStyle(FxStyles.PAGE_BG);

        // Sidebar (left)
        root.setLeft(buildSidebar());

        // Main area (header + content)
        VBox mainArea = new VBox(0);
        mainArea.setStyle(FxStyles.PAGE_BG);
        mainArea.getChildren().add(buildHeader());

        contentArea = new StackPane();
        contentArea.setStyle(FxStyles.PAGE_BG);
        VBox.setVgrow(contentArea, Priority.ALWAYS);
        mainArea.getChildren().add(contentArea);

        root.setCenter(mainArea);

        // Build all page controllers
        dashboardCtrl = new DashboardController(this);
        studentsCtrl = new StudentsController(this);
        subjectsCtrl = new SubjectsController(this);
        reportsCtrl = new ReportsController(this);
        settingsCtrl = new SettingsController(this);
        strategyCtrl = new StudyStrategyController(this);

        if (currentRole.canAccessAnalytics()) {
            analyticsCtrl = new AnalyticsController(this);
        }
        if (currentRole.canAccessSimulation()) {
            simulationCtrl = new SimulationController(this);
        }
        if (currentRole.canEditData()) {
            manageCtrl = new ManageController(this);
        }
        if (currentRole.canAccessAdmin()) {
            adminCtrl = new AdminController(this);
        }

        // Load all pages into content area (all created, only one visible at a time)
        addPage(dashboardCtrl.buildPage(), PAGE_DASHBOARD);
        if (currentRole != UserRole.STUDENT) {
            addPage(studentsCtrl.buildPage(), PAGE_STUDENTS);
        }
        addPage(subjectsCtrl.buildPage(), PAGE_SUBJECTS);
        addPage(reportsCtrl.buildPage(), PAGE_REPORTS);
        if (analyticsCtrl != null)
            addPage(analyticsCtrl.buildPage(), PAGE_ANALYTICS);
        if (simulationCtrl != null)
            addPage(simulationCtrl.buildPage(), PAGE_SIMULATION);
        if (manageCtrl != null)
            addPage(manageCtrl.buildPage(), PAGE_MANAGE);
        if (adminCtrl != null)
            addPage(adminCtrl.buildPage(), PAGE_ADMIN);
        addPage(strategyCtrl.buildPage(), PAGE_STRATEGY);
        addPage(settingsCtrl.buildPage(), PAGE_SETTINGS);

        // Show dashboard by default
        showPage(PAGE_DASHBOARD);

        return new Scene(root, 1280, 820);
    }

    private void addPage(Region page, String name) {
        page.setUserData(name);
        page.setVisible(false);
        page.setManaged(false);
        contentArea.getChildren().add(page);
    }

    void showPage(String name) {
        for (var child : contentArea.getChildren()) {
            boolean match = name.equals(child.getUserData());
            child.setVisible(match);
            child.setManaged(match);
        }
        // Refresh data when switching pages
        if (PAGE_DASHBOARD.equals(name))
            dashboardCtrl.refresh();
        else if (PAGE_SUBJECTS.equals(name))
            subjectsCtrl.refresh();
        else if (PAGE_REPORTS.equals(name))
            reportsCtrl.refresh();
        else if (PAGE_ANALYTICS.equals(name) && analyticsCtrl != null)
            analyticsCtrl.refresh();
        else if (PAGE_SIMULATION.equals(name) && simulationCtrl != null)
            simulationCtrl.refresh();
        else if (PAGE_MANAGE.equals(name) && manageCtrl != null)
            manageCtrl.refresh();
        else if (PAGE_STUDENTS.equals(name))
            studentsCtrl.refresh();

        // Highlight sidebar
        for (Label item : sidebarItems) {
            boolean sel = item.getUserData() != null && item.getUserData().equals(name);
            item.setStyle(FxStyles.sidebarItem(sel));
            selectedItem = sel ? item : selectedItem;
        }
    }

    // ==========================================
    // SIDEBAR
    // ==========================================

    private VBox buildSidebar() {
        VBox sidebar = new VBox(0);
        sidebar.setStyle(FxStyles.SIDEBAR_STYLE);
        sidebar.setPrefWidth(220);
        sidebar.setMinWidth(220);
        sidebar.setMaxWidth(220);

        // Brand area
        VBox brand = new VBox(4);
        brand.setStyle(FxStyles.SIDEBAR_BRAND_STYLE);
        brand.setMaxWidth(Double.MAX_VALUE);

        Label brandLabel = new Label("◆  ALIP");
        brandLabel.setStyle(
                "-fx-font-size: 18; -fx-font-weight: bold; -fx-font-family: 'Segoe UI'; -fx-text-fill: white;");
        Label verLabel = new Label("v2.0 Intelligence Platform");
        verLabel.setStyle("-fx-font-size: 11; -fx-font-family: 'Segoe UI'; -fx-text-fill: #94A3B8;");
        brand.getChildren().addAll(brandLabel, verLabel);
        sidebar.getChildren().add(brand);

        // Spacer
        Region spacer1 = new Region();
        spacer1.setPrefHeight(12);
        sidebar.getChildren().add(spacer1);

        // Navigation items
        addSidebarItem(sidebar, "⌂  " + PAGE_DASHBOARD, PAGE_DASHBOARD);
        if (currentRole != UserRole.STUDENT) {
            addSidebarItem(sidebar, "♙  " + PAGE_STUDENTS, PAGE_STUDENTS);
        }
        addSidebarItem(sidebar, "☰  " + PAGE_SUBJECTS, PAGE_SUBJECTS);
        addSidebarItem(sidebar, "⚑  " + PAGE_REPORTS, PAGE_REPORTS);
        if (currentRole.canAccessAnalytics()) {
            addSidebarItem(sidebar, "≡  " + PAGE_ANALYTICS, PAGE_ANALYTICS);
        }
        if (currentRole.canAccessSimulation()) {
            addSidebarItem(sidebar, "⚙  " + PAGE_SIMULATION, PAGE_SIMULATION);
        }
        if (currentRole.canEditData()) {
            addSidebarItem(sidebar, "✎  " + PAGE_MANAGE, PAGE_MANAGE);
        }
        if (currentRole.canAccessAdmin()) {
            addSidebarItem(sidebar, "⚒  " + PAGE_ADMIN, PAGE_ADMIN);
        }
        if (currentRole == UserRole.STUDENT) {
            addSidebarItem(sidebar, "★  " + PAGE_STRATEGY, PAGE_STRATEGY);
        }
        addSidebarItem(sidebar, "⚙  " + PAGE_SETTINGS, PAGE_SETTINGS);

        // Filler
        Region filler = new Region();
        VBox.setVgrow(filler, Priority.ALWAYS);
        sidebar.getChildren().add(filler);

        // Role badge at bottom
        VBox roleBox = new VBox(4);
        roleBox.setStyle(FxStyles.SIDEBAR_BRAND_STYLE);
        roleBox.setMaxWidth(Double.MAX_VALUE);

        Label roleLabel = new Label("• " + currentRole.getLabel());
        roleLabel.setStyle("-fx-font-size: 12; -fx-font-family: 'Segoe UI'; -fx-text-fill: #22C55E;");
        Label userLabel = new Label(currentUsername);
        userLabel.setStyle("-fx-font-size: 13; -fx-font-family: 'Segoe UI'; -fx-text-fill: #94A3B8;");
        roleBox.getChildren().addAll(roleLabel, userLabel);
        sidebar.getChildren().add(roleBox);

        return sidebar;
    }

    private void addSidebarItem(VBox sidebar, String text, String pageName) {
        Label item = new Label(text);
        item.setStyle(FxStyles.sidebarItem(false));
        item.setUserData(pageName);
        item.setMaxWidth(Double.MAX_VALUE);

        item.setOnMouseEntered(e -> {
            if (!pageName.equals(item.getUserData() == selectedItem ? selectedItem.getUserData() : null)) {
                item.setStyle(
                        FxStyles.sidebarItem(false).replace("background-color: transparent",
                                "background-color: #2D3A4F"));
            }
        });
        item.setOnMouseExited(e -> {
            boolean isSelected = selectedItem == item;
            item.setStyle(FxStyles.sidebarItem(isSelected));
        });
        item.setOnMouseClicked(e -> showPage(pageName));

        sidebarItems.add(item);
        sidebar.getChildren().add(item);
    }

    // ==========================================
    // HEADER
    // ==========================================

    private HBox buildHeader() {
        HBox header = new HBox(12);
        header.setStyle(FxStyles.HEADER_STYLE);
        header.setPrefHeight(60);
        header.setMinHeight(60);
        header.setMaxHeight(60);
        header.setAlignment(Pos.CENTER_LEFT);

        // Left: title + subtitle
        VBox left = new VBox(2);
        left.setAlignment(Pos.CENTER_LEFT);
        Label titleLabel = new Label("ALIP — Adaptive Learning Intelligence Platform");
        titleLabel.setStyle(
                "-fx-font-size: 18; -fx-font-weight: bold; -fx-font-family: 'Segoe UI'; -fx-text-fill: #1E293B;");
        Label subtitleLabel = new Label("AI-powered Academic Risk Intelligence System");
        subtitleLabel.setStyle("-fx-font-size: 12; -fx-font-family: 'Segoe UI'; -fx-text-fill: #64748B;");
        left.getChildren().addAll(titleLabel, subtitleLabel);
        HBox.setHgrow(left, Priority.ALWAYS);

        // Right: role info + logout
        Label roleDisplay = new Label("Logged in as: " + currentRole.getLabel() + " (" + currentUsername + ")");
        roleDisplay.setStyle("-fx-font-size: 12; -fx-font-family: 'Segoe UI'; -fx-text-fill: #64748B;");

        Button btnLogout = new Button("Logout");
        btnLogout.setStyle(FxStyles.dangerButton());
        btnLogout.setOnAction(e -> handleLogout());

        header.getChildren().addAll(left, roleDisplay, btnLogout);
        return header;
    }

    private void handleLogout() {
        SessionManager.logout();
        LoginController loginCtrl = new LoginController(stage);
        stage.setScene(loginCtrl.buildScene());
        stage.setResizable(false);
        stage.setWidth(520);
        stage.setHeight(620);
        stage.centerOnScreen();
    }

    // ==========================================
    // HELPERS
    // ==========================================

    /**
     * Returns the linked student ID from the current session, or null for
     * teacher/admin.
     */
    String sessionLinkedId() {
        return tracker.security.SessionManager.getLinkedStudentId();
    }

    /** Shows a JavaFX alert dialog. */
    void showInfo(String msg) {
        AlertDialog.showInfo("Information", msg);
    }

    void showError(String msg) {
        AlertDialog.showError("Error", msg);
    }
}
