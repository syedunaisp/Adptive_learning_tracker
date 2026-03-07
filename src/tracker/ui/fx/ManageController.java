package tracker.ui.fx;

import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import tracker.model.*;

import java.util.List;
import java.util.Optional;

/**
 * Manage Students page (CRUD for Teacher/Admin).
 * Edit name, edit score, delete student.
 * Logic unchanged from Swing version.
 */
public class ManageController {

    private final MainController main;
    private TableView<ManageRow> table;

    public ManageController(MainController main) {
        this.main = main;
    }

    public Region buildPage() {
        VBox page = new VBox(16);
        page.setStyle(FxStyles.PAGE_BG);
        page.setPadding(new Insets(20, 24, 20, 24));

        Label title = new Label("Manage Students");
        title.setStyle(FxStyles.title());

        // Actions card
        VBox actionsCard = buildCard("Actions");
        HBox actions = new HBox(12);
        actions.setAlignment(Pos.CENTER_LEFT);

        Button btnEditName = new Button("Edit Name");
        btnEditName.setStyle(FxStyles.primaryButton());
        btnEditName.setOnAction(e -> handleEditName());

        Button btnEditScore = new Button("Edit Score");
        btnEditScore.setStyle(FxStyles.coloredButton(FxStyles.C_ORANGE));
        btnEditScore.setOnAction(e -> handleEditScore());

        Button btnDelete = new Button("Delete Student");
        btnDelete.setStyle(FxStyles.dangerButton());
        btnDelete.setOnAction(e -> handleDelete());

        Button btnRefresh = new Button("Refresh");
        btnRefresh.setStyle(FxStyles.coloredButton(FxStyles.C_GREEN));
        btnRefresh.setOnAction(e -> {
            main.dataManager.refreshCache();
            refresh();
        });

        actions.getChildren().addAll(btnEditName, btnEditScore, btnDelete, btnRefresh);
        actionsCard.getChildren().add(actions);

        // Table card
        VBox tableCard = buildCard("All Student Records");
        table = new TableView<>();
        table.setStyle("-fx-font-family: 'Segoe UI'; -fx-font-size: 13;");
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        addCol("ID", "id");
        addCol("Name", "name");
        addCol("Subjects", "subjects");
        addCol("Average", "average");
        addCol("Risk Level", "riskLevel");
        addCol("Risk Score", "riskScore");
        addCol("Trend", "trend");

        VBox.setVgrow(table, Priority.ALWAYS);
        tableCard.getChildren().add(table);
        VBox.setVgrow(tableCard, Priority.ALWAYS);

        page.getChildren().addAll(title, actionsCard, tableCard);
        VBox.setVgrow(tableCard, Priority.ALWAYS);
        return page;
    }

    void refresh() {
        if (table == null)
            return;
        table.getItems().clear();
        for (Student s : main.dataManager.getStudents()) {
            RiskScore risk = s.getSubjects().isEmpty() ? null : main.riskPredictor.assessRisk(s);
            TrendDirection trend = main.trendAnalyzer.getTrend(s.getId());
            table.getItems().add(new ManageRow(
                    s.getId(), s.getName(),
                    String.valueOf(s.getSubjects().size()),
                    String.format("%.2f", s.getAverageScore()),
                    risk != null ? risk.getLevel().getLabel() : "N/A",
                    risk != null ? String.format("%.1f", risk.getNumericScore()) : "N/A",
                    trend.getLabel()));
        }
    }

    private void handleEditName() {
        ManageRow row = table.getSelectionModel().getSelectedItem();
        if (row == null) {
            AlertDialog.showError("No Selection", "Select a student row to edit.");
            return;
        }

        Optional<String> result = AlertDialog.showInput(
                "Edit Student Name",
                "Enter new name for: " + row.id,
                row.name);

        result.ifPresent(newName -> {
            newName = newName.trim();
            if (newName.isEmpty())
                return;
            if (!newName.matches("[a-zA-Z\\s]+")) {
                AlertDialog.showError("Invalid Name", "Name must contain only letters and spaces.");
                return;
            }
            boolean ok = main.dataManager.getStudentDAO().updateName(row.id, newName);
            if (ok) {
                main.dataManager.refreshCache();
                refresh();
                AlertDialog.showInfo("Name Updated", "Student '" + row.id + "' renamed to '" + newName + "'.");
            } else {
                AlertDialog.showError("Update Failed", "Failed to update student name.");
            }
        });
    }

