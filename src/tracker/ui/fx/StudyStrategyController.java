package tracker.ui.fx;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import tracker.data.dao.StudyStrategyDAO;
import tracker.model.UserRole;
import tracker.security.SessionManager;

import java.util.*;

/**
 * Study Strategy Advisor page controller.
 * Same questionnaire and StrategyEngine logic as Swing StudyStrategyPanel.
 * Isolated rule-based engine is unchanged.
 */
public class StudyStrategyController {

    private final MainController main;
    private final StudyStrategyDAO studyStrategyDAO = new StudyStrategyDAO();
    private final String studentId = SessionManager.getLinkedStudentId();

    private final List<ComboBox<String>> questionBoxes = new ArrayList<>();
    private VBox resultsPanel;
    private ScrollPane mainScroll;

    private final String[] QUESTIONS = {
            "1. How do you usually prepare for an exam?",
            "2. How often do you review your notes after a lecture?",
            "3. When studying a difficult concept, what is your first step?",
            "4. How do you manage your study time during a long session?",
            "5. What happens when you test yourself on the material?",
            "6. How do you organize information that needs to be memorized?",
            "7. How do you handle feeling overwhelmed by a large syllabus?"
    };

    private final String[][] OPTIONS = {
            { "I re-read my notes and textbook.", "I summarize the material from memory.",
                    "I spread my studying over several days.", "I try to explain the topics to someone else." },
            { "Rarely, usually only before exams.", "Within a few days.",
                    "I review them the same day and then periodically.", "I create flashcards out of them." },
            { "I read it again and again.", "I look for analogies or real-world examples.",
                    "I break it down into smaller parts.", "I find practice problems related to it." },
            { "I study for hours until I'm done.", "I take breaks whenever I feel tired.",
                    "I use a timer to take structured, frequent breaks (e.g., Pomodoro).",
                    "I switch between different subjects or topics." },
            { "I rarely test myself; I just read.", "I do okay, but I forget things quickly.",
                    "I identify what I got wrong and focus heavily on those parts.",
                    "I usually perform well on self-tests." },
            { "I just read the list over and over.", "I use acronyms, mnemonics, or rhymes.",
                    "I try to understand how concepts connect to each other.", "I draw mind maps or diagrams." },
            { "I panic and cram.", "I make a detailed schedule but struggle to stick to it.",
                    "I rank topics by priority and tackle the hardest ones first.",
                    "I tackle the easiest topics first to build momentum." }
    };

    public StudyStrategyController(MainController main) {
        this.main = main;
    }

    public Region buildPage() {
        VBox page = new VBox(16);
        page.setStyle(FxStyles.PAGE_BG);
        page.setPadding(new Insets(20, 24, 20, 24));

        Label title = new Label("Study Strategy Advisor");
        title.setStyle(FxStyles.title());

        VBox content = new VBox(16);

        // Questions card
        VBox questionsCard = buildCard("Discover Your Optimal Strategy");
        GridPane form = new GridPane();
        form.setHgap(12);
        form.setVgap(12);
        form.setPadding(new Insets(10));

        questionBoxes.clear();
        for (int i = 0; i < QUESTIONS.length; i++) {
            Label qLabel = new Label(QUESTIONS[i]);
            qLabel.setStyle(FxStyles.label());
            qLabel.setWrapText(true);

            ComboBox<String> combo = new ComboBox<>();
            combo.getItems().addAll(OPTIONS[i]);
            combo.getSelectionModel().selectFirst();
            combo.setStyle("-fx-font-family: 'Segoe UI'; -fx-font-size: 12;");
            combo.setMaxWidth(Double.MAX_VALUE);

            form.add(qLabel, 0, i * 2);
            form.add(combo, 0, i * 2 + 1);
            questionBoxes.add(combo);

            ColumnConstraints cc = new ColumnConstraints();
            cc.setHgrow(Priority.ALWAYS);
            if (form.getColumnConstraints().isEmpty()) {
                form.getColumnConstraints().add(cc);
            }
        }

        Button btnAnalyze = new Button("Analyze My Study Strategy");
        btnAnalyze.setStyle(FxStyles.coloredButton(FxStyles.C_BLUE));
        btnAnalyze.setOnAction(e -> analyzeStrategy());
        form.add(btnAnalyze, 0, QUESTIONS.length * 2);

        questionsCard.getChildren().add(form);

        // Results panel (hidden until analysis run)
        resultsPanel = new VBox(12);
        resultsPanel.setVisible(false);
        resultsPanel.setManaged(false);

        content.getChildren().addAll(questionsCard, resultsPanel);

        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background: #F1F5F9; -fx-background-color: #F1F5F9;");
        mainScroll = scroll;
        VBox.setVgrow(scroll, Priority.ALWAYS);

        page.getChildren().addAll(title, scroll);
        VBox.setVgrow(scroll, Priority.ALWAYS);
        return page;
    }

    private void analyzeStrategy() {
        List<Integer> answers = new ArrayList<>();
        StringBuilder answersJson = new StringBuilder("[");
        for (int i = 0; i < questionBoxes.size(); i++) {
            int sel = questionBoxes.get(i).getSelectionModel().getSelectedIndex();
            answers.add(sel);
            answersJson.append(sel);
            if (i < questionBoxes.size() - 1)
                answersJson.append(",");
        }
        answersJson.append("]");

        List<StudyRecommendation> recs = StrategyEngine.generateRecommendations(answers);

        // Save to DB if student
        if (studentId != null) {
            tracker.data.dao.StudentDAO studentDAO = new tracker.data.dao.StudentDAO();
            int dbId = studentDAO.findDbIdByStudentId(studentId);
            if (dbId != -1) {
                StringBuilder recList = new StringBuilder();
                for (StudyRecommendation r : recs)
                    recList.append(r.title).append("|");
                studyStrategyDAO.saveStrategy(dbId, answersJson.toString(), recList.toString());
            }
        }

        displayRecommendations(recs);
    }

