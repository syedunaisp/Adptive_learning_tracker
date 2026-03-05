package tracker.ui.fx.controller;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import tracker.data.DataManager;
import tracker.data.dao.GoalDAO;
import tracker.data.dao.InterventionDAO;
import tracker.data.dao.ReevaluationDAO;
import tracker.model.*;
import tracker.security.SessionManager;
import tracker.service.*;
import tracker.service.ai.AdaptivePlanner;
import tracker.service.ai.RiskPredictor;
import tracker.service.ai.TrendAnalyzer;
import tracker.ui.fx.ViewManager;
import tracker.ui.fx.ViewManagerAware;

import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;

import javafx.stage.FileChooser;
import java.io.File;
import java.util.Optional;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;

/**
 * Controller for the Teacher Dashboard.
 * Class overview + students table + analytics + interventions + simulation.
 */
public class TeacherDashboardController implements ViewManagerAware {

    @FXML
    private Label welcomeLabel;
    @FXML
    private Label totalStudentsLabel;
    @FXML
    private Label atRiskLabel;
    @FXML
    private Label classAvgLabel;
    @FXML
    private Label weakestSubjectLabel;
    @FXML
    private TableView<Student> studentsTable;
    @FXML
    private TableColumn<Student, String> studentIdCol;
    @FXML
    private TableColumn<Student, String> studentNameCol;
    @FXML
    private TableColumn<Student, String> studentAvgCol;
    @FXML
    private TableColumn<Student, String> studentRiskCol;
    @FXML
    private TableColumn<Student, String> studentTrendCol;
    @FXML
    private BarChart<String, Number> scoreDistChart;
    @FXML
    private CategoryAxis scoreDistXAxis;
    @FXML
    private BarChart<String, Number> subjectBarChart;
    @FXML
    private StackPane contentArea;
    @FXML
    private VBox overviewPane;

    private VBox simulationPane;
    private VBox interventionsPane;
    private VBox reevalPane;

    private ViewManager viewManager;
    private final DataManager dataManager = new DataManager();
    private final TrendAnalyzer trendAnalyzer = new TrendAnalyzer();
    private final RiskPredictor riskPredictor = new RiskPredictor(trendAnalyzer);
    private final AdaptivePlanner planner = new AdaptivePlanner();
    private final AnalyticsService analyticsService = new AnalyticsService(riskPredictor, trendAnalyzer);
    private final SimulationService simulationService = new SimulationService(riskPredictor, trendAnalyzer, planner);
    private final InterventionEngine interventionEngine = new InterventionEngine(new InterventionDAO(), new GoalDAO());
    private final ReevaluationWorkflow reevalWorkflow = new ReevaluationWorkflow(new ReevaluationDAO());
    private final StudentManagementService studentMgmtService = new StudentManagementService(dataManager);

    // Panel References
    private VBox studentManagementPane;

    @Override
    public void setViewManager(ViewManager viewManager) {
        this.viewManager = viewManager;
        loadData();
    }

