package tracker.ui.fx;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import tracker.model.*;
import tracker.model.RiskScore.Level;
import tracker.service.AnalyticsService;

import java.util.*;

/**
 * Dashboard page controller.
 * STUDENT: personal profile metrics
 * TEACHER/ADMIN: institutional overview with metric cards
 */
public class DashboardController {

    private final MainController main;

    // Labels updated on refresh
    private Label metricTotal, metricHighRisk, metricAvg, metricWeak;
    // Student-specific
    private Label profileAvg, profileRisk, profileTrend, profileSubjects;
    private VBox profileSubjectsPanel, profileRecsPanel;

    public DashboardController(MainController main) {
        this.main = main;
    }

    public Region buildPage() {
        if (main.currentRole == UserRole.STUDENT) {
            return buildStudentDashboard();
        } else {
            return buildInstitutionalDashboard();
        }
    }

    // ------------------------------------------------------------------
    // STUDENT DASHBOARD
    // ------------------------------------------------------------------

    private VBox buildStudentDashboard() {
        VBox page = new VBox(16);
        page.setStyle(FxStyles.PAGE_BG);
        page.setPadding(new Insets(20, 24, 20, 24));

        Label title = new Label("My Academic Profile");
        title.setStyle(FxStyles.title());

        // Top metric cards
        GridPane metricsRow = new GridPane();
        metricsRow.setHgap(16);
        metricsRow.setVgap(0);
        metricsRow.setPrefHeight(130);

        profileAvg = new Label("--");
        profileRisk = new Label("--");
        profileTrend = new Label("--");
        profileSubjects = new Label("0");

        metricsRow.add(buildMetricCard("My Average", profileAvg, FxStyles.C_PRIMARY, FxStyles.C_PRIMARY_LIGHT), 0, 0);
        metricsRow.add(buildMetricCard("Risk Level", profileRisk, FxStyles.C_RED, FxStyles.C_RED_BG), 1, 0);
        metricsRow.add(buildMetricCard("Trend", profileTrend, FxStyles.C_GREEN, FxStyles.C_GREEN_BG), 2, 0);
        metricsRow.add(buildMetricCard("Subjects", profileSubjects, FxStyles.C_ORANGE, FxStyles.C_ORANGE_BG), 3, 0);

        for (int i = 0; i < 4; i++) {
            ColumnConstraints cc = new ColumnConstraints();
            cc.setPercentWidth(25);
            metricsRow.getColumnConstraints().add(cc);
        }

        // Center: subjects + recommendations
        HBox centerRow = new HBox(16);
        VBox.setVgrow(centerRow, Priority.ALWAYS);

        VBox subjectCard = buildCard("My Subjects & Scores");
        profileSubjectsPanel = new VBox(4);
        profileSubjectsPanel.setPadding(new Insets(4));
        ScrollPane subjectScroll = new ScrollPane(profileSubjectsPanel);
        subjectScroll.setFitToWidth(true);
        subjectScroll.setStyle("-fx-background: white; -fx-background-color: white;");
        VBox.setVgrow(subjectScroll, Priority.ALWAYS);
        subjectCard.getChildren().add(subjectScroll);
        HBox.setHgrow(subjectCard, Priority.ALWAYS);

        VBox recCard = buildCard("AI Recommendations for Me");
        profileRecsPanel = new VBox(6);
        profileRecsPanel.setPadding(new Insets(4));
        ScrollPane recScroll = new ScrollPane(profileRecsPanel);
        recScroll.setFitToWidth(true);
        recScroll.setStyle("-fx-background: white; -fx-background-color: white;");
        VBox.setVgrow(recScroll, Priority.ALWAYS);
        recCard.getChildren().add(recScroll);
        HBox.setHgrow(recCard, Priority.ALWAYS);

        centerRow.getChildren().addAll(subjectCard, recCard);
        page.getChildren().addAll(title, metricsRow, centerRow);
        return page;
    }

    // ------------------------------------------------------------------
    // INSTITUTIONAL DASHBOARD (TEACHER / ADMIN)
    // ------------------------------------------------------------------