    private VBox buildCard(String titleText) {
        VBox card = new VBox(10);
        card.setStyle(FxStyles.CARD_STYLE);
        Label t = new Label(titleText);
        t.setStyle(FxStyles.sectionTitle());
        card.getChildren().add(t);
        return card;
    }

    private void displayRecommendations(List<StudyRecommendation> recs) {
        resultsPanel.getChildren().clear();

        for (StudyRecommendation rec : recs) {
            VBox recCard = new VBox(8);
            recCard.setStyle(FxStyles.CARD_STYLE);

            Label recTitle = new Label(rec.title);
            recTitle.setStyle(FxStyles.sectionTitle());

            Label desc = new Label(rec.description);
            desc.setStyle(FxStyles.label());
            desc.setWrapText(true);

            Label how = new Label("How to apply: " + rec.howToApply);
            how.setStyle("-fx-font-family: 'Segoe UI'; -fx-font-size: 12; -fx-text-fill: #1E293B;");
            how.setWrapText(true);

            recCard.getChildren().addAll(recTitle, desc, how);
            resultsPanel.getChildren().add(recCard);
        }

        resultsPanel.setVisible(true);
        resultsPanel.setManaged(true);

        javafx.application.Platform.runLater(() -> {
            if (mainScroll != null)
                mainScroll.setVvalue(1.0);
        });
    }

    // === Copied exactly from StudyStrategyPanel — no logic changes ===

    private record StudyRecommendation(String title, String description, String howToApply) {
    }

    private static class StrategyEngine {
        static List<StudyRecommendation> generateRecommendations(List<Integer> answers) {
            Map<String, Integer> scores = new HashMap<>();
            String[] techniques = { "Active Recall", "Spaced Repetition", "Feynman Technique",
                    "Pomodoro Sprint", "Interleaving", "Dual Coding", "Elaborative Interrogation" };
            for (String t : techniques)
                scores.put(t, 0);

            if (answers.get(0) == 0)
                scores.merge("Active Recall", 3, Integer::sum);
            if (answers.get(0) == 1)
                scores.merge("Active Recall", -1, Integer::sum);
            if (answers.get(0) == 3)
                scores.merge("Feynman Technique", 2, Integer::sum);
            if (answers.get(1) == 0)
                scores.merge("Spaced Repetition", 3, Integer::sum);
            if (answers.get(1) == 3)
                scores.merge("Active Recall", 1, Integer::sum);
            if (answers.get(2) == 0)
                scores.merge("Elaborative Interrogation", 3, Integer::sum);
            if (answers.get(2) == 1)
                scores.merge("Feynman Technique", 2, Integer::sum);
            if (answers.get(3) == 0)
                scores.merge("Pomodoro Sprint", 4, Integer::sum);
            if (answers.get(3) == 3)
                scores.merge("Interleaving", 2, Integer::sum);
            if (answers.get(4) == 0)
                scores.merge("Active Recall", 2, Integer::sum);
            if (answers.get(4) == 2)
                scores.merge("Spaced Repetition", 2, Integer::sum);
            if (answers.get(5) == 0)
                scores.merge("Dual Coding", 3, Integer::sum);
            if (answers.get(5) == 3)
                scores.merge("Dual Coding", -1, Integer::sum);
            if (answers.get(6) == 0)
                scores.merge("Pomodoro Sprint", 2, Integer::sum);
            if (answers.get(6) == 1)
                scores.merge("Pomodoro Sprint", 1, Integer::sum);

            List<Map.Entry<String, Integer>> list = new ArrayList<>(scores.entrySet());
            list.sort((a, b) -> b.getValue().compareTo(a.getValue()));

            List<StudyRecommendation> results = new ArrayList<>();
            for (int i = 0; i < 3; i++)
                results.add(getDetailsFor(list.get(i).getKey()));
            return results;
        }

        private static StudyRecommendation getDetailsFor(String technique) {
            return switch (technique) {
                case "Active Recall" -> new StudyRecommendation("Active Recall",
                        "Testing yourself on the material instead of passively rereading it.",
                        "Close your book and write down everything you remember. Check gaps and repeat.");
                case "Spaced Repetition" -> new StudyRecommendation("Spaced Repetition",
                        "Reviewing material at increasing intervals over time.",
                        "Use flashcards (like Anki) and review just before you're about to forget.");
                case "Feynman Technique" -> new StudyRecommendation("The Feynman Technique",
                        "Explaining a concept simply to identify knowledge gaps.",
                        "Write an explanation as if teaching it to a 6th grader. Simplify the jargon.");
                case "Pomodoro Sprint" -> new StudyRecommendation("Pomodoro Sprints",
                        "Using a timer to break work into focused intervals with short breaks.",
                        "Study 25 minutes, break 5. Repeat 4x, then take a longer break.");
                case "Interleaving" -> new StudyRecommendation("Interleaving",
                        "Mixing different topics in a single study session.",
                        "Study Math 1hr, Physics 1hr, Chemistry 1hr instead of one topic for 3hrs.");
                case "Dual Coding" -> new StudyRecommendation("Dual Coding",
                        "Combining verbal material with visual materials.",
                        "Draw diagrams or infographics that represent your notes.");
                case "Elaborative Interrogation" -> new StudyRecommendation("Elaborative Interrogation",
                        "Asking 'why' questions to deeply understand facts.",
                        "When reading a fact, ask 'Why is this true?' and answer from prior knowledge.");
                default -> new StudyRecommendation("General Review", "Reviewing all materials.",
                        "Set aside time to review your notes.");
            };
        }
    }
}
