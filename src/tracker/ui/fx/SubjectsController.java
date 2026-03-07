package tracker.ui.fx;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import tracker.model.*;

import java.util.List;

/**
 * Subject Analysis page controller.
 * Unchanged logic from Swing version — same student selector + text areas.
 */
public class SubjectsController {

    private final MainController main;
    private ComboBox<String> cmbStudent;
    private TextArea txtSubjectDetails, txtRecommendations;
    private List<Student> cachedStudents;

    public SubjectsController(MainController main) {
        this.main = main;
    }

    public Region buildPage() {
        VBox page = new VBox(16);
        page.setStyle(FxStyles.PAGE_BG);
        page.setPadding(new Insets(20, 24, 20, 24));

        Label title = new Label("Subject Analysis");
        title.setStyle(FxStyles.title());

        // Selector card
        VBox selectorCard = buildCard("Select Student");
        HBox selectorRow = new HBox(12);
        selectorRow.setAlignment(Pos.CENTER_LEFT);

        cmbStudent = new ComboBox<>();
        cmbStudent.setStyle("-fx-font-family: 'Segoe UI'; -fx-font-size: 13;");
        cmbStudent.setPrefWidth(300);
        cmbStudent.setPrefHeight(36);

        Button btnShow = new Button("Show Details");
        btnShow.setStyle(FxStyles.primaryButton());
        btnShow.setOnAction(e -> handleShowDetails());

        selectorRow.getChildren().addAll(cmbStudent, btnShow);
        selectorCard.getChildren().add(selectorRow);

        // Detail cards (side by side)
        HBox centerRow = new HBox(16);
        VBox.setVgrow(centerRow, Priority.ALWAYS);

        VBox subCard = buildCard("Subject Analysis");
        txtSubjectDetails = new TextArea();
        txtSubjectDetails.setEditable(false);
        txtSubjectDetails.setWrapText(true);
        txtSubjectDetails.setStyle("-fx-font-family: 'Consolas'; -fx-font-size: 12;");
        VBox.setVgrow(txtSubjectDetails, Priority.ALWAYS);
        subCard.getChildren().add(txtSubjectDetails);
        HBox.setHgrow(subCard, Priority.ALWAYS);
        VBox.setVgrow(subCard, Priority.ALWAYS);

        VBox recCard = buildCard("AI Recommendations");
        txtRecommendations = new TextArea();
        txtRecommendations.setEditable(false);
        txtRecommendations.setWrapText(true);
        txtRecommendations.setStyle("-fx-font-family: 'Segoe UI'; -fx-font-size: 12;");
        VBox.setVgrow(txtRecommendations, Priority.ALWAYS);
        recCard.getChildren().add(txtRecommendations);
        HBox.setHgrow(recCard, Priority.ALWAYS);
        VBox.setVgrow(recCard, Priority.ALWAYS);

        centerRow.getChildren().addAll(subCard, recCard);

        page.getChildren().addAll(title, selectorCard, centerRow);
        VBox.setVgrow(centerRow, Priority.ALWAYS);
        return page;
    }

    void refresh() {
        if (cmbStudent == null)
            return;
        cmbStudent.getItems().clear();
        cachedStudents = main.dataManager.getStudents();
        if (cachedStudents.isEmpty()) {
            cmbStudent.getItems().add("-- No students yet --");
        } else {
            for (Student s : cachedStudents) {
                cmbStudent.getItems().add(s.getId() + " - " + s.getName());
            }
            cmbStudent.getSelectionModel().selectFirst();
        }
    }

    private void handleShowDetails() {
        if (cachedStudents == null || cachedStudents.isEmpty()) {
            txtSubjectDetails.setText("No students available. Add students first.");
            txtRecommendations.setText("");
            return;
        }

        int idx = cmbStudent.getSelectionModel().getSelectedIndex();
        if (idx < 0 || idx >= cachedStudents.size()) {
            txtSubjectDetails.setText("Select a valid student.");
            return;
        }

        Student student = cachedStudents.get(idx);
        RiskScore risk = main.riskPredictor.assessRisk(student);
        TrendDirection trend = main.trendAnalyzer.getTrend(student.getId());

        StringBuilder sb = new StringBuilder();
        sb.append("Student: ").append(student.getName()).append(" (").append(student.getId()).append(")\n");
        sb.append("Average: ").append(String.format("%.2f", student.getAverageScore())).append("\n");
        sb.append("Risk: ").append(risk.getLevel().getLabel())
                .append(" (").append(String.format("%.1f", risk.getNumericScore())).append(")\n");
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
            sb.append("\n--- Weak ---\n");
            for (Subject w : weak) {
                sb.append(String.format("  %-18s : %6.2f\n", w.getSubjectName(), w.getScore()));
            }
        }
        sb.append("\n").append(risk.getDetailedBreakdown());
        txtSubjectDetails.setText(sb.toString());
        txtSubjectDetails.positionCaret(0);

        List<String> recs = main.adaptivePlanner.generateRecommendations(student, risk, trend);
        StringBuilder sr = new StringBuilder();
        sr.append("AI Recommendations for ").append(student.getName()).append(":\n\n");
        for (String r : recs)
            sr.append(r).append("\n");
        txtRecommendations.setText(sr.toString());
        txtRecommendations.positionCaret(0);
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