    private VBox buildInstitutionalDashboard() {
        VBox page = new VBox(16);
        page.setStyle(FxStyles.PAGE_BG);
        page.setPadding(new Insets(20, 24, 20, 24));

        String titleText = main.currentRole == UserRole.TEACHER ? "Teacher Dashboard" : "Admin Dashboard";
        Label title = new Label(titleText);
        title.setStyle(FxStyles.title());

        // Metric cards
        GridPane metricsRow = new GridPane();
        metricsRow.setHgap(16);
        metricsRow.setPrefHeight(130);

        metricTotal = new Label("0");
        metricHighRisk = new Label("0");
        metricAvg = new Label("0.0");
        metricWeak = new Label("N/A");

        metricsRow.add(buildMetricCard("Total Students", metricTotal, FxStyles.C_PRIMARY, FxStyles.C_PRIMARY_LIGHT), 0,
                0);
        metricsRow.add(buildMetricCard("High Risk", metricHighRisk, FxStyles.C_RED, FxStyles.C_RED_BG), 1, 0);
        metricsRow.add(buildMetricCard("Institutional Avg", metricAvg, FxStyles.C_GREEN, FxStyles.C_GREEN_BG), 2, 0);
        metricsRow.add(buildMetricCard("Most Weak Subject", metricWeak, FxStyles.C_ORANGE, FxStyles.C_ORANGE_BG), 3, 0);

        for (int i = 0; i < 4; i++) {
            ColumnConstraints cc = new ColumnConstraints();
            cc.setPercentWidth(25);
            metricsRow.getColumnConstraints().add(cc);
        }

        // Summary card
        VBox summaryCard = buildCard("Quick Summary");
        String summary;
        if (main.currentRole == UserRole.TEACHER) {
            summary = "Welcome, " + main.currentUsername + "!\n\n" +
                    "You have full access to student data, analytics, simulation, and reports.\n" +
                    "Use the 'Manage' page to edit or delete student records.\n" +
                    "Use the 'Students' page to add new student scores.\n\n" +
                    "Quick tip: Check the Reports page for at-risk students.";
        } else {
            summary = "Welcome, " + main.currentUsername + "!\n\n" +
                    "Full system administration access enabled.\n" +
                    "Use the Admin page to manage users, roles, and system configuration.\n" +
                    "Use the 'Manage' page to edit or delete student records.\n\n" +
                    "System: SQLite DB active  |  Role: " + main.currentRole.getLabel();
        }

        TextArea txtSummary = new TextArea(summary);
        txtSummary.setEditable(false);
        txtSummary.setWrapText(true);
        txtSummary.setStyle("-fx-font-family: 'Segoe UI'; -fx-font-size: 13; -fx-text-fill: #1E293B;");
        VBox.setVgrow(txtSummary, Priority.ALWAYS);
        summaryCard.getChildren().add(txtSummary);
        VBox.setVgrow(summaryCard, Priority.ALWAYS);

        page.getChildren().addAll(title, metricsRow, summaryCard);
        return page;
    }

    // ------------------------------------------------------------------
    // REFRESH
    // ------------------------------------------------------------------

    void refresh() {
        if (main.currentRole == UserRole.STUDENT) {
            refreshStudentProfile();
        } else {
            refreshInstitutionalMetrics();
        }
    }

    private void refreshInstitutionalMetrics() {
        List<Student> students = main.dataManager.getStudents();
        if (metricTotal == null)
            return;
        metricTotal.setText(String.valueOf(students.size()));

        if (students.isEmpty()) {
            metricHighRisk.setText("0");
            metricAvg.setText("0.0");
            metricWeak.setText("N/A");
            return;
        }

        Map<Level, Integer> dist = main.analyticsService.getRiskDistribution(students);
        metricHighRisk.setText(String.valueOf(dist.getOrDefault(Level.HIGH, 0)));

        double avg = main.analyticsService.getInstitutionalAverage(students);
        metricAvg.setText(String.format("%.1f", avg));

        String weak = main.analyticsService.getMostCommonWeakSubject(students);
        metricWeak.setText(weak.length() > 10 ? weak.substring(0, 10) : weak);
    }

