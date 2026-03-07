package tracker.ui.fx;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import tracker.model.*;
import tracker.model.RiskScore.Level;

import java.util.*;

/**
 * Institutional Analytics page.
 *
 * PART 4 — Teacher Dashboard Chart Changes:
 * - Replaces JFreeChart Pie Chart with JavaFX ScatterChart (risk distribution)
 * - Replaces JFreeChart Bar Chart with JavaFX BarChart (category performance)
 *
 * Business logic unchanged — same AnalyticsService calls.
 */
public class AnalyticsController {

    private final MainController main;

    private Label avgLabel, totalLabel, weakLabel, catLabel;
    private VBox chartsRow;
    private TextArea txtSummary;

    public AnalyticsController(MainController main) {
        this.main = main;
    }

    public Region buildPage() {
        VBox page = new VBox(16);
        page.setStyle(FxStyles.PAGE_BG);
        page.setPadding(new Insets(20, 24, 20, 24));

        Label title = new Label("Institutional Analytics");
        title.setStyle(FxStyles.title());

        // Refresh button
        HBox topRow = new HBox(12);
        topRow.setAlignment(Pos.CENTER_LEFT);
        Button btnRefresh = new Button("Refresh");
        btnRefresh.setStyle(FxStyles.primaryButton());
        btnRefresh.setOnAction(e -> refresh());
        topRow.getChildren().add(btnRefresh);

        // Charts area (populated on refresh)
        chartsRow = new VBox(16);
        chartsRow.setPrefHeight(300);
        chartsRow.setMinHeight(260);

        // Metric cards row
        GridPane metricsGrid = new GridPane();
        metricsGrid.setHgap(16);
        metricsGrid.setPrefHeight(110);

        avgLabel = new Label("--");
        totalLabel = new Label("--");
        weakLabel = new Label("--");
        catLabel = new Label("--");

        metricsGrid.add(buildMetricCard("Inst. Average", avgLabel, FxStyles.C_PRIMARY, FxStyles.C_PRIMARY_LIGHT), 0, 0);
        metricsGrid.add(buildMetricCard("Total Students", totalLabel, FxStyles.C_BLUE, FxStyles.C_BLUE_BG), 1, 0);
        metricsGrid.add(buildMetricCard("Most Weak Subject", weakLabel, FxStyles.C_RED, FxStyles.C_RED_BG), 2, 0);
        metricsGrid.add(buildMetricCard("Weakest Category", catLabel, FxStyles.C_ORANGE, FxStyles.C_ORANGE_BG), 3, 0);

        for (int i = 0; i < 4; i++) {
            ColumnConstraints cc = new ColumnConstraints();
            cc.setPercentWidth(25);
            metricsGrid.getColumnConstraints().add(cc);
        }

        // Summary text
        VBox summaryCard = buildCard("Summary Text");
        txtSummary = new TextArea("Click Refresh to load analytics.");
        txtSummary.setEditable(false);
        txtSummary.setWrapText(true);
        txtSummary.setStyle("-fx-font-family: 'Consolas'; -fx-font-size: 12;");
        VBox.setVgrow(txtSummary, Priority.ALWAYS);
        summaryCard.getChildren().add(txtSummary);
        VBox.setVgrow(summaryCard, Priority.ALWAYS);

        page.getChildren().addAll(title, topRow, chartsRow, metricsGrid, summaryCard);
        VBox.setVgrow(summaryCard, Priority.ALWAYS);
        return page;
    }

    void refresh() {
        List<Student> students = main.dataManager.getStudents();

        if (students.isEmpty()) {
            if (txtSummary != null)
                txtSummary.setText("No data available. Add students first.");
            if (chartsRow != null)
                chartsRow.getChildren().clear();
            return;
        }

        // Update metric cards
        double avg = main.analyticsService.getInstitutionalAverage(students);
        String weakSub = main.analyticsService.getMostCommonWeakSubject(students);
        SubjectCategory weakCat = main.analyticsService.getWeakestCategory(students);

        avgLabel.setText(String.format("%.1f", avg));
        totalLabel.setText(String.valueOf(students.size()));
        weakLabel.setText(weakSub != null ? weakSub : "None");
        catLabel.setText(weakCat != null ? weakCat.name() : "None");

        // --- Build charts ---
        int high = 0, mod = 0, low = 0;
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
        }

        // Pie chart (risk distribution)
        PieChart pie = ChartBuilder.buildRiskPieChart(high, mod, low);
        pie.setPrefHeight(260);

        // Category performance bar chart
        Map<SubjectCategory, Double> catAvgs = main.analyticsService.getCategoryAverages(students);
        Map<String, Double> catMap = new LinkedHashMap<>();
        for (Map.Entry<SubjectCategory, Double> e : catAvgs.entrySet()) {
            if (e.getKey() != SubjectCategory.UNCATEGORIZED) {
                catMap.put(e.getKey().name(), e.getValue());
            }
        }
        BarChart<String, Number> barChart = ChartBuilder.buildCategoryBarChart(catMap);
        barChart.setPrefHeight(260);

        VBox pieCard = ChartBuilder.chartCard("Risk Distribution", pie);
        VBox barCard = ChartBuilder.chartCard("Category Performance", barChart);
        HBox.setHgrow(pieCard, Priority.ALWAYS);
        HBox.setHgrow(barCard, Priority.ALWAYS);

        HBox charts = new HBox(16, pieCard, barCard);
        charts.setPrefHeight(280);

        chartsRow.getChildren().clear();
        chartsRow.getChildren().add(charts);

        // Summary text
        txtSummary.setText(main.analyticsService.generateAnalyticsSummary(students));
        txtSummary.positionCaret(0);
    }

    private VBox buildMetricCard(String labelText, Label valueLabel, String accent, String bg) {
        VBox card = new VBox(4);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 12;" +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.07), 8, 0, 0, 2); -fx-padding: 16 20 12 20;");

        Region bar = new Region();
        bar.setPrefHeight(4);
        bar.setMaxWidth(Double.MAX_VALUE);
        bar.setStyle("-fx-background-color: " + accent + "; -fx-background-radius: 2;");

        valueLabel.setStyle(FxStyles.metricNumber(accent));
        Label desc = new Label(labelText);
        desc.setStyle(FxStyles.smallLabel());

        card.getChildren().addAll(bar, valueLabel, desc);
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
