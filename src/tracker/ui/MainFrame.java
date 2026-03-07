package tracker.ui;

import tracker.data.DataManager;
import tracker.data.DataMigration;
import tracker.data.FileManager;
import tracker.data.dao.ConfigDAO;
import tracker.data.dao.UserDAO;
import tracker.model.*;
import tracker.model.RiskScore.Level;
import tracker.security.PasswordHasher;
import tracker.security.SessionManager;
import tracker.service.RecommendationEngine;
import tracker.service.AnalyticsService;
import tracker.service.SimulationService;
import tracker.service.ai.AdaptivePlanner;
import tracker.service.ai.RiskPredictor;
import tracker.service.ai.TrendAnalyzer;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Main application window for the Adaptive Learning Intelligence Platform.
 *
 * UPGRADED: Modern SaaS-style dashboard with:
 * - Left sidebar navigation (replaces JTabbedPane)
 * - Top header bar with role display and logout
 * - CardLayout center content area
 * - Dashboard metric cards
 * - Role-based access control
 * - Modern table renderers with badges and arrows
 *
 * All business logic remains delegated to service classes.
 */
public class MainFrame extends JFrame {

    // --- Role & Session ---
    private final UserRole currentRole;
    private final String currentUsername;

    // --- Core Dependencies ---
    private final DataManager dataManager;
    private final FileManager fileManager;
    private final RecommendationEngine recommendationEngine;

    // --- AI Dependencies ---
    private final TrendAnalyzer trendAnalyzer;
    private final RiskPredictor riskPredictor;
    private final AdaptivePlanner adaptivePlanner;
    private final AnalyticsService analyticsService;
    private final SimulationService simulationService;

    // --- Layout ---
    private JPanel contentPanel;
    private CardLayout cardLayout;
    private JPanel sidebarPanel;
    private JLabel selectedSidebarItem;

    // --- Sidebar item labels (for highlight management) ---
    private final java.util.List<JLabel> sidebarItems = new ArrayList<>();

    // --- Students Page Components ---
    private JTable studentTable;
    private DefaultTableModel studentTableModel;

    // --- Subjects Page Components ---
    private JComboBox<String> cmbStudentSelector;
    private JTextArea txtWeakSubjects, txtRecommendations;

    // --- Reports Page Components ---
    private JTable reportTable;
    private DefaultTableModel reportTableModel;
    private JTextArea txtReportDetails;

    // --- Analytics Page Components ---
    private JTextArea txtAnalytics;

    // --- Simulation Page Components ---
    private JComboBox<String> cmbSimStudent, cmbSimSubject;
    private JTextField txtSimDelta;
    private JTextArea txtSimResult;

    // --- Admin Page Components ---
    private JTextField txtAdminSubject;
    private JComboBox<String> cmbAdminCategory;
    private JTextArea txtCategoryMappings;

    // --- Admin User Management Components ---
    private JTextField txtNewUsername, txtNewPassword;
    private JComboBox<String> cmbNewUserRole;
    private JTextField txtLinkStudentId;
    private JTable userTable;
    private DefaultTableModel userTableModel;

    // --- Admin Config Components ---
    private JTextField txtConfigValue;
    private JComboBox<String> cmbConfigKey;

    // --- Dashboard Metric Labels ---
    private JLabel metricTotalStudents, metricHighRisk, metricAverage, metricWeakSubject;

    // --- Student Profile Dashboard (STUDENT role only) ---
    private JLabel profileAvg, profileRisk, profileTrend, profileSubjectCount;
    private JTextArea txtProfileSubjects, txtProfileRecs;

    // --- Manage Students Page Components (Teacher CRUD) ---
    private JTable manageTable;
    private DefaultTableModel manageTableModel;

    // --- Page name constants ---
    private static final String PAGE_DASHBOARD = "Dashboard";
    private static final String PAGE_STUDENTS = "Students";
    private static final String PAGE_SUBJECTS = "Subjects";
    private static final String PAGE_REPORTS = "Reports";
    private static final String PAGE_ANALYTICS = "Analytics";
    private static final String PAGE_SIMULATION = "Simulation";
    private static final String PAGE_ADMIN = "Admin";
    private static final String PAGE_MANAGE = "Manage";
    private static final String PAGE_SETTINGS = "Settings";

    /**
     * Constructs MainFrame with role-based access.
     */
    public MainFrame(UserRole role, String username) {
        this.currentRole = role;
        this.currentUsername = username;

        this.dataManager = new DataManager();
        this.fileManager = new FileManager();
        this.recommendationEngine = new RecommendationEngine();
        this.trendAnalyzer = new TrendAnalyzer();
        this.riskPredictor = new RiskPredictor(trendAnalyzer);
        this.adaptivePlanner = new AdaptivePlanner();
        this.analyticsService = new AnalyticsService(riskPredictor, trendAnalyzer);
        this.simulationService = new SimulationService(riskPredictor, trendAnalyzer, adaptivePlanner);

        initializeFrame();
        buildUI();
    }

    private void initializeFrame() {
        setTitle("ALIP - Adaptive Learning Intelligence Platform");
        setSize(1280, 820);
        setMinimumSize(new Dimension(1000, 700));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
    }

    // ==========================================
    // MAIN LAYOUT: SIDEBAR | HEADER + CONTENT
    // ==========================================

    private void buildUI() {
        JPanel root = new JPanel(new BorderLayout(0, 0));
        root.setBackground(StyleConstants.BACKGROUND);

        // WEST: Sidebar
        root.add(buildSidebar(), BorderLayout.WEST);

        // CENTER: Header + Content
        JPanel rightPanel = new JPanel(new BorderLayout(0, 0));
        rightPanel.setBackground(StyleConstants.BACKGROUND);
        rightPanel.add(buildHeader(), BorderLayout.NORTH);

        // Content area with CardLayout
        cardLayout = new CardLayout();
        contentPanel = new JPanel(cardLayout);
        contentPanel.setBackground(StyleConstants.BACKGROUND);

        // Role-based dashboard: STUDENT gets personal profile, others get institutional
        // overview
        contentPanel.add(buildDashboardPage(), PAGE_DASHBOARD);
        contentPanel.add(buildStudentsPage(), PAGE_STUDENTS);
        contentPanel.add(buildSubjectsPage(), PAGE_SUBJECTS);
        contentPanel.add(buildReportsPage(), PAGE_REPORTS);

        if (currentRole.canAccessAnalytics()) {
            contentPanel.add(buildAnalyticsPage(), PAGE_ANALYTICS);
        }
        if (currentRole.canAccessSimulation()) {
            contentPanel.add(buildSimulationPage(), PAGE_SIMULATION);
        }
        // Teacher/Admin: Manage Students page (edit/delete students)
        if (currentRole.canEditData()) {
            contentPanel.add(buildManageStudentsPage(), PAGE_MANAGE);
        }
        if (currentRole.canAccessAdmin()) {
            contentPanel.add(buildAdminPage(), PAGE_ADMIN);
        }
        // Settings page (password change) — all roles
        contentPanel.add(buildSettingsPage(), PAGE_SETTINGS);

        rightPanel.add(contentPanel, BorderLayout.CENTER);
        root.add(rightPanel, BorderLayout.CENTER);

        setContentPane(root);

        // Show dashboard by default
        showPage(PAGE_DASHBOARD);
    }

    // ==========================================
    // SIDEBAR (Phase 1 + Phase 9 Icons)
    // ==========================================