    private void refreshStudentProfile() {
        List<Student> visible = main.dataManager.getStudents()
                .stream()
                .filter(s -> {
                    String linked = main.sessionLinkedId();
                    return linked == null || s.getId().equals(linked);
                })
                .collect(java.util.stream.Collectors.toList());

        if (visible.isEmpty()) {
            if (profileAvg != null)
                profileAvg.setText("--");
            if (profileRisk != null)
                profileRisk.setText("--");
            if (profileTrend != null)
                profileTrend.setText("--");
            if (profileSubjects != null)
                profileSubjects.setText("0");
            if (profileSubjectsPanel != null) {
                profileSubjectsPanel.getChildren().clear();
                Label noData = new Label("No linked student data. Ask your administrator.");
                noData.setStyle(FxStyles.label());
                profileSubjectsPanel.getChildren().add(noData);
            }
            return;
        }

        Student student = visible.get(0);
        RiskScore risk = main.riskPredictor.assessRisk(student);
        TrendDirection trend = main.trendAnalyzer.getTrend(student.getId());

        if (profileAvg != null)
            profileAvg.setText(String.format("%.1f", student.getAverageScore()));
        if (profileRisk != null)
            profileRisk.setText(risk.getLevel().getLabel());
        if (profileTrend != null)
            profileTrend.setText(trend.getLabel());
        if (profileSubjects != null)
            profileSubjects.setText(String.valueOf(student.getSubjects().size()));

        if (profileSubjectsPanel != null) {
            profileSubjectsPanel.getChildren().clear();
            for (Subject sub : student.getSubjects()) {
                boolean weak = sub.getScore() < 60;
                Label row = new Label(String.format("%-18s  %.1f%s",
                        sub.getSubjectName(), sub.getScore(), weak ? "  [WEAK]" : ""));
                row.setStyle("-fx-font-family: 'Consolas'; -fx-font-size: 12; -fx-text-fill: " +
                        (weak ? FxStyles.C_RED : FxStyles.C_TEXT_DARK) + ";");
                profileSubjectsPanel.getChildren().add(row);
            }
        }

        if (profileRecsPanel != null) {
            profileRecsPanel.getChildren().clear();
            List<String> recs = main.adaptivePlanner.generateRecommendations(student, risk, trend);
            for (String rec : recs) {
                if (!rec.trim().isEmpty() && !rec.startsWith("---")) {
                    Label l = new Label(rec);
                    l.setStyle("-fx-font-family: 'Segoe UI'; -fx-font-size: 12; -fx-text-fill: #1E293B;");
                    l.setWrapText(true);
                    profileRecsPanel.getChildren().add(l);
                }
            }
        }
    }

    // ------------------------------------------------------------------
    // HELPERS
    // ------------------------------------------------------------------

    private VBox buildMetricCard(String labelText, Label valueLabel, String accentColor, String bgColor) {
        VBox card = new VBox(4);
        card.setStyle(
                "-fx-background-color: white;" +
                        "-fx-background-radius: 12;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.07), 8, 0, 0, 2);" +
                        "-fx-padding: 20 24 16 24;");

        valueLabel.setStyle(FxStyles.metricNumber(accentColor));
        Label desc = new Label(labelText);
        desc.setStyle(FxStyles.smallLabel());

        // Top accent bar
        Region bar = new Region();
        bar.setPrefHeight(4);
        bar.setMaxWidth(Double.MAX_VALUE);
        bar.setStyle("-fx-background-color: " + accentColor + "; -fx-background-radius: 2;");

        StackPane cardActual = new StackPane();
        VBox inner = new VBox(4, valueLabel, desc);
        inner.setPadding(new Insets(8, 0, 0, 0));
        cardActual.getChildren().addAll(inner);

        card.getChildren().addAll(bar, valueLabel, desc);
        return card;
    }

    private VBox buildCard(String titleText) {
        VBox card = new VBox(10);
        card.setStyle(FxStyles.CARD_STYLE);
        VBox.setVgrow(card, Priority.ALWAYS);

        Label t = new Label(titleText);
        t.setStyle(FxStyles.sectionTitle());
        card.getChildren().add(t);
        return card;
    }
}
