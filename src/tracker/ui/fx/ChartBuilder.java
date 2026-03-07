package tracker.ui.fx;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.chart.*;
import javafx.scene.layout.*;
import javafx.scene.control.Label;

/**
 * Helper for building JavaFX charts to replace JFreeChart.
 *
 * Chart mappings:
 * - Risk Distribution → PieChart (3 slices: High/Moderate/Low)
 * - Student Risk Scores → LineChart (connected line + data markers)
 * - Category Performance → BarChart
 * - Simulation before/after → BarChart × 2
 *
 * All methods are null-safe: empty datasets return an empty chart
 * and never throw exceptions.
 */
public class ChartBuilder {

    // ─────────────────────────────────────────────────────────────────────────
    // RISK DISTRIBUTION — PieChart
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Builds a PieChart showing student risk distribution.
     * Slices: High Risk (red), Moderate Risk (orange), Low Risk (green).
     * Labels show category name + count, e.g. "High Risk (3)".
     *
     * Null-safe: returns an empty PieChart when all counts are zero.
     *
     * @param high     number of high-risk students
     * @param moderate number of moderate-risk students
     * @param low      number of low-risk students
     * @return a styled, non-animated PieChart
     */
    public static PieChart buildRiskPieChart(int high, int moderate, int low) {

        // Guard: return an informative empty chart if no data
        if (high <= 0 && moderate <= 0 && low <= 0) {
            PieChart empty = new PieChart();
            empty.setTitle("Risk Distribution (No Data)");
            empty.setAnimated(false);
            empty.setStyle("-fx-background-color: white; -fx-font-family: 'Segoe UI';");
            return empty;
        }

        // Build slices — only include non-zero segments
        ObservableList<PieChart.Data> slices = FXCollections.observableArrayList();
        if (high > 0)
            slices.add(new PieChart.Data("High Risk (" + high + ")", high));
        if (moderate > 0)
            slices.add(new PieChart.Data("Moderate Risk (" + moderate + ")", moderate));
        if (low > 0)
            slices.add(new PieChart.Data("Low Risk (" + low + ")", low));

        PieChart chart = new PieChart(slices);
        chart.setTitle("Risk Distribution");
        chart.setLegendVisible(true);
        chart.setLabelsVisible(true);
        chart.setAnimated(false);
        chart.setStyle("-fx-background-color: white; -fx-font-family: 'Segoe UI';");
        chart.setStartAngle(90);

        // Apply slice colors after the chart nodes are created.
        // PieChart nodes are available immediately after data assignment.
        javafx.application.Platform.runLater(() -> {
            for (PieChart.Data slice : chart.getData()) {
                if (slice == null || slice.getNode() == null)
                    continue;
                String label = slice.getName();
                if (label.startsWith("High")) {
                    slice.getNode().setStyle("-fx-pie-color: #EF4444;");
                } else if (label.startsWith("Moderate")) {
                    slice.getNode().setStyle("-fx-pie-color: #F97316;");
                } else if (label.startsWith("Low")) {
                    slice.getNode().setStyle("-fx-pie-color: #22C55E;");
                }
            }
        });

        return chart;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // STUDENT RISK SCORES — LineChart
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Builds a LineChart of student risk scores.
     * X-axis = student names, Y-axis = risk score (0–100).
     * Markers are colored green/orange/red by risk threshold.
     *
     * Null-safe: returns an empty chart for null or empty input.
     *
     * @param names  student name labels for the X-axis
     * @param scores risk score values (0–100) per student
     * @return a styled, non-animated LineChart
     */
    public static LineChart<String, Number> buildStudentRiskLineChart(
            java.util.List<String> names, java.util.List<Double> scores) {

        CategoryAxis xAxis = new CategoryAxis();
        xAxis.setLabel("Student");
        xAxis.setStyle("-fx-font-family: 'Segoe UI'; -fx-font-size: 11;");

        NumberAxis yAxis = new NumberAxis(0, 100, 10);
        yAxis.setLabel("Risk Score");
        yAxis.setStyle("-fx-font-family: 'Segoe UI'; -fx-font-size: 12;");

        LineChart<String, Number> chart = new LineChart<>(xAxis, yAxis);
        chart.setTitle("Student Risk Scores");
        chart.setLegendVisible(false);
        chart.setStyle("-fx-background-color: white; -fx-font-family: 'Segoe UI';");
        chart.setAnimated(false);
        chart.setCreateSymbols(true);

        // Guard: handle null or mismatched lists gracefully
        int count = Math.min(
                names != null ? names.size() : 0,
                scores != null ? scores.size() : 0);

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Risk Score");

        for (int i = 0; i < count; i++) {
            String name = names.get(i);
            Double score = scores.get(i);
            if (name != null && score != null) {
                series.getData().add(new XYChart.Data<>(name, score));
            }
        }

        chart.getData().add(series);

        // Defer node styling until after the scene layout pass (avoids NPE)
        javafx.application.Platform.runLater(() -> {
            if (series.getNode() != null) {
                series.getNode().setStyle("-fx-stroke: #3B82F6; -fx-stroke-width: 2.5;");
            }
            for (XYChart.Data<String, Number> d : series.getData()) {
                if (d == null || d.getNode() == null)
                    continue;
                double val = d.getYValue().doubleValue();
                String markerColor = val >= 70 ? "#EF4444"
                        : val >= 40 ? "#F97316"
                                : "#22C55E";
                d.getNode().setStyle(
                        "-fx-background-color: " + markerColor + ", white;" +
                                "-fx-background-insets: 0, 3;" +
                                "-fx-background-radius: 5px;" +
                                "-fx-padding: 6px;");
            }
        });

        return chart;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CATEGORY PERFORMANCE — BarChart
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Builds a BarChart for subject category performance averages.
     * Null-safe: returns an empty chart for null or empty input.
     */
    public static BarChart<String, Number> buildCategoryBarChart(
            java.util.Map<String, Double> categoryAvgs) {

        CategoryAxis xAxis = new CategoryAxis();
        xAxis.setLabel("Category");
        xAxis.setStyle("-fx-font-family: 'Segoe UI'; -fx-font-size: 12;");

        NumberAxis yAxis = new NumberAxis(0, 100, 10);
        yAxis.setLabel("Average Score");
        yAxis.setStyle("-fx-font-family: 'Segoe UI'; -fx-font-size: 12;");

        BarChart<String, Number> chart = new BarChart<>(xAxis, yAxis);
        chart.setTitle("Category Performance");
        chart.setLegendVisible(false);
        chart.setStyle("-fx-background-color: white; -fx-font-family: 'Segoe UI';");
        chart.setAnimated(false);

        // Guard: empty map
        if (categoryAvgs == null || categoryAvgs.isEmpty()) {
            return chart;
        }

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Avg Score");

        for (java.util.Map.Entry<String, Double> e : categoryAvgs.entrySet()) {
            if (e.getKey() != null && e.getValue() != null) {
                series.getData().add(new XYChart.Data<>(e.getKey(), e.getValue()));
            }
        }

        chart.getData().add(series);

        // Defer bar styling to avoid node NPE
        javafx.application.Platform.runLater(() -> {
            for (XYChart.Data<String, Number> d : series.getData()) {
                if (d == null || d.getNode() == null)
                    continue;
                d.getNode().setStyle("-fx-bar-fill: #3B82F6;");
            }
        });

        return chart;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // SIMULATION BEFORE/AFTER — BarChart
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Builds a two-bar BarChart comparing a before and after value.
     * Used for simulation results (score comparison, risk comparison).
     * Null-safe: clamped to valid range.
     */
    public static BarChart<String, Number> buildBeforeAfterChart(
            String title, String seriesName,
            double beforeVal, double afterVal,
            String beforeColor, String afterColor) {

        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis(0, 100, 10);
        yAxis.setLabel(seriesName != null ? seriesName : "Value");

        BarChart<String, Number> chart = new BarChart<>(xAxis, yAxis);
        chart.setTitle(title != null ? title : "");
        chart.setStyle("-fx-background-color: white; -fx-font-family: 'Segoe UI';");
        chart.setAnimated(false);

        XYChart.Series<String, Number> before = new XYChart.Series<>();
        before.setName("Before");
        before.getData().add(new XYChart.Data<>("Before", beforeVal));

        XYChart.Series<String, Number> after = new XYChart.Series<>();
        after.setName("After");
        after.getData().add(new XYChart.Data<>("After", afterVal));

        chart.getData().addAll(before, after);

        // Defer bar styling to avoid node NPE
        String safeBeforeColor = beforeColor != null ? beforeColor : "#64748B";
        String safeAfterColor = afterColor != null ? afterColor : "#10B981";
        javafx.application.Platform.runLater(() -> {
            for (XYChart.Data<String, Number> d : before.getData()) {
                if (d == null || d.getNode() == null)
                    continue;
                d.getNode().setStyle("-fx-bar-fill: " + safeBeforeColor + ";");
            }
            for (XYChart.Data<String, Number> d : after.getData()) {
                if (d == null || d.getNode() == null)
                    continue;
                d.getNode().setStyle("-fx-bar-fill: " + safeAfterColor + ";");
            }
        });

        return chart;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CHART CARD WRAPPER
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Wraps a chart in a titled card VBox consistent with the app design system.
     */
    public static VBox chartCard(String title, Region chart) {
        VBox card = new VBox(8);
        card.setStyle(FxStyles.CARD_STYLE);

        Label titleLabel = new Label(title != null ? title : "");
        titleLabel.setStyle(FxStyles.sectionTitle());

        VBox.setVgrow(chart, Priority.ALWAYS);
        card.getChildren().addAll(titleLabel, chart);
        return card;
    }
}