    private void handleEditScore() {
        ManageRow row = table.getSelectionModel().getSelectedItem();
        if (row == null) {
            AlertDialog.showError("No Selection", "Select a student row first.");
            return;
        }

        Student student = main.dataManager.findStudentById(row.id);
        if (student == null || student.getSubjects().isEmpty()) {
            AlertDialog.showError("No Subjects", "This student has no subjects to edit.");
            return;
        }

        List<String> choices = new java.util.ArrayList<>();
        for (Subject sub : student.getSubjects()) {
            choices.add(sub.getSubjectName() + " (" + String.format("%.1f", sub.getScore()) + ")");
        }

        Optional<String> subResult = AlertDialog.showChoice(
                "Edit Score",
                "Select subject for: " + student.getName(),
                choices);
        if (subResult.isEmpty())
            return;

        String chosen = subResult.get();
        int subIdx = choices.indexOf(chosen);
        if (subIdx < 0)
            return;
        Subject target = student.getSubjects().get(subIdx);

        Optional<String> scoreResult = AlertDialog.showInput(
                "Edit Score",
                "New score for " + target.getSubjectName() + " (0–100):",
                String.format("%.1f", target.getScore()));
        if (scoreResult.isEmpty())
            return;

        double newScore;
        try {
            newScore = Double.parseDouble(scoreResult.get().trim());
        } catch (NumberFormatException e) {
            AlertDialog.showError("Invalid Input", "Score must be a valid number.");
            return;
        }
        if (newScore < 0 || newScore > 100) {
            AlertDialog.showError("Out of Range", "Score must be between 0 and 100.");
            return;
        }

        int studentDbId = main.dataManager.getStudentDAO().findDbIdByStudentId(row.id);
        int subjectDbId = main.dataManager.getSubjectDAO().findIdByName(target.getSubjectName());
        if (studentDbId < 0 || subjectDbId < 0) {
            AlertDialog.showError("Database Error", "Could not resolve DB IDs.");
            return;
        }

        boolean ok = main.dataManager.getScoreDAO().updateScore(studentDbId, subjectDbId, newScore);
        if (ok) {
            main.dataManager.refreshCache();
            refresh();
            AlertDialog.showInfo("Score Updated",
                    "Score updated: " + target.getSubjectName() + " = " + String.format("%.1f", newScore));
        } else {
            AlertDialog.showError("Update Failed", "Failed to update score.");
        }
    }

    private void handleDelete() {
        ManageRow row = table.getSelectionModel().getSelectedItem();
        if (row == null) {
            AlertDialog.showError("No Selection", "Select a student row to delete.");
            return;
        }

        boolean confirmed = AlertDialog.showConfirm(
                "Delete Student",
                "Permanently delete '" + row.name + "' (" + row.id + ") and all their scores?");

        if (confirmed) {
            boolean ok = main.dataManager.getStudentDAO().deleteByStudentId(row.id);
            if (ok) {
                main.dataManager.refreshCache();
                refresh();
                AlertDialog.showInfo("Deleted", "Student '" + row.name + "' has been removed.");
            } else {
                AlertDialog.showError("Delete Failed", "Failed to delete the student.");
            }
        }
    }

    private void addCol(String header, String prop) {
        TableColumn<ManageRow, String> col = new TableColumn<>(header);
        col.setCellValueFactory(data -> new SimpleStringProperty(
                switch (prop) {
                    case "id" -> data.getValue().id;
                    case "name" -> data.getValue().name;
                    case "subjects" -> data.getValue().subjects;
                    case "average" -> data.getValue().average;
                    case "riskLevel" -> data.getValue().riskLevel;
                    case "riskScore" -> data.getValue().riskScore;
                    case "trend" -> data.getValue().trend;
                    default -> "";
                }));
        table.getColumns().add(col);
    }

    private VBox buildCard(String titleText) {
        VBox card = new VBox(10);
        card.setStyle(FxStyles.CARD_STYLE);
        Label t = new Label(titleText);
        t.setStyle(FxStyles.sectionTitle());
        card.getChildren().add(t);
        return card;
    }

    public static class ManageRow {
        public final String id, name, subjects, average, riskLevel, riskScore, trend;

        public ManageRow(String id, String name, String subs, String avg, String rl, String rs, String t) {
            this.id = id;
            this.name = name;
            this.subjects = subs;
            this.average = avg;
            this.riskLevel = rl;
            this.riskScore = rs;
            this.trend = t;
        }
    }
}
