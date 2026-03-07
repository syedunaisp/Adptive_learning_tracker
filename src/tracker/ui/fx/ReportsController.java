package tracker.ui.fx;

import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import tracker.model.*;
import tracker.model.RiskScore.Level;

import java.io.IOException;
import java.util.*;

/**
 * Reports page controller.
 *
 * PART 3 — Chart changes:
 * - Risk Distribution: replaced Pie Chart → JavaFX ScatterChart
 * (green/orange/red)
 * - Student Risk Scores: replaced Bar Chart → JavaFX LineChart (connected line
 * + markers)
 *
 * Business logic unchanged — same RiskPredictor, AnalyticsService, FileManager
 * calls.
 */
public class ReportsController {

    private final MainController main;

    private TableView<ReportRow> reportTable;
    private TextArea detailArea;
    private VBox chartsRow;

    public ReportsController(MainController main) {
        this.main = main;
    }

    public Region buildPage() {
        VBox page = new VBox(16);
        page.setStyle(FxStyles.PAGE_BG);
        page.setPadding(new Insets(20, 24, 20, 24));

        Label title = new Label("Risk Reports");
        title.setStyle(FxStyles.title());

        // Actions card
        VBox actionsCard = buildCard("Actions");
        HBox actions = new HBox(12);
        actions.setAlignment(Pos.CENTER_LEFT);

        Button btnAtRisk = new Button("Show At-Risk");
        btnAtRisk.setStyle(FxStyles.primaryButton());
        btnAtRisk.setOnAction(e -> handleShowAtRisk());
        actions.getChildren().add(btnAtRisk);

        if (main.currentRole.canExportReports()) {
            Button btnExport = new Button("Export All");
            btnExport.setStyle(FxStyles.coloredButton(FxStyles.C_GREEN));
            btnExport.setOnAction(e -> handleExport());
            actions.getChildren().add(btnExport);
        }

        actionsCard.getChildren().add(actions);

        // Charts row — TWO charts side by side
        chartsRow = new VBox(16);
        chartsRow.setPrefHeight(320);
        chartsRow.setMinHeight(300);

        // Get data for initial chart render
        HBox initialCharts = buildCharts();
        chartsRow.getChildren().add(initialCharts);

        // At-Risk table card
        VBox tableCard = buildCard("At-Risk Students");
        reportTable = new TableView<>();
        reportTable.setStyle("-fx-font-family: 'Segoe UI'; -fx-font-size: 13;");
        reportTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        reportTable.setPrefHeight(200);

        addCol("ID", "id");
        addCol("Name", "name");
        addCol("Average", "average");
        addCol("Risk Level", "riskLevel");
        addCol("Risk Score", "riskScore");
        addCol("Trend", "trend");

        VBox.setVgrow(reportTable, Priority.ALWAYS);
        tableCard.getChildren().add(reportTable);

        // Detail card (plain-text report)
        VBox detailCard = buildCard("Intelligence Report");
        detailArea = new TextArea("Click 'Show At-Risk' and select a student to view their intelligence report.");
        detailArea.setEditable(false);
        detailArea.setWrapText(true);
        detailArea.setStyle("-fx-font-family: 'Segoe UI'; -fx-font-size: 12;");
        VBox.setVgrow(detailArea, Priority.ALWAYS);
        detailCard.getChildren().add(detailArea);
        VBox.setVgrow(detailCard, Priority.ALWAYS);

        reportTable.getSelectionModel().selectedItemProperty().addListener(
                (obs, old, selected) -> {
                    if (selected != null)
                        showDetailReport(selected.id);
                });

        page.getChildren().addAll(title, actionsCard, chartsRow, tableCard, detailCard);
        VBox.setVgrow(detailCard, Priority.ALWAYS);
        return page;
    }

    void refresh() {
        // Charts refresh on each page visit
        if (chartsRow != null) {
            chartsRow.getChildren().clear();
            chartsRow.getChildren().add(buildCharts());
        }
    }

    /**
     * Builds the two chart cards side by side:
     * Left: Scatter-style Risk Distribution (green/orange/red dots)
     * Right: Line Graph of Student Risk Scores
     */
    private HBox buildCharts() {
        List<Student> students = main.dataManager.getStudents();

        int high = 0, mod = 0, low = 0;
        List<String> names = new ArrayList<>();
        List<Double> riskScores = new ArrayList<>();

        for (Student s : students) {
            if (s.getSubjects().isEmpty())
                continue;
            RiskScore risk = main.riskPredictor.assessRisk(s);
            if (risk.getLevel() == Level.HIGH)
                high++;
            else if (risk.getLevel() == Level.MODERATE)
                mod++;
            else
                low++;
            names.add(s.getName());
            riskScores.add(risk.getNumericScore());
        }

        // Pie chart (risk distribution)
        PieChart pie = ChartBuilder.buildRiskPieChart(high, mod, low);
        pie.setPrefHeight(260);

        // Line chart (student risk scores)
        LineChart<String, Number> line = ChartBuilder.buildStudentRiskLineChart(names, riskScores);
        line.setPrefHeight(260);

        VBox pieCard = ChartBuilder.chartCard("Risk Distribution", pie);
        VBox lineCard = ChartBuilder.chartCard("Student Risk Scores", line);

        HBox.setHgrow(pieCard, Priority.ALWAYS);
        HBox.setHgrow(lineCard, Priority.ALWAYS);

        HBox row = new HBox(16, pieCard, lineCard);
        row.setPrefHeight(300);
        return row;
    }

