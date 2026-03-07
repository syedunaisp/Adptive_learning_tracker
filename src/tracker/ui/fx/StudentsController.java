package tracker.ui.fx;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.beans.property.SimpleStringProperty;
import tracker.model.*;
import tracker.service.ai.TrendAnalyzer;

import java.io.IOException;
import java.util.*;

/**
 * Student Management page controller.
 * Add student/subject, view student table.
 * All logic delegated to DataManager, RiskPredictor, TrendAnalyzer — unchanged.
 */
public class StudentsController {

    private final MainController main;

    private TextField txtStudentId, txtStudentName, txtSubject, txtScore;
    private TableView<StudentRow> table;
    private ObservableStudentModel tableData;

    public StudentsController(MainController main) {
        this.main = main;
    }

    public Region buildPage() {
        VBox page = new VBox(16);
        page.setStyle(FxStyles.PAGE_BG);
        page.setPadding(new Insets(20, 24, 20, 24));

        Label title = new Label(main.currentRole.canEditData() ? "Student Management" : "Student Records");
        title.setStyle(FxStyles.title());

        // Input card
        VBox inputCard = buildCard(
                main.currentRole.canEditData() ? "Add Student / Subject" : "Student Data (Read Only)");
        GridPane form = new GridPane();
        form.setHgap(12);
        form.setVgap(10);

        txtStudentId = styledField("Unique alphanumeric ID");
        txtStudentName = styledField("Letters and spaces only");
        txtSubject = styledField("e.g. Math, Physics");
        txtScore = styledField("0 to 100");

        boolean editable = main.currentRole.canEditData();
        txtStudentId.setEditable(editable);
        txtStudentName.setEditable(editable);
        txtSubject.setEditable(editable);
        txtScore.setEditable(editable);

        form.add(new Label("Student ID:") {
            {
                setStyle(FxStyles.label());
            }
        }, 0, 0);
        form.add(txtStudentId, 1, 0);
        form.add(new Label("Name:") {
            {
                setStyle(FxStyles.label());
            }
        }, 2, 0);
        form.add(txtStudentName, 3, 0);
        form.add(new Label("Subject:") {
            {
                setStyle(FxStyles.label());
            }
        }, 0, 1);
        form.add(txtSubject, 1, 1);
        form.add(new Label("Score:") {
            {
                setStyle(FxStyles.label());
            }
        }, 2, 1);
        form.add(txtScore, 3, 1);

        ColumnConstraints labelCol = new ColumnConstraints(80);
        ColumnConstraints fieldCol = new ColumnConstraints(200, 200, Double.MAX_VALUE);
        fieldCol.setHgrow(Priority.ALWAYS);
        for (int i = 0; i < 4; i++) {
            form.getColumnConstraints().add(i % 2 == 0 ? labelCol : fieldCol);
        }

        if (editable) {
            Button btnAdd = new Button("Add / Submit");
            btnAdd.setStyle(FxStyles.primaryButton());
            btnAdd.setOnAction(e -> handleAdd());
            GridPane.setColumnSpan(btnAdd, 4);
            GridPane.setHalignment(btnAdd, javafx.geometry.HPos.CENTER);
            form.add(btnAdd, 0, 2);
        }

        inputCard.getChildren().add(form);

        // Table card
        VBox tableCard = buildCard("Student Records");
        table = new TableView<>();
        table.setStyle("-fx-font-family: 'Segoe UI'; -fx-font-size: 13;");
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        addCol("ID", "id");
        addCol("Name", "name");
        addCol("Average", "average");
        addCol("Risk Level", "riskLevel");
        addCol("Risk Score", "riskScore");
        addCol("Trend", "trend");

        tableData = new ObservableStudentModel();
        VBox.setVgrow(table, Priority.ALWAYS);
        tableCard.getChildren().add(table);
        VBox.setVgrow(tableCard, Priority.ALWAYS);

        page.getChildren().addAll(title, inputCard, tableCard);
        VBox.setVgrow(tableCard, Priority.ALWAYS);
        return page;
    }

