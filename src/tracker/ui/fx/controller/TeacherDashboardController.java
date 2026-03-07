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
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;
import tracker.ui.fx.util.TableColumnFormatter;
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
    private Label moderateRiskLabel;
    @FXML
    private Label lowRiskLabel;
    @FXML
    private VBox classTablesContainer;
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
    private VBox advancedStudentManagementPane;

    @Override
    public void setViewManager(ViewManager viewManager) {
        this.viewManager = viewManager;
        loadData();
        setupRealTimeMetrics();
    }

    private void setupRealTimeMetrics() {
        Timeline metricsUpdater = new Timeline(new KeyFrame(Duration.seconds(2), e -> refreshOverviewMetrics()));
        metricsUpdater.setCycleCount(Animation.INDEFINITE);
        metricsUpdater.play();
    }

    private void refreshOverviewMetrics() {
        dataManager.refreshCache();
        // Only include students belonging to the logged-in teacher's classes
        java.util.Set<Integer> teacherClassIds = new java.util.HashSet<>();
        for (ClassRoom c : studentMgmtService.getClassesByTeacher(viewManager.getCurrentUserId())) {
            teacherClassIds.add(c.getId());
        }
        List<Student> students = new java.util.ArrayList<>();
        for (Student s : dataManager.getStudents()) {
            if (s.getClassId() > 0 && teacherClassIds.contains(s.getClassId())) {
                students.add(s);
            }
        }

        totalStudentsLabel.setText(String.valueOf(students.size()));
        int atRisk = analyticsService.getAtRiskCount(students);
        atRiskLabel.setText(String.valueOf(atRisk));
        int moderateRisk = analyticsService.getModerateRiskCount(students);
        moderateRiskLabel.setText(String.valueOf(moderateRisk));
        int lowRisk = analyticsService.getLowRiskCount(students);
        lowRiskLabel.setText(String.valueOf(lowRisk));

        for (Student s : students) {
            if (!s.getSubjects().isEmpty()) {
                trendAnalyzer.recordAverage(s.getId(), s.getAverageScore());
            }
        }

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

    @FXML
    private void initialize() {
        // Table columns are now created dynamically in loadData()
    }

    private void loadData() {
        welcomeLabel.setText("Welcome, " + viewManager.getCurrentUsername());
        refreshOverviewMetrics();

        classTablesContainer.getChildren().clear();

        List<ClassRoom> classes = studentMgmtService.getClassesByTeacher(viewManager.getCurrentUserId());
        for (ClassRoom classroom : classes) {
            List<Student> classStudents = studentMgmtService.getStudentsForClass(classroom.getId());

            Label classTitle = new Label("Class: " + classroom.getDisplayName());
            classTitle.setStyle(
                    "-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: white; -fx-padding: 10 0 5 0;");

            TableView<Student> smTable = new TableView<>();
            smTable.setPrefHeight(300);
            smTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

            TableColumn<Student, String> idCol = new TableColumn<>("ID");
            idCol.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getId()));
            TableColumnFormatter.bindColumnWidth(idCol, smTable, 0.10);

            TableColumn<Student, String> nameCol = new TableColumn<>("Name");
            nameCol.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getName()));
            nameCol.setCellFactory(new tracker.ui.fx.util.AvatarCellFactory<>());
            TableColumnFormatter.bindColumnWidth(nameCol, smTable, 0.25);

            TableColumn<Student, String> rollCol = new TableColumn<>("Roll No");
            rollCol.setCellValueFactory(cd -> new SimpleStringProperty(
                    cd.getValue().getRollNumber() != null ? cd.getValue().getRollNumber() : ""));
            rollCol.setComparator((s1, s2) -> {
                if (s1 == null && s2 == null)
                    return 0;
                if (s1 == null)
                    return -1;
                if (s2 == null)
                    return 1;
                try {
                    return Integer.compare(Integer.parseInt(s1.trim()), Integer.parseInt(s2.trim()));
                } catch (NumberFormatException e) {
                    return s1.compareToIgnoreCase(s2);
                }
            });
            TableColumnFormatter.bindColumnWidth(rollCol, smTable, 0.10);

            TableColumn<Student, String> emailCol = new TableColumn<>("Email");
            emailCol.setCellValueFactory(
                    cd -> new SimpleStringProperty(cd.getValue().getEmail() != null ? cd.getValue().getEmail() : ""));
            TableColumnFormatter.bindColumnWidth(emailCol, smTable, 0.20);

            TableColumn<Student, String> avgCol = new TableColumn<>("Average");
            avgCol.setCellValueFactory(
                    cd -> new SimpleStringProperty(String.format("%.1f", cd.getValue().getAverageScore())));
            TableColumnFormatter.bindColumnWidth(avgCol, smTable, 0.10);

            TableColumn<Student, String> riskCol = new TableColumn<>("Risk");
            riskCol.setCellValueFactory(cd -> {
                Student s = cd.getValue();
                if (s.getSubjects().isEmpty())
                    return new SimpleStringProperty("N/A");
                RiskScore risk = riskPredictor.assessRisk(s);
                return new SimpleStringProperty(risk.getLevel().getLabel());
            });
            riskCol.setCellFactory(col -> new TableCell<>() {
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
            TableColumnFormatter.bindColumnWidth(riskCol, smTable, 0.15);

            TableColumn<Student, String> trendCol = new TableColumn<>("Trend");
            trendCol.setCellValueFactory(cd -> {
                Student s = cd.getValue();
                TrendDirection t = trendAnalyzer.getTrend(s.getId());
                String icon = t == TrendDirection.IMPROVING ? "↑ Improving"
                        : t == TrendDirection.DECLINING ? "↓ Declining" : "→ Stable";
                return new SimpleStringProperty(icon);
            });
            trendCol.setCellFactory(col -> new TableCell<>() {
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
            TableColumnFormatter.bindColumnWidth(trendCol, smTable, 0.10);

            smTable.getColumns().addAll(idCol, nameCol, rollCol, emailCol, avgCol, riskCol, trendCol);
            smTable.setItems(FXCollections.observableArrayList(classStudents));
            smTable.getSortOrder().add(rollCol);
            smTable.sort();

            VBox tableContainer = new VBox(5, classTitle, smTable);
            classTablesContainer.getChildren().add(tableContainer);
        }
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
    private void showAdvancedStudentManagement() {
        // Always rebuild to fetch fresh DB data automatically
        advancedStudentManagementPane = buildAdvancedStudentManagementPanel();
        contentArea.getChildren().setAll(advancedStudentManagementPane);
    }

    @FXML
    private void showAnalytics() {
        contentArea.getChildren().setAll(overviewPane);
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    @FXML
    private void handleAdvancedManualAddStudent(ClassRoom targetClass, ComboBox<ClassRoom> classSelect) {
        if (targetClass == null) {
            showAlert(Alert.AlertType.WARNING, "No Class Selected", "Please select a class first.");
            return;
        }

        Dialog<Student> dialog = new Dialog<>();
        dialog.setTitle("Add Student & Scores");
        dialog.setHeaderText("Enter student details and initial marks.");

        ButtonType saveType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveType, ButtonType.CANCEL);

        VBox content = new VBox(15);
        content.setPadding(new Insets(20));

        // Basic Info
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);

        TextField nameInput = new TextField();
        TextField rollInput = new TextField();
        TextField emailInput = new TextField();

        grid.add(new Label("Name:"), 0, 0);
        grid.add(nameInput, 1, 0);
        grid.add(new Label("Roll Number:"), 0, 1);
        grid.add(rollInput, 1, 1);
        grid.add(new Label("Email:"), 0, 2);
        grid.add(emailInput, 1, 2);

        // Subjects Section
        VBox subjectsContainer = new VBox(10);
        Label subLabel = new Label("Subjects & Scores");
        subLabel.setStyle("-fx-font-weight: bold; -fx-padding: 10 0 0 0;");

        VBox subjectsList = new VBox(5);

        Button addSubjectBtn = new Button("+ Add Subject");
        addSubjectBtn.setOnAction(e -> {
            HBox row = new HBox(10);
            TextField subName = new TextField();
            subName.setPromptText("Subject");
            TextField subScore = new TextField();
            subScore.setPromptText("Score (0-100)");
            subScore.setPrefWidth(80);
            Button removeBtn = new Button("X");
            removeBtn.setStyle("-fx-text-fill: red;");
            row.getChildren().addAll(subName, subScore, removeBtn);

            removeBtn.setOnAction(ev -> subjectsList.getChildren().remove(row));
            subjectsList.getChildren().add(row);
            dialog.getDialogPane().getScene().getWindow().sizeToScene();
        });

        // Add one empty row by default
        addSubjectBtn.fire();

        subjectsContainer.getChildren().addAll(subLabel, subjectsList, addSubjectBtn);
        content.getChildren().addAll(grid, new Separator(), subjectsContainer);

        dialog.getDialogPane().setContent(content);

        dialog.setResultConverter(b -> {
            if (b == saveType) {
                try {
                    String name = nameInput.getText().trim();
                    String roll = rollInput.getText().trim();
                    String email = emailInput.getText().trim();

                    if (name.isEmpty())
                        return null;

                    Student newStudent = new Student(java.util.UUID.randomUUID().toString(), name);
                    newStudent.setRollNumber(roll);
                    newStudent.setEmail(email);

                    // Parse subjects
                    for (javafx.scene.Node node : subjectsList.getChildren()) {
                        HBox row = (HBox) node;
                        TextField sName = (TextField) row.getChildren().get(0);
                        TextField sScore = (TextField) row.getChildren().get(1);

                        String subj = sName.getText().trim();
                        String scoreStr = sScore.getText().trim();

                        if (!subj.isEmpty() && !scoreStr.isEmpty()) {
                            double score = Double.parseDouble(scoreStr);
                            newStudent.addSubject(new Subject(subj, score));
                        }
                    }
                    // Save to Database
                    newStudent.setClassId(targetClass.getId());
                    dataManager.getStudentDAO().insert(newStudent);
                    dataManager.refreshCache(); // Important to refresh

                    // Add scores
                    int studentDbId = dataManager.getStudentDAO().findDbIdByStudentId(newStudent.getId());
                    for (Subject subject : newStudent.getSubjects()) {
                        String cat = tracker.model.SubjectCategory.categorize(subject.getSubjectName()).name();
                        int subjId = dataManager.getSubjectDAO().findOrCreate(subject.getSubjectName(), cat);
                        dataManager.getScoreDAO().insertScore(studentDbId, subjId, subject.getScore());
                    }
                    dataManager.refreshCache(); // Refresh again for subjects

                    return newStudent;
                } catch (NumberFormatException e) {
                    showAlert(Alert.AlertType.ERROR, "Invalid Input", "Scores must be valid numbers.");
                    return null;
                }
            }
            return null;
        });

        Optional<Student> result = dialog.showAndWait();
        result.ifPresent(s -> {
            trendAnalyzer.recordAverage(s.getId(), s.getAverageScore());
            // Trigger table refresh
            classSelect.getSelectionModel().clearSelection();
            classSelect.getSelectionModel().select(targetClass);
        });
    }

    private void handleAdvancedEditStudent(Student student, ComboBox<ClassRoom> classSelect) {
        Dialog<Student> dialog = new Dialog<>();
        dialog.setTitle("Edit Student & Scores");
        dialog.setHeaderText("Edit details for: " + student.getName());

        ButtonType saveType = new ButtonType("Save Changes", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveType, ButtonType.CANCEL);

        VBox content = new VBox(15);
        content.setPadding(new Insets(20));

        // Basic Info
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);

        TextField nameInput = new TextField(student.getName());
        TextField rollInput = new TextField(student.getRollNumber() != null ? student.getRollNumber() : "");
        TextField emailInput = new TextField(student.getEmail() != null ? student.getEmail() : "");

        grid.add(new Label("Name:"), 0, 0);
        grid.add(nameInput, 1, 0);
        grid.add(new Label("Roll Number:"), 0, 1);
        grid.add(rollInput, 1, 1);
        grid.add(new Label("Email:"), 0, 2);
        grid.add(emailInput, 1, 2);

        // Subjects Section
        VBox subjectsContainer = new VBox(10);
        Label subLabel = new Label("Subjects & Scores");
        subLabel.setStyle("-fx-font-weight: bold; -fx-padding: 10 0 0 0;");

        VBox subjectsList = new VBox(5);

        java.util.function.Consumer<Subject> addRow = (subj) -> {
            HBox row = new HBox(10);
            TextField subName = new TextField(subj != null ? subj.getSubjectName() : "");
            subName.setPromptText("Subject");
            TextField subScore = new TextField(subj != null ? String.valueOf(subj.getScore()) : "");
            subScore.setPromptText("Score (0-100)");
            subScore.setPrefWidth(80);
            Button removeBtn = new Button("X");
            removeBtn.setStyle("-fx-text-fill: red;");
            row.getChildren().addAll(subName, subScore, removeBtn);

            removeBtn.setOnAction(ev -> {
                subjectsList.getChildren().remove(row);
                dialog.getDialogPane().getScene().getWindow().sizeToScene();
            });
            subjectsList.getChildren().add(row);
        };

        // Populate existing subjects
        for (Subject sub : student.getSubjects()) {
            addRow.accept(sub);
        }

        Button addSubjectBtn = new Button("+ Add Subject");
        addSubjectBtn.setOnAction(e -> {
            addRow.accept(null);
            dialog.getDialogPane().getScene().getWindow().sizeToScene();
        });

        subjectsContainer.getChildren().addAll(subLabel, subjectsList, addSubjectBtn);
        content.getChildren().addAll(grid, new Separator(), subjectsContainer);

        dialog.getDialogPane().setContent(content);

        dialog.setResultConverter(b -> {
            if (b == saveType) {
                try {
                    String name = nameInput.getText().trim();
                    if (name.isEmpty())
                        return null;

                    student.setName(name);
                    student.setRollNumber(rollInput.getText().trim());
                    student.setEmail(emailInput.getText().trim());

                    // Rebuild subjects list
                    student.getSubjects().clear();
                    for (javafx.scene.Node node : subjectsList.getChildren()) {
                        HBox row = (HBox) node;
                        TextField sName = (TextField) row.getChildren().get(0);
                        TextField sScore = (TextField) row.getChildren().get(1);

                        String subj = sName.getText().trim();
                        String scoreStr = sScore.getText().trim();

                        if (!subj.isEmpty() && !scoreStr.isEmpty()) {
                            double score = Double.parseDouble(scoreStr);
                            student.addSubject(new Subject(subj, score));
                        }
                    }
                    // Rebuild subjects in DB
                    int studentDbId = dataManager.getStudentDAO().findDbIdByStudentId(student.getId());
                    if (studentDbId > 0) {
                        dataManager.updateStudent(student); // Updates basic info

                        // Clear existing scores
                        dataManager.getScoreDAO().deleteByStudentDbId(studentDbId);

                        for (Subject subject : student.getSubjects()) {
                            String cat = tracker.model.SubjectCategory.categorize(subject.getSubjectName()).name();
                            int subjId = dataManager.getSubjectDAO().findOrCreate(subject.getSubjectName(), cat);
                            dataManager.getScoreDAO().insertScore(studentDbId, subjId, subject.getScore());
                        }
                        dataManager.refreshCache();
                    }
                    return student;
                } catch (NumberFormatException e) {
                    showAlert(Alert.AlertType.ERROR, "Invalid Input", "Scores must be valid numbers.");
                    return null;
                }
            }
            return null;
        });

        Optional<Student> result = dialog.showAndWait();
        result.ifPresent(s -> {
            trendAnalyzer.recordAverage(s.getId(), s.getAverageScore());
            // Trigger table refresh
            ClassRoom targetClass = classSelect.getValue();
            if (targetClass != null) {
                classSelect.getSelectionModel().clearSelection();
                classSelect.getSelectionModel().select(targetClass);
            }
        });
    }

    private void handleAdvancedDeleteStudent(Student student, TableView<Student> table) {
        if (student == null)
            return;
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Delete Student");
        confirm.setHeaderText("Are you sure you want to delete " + student.getName() + "?");
        confirm.setContentText("This action cannot be undone.");

        Optional<ButtonType> res = confirm.showAndWait();
        if (res.isPresent() && res.get() == ButtonType.OK) {
            dataManager.deleteStudent(student.getId());
            table.getItems().remove(student);
        }
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
        // Always rebuild to get fresh, teacher-filtered data
        interventionsPane = buildInterventionsPanel();
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

    private VBox buildAdvancedStudentManagementPanel() {
        VBox panel = new VBox(20);
        panel.setPadding(new Insets(0));

        Label title = new Label("🎓 Student Management");
        title.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: white;");

        // Top Control Bar
        HBox topBar = new HBox(12);
        topBar.getStyleClass().add("card");
        topBar.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        ComboBox<ClassRoom> classSelect = new ComboBox<>();
        classSelect.setPromptText("Select a Class");
        classSelect.setPrefHeight(36);
        classSelect.setPrefWidth(180);
        classSelect.setMinWidth(180);
        classSelect.setMaxWidth(180);
        classSelect.setItems(FXCollections
                .observableArrayList(studentMgmtService.getClassesByTeacher(viewManager.getCurrentUserId())));

        Button addStudentBtn = new Button("➕ Add Student");
        addStudentBtn.getStyleClass().add("button-secondary");
        addStudentBtn.setPrefHeight(36);
        addStudentBtn.setPrefWidth(180);
        addStudentBtn.setMinWidth(180);
        addStudentBtn.setMaxWidth(180);

        Button addClassBtn = new Button("➕ Add Class");
        addClassBtn.getStyleClass().add("button-secondary");
        addClassBtn.setPrefHeight(36);
        addClassBtn.setPrefWidth(180);
        addClassBtn.setMinWidth(180);
        addClassBtn.setMaxWidth(180);

        Button deleteClassBtn = new Button("🗑 Delete Class");
        deleteClassBtn.getStyleClass().add("button-danger");
        deleteClassBtn.setPrefHeight(36);
        deleteClassBtn.setPrefWidth(180);
        deleteClassBtn.setMinWidth(180);
        deleteClassBtn.setMaxWidth(180);

        // Spacer
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button importBtn = new Button("📥 Import Excel");
        importBtn.getStyleClass().add("button-primary");
        importBtn.setPrefHeight(36);
        importBtn.setPrefWidth(180);
        importBtn.setMinWidth(180);
        importBtn.setMaxWidth(180);

        topBar.getChildren().addAll(classSelect, addStudentBtn, addClassBtn, deleteClassBtn, spacer, importBtn);

        // Student Table Card
        VBox tableCard = new VBox(12);
        tableCard.getStyleClass().add("card");

        TableView<Student> advTable = new TableView<>();
        advTable.setPrefHeight(400);
        advTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<Student, String> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getId()));
        TableColumnFormatter.bindColumnWidth(idCol, advTable, 0.05);

        TableColumn<Student, String> nameCol = new TableColumn<>("Name");
        nameCol.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getName()));
        nameCol.setCellFactory(new tracker.ui.fx.util.AvatarCellFactory<>());
        TableColumnFormatter.bindColumnWidth(nameCol, advTable, 0.15);

        TableColumn<Student, String> rollCol = new TableColumn<>("Roll No");
        rollCol.setCellValueFactory(cd -> new SimpleStringProperty(
                cd.getValue().getRollNumber() != null ? cd.getValue().getRollNumber() : ""));
        rollCol.setComparator((s1, s2) -> {
            if (s1 == null && s2 == null)
                return 0;
            if (s1 == null)
                return -1;
            if (s2 == null)
                return 1;
            try {
                return Integer.compare(Integer.parseInt(s1.trim()), Integer.parseInt(s2.trim()));
            } catch (NumberFormatException e) {
                return s1.compareToIgnoreCase(s2);
            }
        });
        TableColumnFormatter.bindColumnWidth(rollCol, advTable, 0.05);

        TableColumn<Student, String> emailCol = new TableColumn<>("Email");
        emailCol.setCellValueFactory(
                cd -> new SimpleStringProperty(cd.getValue().getEmail() != null ? cd.getValue().getEmail() : ""));
        TableColumnFormatter.bindColumnWidth(emailCol, advTable, 0.15);

        TableColumn<Student, String> riskCol = new TableColumn<>("Risk");
        riskCol.setCellValueFactory(cd -> {
            Student s = cd.getValue();
            if (s.getSubjects().isEmpty())
                return new SimpleStringProperty("N/A");
            RiskScore risk = riskPredictor.assessRisk(s);
            return new SimpleStringProperty(risk.getLevel().getLabel());
        });
        riskCol.setCellFactory(col -> new TableCell<>() {
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
        TableColumnFormatter.bindColumnWidth(riskCol, advTable, 0.08);

        TableColumn<Student, String> trendCol = new TableColumn<>("Trend");
        trendCol.setCellValueFactory(cd -> {
            Student s = cd.getValue();
            TrendDirection t = trendAnalyzer.getTrend(s.getId());
            String icon = t == TrendDirection.IMPROVING ? "↑ Improving"
                    : t == TrendDirection.DECLINING ? "↓ Declining" : "→ Stable";
            return new SimpleStringProperty(icon);
        });
        trendCol.setCellFactory(col -> new TableCell<>() {
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
        TableColumnFormatter.bindColumnWidth(trendCol, advTable, 0.08);

        TableColumn<Student, String> avgCol = new TableColumn<>("Average");
        avgCol.setCellValueFactory(cd -> {
            Student s = cd.getValue();
            if (s.getSubjects().isEmpty())
                return new SimpleStringProperty("N/A");
            return new SimpleStringProperty(String.format("%.1f", s.getAverageScore()));
        });
        TableColumnFormatter.bindColumnWidth(avgCol, advTable, 0.05);

        advTable.getColumns().addAll(idCol, nameCol, rollCol, emailCol, riskCol, trendCol, avgCol);
        advTable.getSortOrder().add(rollCol);

        // Add Subject 1..5 Columns
        for (int i = 1; i <= 5; i++) {
            final int index = i - 1;
            TableColumn<Student, String> subCol = new TableColumn<>("Subject " + i);
            subCol.setCellValueFactory(cd -> {
                Student s = cd.getValue();
                if (index < s.getSubjects().size()) {
                    Subject sub = s.getSubjects().get(index);
                    return new SimpleStringProperty(sub.getSubjectName() + ": " + sub.getScore());
                }
                return new SimpleStringProperty("-");
            });
            TableColumnFormatter.bindColumnWidth(subCol, advTable, 0.07);
            advTable.getColumns().add(subCol);
        }

        // Action Buttons outside the table
        Button editBtn = new Button("✏️ Edit");
        editBtn.getStyleClass().add("button");
        editBtn.setStyle("-fx-background-color: #4a69bd; -fx-text-fill: white; -fx-padding: 6 16;");
        editBtn.setDisable(true);

        Button deleteBtn = new Button("🗑 Delete");
        deleteBtn.getStyleClass().add("button-danger");
        deleteBtn.setStyle("-fx-padding: 6 16;");
        deleteBtn.setDisable(true);

        HBox actionsBox = new HBox(12, editBtn, deleteBtn);
        actionsBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        advTable.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            boolean disabled = (newVal == null);
            editBtn.setDisable(disabled);
            deleteBtn.setDisable(disabled);
        });

        editBtn.setOnAction(
                e -> handleAdvancedEditStudent(advTable.getSelectionModel().getSelectedItem(), classSelect));
        deleteBtn.setOnAction(
                e -> handleAdvancedDeleteStudent(advTable.getSelectionModel().getSelectedItem(), advTable));

        // Actions
        classSelect.valueProperty().addListener((obs, oldV, newV) -> {
            if (newV != null) {
                advTable.setItems(
                        FXCollections.observableArrayList(studentMgmtService.getStudentsForClass(newV.getId())));
                advTable.sort();
                importBtn.setDisable(false);
                addStudentBtn.setDisable(false);
                deleteClassBtn.setDisable(false);
            } else {
                advTable.getItems().clear();
                importBtn.setDisable(true);
                addStudentBtn.setDisable(true);
                deleteClassBtn.setDisable(true);
            }
        });

        // Initialize state
        importBtn.setDisable(true);
        addStudentBtn.setDisable(true);
        deleteClassBtn.setDisable(true);

        addClassBtn.setOnAction(e -> handleAddClass(classSelect));
        deleteClassBtn.setOnAction(e -> handleDeleteClass(classSelect));
        importBtn.setOnAction(e -> handleImportExcel(classSelect));
        addStudentBtn.setOnAction(e -> handleAdvancedManualAddStudent(classSelect.getValue(), classSelect));

        tableCard.getChildren().addAll(advTable, actionsBox);
        panel.getChildren().addAll(title, topBar, tableCard);
        return panel;
    }

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
        classSelect.setItems(javafx.collections.FXCollections
                .observableArrayList(studentMgmtService.getClassesByTeacher(viewManager.getCurrentUserId())));

        Button addClassBtn = new Button("+ Add Class");
        addClassBtn.getStyleClass().add("button-primary");
        Button importBtn = new Button("📥 Import Excel");
        importBtn.getStyleClass().add("button-secondary");
        Button recordScoreBtn = new Button("➕ Record Score");
        recordScoreBtn.getStyleClass().add("button-primary");
        Button deleteClassBtn = new Button("🗑 Delete Class");
        deleteClassBtn.getStyleClass().add("button-danger");

        // Spacer
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        topBar.getChildren().addAll(classSelect, addClassBtn, deleteClassBtn, spacer, recordScoreBtn,
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
        rollCol.setComparator((s1, s2) -> {
            if (s1 == null && s2 == null)
                return 0;
            if (s1 == null)
                return -1;
            if (s2 == null)
                return 1;
            try {
                return Integer.compare(Integer.parseInt(s1.trim()), Integer.parseInt(s2.trim()));
            } catch (NumberFormatException e) {
                return s1.compareToIgnoreCase(s2);
            }
        });

        TableColumn<Student, String> emailCol = new TableColumn<>("Email");
        emailCol.setCellValueFactory(
                cd -> new SimpleStringProperty(cd.getValue().getEmail() != null ? cd.getValue().getEmail() : ""));

        smTable.getColumns().addAll(idCol, nameCol, rollCol, emailCol);
        smTable.getSortOrder().add(rollCol);

        tableCard.getChildren().addAll(new Label("Class Students"), smTable);

        // Actions
        classSelect.valueProperty().addListener((obs, oldV, newV) -> {
            if (newV != null) {
                smTable.setItems(javafx.collections.FXCollections.observableArrayList(
                        studentMgmtService.getStudentsForClass(newV.getId())));
                smTable.sort();
                importBtn.setDisable(false);
                deleteClassBtn.setDisable(false);
                recordScoreBtn.setDisable(false);
            } else {
                smTable.getItems().clear();
                importBtn.setDisable(true);
                deleteClassBtn.setDisable(true);
                recordScoreBtn.setDisable(true);
            }
        });

        // Initialize state
        importBtn.setDisable(true);
        recordScoreBtn.setDisable(true);
        deleteClassBtn.setDisable(true);

        addClassBtn.setOnAction(e -> handleAddClass(classSelect));
        deleteClassBtn.setOnAction(e -> handleDeleteClass(classSelect));
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
                return studentMgmtService.addClass(nameInput.getText(), sectionInput.getText(),
                        viewManager.getCurrentUserId());
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

        javafx.scene.layout.GridPane inputGrid = new javafx.scene.layout.GridPane();
        inputGrid.setHgap(16);
        inputGrid.setVgap(8);

        TextField rollNoField = new TextField();
        rollNoField.setPromptText("e.g. 15");
        rollNoField.setPrefWidth(120);

        TextField nameField = new TextField();
        nameField.setPromptText("e.g. Alice Smith");
        nameField.setPrefWidth(220);

        TextField subjectField = new TextField();
        subjectField.setPromptText("e.g. Mathematics");
        subjectField.setPrefWidth(200);

        TextField deltaField = new TextField();
        deltaField.setPromptText("e.g. +10 or -5");
        deltaField.setPrefWidth(120);

        Label rollLabel = new Label("Roll No:");
        rollLabel.setStyle("-fx-text-fill: #a4b0be; -fx-font-weight: bold;");
        Label nameLabel = new Label("Student Name:");
        nameLabel.setStyle("-fx-text-fill: #a4b0be; -fx-font-weight: bold;");
        Label subjectLabel = new Label("Subject:");
        subjectLabel.setStyle("-fx-text-fill: #a4b0be; -fx-font-weight: bold;");
        Label deltaLabel = new Label("Score Delta:");
        deltaLabel.setStyle("-fx-text-fill: #a4b0be; -fx-font-weight: bold;");

        inputGrid.add(rollLabel, 0, 0);
        inputGrid.add(rollNoField, 0, 1);
        inputGrid.add(nameLabel, 1, 0);
        inputGrid.add(nameField, 1, 1);
        inputGrid.add(subjectLabel, 2, 0);
        inputGrid.add(subjectField, 2, 1);
        inputGrid.add(deltaLabel, 3, 0);
        inputGrid.add(deltaField, 3, 1);

        VBox resultsContainer = new VBox(12);
        resultsContainer.setPadding(new Insets(16));
        resultsContainer.setStyle(
                "-fx-background-color: #1a1a2e; -fx-background-radius: 8; -fx-border-color: #2a2a4a; -fx-border-radius: 8;");
        resultsContainer.setVisible(false);
        resultsContainer.setManaged(false);

        Button runBtn = new Button("Run Simulation");
        runBtn.getStyleClass().add("button-primary");
        runBtn.setOnAction(e -> {
            resultsContainer.getChildren().clear();
            resultsContainer.setVisible(true);
            resultsContainer.setManaged(true);
            try {
                String rollNo = rollNoField.getText().trim();
                String name = nameField.getText().trim();
                String subject = subjectField.getText().trim();
                double delta = Double.parseDouble(deltaField.getText().trim());

                if (rollNo.isEmpty() || name.isEmpty() || subject.isEmpty()) {
                    Label err = new Label("Error: Please specify Roll No, Name, and Subject.");
                    err.setStyle("-fx-text-fill: #ff4757; -fx-font-weight: bold;");
                    resultsContainer.getChildren().add(err);
                    return;
                }

                Student student = null;
                for (Student s : dataManager.getStudents()) {
                    if (s.getRollNumber() != null && s.getName() != null
                            && s.getRollNumber().equalsIgnoreCase(rollNo)
                            && s.getName().equalsIgnoreCase(name)) {
                        student = s;
                        break;
                    }
                }
                if (student == null) {
                    Label err = new Label("Student with Roll No '" + rollNo + "' and Name '" + name + "' not found.");
                    err.setStyle("-fx-text-fill: #fffa65; -fx-font-weight: bold;");
                    resultsContainer.getChildren().add(err);
                    return;
                }

                SimulationResult result = simulationService.simulateScoreChange(student, subject, delta);

                Label resTitle = new Label("Simulation Results for " + student.getName());
                resTitle.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #2ed573;");

                // Average Score HBox
                HBox avgBox = new HBox(12);
                avgBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                Label avgLabel = new Label("Average Score:");
                avgLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: white; -fx-pref-width: 120px;");
                Label beforeAvg = new Label(String.format("%.1f", result.getOriginalAverage()));
                beforeAvg.setStyle("-fx-text-fill: #a4b0be; -fx-font-size: 16px;");
                Label arrow1 = new Label("→");
                arrow1.setStyle("-fx-text-fill: white;");
                double avgDelta = result.getAverageDelta();
                Label afterAvg = new Label(String.format("%.1f (%+.1f)", result.getSimulatedAverage(), avgDelta));
                afterAvg.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: "
                        + (avgDelta >= 0 ? "#2ed573" : "#ff4757") + ";");
                avgBox.getChildren().addAll(avgLabel, beforeAvg, arrow1, afterAvg);

                // Risk Box
                HBox riskBox = new HBox(12);
                riskBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                Label riskLabelTitle = new Label("Risk Level:");
                riskLabelTitle.setStyle("-fx-font-weight: bold; -fx-text-fill: white; -fx-pref-width: 120px;");
                Label beforeRisk = new Label(result.getOriginalRisk().getLevel().getLabel());
                beforeRisk.setStyle("-fx-text-fill: #a4b0be; -fx-font-size: 16px;");
                Label arrow2 = new Label("→");
                arrow2.setStyle("-fx-text-fill: white;");
                Label afterRisk = new Label(result.getSimulatedRisk().getLevel().getLabel());
                String rColor = result.getSimulatedRisk().getLevel() == tracker.model.RiskScore.Level.HIGH ? "#ff4757"
                        : (result.getSimulatedRisk().getLevel() == tracker.model.RiskScore.Level.MODERATE ? "#ffa502"
                                : "#2ed573");
                afterRisk.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: " + rColor + ";");
                riskBox.getChildren().addAll(riskLabelTitle, beforeRisk, arrow2, afterRisk);

                var recs = result.getSimulatedRecommendations();
                if (recs == null || recs.isEmpty()) {
                    Label noRec = new Label("No specific recommendations generated.");
                    noRec.setStyle("-fx-text-fill: #a4b0be;");
                    resultsContainer.getChildren().addAll(resTitle, avgBox, riskBox, noRec);
                } else {
                    resultsContainer.getChildren().addAll(resTitle, avgBox, riskBox);

                    VBox severityCard = new VBox(6);
                    severityCard.setPadding(new Insets(12));
                    String severityColor = result.getSimulatedRisk().getLevel() == tracker.model.RiskScore.Level.HIGH
                            ? "#ff4757"
                            : (result.getSimulatedRisk().getLevel() == tracker.model.RiskScore.Level.MODERATE
                                    ? "#ffa502"
                                    : "#2ed573");
                    severityCard.setStyle("-fx-background-color: #2a1b24; -fx-background-radius: 8; -fx-border-color: "
                            + severityColor + "; -fx-border-radius: 8; -fx-border-width: 0 0 0 4;");

                    VBox subjectCard = new VBox(6);
                    subjectCard.setPadding(new Insets(12));
                    subjectCard.setStyle(
                            "-fx-background-color: #1b212c; -fx-background-radius: 8; -fx-border-color: #3742fa; -fx-border-radius: 8; -fx-border-width: 0 0 0 4;");

                    VBox trendCard = new VBox(6);
                    trendCard.setPadding(new Insets(12));
                    trendCard.setStyle(
                            "-fx-background-color: #211a2e; -fx-background-radius: 8; -fx-border-color: #9c88ff; -fx-border-radius: 8; -fx-border-width: 0 0 0 4;");

                    final int SEC_SEVERITY = 0, SEC_SUBJECT = 1, SEC_TREND = 2;
                    int currentSection = SEC_SEVERITY;

                    for (String rec : recs) {
                        if (rec == null || rec.trim().isEmpty())
                            continue;

                        if (rec.contains("--- Subject-Specific Recommendations")) {
                            currentSection = SEC_SUBJECT;
                            Label subTitle = new Label("📚 Subject-Specific Advice");
                            subTitle.setStyle("-fx-font-weight: bold; -fx-text-fill: #70a1ff; -fx-padding: 0 0 4 0;");
                            subjectCard.getChildren().add(subTitle);
                            continue;
                        } else if (rec.contains("--- Trend-Based Advice ---")) {
                            currentSection = SEC_TREND;
                            Label trTitle = new Label("📈 Trend-Based Advice");
                            trTitle.setStyle("-fx-font-weight: bold; -fx-text-fill: #a181ff; -fx-padding: 0 0 4 0;");
                            trendCard.getChildren().add(trTitle);
                            continue;
                        }

                        Label recLbl = new Label(rec);
                        recLbl.setWrapText(true);

                        // Mild styling for sub-bullets vs main headers
                        if (rec.startsWith("   ->") || rec.startsWith(" ->")) {
                            recLbl.setStyle("-fx-text-fill: #a4b0be; -fx-padding: 0 0 0 16;");
                        } else if (rec.startsWith("[")) {
                            recLbl.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 0 0 0;");
                        } else {
                            recLbl.setStyle("-fx-text-fill: #dfe4ea;");
                        }

                        if (currentSection == SEC_SEVERITY) {
                            severityCard.getChildren().add(recLbl);
                        } else if (currentSection == SEC_SUBJECT) {
                            subjectCard.getChildren().add(recLbl);
                        } else if (currentSection == SEC_TREND) {
                            trendCard.getChildren().add(recLbl);
                        }
                    }

                    if (!severityCard.getChildren().isEmpty())
                        resultsContainer.getChildren().add(severityCard);
                    if (!subjectCard.getChildren().isEmpty())
                        resultsContainer.getChildren().add(subjectCard);
                    if (!trendCard.getChildren().isEmpty())
                        resultsContainer.getChildren().add(trendCard);
                }

            } catch (NumberFormatException ex) {
                Label err = new Label("Error: Score delta must be a valid number (e.g., '+5' or '-10').");
                err.setStyle("-fx-text-fill: #ff4757; -fx-font-weight: bold;");
                resultsContainer.getChildren().add(err);
            } catch (Exception ex) {
                Label err = new Label("Error: " + ex.getMessage());
                err.setStyle("-fx-text-fill: #ff4757; -fx-font-weight: bold;");
                resultsContainer.getChildren().add(err);
            }
        });

        card.getChildren().addAll(
                new Label("Enter parameters below to simulate changes to a student's performance:"),
                inputGrid, runBtn, resultsContainer);

        panel.getChildren().addAll(title, card);
        return panel;
    }

    private VBox buildInterventionsPanel() {
        VBox panel = new VBox(16);
        panel.setPadding(new Insets(0));

        Label title = new Label("🩺 AI Intervention Suggestions");
        title.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: white;");

        // Filter students to only the logged-in teacher's classes
        java.util.Set<Integer> teacherClassIds = new java.util.HashSet<>();
        for (ClassRoom c : studentMgmtService.getClassesByTeacher(viewManager.getCurrentUserId())) {
            teacherClassIds.add(c.getId());
        }
        List<Student> students = new java.util.ArrayList<>();
        for (Student s : dataManager.getStudents()) {
            if (s.getClassId() > 0 && teacherClassIds.contains(s.getClassId())) {
                students.add(s);
            }
        }
        int highCount = 0, modCount = 0;
        List<Student> atRiskStudents = new java.util.ArrayList<>();
        List<RiskScore> atRiskScores = new java.util.ArrayList<>();

        for (Student s : students) {
            if (s.getSubjects().isEmpty())
                continue;
            RiskScore risk = riskPredictor.assessRisk(s);
            if (risk.getLevel() == RiskScore.Level.HIGH) {
                highCount++;
                atRiskStudents.add(s);
                atRiskScores.add(risk);
            } else if (risk.getLevel() == RiskScore.Level.MODERATE) {
                modCount++;
                atRiskStudents.add(s);
                atRiskScores.add(risk);
            }
        }

        // Summary bar
        HBox summaryBar = new HBox(
                24);
        summaryBar.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        summaryBar.setPadding(new Insets(12, 16, 12, 16));
        summaryBar.setStyle("-fx-background-color: #1a1a2e; -fx-background-radius: 10;");

        Label totalLabel = new Label(atRiskStudents.size()
                + " Students Need Attention");
        totalLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: white;");

        Label highBadge = new Label("🔴 " + highCount
                + " High Risk");
        highBadge.setStyle(
                "-fx-text-fill: #ff4757; -fx-font-weight: bold; -fx-padding: 4 12; -fx-background-color: rgba(255,71,87,0.15); -fx-background-radius: 20;");

        Label modBadge = new Label("🟠 " + modCount
                + " Moderate");
        modBadge.setStyle(
                "-fx-text-fill: #ffa502; -fx-font-weight: bold; -fx-padding: 4 12; -fx-background-color: rgba(255,165,2,0.15); -fx-background-radius: 20;");

        summaryBar.getChildren().addAll(totalLabel, highBadge, modBadge);

        // Student cards container
        VBox cardsContainer = new VBox(12);

        if (atRiskStudents.isEmpty()) {
            VBox emptyCard = new VBox(12);
            emptyCard.getStyleClass().add("card");
            emptyCard.setAlignment(javafx.geometry.Pos.CENTER);
            emptyCard.setPadding(new Insets(32));
            Label check = new Label("✅");
            check.setStyle("-fx-font-size: 48px;");
            Label msg = new Label("All students are performing well!");
            msg.setStyle("-fx-font-size: 16px; -fx-text-fill: #2ed573; -fx-font-weight: bold;");
            Label sub = new Label("No interventions are needed at this time.");
            sub.setStyle("-fx-text-fill: #a4b0be;");
            emptyCard.getChildren().addAll(check, msg, sub);
            cardsContainer.getChildren().add(emptyCard);
        } else {
            for (int i = 0; i < atRiskStudents.size(); i++) {
                Student s = atRiskStudents.get(i);
                RiskScore risk = atRiskScores.get(i);
                String suggestion = interventionEngine.suggestIntervention(risk, null);

                // Parse suggestion: "TYPE: description"
                String interventionType = suggestion;
                String interventionDesc = "";
                int colonIdx = suggestion.indexOf(':');
                if (colonIdx > 0) {
                    interventionType = suggestion.substring(0, colonIdx).trim();
                    interventionDesc = suggestion.substring(colonIdx + 1).trim();
                }

                // Card
                VBox studentCard = new VBox(10);
                studentCard.setPadding(new Insets(16));
                boolean isHigh = risk.getLevel() == RiskScore.Level.HIGH;
                String borderColor = isHigh ? "#ff4757" : "#ffa502";
                studentCard.setStyle("-fx-background-color: #16213e; -fx-background-radius: 10; "
                        + "-fx-border-color: " + borderColor + "; -fx-border-radius: 10; -fx-border-width: 0 0 0 4;");

                // Top row: Avatar + Name + Risk Badge
                HBox topRow = new HBox(12);
                topRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

                // Avatar circle
                String initials = "";
                String[] nameParts = s.getName().split("\\s+");
                if (nameParts.length >= 2) {
                    initials = ("" + nameParts[0].charAt(0) + nameParts[nameParts.length - 1].charAt(0)).toUpperCase();
                } else if (nameParts.length == 1 && !nameParts[0].isEmpty()) {
                    initials = ("" + nameParts[0].charAt(0)).toUpperCase();
                }
                Label avatar = new Label(initials);
                avatar.setMinSize(40, 40);
                avatar.setMaxSize(40, 40);
                avatar.setAlignment(javafx.geometry.Pos.CENTER);
                String avatarBg = isHigh ? "#ff4757" : "#ffa502";
                avatar.setStyle("-fx-background-color: " + avatarBg + "; -fx-background-radius: 20; "
                        + "-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px;");

                // Name and ID
                VBox nameBox = new VBox(2);
                Label nameLabel = new Label(s.getName());
                nameLabel.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: white;");
                Label rollLabel = new Label("Roll No: " + (s.getRollNumber() != null ? s.getRollNumber() : "N/A"));
                rollLabel.setStyle("-fx-text-fill: #a4b0be; -fx-font-size: 12px;");
                nameBox.getChildren().addAll(nameLabel, rollLabel);
                HBox.setHgrow(nameBox, javafx.scene.layout.Priority.ALWAYS);

                // Risk badge
                Label riskBadge = new Label(risk.getLevel().getLabel() + " Risk");
                String badgeBg = isHigh ? "rgba(255,71,87,0.2)" : "rgba(255,165,2,0.2)";
                String badgeFg = isHigh ? "#ff6b81" : "#ffc048";
                riskBadge.setStyle("-fx-padding: 4 14; -fx-background-color: " + badgeBg + "; "
                        + "-fx-background-radius: 20; -fx-text-fill: " + badgeFg
                        + "; -fx-font-weight: bold; -fx-font-size: 12px;");

                // Risk score numeric
                Label scoreLabel = new Label(String.format("Score: %.0f", risk.getNumericScore()));
                scoreLabel.setStyle("-fx-text-fill: #a4b0be; -fx-font-size: 12px; -fx-padding: 4 8;");

                topRow.getChildren().addAll(avatar, nameBox, riskBadge, scoreLabel);

                // Intervention type pill
                HBox interventionRow = new HBox(10);
                interventionRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                Label typePill = new Label("📋 " + interventionType);
                typePill.setStyle("-fx-padding: 4 14; -fx-background-color: rgba(55,66,250,0.2); "
                        + "-fx-background-radius: 20; -fx-text-fill: #70a1ff; -fx-font-weight: bold; -fx-font-size: 12px;");
                interventionRow.getChildren().add(typePill);

                // Description
                Label descLabel = new Label(interventionDesc);
                descLabel.setWrapText(true);
                descLabel.setStyle("-fx-text-fill: #dfe4ea; -fx-font-size: 13px; -fx-padding: 0 0 0 52;");

                studentCard.getChildren().addAll(topRow, interventionRow, descLabel);
                cardsContainer.getChildren().add(studentCard);
            }
        }

        javafx.scene.control.ScrollPane scrollPane = new javafx.scene.control.ScrollPane(
                cardsContainer);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        scrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        VBox.setVgrow(scrollPane, javafx.scene.layout.Priority.ALWAYS);

        panel.getChildren().addAll(title, summaryBar, scrollPane);
        VBox.setVgrow(panel, javafx.scene.layout.Priority.ALWAYS);
        return panel;
    }

    private VBox buildReevaluationsPanel() {
        VBox panel = new VBox(20);
        panel.setPadding(new Insets(0));

        Label title = new Label("\ud83d\udcdd Pending Re-evaluation Requests");
        title.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: white;");

        VBox contentBox = new VBox(12);

        int teacherUserId = viewManager.getCurrentUserId();
        List<ReevaluationRequest> pending = reevalWorkflow.getPendingRequestsForTeacher(teacherUserId);
        if (pending.isEmpty()) {
            VBox emptyCard = new VBox(12);
            emptyCard.getStyleClass().add("card");
            emptyCard.getChildren().add(new Label("No pending re-evaluation requests at this time."));
            contentBox.getChildren().add(emptyCard);
        } else {
            for (ReevaluationRequest req : pending) {
                VBox reqCard = new VBox(12);
                reqCard.getStyleClass().add("card");

                // Student name lookup
                String studentName = reevalWorkflow.getStudentNameById(req.getStudentId());
                String subjectName = req.getSubjectName() != null ? req.getSubjectName() : "N/A";

                Label info = new Label(String.format("Student: %s  \u2014  Subject: %s", studentName, subjectName));
                info.setStyle("-fx-font-weight: bold; -fx-font-size: 16px; -fx-text-fill: #70a1ff;");
                Label reason = new Label("Reason: " + req.getReason());
                reason.setWrapText(true);

                // Action area — dynamically populated
                VBox actionArea = new VBox(10);

                HBox buttonRow = new HBox(12);
                Button approveBtn = new Button("\u2713 Approve");
                approveBtn.getStyleClass().add("button-primary");
                Button rejectBtn = new Button("\u2717 Reject");
                rejectBtn.getStyleClass().add("button-danger");
                buttonRow.getChildren().addAll(approveBtn, rejectBtn);

                approveBtn.setOnAction(e -> {
                    actionArea.getChildren().clear();
                    // Show approve form
                    Label marksLabel = new Label("Updated Marks:");
                    marksLabel.setStyle("-fx-text-fill: #a4b0be; -fx-font-size: 13px;");
                    TextField marksField = new TextField();
                    marksField.setPromptText("Enter updated marks (e.g. 85.0)");

                    Label notesLabel = new Label("Reasoning:");
                    notesLabel.setStyle("-fx-text-fill: #a4b0be; -fx-font-size: 13px;");
                    TextArea notesArea = new TextArea();
                    notesArea.setPromptText("Explain why the marks were changed...");
                    notesArea.setPrefHeight(60);
                    notesArea.setWrapText(true);
                    notesArea.setStyle("-fx-control-inner-background: #16213e; -fx-text-fill: #e0e0e8;");

                    Label errorLabel = new Label();
                    errorLabel.setStyle("-fx-text-fill: #ff4757; -fx-font-weight: bold;");

                    Button confirmBtn = new Button("Confirm Approval");
                    confirmBtn.getStyleClass().add("button-primary");
                    confirmBtn.setOnAction(ev -> {
                        try {
                            double marks = Double.parseDouble(marksField.getText().trim());
                            String notes2 = notesArea.getText().trim();
                            if (notes2.isEmpty()) {
                                errorLabel.setText("Please provide your reasoning.");
                                return;
                            }
                            reevalWorkflow.approveRequest(req.getId(), teacherUserId, notes2, marks);
                            reqCard.setDisable(true);
                            info.setText(info.getText() + " \u2014 \u2705 APPROVED");
                        } catch (NumberFormatException ex) {
                            errorLabel.setText("Enter a valid number for marks.");
                        }
                    });

                    Button cancelBtn = new Button("Cancel");
                    cancelBtn.setOnAction(ev -> {
                        actionArea.getChildren().clear();
                        actionArea.getChildren().add(buttonRow);
                    });

                    HBox confirmRow = new HBox(12);
                    confirmRow.getChildren().addAll(confirmBtn, cancelBtn);

                    actionArea.getChildren().addAll(marksLabel, marksField, notesLabel, notesArea, errorLabel,
                            confirmRow);
                });

                rejectBtn.setOnAction(e -> {
                    actionArea.getChildren().clear();
                    // Show reject form
                    Label notesLabel = new Label("Reasoning for Rejection:");
                    notesLabel.setStyle("-fx-text-fill: #a4b0be; -fx-font-size: 13px;");
                    TextArea notesArea = new TextArea();
                    notesArea.setPromptText("Explain why the request is rejected...");
                    notesArea.setPrefHeight(60);
                    notesArea.setWrapText(true);
                    notesArea.setStyle("-fx-control-inner-background: #16213e; -fx-text-fill: #e0e0e8;");

                    Label errorLabel = new Label();
                    errorLabel.setStyle("-fx-text-fill: #ff4757; -fx-font-weight: bold;");

                    Button confirmBtn = new Button("Confirm Rejection");
                    confirmBtn.getStyleClass().add("button-danger");
                    confirmBtn.setOnAction(ev -> {
                        String notes2 = notesArea.getText().trim();
                        if (notes2.isEmpty()) {
                            errorLabel.setText("Please provide your reasoning.");
                            return;
                        }
                        reevalWorkflow.rejectRequest(req.getId(), teacherUserId, notes2);
                        reqCard.setDisable(true);
                        info.setText(info.getText() + " \u2014 \u274c REJECTED");
                    });

                    Button cancelBtn = new Button("Cancel");
                    cancelBtn.setOnAction(ev -> {
                        actionArea.getChildren().clear();
                        actionArea.getChildren().add(buttonRow);
                    });

                    HBox confirmRow = new HBox(12);
                    confirmRow.getChildren().addAll(confirmBtn, cancelBtn);

                    actionArea.getChildren().addAll(notesLabel, notesArea, errorLabel, confirmRow);
                });

                actionArea.getChildren().add(buttonRow);
                reqCard.getChildren().addAll(info, reason, actionArea);
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