    private void handleShowAtRisk() {
        if (reportTable == null)
            return;
        reportTable.getItems().clear();

        List<Student> atRisk = new ArrayList<>();
        for (Student s : main.dataManager.getStudents()) {
            if (s.getSubjects().isEmpty())
                continue;
            RiskScore risk = main.riskPredictor.assessRisk(s);
            if (risk.getLevel() == Level.HIGH || risk.getLevel() == Level.MODERATE) {
                atRisk.add(s);
            }
        }

        for (Student s : atRisk) {
            RiskScore risk = main.riskPredictor.assessRisk(s);
            TrendDirection trend = main.trendAnalyzer.getTrend(s.getId());
            reportTable.getItems().add(new ReportRow(
                    s.getId(), s.getName(),
                    String.format("%.2f", s.getAverageScore()),
                    risk.getLevel().getLabel(),
                    String.format("%.1f", risk.getNumericScore()),
                    trend.getLabel()));
        }
    }

    private void showDetailReport(String studentId) {
        Student student = main.dataManager.findStudentById(studentId);
        if (student == null)
            return;

        RiskScore risk = main.riskPredictor.assessRisk(student);
        TrendDirection trend = main.trendAnalyzer.getTrend(student.getId());
        List<String> recs = main.adaptivePlanner.generateRecommendations(student, risk, trend);

        StringBuilder sb = new StringBuilder();
        sb.append("=== Academic Intelligence Report ===\n\n");
        sb.append("Student : ").append(student.getName())
                .append(" (").append(student.getId()).append(")\n");
        sb.append("Average : ").append(String.format("%.2f", student.getAverageScore())).append("\n");
        sb.append("Risk    : ").append(risk.getLevel().getLabel())
                .append(" (").append(String.format("%.1f", risk.getNumericScore())).append(")\n");
        sb.append("Trend   : ").append(trend.getLabel()).append(" ").append(trend.getArrow()).append("\n");

        sb.append("\n--- Subject Performance ---\n");
        for (Subject sub : student.getSubjects()) {
            sb.append(String.format("  %-20s %.1f%s\n",
                    sub.getSubjectName(), sub.getScore(),
                    sub.getScore() < 60 ? "  [WEAK]" : ""));
        }

        sb.append("\n--- Risk Breakdown ---\n");
        sb.append(risk.getDetailedBreakdown()).append("\n");

        sb.append("\n--- AI Recommendations ---\n");
        for (String r : recs) {
            if (!r.trim().isEmpty() && !r.contains("---")) {
                sb.append(r).append("\n");
            }
        }

        detailArea.setText(sb.toString());
        detailArea.positionCaret(0);
    }

    private void handleExport() {
        List<Student> students = main.dataManager.getStudents();
        if (students.isEmpty()) {
            main.showError("No students to export.");
            return;
        }

        List<RiskScore> risks = new ArrayList<>();
        List<TrendDirection> trends = new ArrayList<>();
        List<List<String>> allRecs = new ArrayList<>();

        for (Student s : students) {
            RiskScore r = main.riskPredictor.assessRisk(s);
            TrendDirection t = main.trendAnalyzer.getTrend(s.getId());
            risks.add(r);
            trends.add(t);
            allRecs.add(main.adaptivePlanner.generateRecommendations(s, r, t));
        }

        try {
            main.fileManager.saveBulkReport(students, risks, trends, allRecs);
            main.showInfo("Exported " + students.size() + " reports to academic_report.txt.");
        } catch (IOException ex) {
            main.showError("Export failed: " + ex.getMessage());
        }
    }

    // Helpers
    private void addCol(String header, String prop) {
        TableColumn<ReportRow, String> col = new TableColumn<>(header);
        col.setCellValueFactory(data -> new SimpleStringProperty(
                switch (prop) {
                    case "id" -> data.getValue().id;
                    case "name" -> data.getValue().name;
                    case "average" -> data.getValue().average;
                    case "riskLevel" -> data.getValue().riskLevel;
                    case "riskScore" -> data.getValue().riskScore;
                    case "trend" -> data.getValue().trend;
                    default -> "";
                }));
        reportTable.getColumns().add(col);
    }

    private VBox buildCard(String titleText) {
        VBox card = new VBox(10);
        card.setStyle(FxStyles.CARD_STYLE);
        Label t = new Label(titleText);
        t.setStyle(FxStyles.sectionTitle());
        card.getChildren().add(t);
        return card;
    }

    public static class ReportRow {
        public final String id, name, average, riskLevel, riskScore, trend;

        public ReportRow(String id, String name, String avg, String rl, String rs, String t) {
            this.id = id;
            this.name = name;
            this.average = avg;
            this.riskLevel = rl;
            this.riskScore = rs;
            this.trend = t;
        }
    }
}