    private void addCol(String header, String prop) {
        TableColumn<StudentRow, String> col = new TableColumn<>(header);
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
        table.getColumns().add(col);
    }

    void refresh() {
        if (table == null)
            return;
        table.getItems().clear();
        List<Student> list = main.dataManager.getStudents();
        for (Student s : list) {
            if (s.getSubjects().isEmpty())
                continue;
            RiskScore risk = main.riskPredictor.assessRisk(s);
            TrendDirection trend = main.trendAnalyzer.getTrend(s.getId());
            table.getItems().add(new StudentRow(
                    s.getId(), s.getName(),
                    String.format("%.2f", s.getAverageScore()),
                    risk.getLevel().getLabel(),
                    String.format("%.1f", risk.getNumericScore()),
                    trend.getLabel()));
        }
    }

    private void handleAdd() {
        String id = txtStudentId.getText().trim();
        String name = txtStudentName.getText().trim();
        String subject = txtSubject.getText().trim();
        String scoreStr = txtScore.getText().trim();

        if (id.isEmpty() || name.isEmpty() || subject.isEmpty() || scoreStr.isEmpty()) {
            main.showError("All fields are required.");
            return;
        }
        if (!name.matches("[a-zA-Z\\s]+")) {
            main.showError("Name must contain only letters and spaces.");
            return;
        }
        double score;
        try {
            score = Double.parseDouble(scoreStr);
        } catch (NumberFormatException e) {
            main.showError("Score must be a valid number.");
            return;
        }
        if (score < 0 || score > 100) {
            main.showError("Score must be between 0 and 100.");
            return;
        }

        Student existing = main.dataManager.findStudentById(id);
        boolean isNew = (existing == null);

        if (!isNew && !existing.getName().equalsIgnoreCase(name)) {
            main.showError("ID '" + id + "' exists with name '" + existing.getName() + "'.");
            return;
        }

        main.dataManager.addSubjectScore(id, name, subject, score);
        main.dataManager.refreshCache();
        Student student = main.dataManager.findStudentById(id);
        if (student == null) {
            main.showError("Failed to save student data.");
            return;
        }

        main.trendAnalyzer.recordAverage(student.getId(), student.getAverageScore());
        RiskScore risk = main.riskPredictor.assessRisk(student);
        TrendDirection trend = main.trendAnalyzer.getTrend(student.getId());
        List<String> recs = main.adaptivePlanner.generateRecommendations(student, risk, trend);

        try {
            main.fileManager.saveEnhancedReport(student, risk, trend, recs);
        } catch (IOException ex) {
            /* optional */ }

        txtSubject.clear();
        txtScore.clear();
        refresh();
        main.showInfo(isNew
                ? "Student '" + name + "' added.\nRisk: " + risk.getSummary()
                : "Subject added to '" + student.getName() + "'.\nRisk: " + risk.getSummary());
    }

    // Helpers
    private TextField styledField(String prompt) {
        TextField tf = new TextField();
        tf.setPromptText(prompt);
        tf.setStyle(FxStyles.textField());
        tf.setPrefHeight(36);
        return tf;
    }

    private VBox buildCard(String titleText) {
        VBox card = new VBox(10);
        card.setStyle(FxStyles.CARD_STYLE);
        Label t = new Label(titleText);
        t.setStyle(FxStyles.sectionTitle());
        card.getChildren().add(t);
        return card;
    }

    // Row model for the TableView
    public static class StudentRow {
        public final String id, name, average, riskLevel, riskScore, trend;

        public StudentRow(String id, String name, String avg, String rl, String rs, String t) {
            this.id = id;
            this.name = name;
            this.average = avg;
            this.riskLevel = rl;
            this.riskScore = rs;
            this.trend = t;
        }
    }

    private static class ObservableStudentModel {
    }
}