    private JPanel buildSidebar() {
        sidebarPanel = new JPanel();
        sidebarPanel.setLayout(new BoxLayout(sidebarPanel, BoxLayout.Y_AXIS));
        sidebarPanel.setBackground(StyleConstants.SIDEBAR_BG);
        sidebarPanel.setPreferredSize(new Dimension(StyleConstants.SIDEBAR_WIDTH, 0));

        // Brand area
        JPanel brandPanel = new JPanel();
        brandPanel.setLayout(new BoxLayout(brandPanel, BoxLayout.Y_AXIS));
        brandPanel.setBackground(StyleConstants.SIDEBAR_BRAND_BG);
        brandPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        brandPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));

        JLabel brandLabel = new JLabel("\u25C6  ALIP");
        brandLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        brandLabel.setForeground(Color.WHITE);
        brandLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        brandPanel.add(brandLabel);

        JLabel versionLabel = new JLabel("v2.0 Intelligence Platform");
        versionLabel.setFont(StyleConstants.SMALL_FONT);
        versionLabel.setForeground(StyleConstants.SIDEBAR_FG);
        versionLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        brandPanel.add(versionLabel);

        sidebarPanel.add(brandPanel);
        sidebarPanel.add(Box.createVerticalStrut(12));

        // Navigation items with Unicode icons
        addSidebarItem("\u2302  " + PAGE_DASHBOARD, PAGE_DASHBOARD); // House
        addSidebarItem("\u2659  " + PAGE_STUDENTS, PAGE_STUDENTS); // Person
        addSidebarItem("\u2630  " + PAGE_SUBJECTS, PAGE_SUBJECTS); // Menu lines
        addSidebarItem("\u2691  " + PAGE_REPORTS, PAGE_REPORTS); // Flag

        if (currentRole.canAccessAnalytics()) {
            addSidebarItem("\u2261  " + PAGE_ANALYTICS, PAGE_ANALYTICS); // Lines
        }
        if (currentRole.canAccessSimulation()) {
            addSidebarItem("\u2699  " + PAGE_SIMULATION, PAGE_SIMULATION); // Gear
        }
        if (currentRole.canEditData()) {
            addSidebarItem("\u270E  " + PAGE_MANAGE, PAGE_MANAGE); // Pencil — manage students
        }
        if (currentRole.canAccessAdmin()) {
            addSidebarItem("\u2692  " + PAGE_ADMIN, PAGE_ADMIN); // Hammer
        }
        addSidebarItem("\u2638  " + PAGE_SETTINGS, PAGE_SETTINGS); // Settings wheel

        sidebarPanel.add(Box.createVerticalGlue());

        // Role badge at bottom
        JPanel rolePanel = new JPanel();
        rolePanel.setLayout(new BoxLayout(rolePanel, BoxLayout.Y_AXIS));
        rolePanel.setBackground(StyleConstants.SIDEBAR_BRAND_BG);
        rolePanel.setBorder(BorderFactory.createEmptyBorder(12, 20, 16, 20));
        rolePanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));

        JLabel roleLabel = new JLabel("\u2022 " + currentRole.getLabel());
        roleLabel.setFont(StyleConstants.SMALL_FONT);
        roleLabel.setForeground(StyleConstants.ACCENT_GREEN);
        roleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        rolePanel.add(roleLabel);

        JLabel userLabel = new JLabel(currentUsername);
        userLabel.setFont(StyleConstants.SIDEBAR_FONT);
        userLabel.setForeground(StyleConstants.SIDEBAR_FG);
        userLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        rolePanel.add(userLabel);

        sidebarPanel.add(rolePanel);

        return sidebarPanel;
    }

    private void addSidebarItem(String displayText, String pageName) {
        JLabel item = new JLabel(displayText);
        item.setFont(StyleConstants.SIDEBAR_FONT);
        item.setForeground(StyleConstants.SIDEBAR_FG);
        item.setOpaque(true);
        item.setBackground(StyleConstants.SIDEBAR_BG);
        item.setBorder(BorderFactory.createEmptyBorder(12, 20, 12, 20));
        item.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        item.setCursor(new Cursor(Cursor.HAND_CURSOR));
        item.setAlignmentX(Component.LEFT_ALIGNMENT);

        item.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (item != selectedSidebarItem) {
                    item.setBackground(StyleConstants.SIDEBAR_HOVER_BG);
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                if (item != selectedSidebarItem) {
                    item.setBackground(StyleConstants.SIDEBAR_BG);
                }
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                showPage(pageName);
                highlightSidebarItem(item);
            }
        });

        sidebarItems.add(item);
        sidebarPanel.add(item);
    }

    private void highlightSidebarItem(JLabel item) {
        for (JLabel si : sidebarItems) {
            si.setBackground(StyleConstants.SIDEBAR_BG);
            si.setForeground(StyleConstants.SIDEBAR_FG);
            si.setFont(StyleConstants.SIDEBAR_FONT);
        }
        item.setBackground(StyleConstants.SIDEBAR_SELECTED_BG);
        item.setForeground(StyleConstants.SIDEBAR_SELECTED_FG);
        item.setFont(StyleConstants.SIDEBAR_FONT_SELECTED);
        selectedSidebarItem = item;
    }

    private void showPage(String pageName) {
        cardLayout.show(contentPanel, pageName);
        // Refresh data for specific pages
        if (PAGE_DASHBOARD.equals(pageName))
            refreshDashboardMetrics();
        else if (PAGE_SUBJECTS.equals(pageName))
            refreshStudentSelector();
        else if (PAGE_ANALYTICS.equals(pageName))
            handleRefreshAnalytics();
        else if (PAGE_SIMULATION.equals(pageName))
            refreshSimulationSelectors();
        else if (PAGE_MANAGE.equals(pageName))
            refreshManageTable();

        // Highlight correct sidebar item
        for (JLabel si : sidebarItems) {
            if (si.getText().contains(pageName)) {
                highlightSidebarItem(si);
                break;
            }
        }
    }

    // ==========================================
    // HEADER BAR (Phase 2)
    // ==========================================

    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(StyleConstants.HEADER_BG);
        header.setPreferredSize(new Dimension(0, StyleConstants.HEADER_HEIGHT));
        header.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, StyleConstants.HEADER_BORDER),
                BorderFactory.createEmptyBorder(0, 24, 0, 24)));

        // Left: Title
        JPanel leftPanel = new JPanel();
        leftPanel.setOpaque(false);
        leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.Y_AXIS));
        leftPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));

        JLabel title = new JLabel("ALIP \u2014 Adaptive Learning Intelligence Platform");
        title.setFont(StyleConstants.HEADER_TITLE_FONT);
        title.setForeground(StyleConstants.TEXT_FG);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        leftPanel.add(title);

        JLabel subtitle = new JLabel("AI-powered Academic Risk Intelligence System");
        subtitle.setFont(StyleConstants.HEADER_SUBTITLE_FONT);
        subtitle.setForeground(StyleConstants.TEXT_SECONDARY);
        subtitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        leftPanel.add(subtitle);

        header.add(leftPanel, BorderLayout.WEST);

        // Right: Role + Logout
        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        rightPanel.setOpaque(false);
        rightPanel.setBorder(BorderFactory.createEmptyBorder(12, 0, 12, 0));

        JLabel roleDisplay = new JLabel("Logged in as: " + currentRole.getLabel() + " (" + currentUsername + ")");
        roleDisplay.setFont(StyleConstants.SMALL_FONT);
        roleDisplay.setForeground(StyleConstants.TEXT_SECONDARY);
        rightPanel.add(roleDisplay);

        StyledButton btnLogout = new StyledButton("Logout");
        btnLogout.setFont(StyleConstants.BUTTON_FONT);
        btnLogout.setBackground(new Color(0xEF4444));
        btnLogout.setForeground(Color.WHITE);
        btnLogout.setPreferredSize(new Dimension(90, 34));
        btnLogout.addActionListener(e -> handleLogout());
        rightPanel.add(btnLogout);

        header.add(rightPanel, BorderLayout.EAST);
        return header;
    }

    private void handleLogout() {
        SessionManager.logout();
        dispose();
        SwingUtilities.invokeLater(() -> {
            LoginFrame login = new LoginFrame();
            login.setVisible(true);
        });
    }

    // ==========================================
    // DASHBOARD PAGE — Role-Based Routing
    // STUDENT: Personal profile dashboard
    // TEACHER/ADMIN: Institutional overview
    // ==========================================

    private JPanel buildDashboardPage() {
        if (currentRole == UserRole.STUDENT) {
            return buildStudentDashboard();
        } else if (currentRole == UserRole.TEACHER) {
            return buildTeacherDashboard();
        } else {
            return buildAdminDashboard();
        }
    }

    /**
     * STUDENT DASHBOARD — Personal profile view.
     * Shows: name, ID, average, risk, trend, all subjects, AI recommendations.
     */
    private JPanel buildStudentDashboard() {
        JPanel page = new JPanel(new BorderLayout(0, StyleConstants.GAP_V));
        page.setOpaque(false);
        page.setBorder(StyleConstants.PANEL_PADDING);

        JLabel pageTitle = new JLabel("My Academic Profile");
        pageTitle.setFont(StyleConstants.TITLE_FONT);
        pageTitle.setForeground(StyleConstants.TEXT_FG);
        page.add(pageTitle, BorderLayout.NORTH);

        JPanel body = new JPanel(new BorderLayout(0, StyleConstants.GAP_V));
        body.setOpaque(false);

        // Top: 4 Personal Metric Cards
        JPanel metricsRow = new JPanel(new GridLayout(1, 4, StyleConstants.GAP_H, 0));
        metricsRow.setOpaque(false);
        metricsRow.setPreferredSize(new Dimension(0, 130));

        profileAvg = new JLabel("--");
        metricsRow.add(buildMetricCard("My Average", profileAvg,
                StyleConstants.PRIMARY, StyleConstants.PRIMARY_LIGHT));

        profileRisk = new JLabel("--");
        metricsRow.add(buildMetricCard("Risk Level", profileRisk,
                StyleConstants.ACCENT_RED, StyleConstants.ACCENT_RED_BG));

        profileTrend = new JLabel("--");
        metricsRow.add(buildMetricCard("Trend", profileTrend,
                StyleConstants.ACCENT_GREEN, StyleConstants.ACCENT_GREEN_BG));

        profileSubjectCount = new JLabel("0");
        metricsRow.add(buildMetricCard("Subjects", profileSubjectCount,
                StyleConstants.ACCENT_ORANGE, StyleConstants.ACCENT_ORANGE_BG));

        body.add(metricsRow, BorderLayout.NORTH);

        // Center: subjects + recommendations side by side
        JPanel centerPanel = new JPanel(new GridLayout(1, 2, StyleConstants.GAP_H, 0));
        centerPanel.setOpaque(false);

        DashboardCard subjectCard = new DashboardCard("My Subjects & Scores");
        txtProfileSubjects = createTextArea();
        subjectCard.setContent(new JScrollPane(txtProfileSubjects));
        centerPanel.add(subjectCard);

        DashboardCard recCard = new DashboardCard("AI Recommendations for Me");
        txtProfileRecs = createTextArea();
        recCard.setContent(new JScrollPane(txtProfileRecs));
        centerPanel.add(recCard);

        body.add(centerPanel, BorderLayout.CENTER);

        page.add(body, BorderLayout.CENTER);
        return page;
    }

    /**
     * TEACHER DASHBOARD — Institutional overview with class-level metrics.
     */
    private JPanel buildTeacherDashboard() {
        return buildInstitutionalDashboard("Teacher Dashboard",
                "Welcome, " + currentUsername + "!\n\n"
                        + "You have full access to student data, analytics, simulation, and reports.\n"
                        + "Use the 'Manage' page to edit or delete student records.\n"
                        + "Use the 'Students' page to add new student scores.\n\n"
                        + "Quick tip: Check the Reports page for at-risk students.");
    }

    /**
     * ADMIN DASHBOARD — Institutional overview with system-level context.
     */
    private JPanel buildAdminDashboard() {
        return buildInstitutionalDashboard("Admin Dashboard",
                "Welcome, " + currentUsername + "!\n\n"
                        + "Full system administration access enabled.\n"
                        + "Use the Admin page to manage users, roles, and system configuration.\n"
                        + "Use the 'Manage' page to edit or delete student records.\n\n"
                        + "System: SQLite DB active  |  Role: " + currentRole.getLabel());
    }

    /**
     * Shared institutional dashboard builder for TEACHER and ADMIN roles.
     */
    private JPanel buildInstitutionalDashboard(String titleText, String summaryText) {
        JPanel page = new JPanel(new BorderLayout(0, StyleConstants.GAP_V));
        page.setOpaque(false);
        page.setBorder(StyleConstants.PANEL_PADDING);

        JLabel pageTitle = new JLabel(titleText);
        pageTitle.setFont(StyleConstants.TITLE_FONT);
        pageTitle.setForeground(StyleConstants.TEXT_FG);
        page.add(pageTitle, BorderLayout.NORTH);

        JPanel body = new JPanel(new BorderLayout(0, StyleConstants.GAP_V));
        body.setOpaque(false);

        // Top: 4 Metric Summary Cards
        JPanel metricsRow = new JPanel(new GridLayout(1, 4, StyleConstants.GAP_H, 0));
        metricsRow.setOpaque(false);
        metricsRow.setPreferredSize(new Dimension(0, 130));

        metricTotalStudents = new JLabel("0");
        metricsRow.add(buildMetricCard("Total Students", metricTotalStudents,
                StyleConstants.PRIMARY, StyleConstants.PRIMARY_LIGHT));

        metricHighRisk = new JLabel("0");
        metricsRow.add(buildMetricCard("High Risk", metricHighRisk,
                StyleConstants.ACCENT_RED, StyleConstants.ACCENT_RED_BG));

        metricAverage = new JLabel("0.0");
        metricsRow.add(buildMetricCard("Institutional Avg", metricAverage,
                StyleConstants.ACCENT_GREEN, StyleConstants.ACCENT_GREEN_BG));

        metricWeakSubject = new JLabel("N/A");
        metricsRow.add(buildMetricCard("Most Weak Subject", metricWeakSubject,
                StyleConstants.ACCENT_ORANGE, StyleConstants.ACCENT_ORANGE_BG));

        body.add(metricsRow, BorderLayout.NORTH);

        DashboardCard summaryCard = new DashboardCard("Quick Summary");
        JTextArea txtSummary = createTextArea();
        txtSummary.setText(summaryText);
        summaryCard.setContent(new JScrollPane(txtSummary));
        body.add(summaryCard, BorderLayout.CENTER);

        page.add(body, BorderLayout.CENTER);
        return page;
    }

    private JPanel buildMetricCard(String label, JLabel valueLabel,
            Color accentColor, Color bgTint) {
        JPanel card = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int r = StyleConstants.CARD_RADIUS;

                // Shadow
                g2.setColor(new Color(0, 0, 0, 15));
                g2.fillRoundRect(2, 3, getWidth() - 4, getHeight() - 4, r, r);

                // Card background
                g2.setColor(StyleConstants.CARD_BG);
                g2.fillRoundRect(0, 0, getWidth() - 3, getHeight() - 3, r, r);

                // Top accent bar
                g2.setColor(accentColor);
                g2.fillRoundRect(0, 0, getWidth() - 3, 4, 4, 4);

                g2.dispose();
                super.paintComponent(g);
            }
        };
        card.setOpaque(false);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createEmptyBorder(20, 24, 16, 24));

        // Large number
        valueLabel.setFont(StyleConstants.METRIC_NUMBER_FONT);
        valueLabel.setForeground(accentColor);
        valueLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(valueLabel);

        card.add(Box.createVerticalStrut(4));

        // Label
        JLabel descLabel = new JLabel(label);
        descLabel.setFont(StyleConstants.METRIC_LABEL_FONT);
        descLabel.setForeground(StyleConstants.TEXT_SECONDARY);
        descLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(descLabel);

        return card;
    }

    private void refreshDashboardMetrics() {
        if (currentRole == UserRole.STUDENT) {
            refreshStudentProfile();
            return;
        }

        // TEACHER/ADMIN: institutional metrics
        List<Student> students = getVisibleStudents();
        metricTotalStudents.setText(String.valueOf(students.size()));

        if (students.isEmpty()) {
            metricHighRisk.setText("0");
            metricAverage.setText("0.0");
            metricWeakSubject.setText("N/A");
            return;
        }

        Map<Level, Integer> dist = analyticsService.getRiskDistribution(students);
        int highCount = dist.getOrDefault(Level.HIGH, 0);
        metricHighRisk.setText(String.valueOf(highCount));

        double avg = analyticsService.getInstitutionalAverage(students);
        metricAverage.setText(String.format("%.1f", avg));

        String weakSub = analyticsService.getMostCommonWeakSubject(students);
        metricWeakSubject.setText(weakSub.length() > 10 ? weakSub.substring(0, 10) : weakSub);
    }

    /**
     * Refreshes the Student profile dashboard with personal data.
     */
    private void refreshStudentProfile() {
        List<Student> visible = getVisibleStudents();
        if (visible.isEmpty()) {
            if (profileAvg != null)
                profileAvg.setText("--");
            if (profileRisk != null)
                profileRisk.setText("--");
            if (profileTrend != null)
                profileTrend.setText("--");
            if (profileSubjectCount != null)
                profileSubjectCount.setText("0");
            if (txtProfileSubjects != null)
                txtProfileSubjects.setText(
                        "No linked student data.\n\nAsk your administrator to link your account to a student record.");
            if (txtProfileRecs != null)
                txtProfileRecs.setText("");
            return;
        }

        Student student = visible.get(0);
        RiskScore risk = riskPredictor.assessRisk(student);
        TrendDirection trend = trendAnalyzer.getTrend(student.getId());

        if (profileAvg != null)
            profileAvg.setText(String.format("%.1f", student.getAverageScore()));
        if (profileRisk != null)
            profileRisk.setText(risk.getLevel().getLabel());
        if (profileTrend != null)
            profileTrend.setText(trend.getLabel());
        if (profileSubjectCount != null)
            profileSubjectCount.setText(String.valueOf(student.getSubjects().size()));

        // Subject details
        if (txtProfileSubjects != null) {
            StringBuilder sb = new StringBuilder();
            sb.append("Student: ").append(student.getName()).append(" (").append(student.getId()).append(")\n");
            sb.append("Average: ").append(String.format("%.2f", student.getAverageScore())).append("\n");
            sb.append("Risk: ").append(risk.getLevel().getLabel())
                    .append(" (").append(String.format("%.1f", risk.getNumericScore())).append("/100)\n");
            sb.append("Trend: ").append(trend.getLabel()).append(" ").append(trend.getArrow()).append("\n\n");
            sb.append("--- All Subjects ---\n");
            for (Subject sub : student.getSubjects()) {
                SubjectCategory cat = SubjectCategory.categorize(sub.getSubjectName());
                String marker = sub.getScore() < 60 ? " [WEAK]" : "";
                sb.append(String.format("  %-18s : %6.2f  [%s]%s\n",
                        sub.getSubjectName(), sub.getScore(), cat.name(), marker));
            }
            List<Subject> weak = student.getWeakSubjects();
            if (!weak.isEmpty()) {
                sb.append("\n--- Weak Subjects ---\n");
                for (Subject w : weak) {
                    sb.append(String.format("  %-18s : %6.2f\n", w.getSubjectName(), w.getScore()));
                }
            }
            sb.append("\n").append(risk.getDetailedBreakdown());
            txtProfileSubjects.setText(sb.toString());
            txtProfileSubjects.setCaretPosition(0);
        }

        // AI Recommendations
        if (txtProfileRecs != null) {
            List<String> recs = adaptivePlanner.generateRecommendations(student, risk, trend);
            StringBuilder sr = new StringBuilder();
            sr.append("Personalized AI Recommendations\n");
            sr.append("================================\n\n");
            for (String r : recs)
                sr.append(r).append("\n");
            txtProfileRecs.setText(sr.toString());
            txtProfileRecs.setCaretPosition(0);
        }
    }

    // ==========================================
    // STUDENTS PAGE (preserved logic, card UI)
    // ==========================================

    private JPanel buildStudentsPage() {
        JPanel page = new JPanel(new BorderLayout(0, StyleConstants.GAP_V));
        page.setOpaque(false);
        page.setBorder(StyleConstants.PANEL_PADDING);

        JLabel pageTitle = new JLabel("Student Management");
        pageTitle.setFont(StyleConstants.TITLE_FONT);
        pageTitle.setForeground(StyleConstants.TEXT_FG);
        page.add(pageTitle, BorderLayout.NORTH);

        JPanel body = new JPanel(new BorderLayout(0, StyleConstants.GAP_V));
        body.setOpaque(false);

        // Table card
        DashboardCard tableCard = new DashboardCard("Student Records");
        String[] columns = { "ID", "Name", "Average", "Risk Level", "Risk Score", "Trend" };
        studentTableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };
        studentTable = new JTable(studentTableModel);
        setupModernTable(studentTable);
        studentTable.getColumnModel().getColumn(3).setCellRenderer(new RiskBadgeRenderer());
        studentTable.getColumnModel().getColumn(5).setCellRenderer(new TrendArrowRenderer());

        JScrollPane scroll = new JScrollPane(studentTable);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(Color.WHITE);
        tableCard.setContent(scroll);
        body.add(tableCard, BorderLayout.CENTER);

        return page;
    }

    private void refreshStudentTable() {
        studentTableModel.setRowCount(0);
        for (Student s : getVisibleStudents()) {
            if (s.getSubjects().isEmpty())
                continue;
            RiskScore risk = riskPredictor.assessRisk(s);
            TrendDirection trend = trendAnalyzer.getTrend(s.getId());
            studentTableModel.addRow(new Object[] {
                    s.getId(), s.getName(),
                    String.format("%.2f", s.getAverageScore()),
                    risk.getLevel().getLabel(),
                    String.format("%.1f", risk.getNumericScore()),
                    trend.getLabel()
            });
        }
    }

    // ==========================================
    // SUBJECTS PAGE
    // ==========================================

    private JPanel buildSubjectsPage() {
        JPanel page = new JPanel(new BorderLayout(0, StyleConstants.GAP_V));
        page.setOpaque(false);
        page.setBorder(StyleConstants.PANEL_PADDING);

        JLabel pageTitle = new JLabel("Subject Analysis");
        pageTitle.setFont(StyleConstants.TITLE_FONT);
        pageTitle.setForeground(StyleConstants.TEXT_FG);
        page.add(pageTitle, BorderLayout.NORTH);

        JPanel body = new JPanel(new BorderLayout(0, StyleConstants.GAP_V));
        body.setOpaque(false);

        // Selector card
        DashboardCard selectorCard = new DashboardCard("Select Student");
        JPanel selectorPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        selectorPanel.setOpaque(false);
        cmbStudentSelector = new JComboBox<>();
        cmbStudentSelector.setFont(StyleConstants.BODY_FONT);
        cmbStudentSelector.setPreferredSize(new Dimension(300, StyleConstants.COMBO_HEIGHT));
        selectorPanel.add(cmbStudentSelector);

        StyledButton btnShow = new StyledButton("Show Details");
        btnShow.setFont(StyleConstants.BUTTON_FONT);
        btnShow.setBackground(StyleConstants.PRIMARY);
        btnShow.setForeground(Color.WHITE);
        btnShow.setPreferredSize(new Dimension(140, 36));
        btnShow.addActionListener(e -> handleShowSubjectDetails());
        selectorPanel.add(btnShow);
        selectorCard.setContent(selectorPanel);
        body.add(selectorCard, BorderLayout.NORTH);

        // Content cards
        JPanel centerPanel = new JPanel(new GridLayout(1, 2, StyleConstants.GAP_H, 0));
        centerPanel.setOpaque(false);

        DashboardCard weakCard = new DashboardCard("Subject Analysis");
        txtWeakSubjects = createTextArea();
        weakCard.setContent(new JScrollPane(txtWeakSubjects));
        centerPanel.add(weakCard);

        DashboardCard recCard = new DashboardCard("AI Recommendations");
        txtRecommendations = createTextArea();
        recCard.setContent(new JScrollPane(txtRecommendations));
        centerPanel.add(recCard);

        body.add(centerPanel, BorderLayout.CENTER);
        page.add(body, BorderLayout.CENTER);
        return page;
    }

    private void refreshStudentSelector() {
        cmbStudentSelector.removeAllItems();
        List<Student> students = getVisibleStudents();
        if (students.isEmpty()) {
            cmbStudentSelector.addItem("-- No students yet --");
        } else {
            for (Student s : students)
                cmbStudentSelector.addItem(s.getId() + " - " + s.getName());
        }
    }

    private void handleShowSubjectDetails() {
        List<Student> students = getVisibleStudents();
        if (students.isEmpty()) {
            txtWeakSubjects.setText("No students available.\nAdd students first.");
            txtRecommendations.setText("");
            return;
        }
        int idx = cmbStudentSelector.getSelectedIndex();
        if (idx < 0 || idx >= students.size()) {
            txtWeakSubjects.setText("Select a valid student.");
            txtRecommendations.setText("");
            return;
        }

        Student student = students.get(idx);
        RiskScore risk = riskPredictor.assessRisk(student);
        TrendDirection trend = trendAnalyzer.getTrend(student.getId());

        StringBuilder sb = new StringBuilder();
        sb.append("Student: ").append(student.getName()).append(" (").append(student.getId()).append(")\n");
        sb.append("Average: ").append(String.format("%.2f", student.getAverageScore())).append("\n");
        sb.append("Risk: ").append(risk.getLevel().getLabel()).append(" (")
                .append(String.format("%.1f", risk.getNumericScore())).append(")\n");
        sb.append("Trend: ").append(trend.getLabel()).append(" ").append(trend.getArrow()).append("\n\n");
        sb.append("--- All Subjects ---\n");
        for (Subject sub : student.getSubjects()) {
            SubjectCategory cat = SubjectCategory.categorize(sub.getSubjectName());
            String marker = sub.getScore() < 60 ? " [WEAK]" : "";
            sb.append(String.format("  %-18s : %6.2f  [%s]%s\n", sub.getSubjectName(), sub.getScore(), cat.name(),
                    marker));
        }
        List<Subject> weak = student.getWeakSubjects();
        if (weak.isEmpty())
            sb.append("\nNo weak subjects.");
        else {
            sb.append("\n--- Weak ---\n");
            for (Subject w : weak)
                sb.append(String.format("  %-18s : %6.2f\n", w.getSubjectName(), w.getScore()));
        }
        sb.append("\n").append(risk.getDetailedBreakdown());
        txtWeakSubjects.setText(sb.toString());
        txtWeakSubjects.setCaretPosition(0);

        List<String> recs = adaptivePlanner.generateRecommendations(student, risk, trend);
        StringBuilder sr = new StringBuilder();
        sr.append("AI Recommendations for ").append(student.getName()).append(":\n\n");
        for (String r : recs)
            sr.append(r).append("\n");
        txtRecommendations.setText(sr.toString());
        txtRecommendations.setCaretPosition(0);
    }

    // ==========================================
    // REPORTS PAGE
    // ==========================================

    private JPanel buildReportsPage() {
        JPanel page = new JPanel(new BorderLayout(0, StyleConstants.GAP_V));
        page.setOpaque(false);
        page.setBorder(StyleConstants.PANEL_PADDING);

        JLabel pageTitle = new JLabel("Risk Reports");
        pageTitle.setFont(StyleConstants.TITLE_FONT);
        pageTitle.setForeground(StyleConstants.TEXT_FG);
        page.add(pageTitle, BorderLayout.NORTH);

        JPanel body = new JPanel(new BorderLayout(0, StyleConstants.GAP_V));
        body.setOpaque(false);

        // Actions card
        DashboardCard actionsCard = new DashboardCard("Actions");
        JPanel actionsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        actionsPanel.setOpaque(false);

        StyledButton btnAtRisk = new StyledButton("Show At-Risk");
        btnAtRisk.setFont(StyleConstants.BUTTON_FONT);
        btnAtRisk.setBackground(StyleConstants.PRIMARY);
        btnAtRisk.setForeground(Color.WHITE);
        btnAtRisk.setPreferredSize(new Dimension(150, 36));
        btnAtRisk.addActionListener(e -> handleShowAtRisk());
        actionsPanel.add(btnAtRisk);

        if (currentRole.canExportReports()) {
            StyledButton btnExport = new StyledButton("Export All");
            btnExport.setFont(StyleConstants.BUTTON_FONT);
            btnExport.setBackground(StyleConstants.ACCENT_GREEN);
            btnExport.setForeground(Color.WHITE);
            btnExport.setPreferredSize(new Dimension(130, 36));
            btnExport.addActionListener(e -> handleBulkExport());
            actionsPanel.add(btnExport);
        }

        actionsCard.setContent(actionsPanel);
        body.add(actionsCard, BorderLayout.NORTH);

        // Table + Detail
        JPanel centerPanel = new JPanel(new BorderLayout(0, StyleConstants.GAP_V));
        centerPanel.setOpaque(false);

        DashboardCard tableCard = new DashboardCard("At-Risk Students");
        String[] cols = { "ID", "Name", "Average", "Risk Level", "Risk Score", "Trend" };
        reportTableModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };
        reportTable = new JTable(reportTableModel);
        setupModernTable(reportTable);
        reportTable.getColumnModel().getColumn(3).setCellRenderer(new RiskBadgeRenderer());
        reportTable.getColumnModel().getColumn(5).setCellRenderer(new TrendArrowRenderer());
        JScrollPane ts = new JScrollPane(reportTable);
        ts.setBorder(BorderFactory.createEmptyBorder());
        ts.getViewport().setBackground(Color.WHITE);
        ts.setPreferredSize(new Dimension(0, 200));
        tableCard.setContent(ts);
        centerPanel.add(tableCard, BorderLayout.NORTH);

        DashboardCard detailCard = new DashboardCard("Intelligence Report");
        txtReportDetails = createTextArea();
        detailCard.setContent(new JScrollPane(txtReportDetails));
        centerPanel.add(detailCard, BorderLayout.CENTER);

        body.add(centerPanel, BorderLayout.CENTER);
        page.add(body, BorderLayout.CENTER);

        reportTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting())
                handleAtRiskRowSelected();
        });

        return page;
    }

    private void handleShowAtRisk() {
        reportTableModel.setRowCount(0);
        List<Student> atRisk = new ArrayList<>();
        for (Student s : getVisibleStudents()) {
            if (s.getSubjects().isEmpty())
                continue;
            RiskScore risk = riskPredictor.assessRisk(s);
            if (risk.getLevel() == Level.HIGH || risk.getLevel() == Level.MODERATE)
                atRisk.add(s);
        }
        if (atRisk.isEmpty()) {
            txtReportDetails.setText("No at-risk students found.");
            return;
        }
        for (Student s : atRisk) {
            RiskScore risk = riskPredictor.assessRisk(s);
            TrendDirection trend = trendAnalyzer.getTrend(s.getId());
            reportTableModel.addRow(new Object[] { s.getId(), s.getName(),
                    String.format("%.2f", s.getAverageScore()), risk.getLevel().getLabel(),
                    String.format("%.1f", risk.getNumericScore()), trend.getLabel() });
        }
        txtReportDetails.setText("Found " + atRisk.size() + " at-risk student(s).\nSelect a row for details.");
    }

    private void handleAtRiskRowSelected() {
        int row = reportTable.getSelectedRow();
        if (row < 0)
            return;
        String id = (String) reportTableModel.getValueAt(row, 0);
        Student student = dataManager.findStudentById(id);
        if (student == null)
            return;

        RiskScore risk = riskPredictor.assessRisk(student);
        TrendDirection trend = trendAnalyzer.getTrend(student.getId());
        List<String> recs = adaptivePlanner.generateRecommendations(student, risk, trend);

        StringBuilder sb = new StringBuilder();
        sb.append("ACADEMIC INTELLIGENCE REPORT\n");
        sb.append("========================================\n");
        sb.append("Student: ").append(student.getName()).append(" (").append(student.getId()).append(")\n");
        sb.append("Average: ").append(String.format("%.2f", student.getAverageScore())).append("\n");
        sb.append("Risk: ").append(risk.getLevel().getLabel()).append(" (")
                .append(String.format("%.1f", risk.getNumericScore())).append(")\n");
        sb.append("Trend: ").append(trend.getLabel()).append(" ").append(trend.getArrow()).append("\n\n");
        sb.append("--- Subjects ---\n");
        for (Subject sub : student.getSubjects()) {
            String m = sub.getScore() < 60 ? " [WEAK]" : "";
            sb.append(String.format("  %-20s : %6.2f%s\n", sub.getSubjectName(), sub.getScore(), m));
        }
        sb.append("\n").append(risk.getDetailedBreakdown());
        sb.append("\n--- AI Recommendations ---\n");
        for (String r : recs)
            sb.append(r).append("\n");
        txtReportDetails.setText(sb.toString());
        txtReportDetails.setCaretPosition(0);
    }

    private void handleBulkExport() {
        List<Student> students = getVisibleStudents();
        if (students.isEmpty()) {
            showError("No students to export.");
            return;
        }
        List<RiskScore> risks = new ArrayList<>();
        List<TrendDirection> trends = new ArrayList<>();
        List<List<String>> allRecs = new ArrayList<>();
        for (Student s : students) {
            RiskScore r = riskPredictor.assessRisk(s);
            TrendDirection t = trendAnalyzer.getTrend(s.getId());
            risks.add(r);
            trends.add(t);
            allRecs.add(adaptivePlanner.generateRecommendations(s, r, t));
        }
        try {
            fileManager.saveBulkReport(students, risks, trends, allRecs);
            showInfo("Exported " + students.size() + " reports to academic_report.txt.");
        } catch (IOException ex) {
            showError("Export failed: " + ex.getMessage());
        }
    }

    // ==========================================
    // ANALYTICS PAGE
    // ==========================================

    private JPanel buildAnalyticsPage() {
        JPanel page = new JPanel(new BorderLayout(0, StyleConstants.GAP_V));
        page.setOpaque(false);
        page.setBorder(StyleConstants.PANEL_PADDING);

        JLabel pageTitle = new JLabel("Institutional Analytics");
        pageTitle.setFont(StyleConstants.TITLE_FONT);
        pageTitle.setForeground(StyleConstants.TEXT_FG);
        page.add(pageTitle, BorderLayout.NORTH);

        DashboardCard card = new DashboardCard("Analytics Dashboard");
        JPanel topRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        topRow.setOpaque(false);
        StyledButton btnRefresh = new StyledButton("Refresh");
        btnRefresh.setFont(StyleConstants.BUTTON_FONT);
        btnRefresh.setBackground(StyleConstants.PRIMARY);
        btnRefresh.setForeground(Color.WHITE);
        btnRefresh.setPreferredSize(new Dimension(120, 36));
        btnRefresh.addActionListener(e -> handleRefreshAnalytics());
        topRow.add(btnRefresh);

        JPanel cardBody = new JPanel(new BorderLayout(0, 10));
        cardBody.setOpaque(false);
        cardBody.add(topRow, BorderLayout.NORTH);

        txtAnalytics = createTextArea();
        cardBody.add(new JScrollPane(txtAnalytics), BorderLayout.CENTER);
        card.setContent(cardBody);
        page.add(card, BorderLayout.CENTER);
        return page;
    }

    private void handleRefreshAnalytics() {
        List<Student> students = getVisibleStudents();
        if (students.isEmpty()) {
            txtAnalytics.setText("No data available. Add students first.");
            return;
        }
        txtAnalytics.setText(analyticsService.generateAnalyticsSummary(students));
        txtAnalytics.setCaretPosition(0);
    }

    // ==========================================
    // SIMULATION PAGE
    // ==========================================

    private JPanel buildSimulationPage() {
        JPanel page = new JPanel(new BorderLayout(0, StyleConstants.GAP_V));
        page.setOpaque(false);
        page.setBorder(StyleConstants.PANEL_PADDING);

        JLabel pageTitle = new JLabel("What-If Simulation");
        pageTitle.setFont(StyleConstants.TITLE_FONT);
        pageTitle.setForeground(StyleConstants.TEXT_FG);
        page.add(pageTitle, BorderLayout.NORTH);

        JPanel body = new JPanel(new BorderLayout(0, StyleConstants.GAP_V));
        body.setOpaque(false);

        DashboardCard inputCard = new DashboardCard("Simulation Parameters");
        JPanel inputPanel = new JPanel(new GridBagLayout());
        inputPanel.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = StyleConstants.FORM_INSETS;
        gbc.anchor = GridBagConstraints.WEST;

        gbc.gridx = 0;
        gbc.gridy = 0;
        inputPanel.add(createLabel("Student:"), gbc);
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        cmbSimStudent = new JComboBox<>();
        cmbSimStudent.setFont(StyleConstants.BODY_FONT);
        cmbSimStudent.setPreferredSize(new Dimension(250, StyleConstants.COMBO_HEIGHT));
        inputPanel.add(cmbSimStudent, gbc);

        gbc.gridx = 2;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        inputPanel.add(createLabel("Subject:"), gbc);
        gbc.gridx = 3;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        cmbSimSubject = new JComboBox<>();
        cmbSimSubject.setFont(StyleConstants.BODY_FONT);
        cmbSimSubject.setPreferredSize(new Dimension(200, StyleConstants.COMBO_HEIGHT));
        inputPanel.add(cmbSimSubject, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        inputPanel.add(createLabel("Score Change:"), gbc);
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        txtSimDelta = createTextField();
        txtSimDelta.setToolTipText("+10 or -5");
        inputPanel.add(txtSimDelta, gbc);

        gbc.gridx = 2;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.CENTER;
        StyledButton btnRun = new StyledButton("Run Simulation");
        btnRun.setFont(StyleConstants.BUTTON_FONT);
        btnRun.setBackground(StyleConstants.ACCENT_ORANGE);
        btnRun.setForeground(Color.WHITE);
        btnRun.setPreferredSize(StyleConstants.BUTTON_SIZE);
        btnRun.addActionListener(e -> handleRunSimulation());
        inputPanel.add(btnRun, gbc);

        cmbSimStudent.addActionListener(e -> populateSimSubjects());
        inputCard.setContent(inputPanel);
        body.add(inputCard, BorderLayout.NORTH);

        DashboardCard resultCard = new DashboardCard("Simulation Result");
        txtSimResult = createTextArea();
        resultCard.setContent(new JScrollPane(txtSimResult));
        body.add(resultCard, BorderLayout.CENTER);

        page.add(body, BorderLayout.CENTER);
        return page;
    }

    private void refreshSimulationSelectors() {
        cmbSimStudent.removeAllItems();
        List<Student> students = getVisibleStudents();
        if (students.isEmpty())
            cmbSimStudent.addItem("-- No students --");
        else
            for (Student s : students)
                cmbSimStudent.addItem(s.getId() + " - " + s.getName());
    }

    private void populateSimSubjects() {
        cmbSimSubject.removeAllItems();
        int idx = cmbSimStudent.getSelectedIndex();
        List<Student> students = getVisibleStudents();
        if (idx < 0 || idx >= students.size())
            return;
        for (Subject sub : students.get(idx).getSubjects())
            cmbSimSubject.addItem(sub.getSubjectName());
    }

    private void handleRunSimulation() {
        List<Student> students = getVisibleStudents();
        int idx = cmbSimStudent.getSelectedIndex();
        if (idx < 0 || idx >= students.size()) {
            showError("Select a student.");
            return;
        }
        String subName = (String) cmbSimSubject.getSelectedItem();
        if (subName == null) {
            showError("Select a subject.");
            return;
        }
        double delta;
        try {
            delta = Double.parseDouble(txtSimDelta.getText().trim());
        } catch (NumberFormatException ex) {
            showError("Enter a valid number.");
            return;
        }
        try {
            SimulationResult result = simulationService.simulateScoreChange(students.get(idx), subName, delta);
            txtSimResult.setText(result.getReport());
            txtSimResult.setCaretPosition(0);
        } catch (IllegalArgumentException ex) {
            showError(ex.getMessage());
        }
    }

    // ==========================================
    // MANAGE STUDENTS PAGE (Teacher CRUD — Edit/Delete)
    // ==========================================

    private JPanel buildManageStudentsPage() {
        JPanel page = new JPanel(new BorderLayout(0, StyleConstants.GAP_V));
        page.setOpaque(false);
        page.setBorder(StyleConstants.PANEL_PADDING);

        JLabel pageTitle = new JLabel("Manage Students");
        pageTitle.setFont(StyleConstants.TITLE_FONT);
        pageTitle.setForeground(StyleConstants.TEXT_FG);
        page.add(pageTitle, BorderLayout.NORTH);

        JPanel body = new JPanel(new BorderLayout(0, StyleConstants.GAP_V));
        body.setOpaque(false);

        // Actions card
        DashboardCard actionsCard = new DashboardCard("Actions");
        JPanel actionsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        actionsPanel.setOpaque(false);

        StyledButton btnEditName = new StyledButton("Edit Name");
        btnEditName.setFont(StyleConstants.BUTTON_FONT);
        btnEditName.setBackground(StyleConstants.PRIMARY);
        btnEditName.setForeground(Color.WHITE);
        btnEditName.setPreferredSize(new Dimension(130, 36));
        btnEditName.addActionListener(e -> handleEditStudentName());
        actionsPanel.add(btnEditName);

        StyledButton btnEditScore = new StyledButton("Edit Score");
        btnEditScore.setFont(StyleConstants.BUTTON_FONT);
        btnEditScore.setBackground(StyleConstants.ACCENT_ORANGE);
        btnEditScore.setForeground(Color.WHITE);
        btnEditScore.setPreferredSize(new Dimension(130, 36));
        btnEditScore.addActionListener(e -> handleEditScore());
        actionsPanel.add(btnEditScore);

        StyledButton btnDelete = new StyledButton("Delete Student");
        btnDelete.setFont(StyleConstants.BUTTON_FONT);
        btnDelete.setBackground(StyleConstants.ACCENT_RED);
        btnDelete.setForeground(Color.WHITE);
        btnDelete.setPreferredSize(new Dimension(150, 36));
        btnDelete.addActionListener(e -> handleDeleteStudent());
        actionsPanel.add(btnDelete);

        StyledButton btnRefreshManage = new StyledButton("Refresh");
        btnRefreshManage.setFont(StyleConstants.BUTTON_FONT);
        btnRefreshManage.setBackground(StyleConstants.ACCENT_GREEN);
        btnRefreshManage.setForeground(Color.WHITE);
        btnRefreshManage.setPreferredSize(new Dimension(110, 36));
        btnRefreshManage.addActionListener(e -> {
            dataManager.refreshCache();
            refreshManageTable();
        });
        actionsPanel.add(btnRefreshManage);

        actionsCard.setContent(actionsPanel);
        body.add(actionsCard, BorderLayout.NORTH);

        // Student table with all details
        DashboardCard tableCard = new DashboardCard("All Student Records");
        String[] cols = { "ID", "Name", "Subjects", "Average", "Risk Level", "Risk Score", "Trend" };
        manageTableModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };
        manageTable = new JTable(manageTableModel);
        setupModernTable(manageTable);
        // Override renderers for Risk and Trend columns (indices 4 and 6)
        if (manageTable.getColumnCount() > 4)
            manageTable.getColumnModel().getColumn(4).setCellRenderer(new RiskBadgeRenderer());
        if (manageTable.getColumnCount() > 6)
            manageTable.getColumnModel().getColumn(6).setCellRenderer(new TrendArrowRenderer());

        JScrollPane scroll = new JScrollPane(manageTable);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(Color.WHITE);
        tableCard.setContent(scroll);
        body.add(tableCard, BorderLayout.CENTER);

        page.add(body, BorderLayout.CENTER);
        return page;
    }

    private void refreshManageTable() {
        if (manageTableModel == null)
            return;
        manageTableModel.setRowCount(0);
        for (Student s : getVisibleStudents()) {
            RiskScore risk = s.getSubjects().isEmpty() ? null : riskPredictor.assessRisk(s);
            TrendDirection trend = trendAnalyzer.getTrend(s.getId());
            manageTableModel.addRow(new Object[] {
                    s.getId(), s.getName(),
                    String.valueOf(s.getSubjects().size()),
                    String.format("%.2f", s.getAverageScore()),
                    risk != null ? risk.getLevel().getLabel() : "N/A",
                    risk != null ? String.format("%.1f", risk.getNumericScore()) : "N/A",
                    trend.getLabel()
            });
        }
    }

    private void handleEditStudentName() {
        int row = manageTable.getSelectedRow();
        if (row < 0) {
            showError("Select a student row to edit.");
            return;
        }
        String studentId = (String) manageTableModel.getValueAt(row, 0);
        String currentName = (String) manageTableModel.getValueAt(row, 1);

        String newName = (String) JOptionPane.showInputDialog(this,
                "Enter new name for student '" + studentId + "':",
                "Edit Student Name", JOptionPane.PLAIN_MESSAGE,
                null, null, currentName);

        if (newName == null || newName.trim().isEmpty())
            return;
        newName = newName.trim();
        if (!newName.matches("[a-zA-Z\\s]+")) {
            showError("Name must contain only letters and spaces.");
            return;
        }

        boolean ok = dataManager.getStudentDAO().updateName(studentId, newName);
        if (ok) {
            dataManager.refreshCache();
            refreshManageTable();
            refreshStudentTable();
            showInfo("Student '" + studentId + "' renamed to '" + newName + "'.");
        } else {
            showError("Failed to update student name.");
        }
    }

    private void handleEditScore() {
        int row = manageTable.getSelectedRow();
        if (row < 0) {
            showError("Select a student row first.");
            return;
        }
        String studentId = (String) manageTableModel.getValueAt(row, 0);
        Student student = dataManager.findStudentById(studentId);
        if (student == null) {
            showError("Student not found.");
            return;
        }

        if (student.getSubjects().isEmpty()) {
            showError("This student has no subjects to edit.");
            return;
        }

        // Build subject list for selection
        String[] subjectNames = new String[student.getSubjects().size()];
        for (int i = 0; i < student.getSubjects().size(); i++) {
            Subject sub = student.getSubjects().get(i);
            subjectNames[i] = sub.getSubjectName() + " (" + String.format("%.1f", sub.getScore()) + ")";
        }

        String selected = (String) JOptionPane.showInputDialog(this,
                "Select subject to edit score for '" + student.getName() + "':",
                "Edit Score", JOptionPane.PLAIN_MESSAGE,
                null, subjectNames, subjectNames[0]);

        if (selected == null)
            return;
        int subIdx = -1;
        for (int i = 0; i < subjectNames.length; i++) {
            if (subjectNames[i].equals(selected)) {
                subIdx = i;
                break;
            }
        }
        if (subIdx < 0)
            return;

        Subject targetSubject = student.getSubjects().get(subIdx);
        String newScoreStr = JOptionPane.showInputDialog(this,
                "Enter new score for " + targetSubject.getSubjectName() + ":",
                String.format("%.1f", targetSubject.getScore()));

        if (newScoreStr == null || newScoreStr.trim().isEmpty())
            return;
        double newScore;
        try {
            newScore = Double.parseDouble(newScoreStr.trim());
        } catch (NumberFormatException e) {
            showError("Score must be a valid number.");
            return;
        }
        if (newScore < 0 || newScore > 100) {
            showError("Score must be between 0 and 100.");
            return;
        }

        // Find DB IDs and update
        int studentDbId = dataManager.getStudentDAO().findDbIdByStudentId(studentId);
        int subjectDbId = dataManager.getSubjectDAO().findIdByName(targetSubject.getSubjectName());
        if (studentDbId < 0 || subjectDbId < 0) {
            showError("Could not resolve DB IDs.");
            return;
        }

        boolean ok = dataManager.getScoreDAO().updateScore(studentDbId, subjectDbId, newScore);
        if (ok) {
            dataManager.refreshCache();
            refreshManageTable();
            refreshStudentTable();
            showInfo("Score updated: " + targetSubject.getSubjectName() + " = " + String.format("%.1f", newScore));
        } else {
            showError("Failed to update score.");
        }
    }

    private void handleDeleteStudent() {
        int row = manageTable.getSelectedRow();
        if (row < 0) {
            showError("Select a student row to delete.");
            return;
        }
        String studentId = (String) manageTableModel.getValueAt(row, 0);
        String studentName = (String) manageTableModel.getValueAt(row, 1);

        int confirm = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to delete student '" + studentName + "' (" + studentId + ")?\n"
                        + "This will permanently remove all their scores.",
                "Confirm Delete", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

        if (confirm != JOptionPane.YES_OPTION)
            return;

        boolean ok = dataManager.getStudentDAO().deleteByStudentId(studentId);
        if (ok) {
            dataManager.refreshCache();
            refreshManageTable();
            refreshStudentTable();
            showInfo("Student '" + studentName + "' (" + studentId + ") deleted.");
        } else {
            showError("Failed to delete student.");
        }
    }

    // ==========================================
    // SETTINGS PAGE (Password Change + Data Migration)
    // ==========================================

    private JPanel buildSettingsPage() {
        JPanel page = new JPanel(new BorderLayout(0, StyleConstants.GAP_V));
        page.setOpaque(false);
        page.setBorder(StyleConstants.PANEL_PADDING);

        JLabel pageTitle = new JLabel("Settings");
        pageTitle.setFont(StyleConstants.TITLE_FONT);
        pageTitle.setForeground(StyleConstants.TEXT_FG);
        page.add(pageTitle, BorderLayout.NORTH);

        JPanel body = new JPanel();
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setOpaque(false);

        // --- Section 1: Change Password ---
        DashboardCard pwCard = new DashboardCard("Change Password");
        JPanel pwPanel = new JPanel(new GridBagLayout());
        pwPanel.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = StyleConstants.FORM_INSETS;
        gbc.anchor = GridBagConstraints.WEST;

        gbc.gridx = 0;
        gbc.gridy = 0;
        pwPanel.add(createLabel("Current Password:"), gbc);
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        JPasswordField txtCurrentPw = new JPasswordField(20);
        txtCurrentPw.setMargin(StyleConstants.TEXT_FIELD_MARGIN);
        pwPanel.add(txtCurrentPw, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        pwPanel.add(createLabel("New Password:"), gbc);
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        JPasswordField txtNewPw = new JPasswordField(20);
        txtNewPw.setMargin(StyleConstants.TEXT_FIELD_MARGIN);
        pwPanel.add(txtNewPw, gbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        pwPanel.add(createLabel("Confirm Password:"), gbc);
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        JPasswordField txtConfirmPw = new JPasswordField(20);
        txtConfirmPw.setMargin(StyleConstants.TEXT_FIELD_MARGIN);
        pwPanel.add(txtConfirmPw, gbc);

        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.CENTER;
        StyledButton btnChangePw = new StyledButton("Change Password");
        btnChangePw.setFont(StyleConstants.BUTTON_FONT);
        btnChangePw.setBackground(StyleConstants.PRIMARY);
        btnChangePw.setForeground(Color.WHITE);
        btnChangePw.setPreferredSize(new Dimension(180, 36));
        btnChangePw.addActionListener(e -> {
            String currentPw = new String(txtCurrentPw.getPassword()).trim();
            String newPw = new String(txtNewPw.getPassword()).trim();
            String confirmPw = new String(txtConfirmPw.getPassword()).trim();

            if (currentPw.isEmpty() || newPw.isEmpty() || confirmPw.isEmpty()) {
                showError("All password fields are required.");
                return;
            }
            if (newPw.length() < 4) {
                showError("New password must be at least 4 characters.");
                return;
            }
            if (!newPw.equals(confirmPw)) {
                showError("New password and confirmation do not match.");
                return;
            }

            User currentUser = SessionManager.getCurrentUser();
            if (currentUser == null) {
                showError("No active session.");
                return;
            }

            UserDAO userDAO = new UserDAO();
            if (!userDAO.verifyPassword(currentUser.getId(), currentPw)) {
                showError("Current password is incorrect.");
                return;
            }

            boolean ok = userDAO.updatePassword(currentUser.getId(), newPw);
            if (ok) {
                showInfo("Password changed successfully.");
                txtCurrentPw.setText("");
                txtNewPw.setText("");
                txtConfirmPw.setText("");
            } else {
                showError("Failed to change password.");
            }
        });
        pwPanel.add(btnChangePw, gbc);

        pwCard.setContent(pwPanel);
        pwCard.setPreferredSize(new Dimension(0, 260));
        body.add(pwCard);
        body.add(Box.createVerticalStrut(StyleConstants.GAP_V));

        // --- Section 2: Data Migration (Admin/Teacher only) ---
        if (currentRole.canEditData()) {
            DashboardCard migrationCard = new DashboardCard("Data Migration");
            JPanel migrationPanel = new JPanel(new BorderLayout(0, 10));
            migrationPanel.setOpaque(false);

            JPanel migrationActions = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
            migrationActions.setOpaque(false);

            StyledButton btnMigrate = new StyledButton("Import from academic_report.txt");
            btnMigrate.setFont(StyleConstants.BUTTON_FONT);
            btnMigrate.setBackground(StyleConstants.ACCENT_ORANGE);
            btnMigrate.setForeground(Color.WHITE);
            btnMigrate.setPreferredSize(new Dimension(280, 36));
            migrationActions.add(btnMigrate);

            StyledButton btnMigrateFile = new StyledButton("Import from File...");
            btnMigrateFile.setFont(StyleConstants.BUTTON_FONT);
            btnMigrateFile.setBackground(StyleConstants.PRIMARY);
            btnMigrateFile.setForeground(Color.WHITE);
            btnMigrateFile.setPreferredSize(new Dimension(180, 36));
            migrationActions.add(btnMigrateFile);

            migrationPanel.add(migrationActions, BorderLayout.NORTH);

            JTextArea txtMigrationResult = createTextArea();
            txtMigrationResult.setText("Click a button above to import data from a legacy report file.\n\n"
                    + "This will parse the report format and insert student/subject/score records into the database.\n"
                    + "Existing records are skipped (no duplicates).");
            migrationPanel.add(new JScrollPane(txtMigrationResult), BorderLayout.CENTER);

            btnMigrate.addActionListener(e -> {
                String result = DataMigration.migrateFromReportFile();
                txtMigrationResult.setText(result);
                txtMigrationResult.setCaretPosition(0);
                dataManager.refreshCache();
                showInfo("Migration complete. See results below.");
            });

            btnMigrateFile.addActionListener(e -> {
                JFileChooser fc = new JFileChooser(System.getProperty("user.dir"));
                fc.setDialogTitle("Select Report File to Import");
                fc.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Text Files", "txt"));
                if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                    String result = DataMigration.migrateFromReportFile(fc.getSelectedFile().getAbsolutePath());
                    txtMigrationResult.setText(result);
                    txtMigrationResult.setCaretPosition(0);
                    dataManager.refreshCache();
                    showInfo("Migration complete. See results below.");
                }
            });

            migrationCard.setContent(migrationPanel);
            body.add(migrationCard);
            body.add(Box.createVerticalStrut(StyleConstants.GAP_V));
        }

        // --- Section 3: Session Info ---
        DashboardCard infoCard = new DashboardCard("Session Information");
        JTextArea txtSessionInfo = createTextArea();
        User currentUser = SessionManager.getCurrentUser();
        StringBuilder sb = new StringBuilder();
        sb.append("Username:     ").append(currentUsername).append("\n");
        sb.append("Role:         ").append(currentRole.getLabel()).append("\n");
        String linkedId = SessionManager.getLinkedStudentId();
        if (linkedId != null) {
            sb.append("Linked Student: ").append(linkedId).append("\n");
        }
        sb.append("\nPermissions:\n");
        sb.append("  Edit Data:      ").append(currentRole.canEditData() ? "Yes" : "No").append("\n");
        sb.append("  Analytics:      ").append(currentRole.canAccessAnalytics() ? "Yes" : "No").append("\n");
        sb.append("  Simulation:     ").append(currentRole.canAccessSimulation() ? "Yes" : "No").append("\n");
        sb.append("  Export Reports: ").append(currentRole.canExportReports() ? "Yes" : "No").append("\n");
        sb.append("  Admin Access:   ").append(currentRole.canAccessAdmin() ? "Yes" : "No").append("\n");
        sb.append("\nApplication:  ALIP v3.0 — Database-Driven Edition\n");
        sb.append("Database:     SQLite (alip_data.db)\n");
        txtSessionInfo.setText(sb.toString());
        infoCard.setContent(new JScrollPane(txtSessionInfo));
        body.add(infoCard);

        // Wrap body in scroll pane
        JScrollPane bodyScroll = new JScrollPane(body);
        bodyScroll.setBorder(BorderFactory.createEmptyBorder());
        bodyScroll.getVerticalScrollBar().setUnitIncrement(16);
        bodyScroll.setOpaque(false);
        bodyScroll.getViewport().setOpaque(false);

        page.add(bodyScroll, BorderLayout.CENTER);
        return page;
    }

    // ==========================================
    // ADMIN PAGE (UPGRADED — User Mgmt + Config + Categories)
    // ==========================================

    private JPanel buildAdminPage() {
        JPanel page = new JPanel(new BorderLayout(0, StyleConstants.GAP_V));
        page.setOpaque(false);
        page.setBorder(StyleConstants.PANEL_PADDING);

        JLabel pageTitle = new JLabel("Administration");
        pageTitle.setFont(StyleConstants.TITLE_FONT);
        pageTitle.setForeground(StyleConstants.TEXT_FG);
        page.add(pageTitle, BorderLayout.NORTH);

        // Use a vertical split: top = user management, bottom = config + categories
        JPanel body = new JPanel();
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setOpaque(false);

        // --- Section 1: User Management ---
        DashboardCard userCard = new DashboardCard("User Management");
        JPanel userPanel = new JPanel(new BorderLayout(0, 10));
        userPanel.setOpaque(false);

        // Add user form
        JPanel addUserForm = new JPanel(new GridBagLayout());
        addUserForm.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = StyleConstants.FORM_INSETS;
        gbc.anchor = GridBagConstraints.WEST;

        gbc.gridx = 0;
        gbc.gridy = 0;
        addUserForm.add(createLabel("Username:"), gbc);
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        txtNewUsername = createTextField();
        addUserForm.add(txtNewUsername, gbc);

        gbc.gridx = 2;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        addUserForm.add(createLabel("Password:"), gbc);
        gbc.gridx = 3;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        txtNewPassword = createTextField();
        addUserForm.add(txtNewPassword, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        addUserForm.add(createLabel("Role:"), gbc);
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        cmbNewUserRole = new JComboBox<>(new String[] { "ADMIN", "TEACHER", "STUDENT" });
        cmbNewUserRole.setFont(StyleConstants.BODY_FONT);
        addUserForm.add(cmbNewUserRole, gbc);

        gbc.gridx = 2;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        addUserForm.add(createLabel("Link Student ID:"), gbc);
        gbc.gridx = 3;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        txtLinkStudentId = createTextField();
        txtLinkStudentId.setToolTipText("Business student ID (e.g. S001) — only for STUDENT role");
        addUserForm.add(txtLinkStudentId, gbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.CENTER;
        StyledButton btnAddUser = new StyledButton("Create User");
        btnAddUser.setFont(StyleConstants.BUTTON_FONT);
        btnAddUser.setBackground(StyleConstants.PRIMARY);
        btnAddUser.setForeground(Color.WHITE);
        btnAddUser.setPreferredSize(new Dimension(140, 36));
        btnAddUser.addActionListener(e -> handleCreateUser());
        addUserForm.add(btnAddUser, gbc);

        gbc.gridx = 2;
        gbc.gridwidth = 2;
        StyledButton btnToggle = new StyledButton("Toggle Enabled");
        btnToggle.setFont(StyleConstants.BUTTON_FONT);
        btnToggle.setBackground(StyleConstants.ACCENT_ORANGE);
        btnToggle.setForeground(Color.WHITE);
        btnToggle.setPreferredSize(new Dimension(150, 36));
        btnToggle.addActionListener(e -> handleToggleUser());
        addUserForm.add(btnToggle, gbc);

        userPanel.add(addUserForm, BorderLayout.NORTH);

        // User table
        String[] userCols = { "ID", "Username", "Role", "Linked Student", "Enabled", "Created" };
        userTableModel = new DefaultTableModel(userCols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };
        userTable = new JTable(userTableModel);
        setupModernTable(userTable);
        JScrollPane userScroll = new JScrollPane(userTable);
        userScroll.setBorder(BorderFactory.createEmptyBorder());
        userScroll.getViewport().setBackground(Color.WHITE);
        userScroll.setPreferredSize(new Dimension(0, 160));
        userPanel.add(userScroll, BorderLayout.CENTER);

        userCard.setContent(userPanel);
        userCard.setPreferredSize(new Dimension(0, 340));
        body.add(userCard);
        body.add(Box.createVerticalStrut(StyleConstants.GAP_V));

        // --- Section 2: Risk Configuration ---
        DashboardCard riskConfigCard = new DashboardCard("Risk Configuration (DB-backed)");
        JPanel riskPanel = new JPanel(new GridBagLayout());
        riskPanel.setOpaque(false);
        GridBagConstraints gc2 = new GridBagConstraints();
        gc2.insets = StyleConstants.FORM_INSETS;
        gc2.anchor = GridBagConstraints.WEST;

        gc2.gridx = 0;
        gc2.gridy = 0;
        riskPanel.add(createLabel("Config Key:"), gc2);
        gc2.gridx = 1;
        gc2.fill = GridBagConstraints.HORIZONTAL;
        gc2.weightx = 1.0;
        cmbConfigKey = new JComboBox<>(new String[] {
                "risk.weight.average", "risk.weight.weak_count", "risk.weight.lowest", "risk.weight.trend",
                "risk.threshold.high", "risk.threshold.moderate"
        });
        cmbConfigKey.setFont(StyleConstants.BODY_FONT);
        riskPanel.add(cmbConfigKey, gc2);

        gc2.gridx = 2;
        gc2.weightx = 0;
        gc2.fill = GridBagConstraints.NONE;
        riskPanel.add(createLabel("Value:"), gc2);
        gc2.gridx = 3;
        gc2.fill = GridBagConstraints.HORIZONTAL;
        gc2.weightx = 1.0;
        txtConfigValue = createTextField();
        riskPanel.add(txtConfigValue, gc2);

        gc2.gridx = 0;
        gc2.gridy = 1;
        gc2.gridwidth = 4;
        gc2.fill = GridBagConstraints.NONE;
        gc2.anchor = GridBagConstraints.CENTER;
        StyledButton btnSaveConfig = new StyledButton("Save Config");
        btnSaveConfig.setFont(StyleConstants.BUTTON_FONT);
        btnSaveConfig.setBackground(StyleConstants.ACCENT_GREEN);
        btnSaveConfig.setForeground(Color.WHITE);
        btnSaveConfig.setPreferredSize(new Dimension(140, 36));
        btnSaveConfig.addActionListener(e -> handleSaveConfig());
        riskPanel.add(btnSaveConfig, gc2);

        riskConfigCard.setContent(riskPanel);
        riskConfigCard.setPreferredSize(new Dimension(0, 160));
        body.add(riskConfigCard);
        body.add(Box.createVerticalStrut(StyleConstants.GAP_V));

        // --- Section 3: Subject Category Configuration (preserved) ---
        DashboardCard configCard = new DashboardCard("Subject Category Configuration");
        JPanel catPanel = new JPanel(new GridBagLayout());
        catPanel.setOpaque(false);
        GridBagConstraints gc3 = new GridBagConstraints();
        gc3.insets = StyleConstants.FORM_INSETS;
        gc3.anchor = GridBagConstraints.WEST;

        gc3.gridx = 0;
        gc3.gridy = 0;
        catPanel.add(createLabel("Subject:"), gc3);
        gc3.gridx = 1;
        gc3.fill = GridBagConstraints.HORIZONTAL;
        gc3.weightx = 1.0;
        txtAdminSubject = createTextField();
        catPanel.add(txtAdminSubject, gc3);

        gc3.gridx = 2;
        gc3.weightx = 0;
        gc3.fill = GridBagConstraints.NONE;
        catPanel.add(createLabel("Category:"), gc3);
        gc3.gridx = 3;
        gc3.fill = GridBagConstraints.HORIZONTAL;
        gc3.weightx = 1.0;
        cmbAdminCategory = new JComboBox<>();
        cmbAdminCategory.setFont(StyleConstants.BODY_FONT);
        for (SubjectCategory cat : SubjectCategory.values())
            if (cat != SubjectCategory.UNCATEGORIZED)
                cmbAdminCategory.addItem(cat.name() + " - " + cat.getDisplayName());
        catPanel.add(cmbAdminCategory, gc3);

        gc3.gridx = 0;
        gc3.gridy = 1;
        gc3.gridwidth = 4;
        gc3.fill = GridBagConstraints.NONE;
        gc3.anchor = GridBagConstraints.CENTER;
        StyledButton btnMap = new StyledButton("Add / Update");
        btnMap.setFont(StyleConstants.BUTTON_FONT);
        btnMap.setBackground(StyleConstants.PRIMARY);
        btnMap.setForeground(Color.WHITE);
        btnMap.setPreferredSize(new Dimension(160, 36));
        btnMap.addActionListener(e -> handleAddCategoryMapping());
        catPanel.add(btnMap, gc3);

        configCard.setContent(catPanel);
        configCard.setPreferredSize(new Dimension(0, 160));
        body.add(configCard);
        body.add(Box.createVerticalStrut(StyleConstants.GAP_V));

        // Mappings display
        DashboardCard mappingCard = new DashboardCard("Current Mappings & Config");
        txtCategoryMappings = createTextArea();
        mappingCard.setContent(new JScrollPane(txtCategoryMappings));
        body.add(mappingCard);

        // Wrap body in scroll pane for vertical scrolling
        JScrollPane bodyScroll = new JScrollPane(body);
        bodyScroll.setBorder(BorderFactory.createEmptyBorder());
        bodyScroll.getVerticalScrollBar().setUnitIncrement(16);
        bodyScroll.setOpaque(false);
        bodyScroll.getViewport().setOpaque(false);

        page.add(bodyScroll, BorderLayout.CENTER);
        refreshAdminData();
        return page;
    }

    private void handleCreateUser() {
        String username = txtNewUsername.getText().trim();
        String password = txtNewPassword.getText().trim();
        if (username.isEmpty() || password.isEmpty()) {
            showError("Username and password are required.");
            return;
        }
        if (password.length() < 4) {
            showError("Password must be at least 4 characters.");
            return;
        }
        String roleStr = (String) cmbNewUserRole.getSelectedItem();
        UserRole role;
        switch (roleStr) {
            case "ADMIN":
                role = UserRole.ADMIN;
                break;
            case "TEACHER":
                role = UserRole.TEACHER;
                break;
            default:
                role = UserRole.STUDENT;
                break;
        }

        // Check linked student
        Integer linkedStudentDbId = null;
        String linkId = txtLinkStudentId.getText().trim();
        if (!linkId.isEmpty() && role == UserRole.STUDENT) {
            int dbId = dataManager.getStudentDAO().findDbIdByStudentId(linkId);
            if (dbId < 0) {
                showError("Student ID '" + linkId + "' not found in database.");
                return;
            }
            linkedStudentDbId = dbId;
        }

        UserDAO userDAO = new UserDAO();
        if (userDAO.findByUsername(username) != null) {
            showError("Username '" + username + "' already exists.");
            return;
        }

        boolean ok = userDAO.createUser(username, password, role, linkedStudentDbId);
        if (ok) {
            showInfo("User '" + username + "' created as " + role.getLabel() + ".");
            txtNewUsername.setText("");
            txtNewPassword.setText("");
            txtLinkStudentId.setText("");
            refreshAdminData();
        } else {
            showError("Failed to create user.");
        }
    }

    private void handleToggleUser() {
        int row = userTable.getSelectedRow();
        if (row < 0) {
            showError("Select a user row to toggle.");
            return;
        }
        int userId = Integer.parseInt(userTableModel.getValueAt(row, 0).toString());
        String currentEnabled = userTableModel.getValueAt(row, 4).toString();
        boolean newState = !"Yes".equals(currentEnabled);
        UserDAO userDAO = new UserDAO();
        userDAO.setEnabled(userId, newState);
        showInfo("User " + (newState ? "enabled" : "disabled") + ".");
        refreshAdminData();
    }

    private void handleSaveConfig() {
        String key = (String) cmbConfigKey.getSelectedItem();
        String value = txtConfigValue.getText().trim();
        if (key == null || value.isEmpty()) {
            showError("Select a config key and enter a value.");
            return;
        }
        try {
            Double.parseDouble(value); // validate numeric
        } catch (NumberFormatException e) {
            showError("Config value must be a valid number.");
            return;
        }
        ConfigDAO configDAO = new ConfigDAO();
        configDAO.setValue(key, value);
        showInfo("Configuration '" + key + "' updated to " + value + ".");
        txtConfigValue.setText("");
        refreshAdminData();
    }

    private void handleAddCategoryMapping() {
        String subName = txtAdminSubject.getText().trim();
        if (subName.isEmpty()) {
            showError("Enter a subject name.");
            return;
        }
        int catIdx = cmbAdminCategory.getSelectedIndex();
        SubjectCategory[] cats = { SubjectCategory.STEM, SubjectCategory.LANGUAGE, SubjectCategory.SOCIAL_SCIENCES };
        if (catIdx < 0 || catIdx >= cats.length) {
            showError("Select a category.");
            return;
        }
        SubjectCategory.addSubjectToCategory(subName, cats[catIdx]);
        showInfo("'" + subName + "' mapped to " + cats[catIdx].getDisplayName() + ".");
        txtAdminSubject.setText("");
        refreshAdminData();
    }

    private void refreshAdminData() {
        // Refresh user table
        if (userTableModel != null) {
            userTableModel.setRowCount(0);
            UserDAO userDAO = new UserDAO();
            for (User u : userDAO.findAll()) {
                String linkedSid = "";
                if (u.getLinkedStudentDbId() != null) {
                    String sid = userDAO.resolveLinkedStudentId(u.getLinkedStudentDbId());
                    linkedSid = sid != null ? sid : "ID#" + u.getLinkedStudentDbId();
                }
                userTableModel.addRow(new Object[] {
                        u.getId(), u.getUsername(), u.getRole().getLabel(),
                        linkedSid, u.isEnabled() ? "Yes" : "No",
                        u.getCreatedAt() != null ? u.getCreatedAt() : ""
                });
            }
        }

        // Refresh mappings + config display
        if (txtCategoryMappings != null) {
            StringBuilder sb = new StringBuilder();

            // Config values
            sb.append("=== Risk Configuration ===\n");
            ConfigDAO configDAO = new ConfigDAO();
            Map<String, String> configs = configDAO.getAll();
            for (Map.Entry<String, String> entry : configs.entrySet()) {
                sb.append("  ").append(entry.getKey()).append(" = ").append(entry.getValue()).append("\n");
            }
            sb.append("\n");

            // Category mappings
            SubjectCategory[] cats = { SubjectCategory.STEM, SubjectCategory.LANGUAGE,
                    SubjectCategory.SOCIAL_SCIENCES };
            for (SubjectCategory cat : cats) {
                sb.append("--- ").append(cat.getDisplayName()).append(" ---\n");
                List<String> subs = SubjectCategory.getSubjectsInCategory(cat);
                if (subs.isEmpty())
                    sb.append("  (none)\n");
                else
                    for (String s : subs)
                        sb.append("  - ").append(s).append("\n");
                sb.append("\n");
            }

            // Summary stats
            sb.append("=== System Summary ===\n");
            UserDAO uDAO = new UserDAO();
            sb.append("  Total users: ").append(uDAO.countAll()).append("\n");
            sb.append("  Total students: ").append(dataManager.getStudentDAO().countAll()).append("\n");

            txtCategoryMappings.setText(sb.toString());
            txtCategoryMappings.setCaretPosition(0);
        }
    }

    // ==========================================
    // UTILITY & STYLE HELPERS
    // ==========================================

    /**
     * Returns the list of students visible to the current user.
     * STUDENT role: only their own data (filtered by
     * SessionManager.getLinkedStudentId()).
     * TEACHER/ADMIN: all students.
     */
    private List<Student> getVisibleStudents() {
        List<Student> all = dataManager.getStudents();
        if (currentRole == UserRole.STUDENT) {
            String linkedId = SessionManager.getLinkedStudentId();
            if (linkedId != null) {
                List<Student> filtered = new ArrayList<>();
                for (Student s : all) {
                    if (linkedId.equals(s.getId())) {
                        filtered.add(s);
                    }
                }
                return filtered;
            }
            // No linked student — show nothing
            return new ArrayList<>();
        }
        return all;
    }

    private void showError(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Error", JOptionPane.ERROR_MESSAGE);
    }

    private void showInfo(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Success", JOptionPane.INFORMATION_MESSAGE);
    }

    private JLabel createLabel(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(StyleConstants.TEXT_FG);
        return label;
    }

    private JTextField createTextField() {
        JTextField tf = new JTextField(15);
        tf.setMargin(StyleConstants.TEXT_FIELD_MARGIN);
        return tf;
    }

    private JTextArea createTextArea() {
        JTextArea area = new JTextArea();
        area.setEditable(false);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setFont(StyleConstants.MONO_FONT);
        area.setMargin(new Insets(8, 10, 8, 10));
        area.setBackground(Color.WHITE);
        return area;
    }

    // ==========================================
    // MODERN TABLE SETUP (Phase 5)
    // ==========================================

    private void setupModernTable(JTable table) {
        table.setRowHeight(StyleConstants.TABLE_ROW_HEIGHT);
        table.setShowHorizontalLines(true);
        table.setShowVerticalLines(false);
        table.setGridColor(StyleConstants.DIVIDER);
        table.setSelectionBackground(StyleConstants.PRIMARY_LIGHT);
        table.setSelectionForeground(StyleConstants.TEXT_FG);
        table.setIntercellSpacing(new Dimension(0, 1));
        table.setFillsViewportHeight(true);
        table.getTableHeader().setReorderingAllowed(false);

        // Alternating row renderer for non-special columns
        DefaultTableCellRenderer altRowRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object v,
                    boolean sel, boolean focus, int row, int col) {
                Component c = super.getTableCellRendererComponent(t, v, sel, focus, row, col);
                if (!sel) {
                    c.setBackground(row % 2 == 0 ? Color.WHITE : StyleConstants.TABLE_ALT_ROW);
                }
                c.setForeground(StyleConstants.TEXT_FG);
                setFont(StyleConstants.TABLE_BODY_FONT);
                setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 12));
                return c;
            }
        };
        for (int i = 0; i < table.getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setCellRenderer(altRowRenderer);
        }

        // Center-align score columns
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object v,
                    boolean sel, boolean focus, int row, int col) {
                Component c = super.getTableCellRendererComponent(t, v, sel, focus, row, col);
                if (!sel)
                    c.setBackground(row % 2 == 0 ? Color.WHITE : StyleConstants.TABLE_ALT_ROW);
                setHorizontalAlignment(SwingConstants.RIGHT);
                setFont(StyleConstants.TABLE_BODY_FONT);
                setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 12));
                return c;
            }
        };
        if (table.getColumnCount() > 2)
            table.getColumnModel().getColumn(2).setCellRenderer(centerRenderer);
        if (table.getColumnCount() > 4)
            table.getColumnModel().getColumn(4).setCellRenderer(centerRenderer);

        // Table header styling
        JTableHeader header = table.getTableHeader();
        header.setBackground(StyleConstants.TABLE_HEADER_BG);
        header.setForeground(StyleConstants.TEXT_SECONDARY);
        header.setFont(StyleConstants.TABLE_HEADER_FONT);
        header.setBorder(BorderFactory.createMatteBorder(0, 0, 2, 0, StyleConstants.DIVIDER));
        header.setPreferredSize(new Dimension(0, 42));

        DefaultTableCellRenderer headerRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object v,
                    boolean sel, boolean focus, int row, int col) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(t, v, sel, focus, row, col);
                l.setBackground(StyleConstants.TABLE_HEADER_BG);
                l.setForeground(StyleConstants.TEXT_SECONDARY);
                l.setFont(StyleConstants.TABLE_HEADER_FONT);
                l.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(0, 0, 2, 0, StyleConstants.DIVIDER),
                        BorderFactory.createEmptyBorder(0, 12, 0, 12)));
                l.setHorizontalAlignment(SwingConstants.LEFT);
                return l;
            }
        };
        for (int i = 0; i < table.getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setHeaderRenderer(headerRenderer);
        }

        // Override special columns AFTER general setup
        if (table.getColumnCount() > 3)
            table.getColumnModel().getColumn(3).setCellRenderer(new RiskBadgeRenderer());
        if (table.getColumnCount() > 5)
            table.getColumnModel().getColumn(5).setCellRenderer(new TrendArrowRenderer());
    }

    // ==========================================
    // CUSTOM CELL RENDERERS (Phase 5 + Phase 9)
    // ==========================================

    /**
     * Risk level rendered as a colored badge with rounded background.
     */
    private static class RiskBadgeRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean sel, boolean focus, int row, int col) {
            JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, sel, focus, row, col);
            label.setHorizontalAlignment(SwingConstants.CENTER);
            label.setFont(new Font("Segoe UI", Font.BOLD, 11));
            label.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));

            if (!sel && value != null) {
                String level = value.toString();
                if (level.contains("High")) {
                    label.setForeground(StyleConstants.ACCENT_RED);
                    label.setBackground(StyleConstants.ACCENT_RED_BG);
                    label.setText("\u26A0 " + level);
                } else if (level.contains("Moderate")) {
                    label.setForeground(StyleConstants.ACCENT_ORANGE);
                    label.setBackground(StyleConstants.ACCENT_ORANGE_BG);
                    label.setText("\u25CF " + level);
                } else {
                    label.setForeground(StyleConstants.ACCENT_GREEN);
                    label.setBackground(StyleConstants.ACCENT_GREEN_BG);
                    label.setText("\u2713 " + level);
                }
                label.setOpaque(true);
            } else {
                label.setBackground(row % 2 == 0 ? Color.WHITE : StyleConstants.TABLE_ALT_ROW);
            }
            return label;
        }
    }

    /**
     * Trend rendered with colored arrow symbols.
     */
    private static class TrendArrowRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean sel, boolean focus, int row, int col) {
            JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, sel, focus, row, col);
            label.setHorizontalAlignment(SwingConstants.CENTER);
            label.setFont(new Font("Segoe UI", Font.BOLD, 13));
            label.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));

            if (!sel && value != null) {
                String trend = value.toString();
                if (trend.contains("Improving")) {
                    label.setText("\u2191 Improving");
                    label.setForeground(StyleConstants.ACCENT_GREEN);
                } else if (trend.contains("Declining")) {
                    label.setText("\u2193 Declining");
                    label.setForeground(StyleConstants.ACCENT_RED);
                } else {
                    label.setText("\u2192 Stable");
                    label.setForeground(StyleConstants.TEXT_SECONDARY);
                }
                label.setBackground(row % 2 == 0 ? Color.WHITE : StyleConstants.TABLE_ALT_ROW);
                label.setOpaque(true);
            }
            return label;
        }
    }
}