    @FXML
    private void initialize() {
        studentIdCol.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getId()));
        studentNameCol.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getName()));
        studentAvgCol.setCellValueFactory(
                cd -> new SimpleStringProperty(String.format("%.1f", cd.getValue().getAverageScore())));
        studentRiskCol.setCellValueFactory(cd -> {
            Student s = cd.getValue();
            if (s.getSubjects().isEmpty())
                return new SimpleStringProperty("N/A");
            RiskScore risk = riskPredictor.assessRisk(s);
            return new SimpleStringProperty(risk.getLevel().getLabel());
        });
        studentRiskCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    Label badge = new Label(item);
                    badge.setStyle(
                            "-fx-padding: 4 10; -fx-background-radius: 12; -fx-text-fill: white; -fx-font-weight: bold;");
                    if (item.contains("High")) {
                        badge.setStyle(badge.getStyle() + "-fx-background-color: #ff4757;");
                        badge.setText("🔴 " + item);
                    } else if (item.contains("Moderate")) {
                        badge.setStyle(badge.getStyle() + "-fx-background-color: #ffa502;");
                        badge.setText("🟡 " + item);
                    } else if (item.contains("Low")) {
                        badge.setStyle(badge.getStyle() + "-fx-background-color: #2ed573;");
                        badge.setText("🟢 " + item);
                    } else {
                        badge.setStyle(badge.getStyle() + "-fx-background-color: #747d8c;");
                    }
                    setGraphic(badge);
                }
            }
        });

        studentTrendCol.setCellValueFactory(cd -> {
            Student s = cd.getValue();
            TrendDirection t = trendAnalyzer.getTrend(s.getId());
            String icon = t == TrendDirection.IMPROVING ? "↑ Improving"
                    : t == TrendDirection.DECLINING ? "↓ Declining" : "→ Stable";
            return new SimpleStringProperty(icon);
        });

        studentTrendCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    Label badge = new Label(item);
                    badge.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
                    if (item.contains("↑"))
                        badge.setStyle(badge.getStyle() + "-fx-text-fill: #2ed573;");
                    else if (item.contains("↓"))
                        badge.setStyle(badge.getStyle() + "-fx-text-fill: #ff4757;");
                    else
                        badge.setStyle(badge.getStyle() + "-fx-text-fill: #ffa502;");
                    setGraphic(badge);
                }
            }
        });
    }

    private void loadData() {
        welcomeLabel.setText("Welcome, " + viewManager.getCurrentUsername());
        List<Student> students = dataManager.getStudents();

        totalStudentsLabel.setText(String.valueOf(students.size()));
        int atRisk = analyticsService.getAtRiskCount(students);
        atRiskLabel.setText(String.valueOf(atRisk));
        double classAvg = analyticsService.getInstitutionalAverage(students);
        classAvgLabel.setText(String.format("%.1f", classAvg));
        String weakest = analyticsService.getMostCommonWeakSubject(students);
        weakestSubjectLabel.setText(weakest);

        for (Student s : students) {
            if (!s.getSubjects().isEmpty()) {
                trendAnalyzer.recordAverage(s.getId(), s.getAverageScore());
            }
        }

        studentsTable.setItems(FXCollections.observableArrayList(students));

        // Populate Score Distribution BarChart
        int countFail = 0; // < 40
        int countRisk = 0; // 40 - 59.9
        int countAvg = 0; // 60 - 74.9
        int countGood = 0; // 75 - 89.9
        int countExcel = 0; // 90 - 100

        for (Student s : students) {
            double avg = s.getAverageScore();
            if (avg < 40)
                countFail++;
            else if (avg < 60)
                countRisk++;
            else if (avg < 75)
                countAvg++;
            else if (avg < 90)
                countGood++;
            else
                countExcel++;
        }

        XYChart.Series<String, Number> distSeries = new XYChart.Series<>();
        distSeries.setName("Students");
        distSeries.getData().add(new XYChart.Data<>("Fail (<40)", countFail));
        distSeries.getData().add(new XYChart.Data<>("At Risk (40-59)", countRisk));
        distSeries.getData().add(new XYChart.Data<>("Average (60-74)", countAvg));
        distSeries.getData().add(new XYChart.Data<>("Good (75-89)", countGood));
        distSeries.getData().add(new XYChart.Data<>("Excellent (90+)", countExcel));

        scoreDistChart.setAnimated(false);
        scoreDistChart.getData().clear();
        scoreDistChart.getData().add(distSeries);

        // Populate BarChart
        Map<String, Double> subjectSum = new HashMap<>();
        Map<String, Integer> subjectCount = new HashMap<>();
        for (Student s : students) {
            for (Subject sub : s.getSubjects()) {
                subjectSum.merge(sub.getSubjectName(), sub.getScore(), Double::sum);
                subjectCount.merge(sub.getSubjectName(), 1, Integer::sum);
            }
        }

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Average Score");
        subjectSum.forEach((name, sum) -> {
            double avg = sum / subjectCount.get(name);
            series.getData().add(new XYChart.Data<>(name, avg));
        });
        subjectBarChart.setAnimated(false);
        subjectBarChart.getData().clear();
        subjectBarChart.getData().add(series);
    }

    // =========================================================================
    // Sidebar Navigation
    // =========================================================================

    @FXML
    private void showOverview() {
        loadData();
        contentArea.getChildren().setAll(overviewPane);
    }

    @FXML
    private void showStudentManagement() {
        if (studentManagementPane == null) {
            studentManagementPane = buildStudentManagementPanel();
        } else {
            // refresh data implicitly by rebuilding or we can just rebuild it always
            studentManagementPane = buildStudentManagementPanel();
        }
        contentArea.getChildren().setAll(studentManagementPane);
    }

    @FXML
    private void showAnalytics() {
        contentArea.getChildren().setAll(overviewPane);
    }

    @FXML
    private void showSimulation() {
        if (simulationPane == null) {
            simulationPane = buildSimulationPanel();
        }
        contentArea.getChildren().setAll(simulationPane);
    }

    @FXML
    private void showInterventions() {
        if (interventionsPane == null) {
            interventionsPane = buildInterventionsPanel();
        }
        contentArea.getChildren().setAll(interventionsPane);
    }

    @FXML
    private void showReevaluations() {
        // Always rebuild to get fresh data
        reevalPane = buildReevaluationsPanel();
        contentArea.getChildren().setAll(reevalPane);
    }

    // =========================================================================
    // PANELS BUILDERS
    // =========================================================================

    private VBox buildStudentManagementPanel() {
        VBox panel = new VBox(20);
        panel.setPadding(new Insets(0));

        Label title = new Label("🏫 Student Management");
        title.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: white;");

        // Top Control Bar
        HBox topBar = new HBox(12);
        topBar.getStyleClass().add("card");

        ComboBox<ClassRoom> classSelect = new ComboBox<>();
        classSelect.setPromptText("Select a Class");
        classSelect.setItems(javafx.collections.FXCollections.observableArrayList(studentMgmtService.getAllClasses()));

        Button addClassBtn = new Button("+ Add Class");
        addClassBtn.getStyleClass().add("button-primary");
        Button addStudentBtn = new Button("+ Add Student");
        addStudentBtn.getStyleClass().add("button-secondary");
        Button importBtn = new Button("📥 Import Excel");
        importBtn.getStyleClass().add("button-secondary");
        Button recordScoreBtn = new Button("➕ Record Score");
        recordScoreBtn.getStyleClass().add("button-primary");
        Button deleteClassBtn = new Button("🗑 Delete Class");
        deleteClassBtn.getStyleClass().add("button-danger");

        // Spacer
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        topBar.getChildren().addAll(classSelect, addClassBtn, deleteClassBtn, spacer, recordScoreBtn, addStudentBtn,
                importBtn);

        // Student Table
        VBox tableCard = new VBox(12);
        tableCard.getStyleClass().add("card");

        TableView<Student> smTable = new TableView<>();
        smTable.setPrefHeight(400);

        TableColumn<Student, String> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getId()));

        TableColumn<Student, String> nameCol = new TableColumn<>("Name");
        nameCol.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getName()));

        TableColumn<Student, String> rollCol = new TableColumn<>("Roll No");
        rollCol.setCellValueFactory(cd -> new SimpleStringProperty(
                cd.getValue().getRollNumber() != null ? cd.getValue().getRollNumber() : ""));

        TableColumn<Student, String> emailCol = new TableColumn<>("Email");
        emailCol.setCellValueFactory(
                cd -> new SimpleStringProperty(cd.getValue().getEmail() != null ? cd.getValue().getEmail() : ""));

        smTable.getColumns().addAll(idCol, nameCol, rollCol, emailCol);
        tableCard.getChildren().addAll(new Label("Class Students"), smTable);

        // Actions
        classSelect.valueProperty().addListener((obs, oldV, newV) -> {
            if (newV != null) {
                smTable.setItems(javafx.collections.FXCollections.observableArrayList(
                        studentMgmtService.getStudentsForClass(newV.getId())));
                addStudentBtn.setDisable(false);
                importBtn.setDisable(false);
                deleteClassBtn.setDisable(false);
                recordScoreBtn.setDisable(false);
            } else {
                smTable.getItems().clear();
                addStudentBtn.setDisable(true);
                importBtn.setDisable(true);
                deleteClassBtn.setDisable(true);
                recordScoreBtn.setDisable(true);
            }
        });

        // Initialize state
        addStudentBtn.setDisable(true);
        importBtn.setDisable(true);
        recordScoreBtn.setDisable(true);
        deleteClassBtn.setDisable(true);

        addClassBtn.setOnAction(e -> handleAddClass(classSelect));
        deleteClassBtn.setOnAction(e -> handleDeleteClass(classSelect));
        addStudentBtn.setOnAction(e -> handleAddStudent(classSelect));
        importBtn.setOnAction(e -> handleImportExcel(classSelect));
        recordScoreBtn.setOnAction(e -> handleRecordScore(classSelect));

        panel.getChildren().addAll(title, topBar, tableCard);
        return panel;
    }

    private void handleAddClass(ComboBox<ClassRoom> classSelect) {
        Dialog<ClassRoom> dialog = new Dialog<>();
        dialog.setTitle("Add New Class");
        dialog.setHeaderText("Enter class details.");

        ButtonType addType = new ButtonType("Add", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(addType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField nameInput = new TextField();
        nameInput.setPromptText("Class Name (e.g. 10th Grade)");
        TextField sectionInput = new TextField();
        sectionInput.setPromptText("Section (e.g. A)");

        grid.add(new Label("Class Name:"), 0, 0);
        grid.add(nameInput, 1, 0);
        grid.add(new Label("Section:"), 0, 1);
        grid.add(sectionInput, 1, 1);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(b -> {
            if (b == addType) {
                return studentMgmtService.addClass(nameInput.getText(), sectionInput.getText());
            }
            return null;
        });

        Optional<ClassRoom> result = dialog.showAndWait();
        result.ifPresent(cr -> {
            classSelect.getItems().add(cr);
            classSelect.getSelectionModel().select(cr);
        });
    }

    private void handleDeleteClass(ComboBox<ClassRoom> classSelect) {
        ClassRoom cr = classSelect.getValue();
        if (cr == null)
            return;

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Delete Class");
        alert.setHeaderText("Are you sure you want to delete " + cr.getDisplayName() + "?");
        alert.setContentText("This will untrack students associated with the class.");
        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            studentMgmtService.deleteClass(cr.getId());
            classSelect.getItems().remove(cr);
        }
    }

    private void handleAddStudent(ComboBox<ClassRoom> classSelect) {
        ClassRoom cr = classSelect.getValue();
        if (cr == null)
            return;

        Dialog<Student> dialog = new Dialog<>();
        dialog.setTitle("Add Student");
        dialog.setHeaderText("Add student to " + cr.getDisplayName());

        ButtonType addType = new ButtonType("Add", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(addType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField idInput = new TextField();
        idInput.setPromptText("Student ID (optional)");
        TextField nameInput = new TextField();
        nameInput.setPromptText("Full Name");
        TextField rollInput = new TextField();
        rollInput.setPromptText("Roll Number (optional)");
        TextField emailInput = new TextField();
        emailInput.setPromptText("Email (optional)");

        grid.add(new Label("ID (System Will Auto-gen if empty):"), 0, 0);
        grid.add(idInput, 1, 0);
        grid.add(new Label("Name:"), 0, 1);
        grid.add(nameInput, 1, 1);
        grid.add(new Label("Roll Number:"), 0, 2);
        grid.add(rollInput, 1, 2);
        grid.add(new Label("Email:"), 0, 3);
        grid.add(emailInput, 1, 3);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(b -> {
            if (b == addType && !nameInput.getText().trim().isEmpty()) {
                boolean success = studentMgmtService.addStudent(idInput.getText(), nameInput.getText(), cr.getId(),
                        rollInput.getText(), emailInput.getText());
                if (success) {
                    // Just return a dummy object to refresh
                    return new Student("", "");
                }
            }
            return null;
        });

        dialog.showAndWait().ifPresent(s -> {
            // Trigger refresh by re-selecting the class
            int index = classSelect.getSelectionModel().getSelectedIndex();
            classSelect.getSelectionModel().clearSelection();
            classSelect.getSelectionModel().select(index);
        });
    }

    private void handleImportExcel(ComboBox<ClassRoom> classSelect) {
        ClassRoom cr = classSelect.getValue();
        if (cr == null)
            return;

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open Excel File");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Excel Files", "*.xlsx", "*.xls"));
        File file = fileChooser.showOpenDialog(classSelect.getScene().getWindow());
        if (file != null) {
            int count = studentMgmtService.importStudentsFromExcel(file, cr.getId());
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Import Successful");
            alert.setHeaderText(null);
            alert.setContentText("Successfully imported " + count + " students into " + cr.getDisplayName());
            alert.showAndWait();

            // Refresh table
            int index = classSelect.getSelectionModel().getSelectedIndex();
            classSelect.getSelectionModel().clearSelection();
            classSelect.getSelectionModel().select(index);
        }
    }

    private void handleRecordScore(ComboBox<ClassRoom> classSelect) {
        ClassRoom cr = classSelect.getValue();
        if (cr == null)
            return;

        List<Student> classStudents = studentMgmtService.getStudentsForClass(cr.getId());
        if (classStudents.isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.WARNING, "No students in this class to record scores for.");
            alert.showAndWait();
            return;
        }

        Dialog<Boolean> dialog = new Dialog<>();
        dialog.setTitle("Record Exam Score");
        dialog.setHeaderText("Record a score for a student in " + cr.getDisplayName());

        ButtonType addType = new ButtonType("Record", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(addType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        ComboBox<Student> studentCombo = new ComboBox<>();
        studentCombo.setItems(FXCollections.observableArrayList(classStudents));
        // Use StringConverter to display student names cleanly
        studentCombo.setConverter(new javafx.util.StringConverter<Student>() {
            @Override
            public String toString(Student s) {
                return s == null ? "" : s.getName() + " (" + s.getId() + ")";
            }

            @Override
            public Student fromString(String string) {
                return null;
            }
        });
        studentCombo.setPromptText("Select Student");

        TextField subjectInput = new TextField();
        subjectInput.setPromptText("Subject (e.g., Mathematics)");

        TextField scoreInput = new TextField();
        scoreInput.setPromptText("Score (0-100)");

        grid.add(new Label("Student:"), 0, 0);
        grid.add(studentCombo, 1, 0);
        grid.add(new Label("Subject:"), 0, 1);
        grid.add(subjectInput, 1, 1);
        grid.add(new Label("Score:"), 0, 2);
        grid.add(scoreInput, 1, 2);

        // Validation logic
        javafx.scene.Node recordButton = dialog.getDialogPane().lookupButton(addType);
        recordButton.setDisable(true);

        Runnable validateInput = () -> {
            boolean valid = studentCombo.getValue() != null &&
                    !subjectInput.getText().trim().isEmpty() &&
                    !scoreInput.getText().trim().isEmpty();
            if (valid) {
                try {
                    double v = Double.parseDouble(scoreInput.getText().trim());
                    if (v < 0 || v > 100)
                        valid = false;
                } catch (NumberFormatException e) {
                    valid = false;
                }
            }
            recordButton.setDisable(!valid);
        };

        studentCombo.valueProperty().addListener((obs, oldV, newV) -> validateInput.run());
        subjectInput.textProperty().addListener((obs, oldV, newV) -> validateInput.run());
        scoreInput.textProperty().addListener((obs, oldV, newV) -> validateInput.run());

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(b -> {
            if (b == addType) {
                Student selected = studentCombo.getValue();
                double score = Double.parseDouble(scoreInput.getText().trim());
                studentMgmtService.addExamScore(selected.getId(), selected.getName(), subjectInput.getText().trim(),
                        score);
                loadData(); // Re-calculate risk scores globally
                return Boolean.TRUE;
            }
            return null;
        });

        dialog.showAndWait().ifPresent(v -> {
            int index = classSelect.getSelectionModel().getSelectedIndex();
            classSelect.getSelectionModel().clearSelection();
            classSelect.getSelectionModel().select(index);
        });
    }

    private VBox buildSimulationPanel() {
        VBox panel = new VBox(20);
        panel.setPadding(new Insets(0));

        Label title = new Label("🔬 What-If Simulation");
        title.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: white;");

        VBox card = new VBox(12);
        card.getStyleClass().add("card");

        TextField studentIdField = new TextField();
        studentIdField.setPromptText("Student ID (e.g. S001)");
        studentIdField.setMaxWidth(300);

        TextField subjectField = new TextField();
        subjectField.setPromptText("Subject name");
        subjectField.setMaxWidth(300);

        TextField deltaField = new TextField();
        deltaField.setPromptText("Score delta (e.g. +10 or -5)");
        deltaField.setMaxWidth(300);

        TextArea resultArea = new TextArea();
        resultArea.setPrefHeight(300);
        resultArea.setEditable(false);
        resultArea.setWrapText(true);
        resultArea
                .setStyle("-fx-control-inner-background: #16213e; -fx-text-fill: #e0e0e8; -fx-font-family: monospace;");

        Button runBtn = new Button("Run Simulation");
        runBtn.getStyleClass().add("button-primary");
        runBtn.setOnAction(e -> {
            try {
                String sid = studentIdField.getText().trim();
                String subject = subjectField.getText().trim();
                double delta = Double.parseDouble(deltaField.getText().trim());
                Student student = null;
                for (Student s : dataManager.getStudents()) {
                    if (sid.equals(s.getId())) {
                        student = s;
                        break;
                    }
                }
                if (student == null) {
                    resultArea.setText("Student '" + sid + "' not found.");
                    return;
                }
                SimulationResult result = simulationService.simulateScoreChange(student, subject, delta);
                resultArea.setText("Simulation Results for " + student.getName() + " (" + sid + ")\n");
                resultArea.appendText("==================================================\n\n");
                resultArea.appendText(result.toString());
            } catch (NumberFormatException ex) {
                resultArea.setText("Error: Score delta must be a valid number.");
            } catch (Exception ex) {
                resultArea.setText("Error: " + ex.getMessage());
            }
        });

        card.getChildren().addAll(
                new Label("Enter parameters below to simulate changes to a student's performance:"),
                studentIdField, subjectField, deltaField, runBtn, resultArea);

        panel.getChildren().addAll(title, card);
        return panel;
    }

    private VBox buildInterventionsPanel() {
        VBox panel = new VBox(20);
        panel.setPadding(new Insets(0));

        Label title = new Label("🩺 AI Intervention Suggestions");
        title.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: white;");

        VBox card = new VBox(12);
        card.getStyleClass().add("card");

        List<Student> students = dataManager.getStudents();
        StringBuilder sb = new StringBuilder();
        for (Student s : students) {
            if (s.getSubjects().isEmpty())
                continue;
            RiskScore risk = riskPredictor.assessRisk(s);
            if (risk.getLevel() == RiskScore.Level.HIGH || risk.getLevel() == RiskScore.Level.MODERATE) {
                sb.append(
                        String.format("━━ %s (%s) — %s Risk ━━\n", s.getName(), s.getId(), risk.getLevel().getLabel()));
                String suggestion = interventionEngine.suggestIntervention(risk, null); // profile could be fetched here
                sb.append("  • ").append(suggestion).append("\n\n");
            }
        }
        if (sb.length() == 0) {
            sb.append("No at-risk students. All students are performing well!");
        }

        TextArea area = new TextArea(sb.toString());
        area.setEditable(false);
        area.setWrapText(true);
        area.setPrefHeight(450);
        area.setStyle("-fx-control-inner-background: #16213e; -fx-text-fill: #e0e0e8; -fx-font-family: monospace;");

        card.getChildren().addAll(
                new Label("Recommended actions for students identified as HIGH or MODERATE risk:"),
                area);
        panel.getChildren().addAll(title, card);
        return panel;
    }

    private VBox buildReevaluationsPanel() {
        VBox panel = new VBox(20);
        panel.setPadding(new Insets(0));

        Label title = new Label("📝 Pending Re-evaluation Requests");
        title.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: white;");

        VBox contentBox = new VBox(12);

        List<ReevaluationRequest> pending = reevalWorkflow.getPendingRequests();
        if (pending.isEmpty()) {
            VBox emptyCard = new VBox(12);
            emptyCard.getStyleClass().add("card");
            emptyCard.getChildren().add(new Label("No pending re-evaluation requests at this time."));
            contentBox.getChildren().add(emptyCard);
        } else {
            for (ReevaluationRequest req : pending) {
                VBox reqCard = new VBox(12);
                reqCard.getStyleClass().add("card");

                Label info = new Label(String.format("Request #%d — Score #%d — Student #%d",
                        req.getId(), req.getScoreId(), req.getStudentId()));
                info.setStyle("-fx-font-weight: bold; -fx-font-size: 16px;");
                Label reason = new Label("Reason: " + req.getReason());
                reason.setWrapText(true);

                HBox actions = new HBox(12);
                Button approveBtn = new Button("✓ Approve");
                approveBtn.getStyleClass().add("button-primary");

                Button rejectBtn = new Button("✗ Reject");
                rejectBtn.getStyleClass().add("button-danger");

                int userId = viewManager.getCurrentUserId();
                approveBtn.setOnAction(e -> {
                    reevalWorkflow.approveRequest(req.getId(), userId, "Approved by teacher");
                    reqCard.setDisable(true);
                    info.setText(info.getText() + " — RESOLVED (Approved)");
                });
                rejectBtn.setOnAction(e -> {
                    reevalWorkflow.rejectRequest(req.getId(), userId, "Rejected");
                    reqCard.setDisable(true);
                    info.setText(info.getText() + " — RESOLVED (Rejected)");
                });

                actions.getChildren().addAll(approveBtn, rejectBtn);
                reqCard.getChildren().addAll(info, reason, actions);
                contentBox.getChildren().add(reqCard);
            }
        }

        panel.getChildren().addAll(title, contentBox);
        return panel;
    }

    @FXML
    private void handleLogout() {
        SessionManager.logout();
        viewManager.clearSession();
        viewManager.showLogin();
    }
}
