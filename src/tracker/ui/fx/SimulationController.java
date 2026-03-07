package tracker.ui.fx;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import tracker.model.*;

import java.util.*;

/**
 * What-If Simulation page controller.
 * Unchanged logic from Swing version.
 * Charts: JavaFX BarChart (before/after score + risk).
 */
public class SimulationController {

    private final MainController main;

    private ComboBox<String> cmbStudent, cmbSubject;
    private TextField txtDelta;
    private TextArea txtResult;
    private HBox chartsRow;

    private List<Student> cachedStudents;

    public SimulationController(MainController main) {
        this.main = main;
    }

    public Region buildPage() {
        VBox page = new VBox(16);
        page.setStyle(FxStyles.PAGE_BG);
        page.setPadding(new Insets(20, 24, 20, 24));

        Label title = new Label("What-If Simulation");
        title.setStyle(FxStyles.title());

        // Input card
        VBox inputCard = buildCard("Simulation Parameters");
        GridPane form = new GridPane();
        form.setHgap(12);
        form.setVgap(10);

        cmbStudent = new ComboBox<>();
        cmbStudent.setStyle("-fx-font-family: 'Segoe UI'; -fx-font-size: 13;");
        cmbStudent.setPrefWidth(250);
        cmbStudent.setPrefHeight(36);
        cmbStudent.setOnAction(e -> populateSubjects());

        cmbSubject = new ComboBox<>();
        cmbSubject.setStyle("-fx-font-family: 'Segoe UI'; -fx-font-size: 13;");
        cmbSubject.setPrefWidth(200);
        cmbSubject.setPrefHeight(36);

        txtDelta = new TextField();
        txtDelta.setPromptText("+10 or -5");
        txtDelta.setStyle(FxStyles.textField());
        txtDelta.setPrefHeight(36);
        txtDelta.setPrefWidth(150);

        Button btnRun = new Button("Run Simulation");
        btnRun.setStyle(FxStyles.coloredButton(FxStyles.C_ORANGE));
        btnRun.setOnAction(e -> handleRunSimulation());

        form.add(new Label("Student:") {
            {
                setStyle(FxStyles.label());
            }
        }, 0, 0);
        form.add(cmbStudent, 1, 0);
        form.add(new Label("Subject:") {
            {
                setStyle(FxStyles.label());
            }
        }, 2, 0);
        form.add(cmbSubject, 3, 0);
        form.add(new Label("Score Change:") {
            {
                setStyle(FxStyles.label());
            }
        }, 0, 1);
        form.add(txtDelta, 1, 1);
        form.add(btnRun, 2, 1);

        inputCard.getChildren().add(form);

        // Charts row (filled after simulation)
        chartsRow = new HBox(16);
        chartsRow.setPrefHeight(260);

        // Result card
        VBox resultCard = buildCard("Simulation Result");
        txtResult = new TextArea("Configure parameters above and click Run Simulation.");
        txtResult.setEditable(false);
        txtResult.setWrapText(true);
        txtResult.setStyle("-fx-font-family: 'Consolas'; -fx-font-size: 12;");
        VBox.setVgrow(txtResult, Priority.ALWAYS);
        resultCard.getChildren().add(txtResult);
        VBox.setVgrow(resultCard, Priority.ALWAYS);

        page.getChildren().addAll(title, inputCard, chartsRow, resultCard);
        VBox.setVgrow(resultCard, Priority.ALWAYS);
        return page;
    }

    void refresh() {
        if (cmbStudent == null)
            return;
        cmbStudent.getItems().clear();
        cachedStudents = main.dataManager.getStudents();
        if (cachedStudents.isEmpty()) {
            cmbStudent.getItems().add("-- No students --");
        } else {
            for (Student s : cachedStudents) {
                cmbStudent.getItems().add(s.getId() + " - " + s.getName());
            }
            cmbStudent.getSelectionModel().selectFirst();
            populateSubjects();
        }
    }

    private void populateSubjects() {
        if (cmbSubject == null)
            return;
        cmbSubject.getItems().clear();
        int idx = cmbStudent.getSelectionModel().getSelectedIndex();
        if (idx < 0 || cachedStudents == null || idx >= cachedStudents.size())
            return;
        for (Subject sub : cachedStudents.get(idx).getSubjects()) {
            cmbSubject.getItems().add(sub.getSubjectName());
        }
        if (!cmbSubject.getItems().isEmpty())
            cmbSubject.getSelectionModel().selectFirst();
    }

    private void handleRunSimulation() {
        int idx = cmbStudent.getSelectionModel().getSelectedIndex();
        if (idx < 0 || cachedStudents == null || idx >= cachedStudents.size()) {
            main.showError("Select a student.");
            return;
        }
        String subName = cmbSubject.getValue();
        if (subName == null) {
            main.showError("Select a subject.");
            return;
        }

        double delta;
        try {
            delta = Double.parseDouble(txtDelta.getText().trim());
        } catch (NumberFormatException e) {
            main.showError("Enter a valid number.");
            return;
        }

        try {
            SimulationResult result = main.simulationService.simulateScoreChange(
                    cachedStudents.get(idx), subName, delta);
            txtResult.setText(result.getReport());
            txtResult.positionCaret(0);
            buildSimCharts(result);
        } catch (IllegalArgumentException ex) {
            main.showError(ex.getMessage());
        }
    }

    private void buildSimCharts(SimulationResult result) {
        chartsRow.getChildren().clear();

        // Score before vs after
        BarChart<String, Number> scoreChart = ChartBuilder.buildBeforeAfterChart(
                "Score: Before vs After", "Score",
                result.getOriginalAverage(), result.getSimulatedAverage(),
                "#64748B", "#10B981");
        scoreChart.setPrefHeight(240);

        // Risk score before vs after
        BarChart<String, Number> riskChart = ChartBuilder.buildBeforeAfterChart(
                "Risk: Before vs After", "Risk Score",
                result.getOriginalRisk().getNumericScore(),
                result.getSimulatedRisk().getNumericScore(),
                "#EF4444", "#10B981");
        riskChart.setPrefHeight(240);

        VBox sc = ChartBuilder.chartCard("Score Comparison", scoreChart);
        VBox rc = ChartBuilder.chartCard("Risk Score Comparison", riskChart);
        HBox.setHgrow(sc, Priority.ALWAYS);
        HBox.setHgrow(rc, Priority.ALWAYS);

        chartsRow.getChildren().addAll(sc, rc);
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
